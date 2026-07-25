package com.haw.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class hawClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册调试指令
        ClientCommandRegistrationCallback.EVENT.register(ModChatCommand::register);
        // 注册命令返回数据监听
        ClientTickEvents.START_CLIENT_TICK.register((client) -> ModDataManagement.sendCommandSuggestionsRequest());
        // 注册数据自动加载
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {/*TODO: 在此处添加自动加载数据*/});
    }
}