package com.haw.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.haw.client.hawClient;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onCommandSuggestions", at = @At("TAIL"))
    private void onCommandSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        // 如果打开了聊天框, 既不是有模组发送, 忽略消息
        if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {return;}

        // 遍历并处理所有命令提示
        List<String> suggestions = new ArrayList<>();
        for (CommandSuggestionsS2CPacket.Suggestion suggestion : packet.suggestions()) {
            String suggestionText = suggestion.text();
            suggestions.add(suggestionText);
        }

        // 处理获取的命令提示
        hawClient.getCommandSuggestionCallback(suggestions);
    }
}
