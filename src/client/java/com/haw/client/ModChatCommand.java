package com.haw.client;

import com.haw.client.object.CommandSuggestionRequest;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class ModChatCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess ignoredRegistryAccess) {
        dispatcher.register(ClientCommandManager.literal("hawc")
                .then(ClientCommandManager.literal("list").executes(ModChatCommand::test)));
    }

    public static int test(CommandContext<FabricClientCommandSource> context) {
        ModDataManagement.commandSuggestionSchedule.add(new CommandSuggestionRequest("warp look", true));
        ModDataManagement.commandSuggestionSchedule.add(new CommandSuggestionRequest("home look", true));
        context.getSource().sendFeedback(Text.literal("Hello World"));
        return 1;
    }
}
