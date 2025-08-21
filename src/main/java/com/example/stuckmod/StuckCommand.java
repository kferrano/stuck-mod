package com.example.stuckmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StuckCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NBT_KEY_LAST_USED = "stuck_last_used"; // Cooldown timestamp

    public static int execute(net.minecraft.commands.CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(new TranslatableComponent("stuck.error.player_only"));
            return 0;
        }

        // Permission
        if (!source.hasPermission(Config.minPermissionLevel)) {
            source.sendFailure(new TranslatableComponent("stuck.error.perm"));
            return 0;
        }

        ServerLevel level = player.getLevel();
        CompoundTag tag = player.getPersistentData();

        // Restrictions
        if (Config.survivalOnly && (player.isCreative() || player.isSpectator())) {
            source.sendFailure(new TranslatableComponent("stuck.error.gamemode"));
            return 0;
        }
        String dim = level.dimension().location().toString();
        boolean isNether = dim.contains("the_nether");
        boolean isEnd = dim.contains("the_end");
        if ((isNether && !Config.enableInNether) || (isEnd && !Config.enableInEnd)) {
            source.sendFailure(new TranslatableComponent("stuck.error.dimension"));
            return 0;
        }

        // Usage mode
        if (Config.usageMode == Config.UsageMode.COOLDOWN) {
            long now = level.getGameTime();
            long last = tag.getLong(NBT_KEY_LAST_USED);
            int cooldownTicks = Config.cooldownSeconds * 20;
            long remaining = cooldownTicks - (now - last);
            if (remaining > 0) {
                source.sendFailure(new TranslatableComponent("stuck.error.cooldown", (remaining / 20)));
                return 0;
            }
        } else { // ONCE_PER_LIFE
            boolean usedThisLife = tag.getBoolean(StuckEvents.NBT_USED_LIFE);
            if (usedThisLife) {
                source.sendFailure(new TranslatableComponent("stuck.error.mode_once"));
                return 0;
            }
        }

        // Safety check
        if (!isStuck(player)) {
            source.sendFailure(new TranslatableComponent("stuck.error.not_stuck"));
            LOGGER.warn("[StuckMod] Player {} used /stuck but was not stuck (Pos: {}, Dim: {})",
                    player.getGameProfile().getName(),
                    player.blockPosition(),
                    player.getLevel().dimension().location());
            return 0;
        }

        // Find target
        int maxUp = Math.max(1, Config.maxEscapeHeight);
        BlockPos dest = findFirstExitNearby(level, player.blockPosition(), maxUp, Config.wallSearchRadius, Config.minWallHeight);
        if (dest == null) {
            source.sendFailure(new TranslatableComponent("stuck.error.no_safe"));
            return 0;
        }

        // Teleport
        player.teleportTo(level, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, player.getYRot(), player.getXRot());

        // Save usage state
        if (Config.usageMode == Config.UsageMode.COOLDOWN) {
            tag.putLong(NBT_KEY_LAST_USED, level.getGameTime());
        } else {
            tag.putBoolean(StuckEvents.NBT_USED_LIFE, true);
        }

        source.sendSuccess(new TranslatableComponent("stuck.success"), true);
        LOGGER.info("[StuckMod] Player {} was freed with /stuck at {} in dimension {}",
                player.getGameProfile().getName(),
                dest,
                level.dimension().location());
        return 1;
    }

    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return true;
        if (s.getCollisionShape(level, pos).isEmpty()) return true;
        if (s.getMaterial().isReplaceable()) return true;
        return false;
    }

    private static boolean isStuck(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        BlockPos pos = player.blockPosition();

        if (Config.allowIfInWall && player.isInWall()) return true;

        int closedSides = countClosedSides(level, pos, Config.minWallHeight, Config.wallSearchRadius);
        if (Config.debugLogs) {
            LOGGER.info("[StuckMod] isStuck: closedSides={} (minWallHeight={}, radius={})", closedSides, Config.minWallHeight, Config.wallSearchRadius);
        }

        // Can step up?
        if (canStepUpSomewhere(level, pos)) {
            if (Config.debugLogs) LOGGER.info("[StuckMod] isStuck: 1-block step possible -> NOT STUCK");
            return false;
        }

        if (closedSides >= 3) return true; // almost fully enclosed
        if (closedSides == 2) {
            boolean north = isColumnSolid(level, pos.north(), Config.minWallHeight);
            boolean south = isColumnSolid(level, pos.south(), Config.minWallHeight);
            boolean east  = isColumnSolid(level, pos.east(), Config.minWallHeight);
            boolean west  = isColumnSolid(level, pos.west(), Config.minWallHeight);

            if ((north && east) || (north && west) || (south && east) || (south && west)) {
                if (Config.debugLogs) LOGGER.info("[StuckMod] isStuck: L-shaped hole detected -> STUCK");
                return true;
            }
        }

        return false;
    }

    private static int countClosedSides(ServerLevel level, BlockPos pos, int minHeight, int radius) {
        int closed = 0;
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : dirs) {
            boolean sideBlocked = true;
            for (int dist = 1; dist <= radius; dist++) {
                BlockPos base = pos.relative(dir, dist);
                if (!isColumnSolid(level, base, minHeight)) {
                    sideBlocked = false;
                    break;
                }
            }
            if (sideBlocked) closed++;
        }
        return closed;
    }

    private static boolean isColumnSolid(ServerLevel level, BlockPos base, int height) {
        for (int y = 0; y < Math.max(1, height); y++) {
            if (!isSolid(level, base.above(y))) return false;
        }
        return true;
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean canStandOn(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean canStepUpSomewhere(ServerLevel level, BlockPos pos) {
        int radius = 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos feet = pos.offset(dx, 1, dz);
                BlockPos head = feet.above();
                BlockPos below = feet.below();
                if (isPassable(level, feet) && isPassable(level, head) && canStandOn(level, below)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockPos findFirstExitNearby(ServerLevel level, BlockPos from, int maxUp, int horizontalRadius, int minWallHeight) {
        BlockPos bestInside = null;
        for (int dy = 1; dy <= maxUp; dy++) {
            int r = Math.max(0, horizontalRadius);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos feet = from.offset(dx, dy, dz);
                    BlockPos head = feet.above();
                    BlockPos below = feet.below();

                    if (!isPassable(level, feet) || !isPassable(level, head)) continue;
                    if (!canStandOn(level, below)) continue;

                    boolean stillEnclosed = (countClosedSides(level, feet, minWallHeight, horizontalRadius) >= 3);
                    if (!stillEnclosed) return feet;
                    else if (bestInside == null) bestInside = feet;
                }
            }
        }
        return bestInside;
    }
}
