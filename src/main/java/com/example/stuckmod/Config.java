package com.example.stuckmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Stuckmod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {


    public enum UsageMode { COOLDOWN, ONCE_PER_LIFE }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<UsageMode> USAGE_MODE_SPEC = BUILDER
            .comment("Usage mode for /stuck: COOLDOWN or ONCE_PER_LIFE")
            .defineEnum("stuck.usageMode", UsageMode.COOLDOWN);

    private static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS_SPEC = BUILDER
            .comment("Cooldown in seconds between uses (only if usageMode=COOLDOWN)")
            .defineInRange("stuck.cooldownSeconds", 300, 0, 86400);

    private static final ForgeConfigSpec.IntValue MAX_ESCAPE_HEIGHT_SPEC = BUILDER
            .comment("Maximum teleport height upwards (in blocks)")
            .defineInRange("stuck.maxEscapeHeight", 5, 1, 32);

    private static final ForgeConfigSpec.BooleanValue ALLOW_IF_IN_WALL_SPEC = BUILDER
            .comment("Allow rescue if Vanilla isInWall() is true")
            .define("stuck.allowIfInWall", true);

    private static final ForgeConfigSpec.IntValue MIN_WALL_HEIGHT_SPEC = BUILDER
            .comment("Minimum wall height to count as enclosed (2 means higher than jump height)")
            .defineInRange("stuck.minWallHeight", 2, 1, 8);

    private static final ForgeConfigSpec.IntValue WALL_SEARCH_RADIUS_SPEC = BUILDER
            .comment("How far to check in N/S/E/W for enclosing walls")
            .defineInRange("stuck.wallSearchRadius", 2, 1, 6);

    private static final ForgeConfigSpec.BooleanValue SURVIVAL_ONLY_SPEC = BUILDER
            .comment("Allow /stuck only in Survival/Adventure")
            .define("stuck.survivalOnly", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_IN_NETHER_SPEC = BUILDER
            .comment("Allow /stuck in the Nether")
            .define("stuck.enableInNether", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_IN_END_SPEC = BUILDER
            .comment("Allow /stuck in the End")
            .define("stuck.enableInEnd", true);

    private static final ForgeConfigSpec.IntValue MIN_PERMISSION_LEVEL_SPEC = BUILDER
            .comment("Minimum permission level required for /stuck (0..4)")
            .defineInRange("stuck.minPermissionLevel", 0, 0, 4);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING_SPEC = BUILDER
            .comment("Enable verbose debug logging for /stuck checks (default: false)")
            .define("stuck.debugLogging", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cache
    public static UsageMode usageMode;
    public static int cooldownSeconds;
    public static int maxEscapeHeight;
    public static boolean allowIfInWall;
    public static int minWallHeight;
    public static int wallSearchRadius;
    public static boolean survivalOnly;
    public static boolean enableInNether;
    public static boolean enableInEnd;
    public static int minPermissionLevel;
    public static boolean debugLogs;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) { refresh(); }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) { refresh(); }

    private static void refresh() {
        debugLogs = DEBUG_LOGGING_SPEC.get();
        usageMode = USAGE_MODE_SPEC.get();
        cooldownSeconds = Math.max(0, COOLDOWN_SECONDS_SPEC.get());
        maxEscapeHeight = Math.max(1, MAX_ESCAPE_HEIGHT_SPEC.get());
        allowIfInWall = ALLOW_IF_IN_WALL_SPEC.get();
        minWallHeight = Math.max(1, MIN_WALL_HEIGHT_SPEC.get());
        wallSearchRadius = Math.max(1, WALL_SEARCH_RADIUS_SPEC.get());
        survivalOnly = SURVIVAL_ONLY_SPEC.get();
        enableInNether = ENABLE_IN_NETHER_SPEC.get();
        enableInEnd = ENABLE_IN_END_SPEC.get();
        minPermissionLevel = Math.max(0, Math.min(4, MIN_PERMISSION_LEVEL_SPEC.get()));
    }
}