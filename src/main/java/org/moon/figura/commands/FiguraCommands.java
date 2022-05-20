package org.moon.figura.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.moon.figura.FiguraMod;
import org.moon.figura.backend.NetworkManager;
import org.moon.figura.lua.docs.FiguraDocsManager;

public class FiguraCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        //root
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(FiguraMod.MOD_ID);

        //docs
        root.then(FiguraDocsManager.get());

        //links
        root.then(FiguraLinkCommand.get());

        //run
        root.then(FiguraRunCommand.get());

        //load
        root.then(FiguraLoadCommand.get());

        //force backend auth
        if (FiguraMod.DEBUG_MODE)
            root.then(NetworkManager.getCommand());

        //register
        event.getDispatcher().register(root);
    }
}
