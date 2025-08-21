package com.example.stuckmod;


import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;


@Mod(Stuckmod.MODID)
public class Stuckmod {
    public static final String MODID = "stuckmod"; // muss zu gradle.properties mod_id passen


    public Stuckmod() {
// Registriert COMMON-Config → erzeugt/liest config/stuckmod-common.toml
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}