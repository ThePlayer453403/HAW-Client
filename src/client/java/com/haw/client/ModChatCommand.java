package com.haw.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class ModChatCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess ignoredRegistryAccess) {
        dispatcher.register(ClientCommandManager.literal("hawc")
                .then(ClientCommandManager.literal("waypoint")
                        .then(ClientCommandManager.literal("files").executes(ModChatCommand::serverWaypointFolder))
                        .then(ClientCommandManager.literal("list"))));
    }

    public static int serverWaypointFolder(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal(ModMapWaypointManagement.getCurrentWaypointFile()));
        return 1;
    }
}
