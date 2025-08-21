package com.example.stuckmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stuckmod.MODID)
public class StuckEvents {
    public static final String NBT_USED_LIFE = "stuck_used_this_life";

    // Nach dem Tod: Flag entfernen → im neuen Leben wieder nutzbar
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag tag = event.getEntity().getPersistentData();
            tag.remove(NBT_USED_LIFE);
        }
    }
}