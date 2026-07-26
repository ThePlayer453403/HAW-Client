package com.haw.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class hawClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册调试指令
        ClientCommandRegistrationCallback.EVENT.register(ModChatCommand::register);
        // 注册自动发送命令提示、自动发送指令
        ClientTickEvents.START_CLIENT_TICK.register((client) -> ModDataManagement.sendCommandSuggestionsRequest());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> ModDataManagement.sendChatCommand());
        // 注册数据自动加载
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {/*TODO: 在此处添加自动加载数据*/});
        // 清除计划，防止下次进服被踢
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModDataManagement.clearSchedule());
        // 监听聊天，获取命令返回
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> ModDataManagement.receiveChatCommandResult(message));
    }
}