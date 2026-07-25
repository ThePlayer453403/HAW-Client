package com.haw.client.mixin;

import com.haw.client.ModDataManagement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkMixin {
    /*
    由于Xaero的路径点只能保存名字，所以采取「名称 - 注释」保存
    但这样无法将传送命令设为「warp tp {name}」传送
    所以通过mixin将发出的指令「 - 注释」的部分删去，还原正确的指令
     */
    @Unique
    private static final Pattern notePattern = Pattern.compile(" - .+");

    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String RemoveNoteFromChatCommand(String chatCommand) {
        if (chatCommand.startsWith("warp") || chatCommand.startsWith("home")) {  // 仅处理home或warp
            return notePattern.matcher(chatCommand).replaceAll("");
        }
        return chatCommand;
    }

    /*
    监听服务器发出的命令提示，筛选出由mod发出的
     */
    @Inject(method = "onCommandSuggestions", at = @At("TAIL"))
    private void RecieveCommandSuggestion(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        // 如果是聊天屏幕（打开聊天框），说明请求并非由mod发出，忽略
        if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
            return;
        }

        List<String> suggestions = new ArrayList<>();
        for (CommandSuggestionsS2CPacket.Suggestion suggestion : packet.suggestions()) {
            suggestions.add(suggestion.text());
        }

        ModDataManagement.receiveCommandSuggestions(suggestions);
    }
}
