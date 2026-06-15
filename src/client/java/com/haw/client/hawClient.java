package com.haw.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unused")
public class hawClient implements ClientModInitializer {
    public static final KeyBinding OpenScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw.screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.haw.client"));
    
    public static String mode;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenScreenKeyBinding.wasPressed()) {
                getCommandSuggestion("home");
            }
        });
    }
    public static void getCommandSuggestionCallback(List<String> message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(message.toString()), false);
        }
    }

    public static void getCommandSuggestion(String mode) {
        if (MinecraftClient.getInstance().player != null) {
            int requestID = UUID.randomUUID().hashCode();
            RequestCommandCompletionsC2SPacket requestPacket = new RequestCommandCompletionsC2SPacket(requestID, String.format("/%s look ", mode));
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().sendPacket(requestPacket);
            }
        }
    }
}