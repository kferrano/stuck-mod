package com.example.stuckmod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stuckmod.MODID)
public class StuckCommandRegistrar {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("stuck")
                .requires(src -> src.hasPermission(Config.minPermissionLevel))
                .executes(ctx -> StuckCommand.execute(ctx.getSource())));
    }
}