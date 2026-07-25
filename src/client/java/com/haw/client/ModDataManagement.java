package com.haw.client;

import com.haw.client.object.CommandSuggestionRequest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModDataManagement {
    /*
    目前从服务器获取数据的方法由两种
    A: 向服务器发送RequestCommandCompletionsC2SPacket，使用mixin获取结果
    B: 向服务器发送指令，通过Event获取结果
     */

    // 待发送的命令提示请求，需要以「/」开头
    public static List<CommandSuggestionRequest> commandSuggestionSchedule = new ArrayList<>();
    public static CommandSuggestionRequest currentSuggestionRequest;

    public static void sendCommandSuggestionsRequest() {
        MinecraftClient client = MinecraftClient.getInstance();

        // 如果请求计划为空/打开了聊天栏，不进行请求
        if (commandSuggestionSchedule.isEmpty() || client.currentScreen instanceof ChatScreen || currentSuggestionRequest != null) {return;}

        if (client.player != null && client.getNetworkHandler() != null) {
            int requestID = UUID.randomUUID().hashCode();
            currentSuggestionRequest = commandSuggestionSchedule.removeFirst();
            RequestCommandCompletionsC2SPacket requestPacket = new RequestCommandCompletionsC2SPacket(requestID, currentSuggestionRequest.chatCommand);
            client.getNetworkHandler().sendPacket(requestPacket);
        }

    }

    public static void receiveCommandSuggestions (List<String> suggestions) {
        // 如果当前没有任务，返回
        if (currentSuggestionRequest == null) {return;}

        if (currentSuggestionRequest.outputToChat && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(suggestions.toString()), false);
        }

        currentSuggestionRequest = null;
    }
}
