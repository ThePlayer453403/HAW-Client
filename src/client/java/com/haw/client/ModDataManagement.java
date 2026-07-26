package com.haw.client;

import com.haw.client.object.CommandSuggestionRequest;
import com.haw.client.object.Waypoint;
import com.haw.client.object.WaypointInfoRequest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModDataManagement {
    /*
    目前从服务器获取数据的方法由两种
    A: 向服务器发送RequestCommandCompletionsC2SPacket，使用mixin获取结果
    B: 向服务器发送指令，通过Event获取结果
     */
    public static long lastUpdatedTime = 0;
    // 待发送的命令提示请求，需要以「/」开头
    public static List<CommandSuggestionRequest> commandSuggestionSchedule = new ArrayList<>();
    public static CommandSuggestionRequest currentSuggestionRequest;

    // 记录发送指令的时间，防止被踢
    public static long lastCommandSentTimeStamp = 0;
    // 待发送命令
    public static List<WaypointInfoRequest> infoRequestSchedule = new ArrayList<>();
    public static WaypointInfoRequest currentInfoRequest;

    public static HashMap<String, Waypoint> warpList = new HashMap<>();
    public static HashMap<String, Waypoint> homeList = new HashMap<>();

    public static final Pattern pattern = Pattern.compile("(.+)\\s@\\s(.+)\\s([\\d.-]+),\\s([\\d.-]+),\\s([\\d.-]+)\\s.+\\s-\\s(.+)");
    public static final Pattern patternWithoutNote = Pattern.compile("(.+)\\s@\\s(.+)\\s([\\d.-]+),\\s([\\d.-]+),\\s([\\d.-]+)\\s.+");

    public static void sendCommandSuggestionsRequest() {
        // 每5秒自动请求一次更新
        if (System.currentTimeMillis() - lastUpdatedTime > 5000 && commandSuggestionSchedule.isEmpty() && infoRequestSchedule.isEmpty()) {
            lastUpdatedTime = System.currentTimeMillis();
            commandSuggestionSchedule.add(new CommandSuggestionRequest("warp look"));
            commandSuggestionSchedule.add(new CommandSuggestionRequest("home look"));
        }

        MinecraftClient client = MinecraftClient.getInstance();

        // 如果请求大于1秒没有响应，取消等待
        if (currentSuggestionRequest != null && System.currentTimeMillis() - currentSuggestionRequest.timestamp > 1000) {
            currentSuggestionRequest = null;
        }

        // 如果请求计划为空/打开了聊天栏，不进行请求
        if (commandSuggestionSchedule.isEmpty() || client.currentScreen instanceof ChatScreen || currentSuggestionRequest != null) {
            return;
        }

        if (client.player != null && client.getNetworkHandler() != null) {
            int requestID = UUID.randomUUID().hashCode();
            currentSuggestionRequest = commandSuggestionSchedule.removeFirst();
            currentSuggestionRequest.timestamp = System.currentTimeMillis();
            RequestCommandCompletionsC2SPacket requestPacket = new RequestCommandCompletionsC2SPacket(requestID, currentSuggestionRequest.chatCommand);
            client.getNetworkHandler().sendPacket(requestPacket);
        }

    }

    public static void receiveCommandSuggestions(List<String> suggestions) {
        // 如果当前没有任务，返回
        if (currentSuggestionRequest == null) {return;}

        if (currentSuggestionRequest.outputToChat && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(suggestions.toString()), false);
        }

        if (currentSuggestionRequest.chatCommand.contains("warp")) {
            suggestions.forEach((name) -> {
                if (!warpList.containsKey(name)) {
                    infoRequestSchedule.add(new WaypointInfoRequest("warp", name));
                }
            });
        }
        if (currentSuggestionRequest.chatCommand.contains("home")) {
            suggestions.forEach((name) -> {
                if (!homeList.containsKey(name)) {
                    infoRequestSchedule.add(new WaypointInfoRequest("home", name));
                }
            });
        }
        currentSuggestionRequest = null;
    }

    public static void sendChatCommand() {
        if (System.currentTimeMillis() - lastCommandSentTimeStamp < 1000) {  // 限制执行后等待1s防止被踢
            return;
        } else {  // 1s后超时
            currentInfoRequest = null;
        }
        if (!infoRequestSchedule.isEmpty() && MinecraftClient.getInstance().getNetworkHandler() != null) {
            currentInfoRequest = infoRequestSchedule.removeFirst();
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand(String.format("%s look %s", currentInfoRequest.type, currentInfoRequest.name));
            lastCommandSentTimeStamp = System.currentTimeMillis();
        }
    }

    public static boolean receiveChatCommandResult(Text message) {
        if (currentInfoRequest == null) {return true;}
        Matcher matcher;
        Waypoint waypoint;
        matcher = pattern.matcher(message.getString());
        if (matcher.find() && Objects.equals(matcher.group(1), currentInfoRequest.name)) {
            waypoint = new Waypoint(currentInfoRequest.name, matcher.group(6), currentInfoRequest.type, string2int(matcher.group(3)), string2int(matcher.group(4)), string2int(matcher.group(5)), matcher.group(2));
        } else {
            matcher = patternWithoutNote.matcher(message.getString());
            if (matcher.find() && Objects.equals(matcher.group(1), currentInfoRequest.name)) {
                waypoint = new Waypoint(currentInfoRequest.name, currentInfoRequest.type, string2int(matcher.group(3)), string2int(matcher.group(4)), string2int(matcher.group(5)), matcher.group(2));
            } else {
                return true;
            }
        }

        if (Objects.equals(currentInfoRequest.type, "warp")) {
            warpList.put(currentInfoRequest.name, waypoint);
        } else {
            homeList.put(currentInfoRequest.name, waypoint);
        }
        currentInfoRequest = null;
        ModMapWaypointManagement.saveWaypointListsToFile(warpList, homeList);
        return false;
    }

    public static void clearSchedule() {
        commandSuggestionSchedule = new ArrayList<>();
        currentSuggestionRequest = null;
        infoRequestSchedule = new ArrayList<>();
        currentInfoRequest = null;
    }

    public static int string2int(String number) {
        return (int) Math.round(Double.parseDouble(number));
    }
}
