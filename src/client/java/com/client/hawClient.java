package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class hawClient implements ClientModInitializer {
    public static KeyBinding keyWarp = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.haw"));

    public static boolean shouldListen = false;
    public static boolean shouldOpenWarp = false;
    public static boolean isRunning = false;
    public static String nextCommand = "";
    public static long antiSpamTime = 0;

    public static int warpCount = 0;
    public static int warpCountWorking = 0;
    public static int warpPageCount = 0;
    public static HashMap<Integer, String> warpName = new HashMap<>();
    public static HashMap<Integer, String> warpComment = new HashMap<>();
    public static HashMap<Integer, String> warpTimeStamp = new HashMap<>();
    public static List<String> warpFavorite = new ArrayList<>();

    public static Pattern patternCurrentPage = Pattern.compile("第(\\d)页");
    public static Pattern patternPageCount = Pattern.compile("共(\\d)页");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {  // 如果没有玩家 不进行处理
                return;
            }

            if (shouldOpenWarp) {
                System.out.println(warpName);
                System.out.println(warpComment);
                System.out.println(warpTimeStamp);
                client.setScreen(new TeleportScreen());
                shouldOpenWarp = false;
            }

            if (keyWarp.isPressed()) {  // 当打开公共传送点列表 且 没有正在加载的列表 -> 开始加载公共传送点列表
                if (System.currentTimeMillis() - antiSpamTime > 5000 && !isRunning) {
                    nextCommand = "warp list";
                    isRunning = true;
                    warpCountWorking = 0;
                }
                client.setScreen(new TeleportScreen());
            }

            if (!Objects.equals(nextCommand, "")) {  // 如果要执行的指令不为空 -> 执行指令
                client.player.networkHandler.sendChatCommand(nextCommand);

                if (nextCommand.contains("list")) {  // 如果指令为获取列表 -> 开始监听聊天
                    shouldListen = true;
                }
                nextCommand = "";
            }
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!shouldListen) {  // 如果不监听聊天 直接返回
                return true;
            }
            if (message.getString().startsWith("ID:")) {  // 如果以ID开头 -> 说明是传送点信息 拦截聊天
                String[] warp = message.getString().substring(3).split("\\s");
                int warpID = Integer.parseInt(warp[0]);
                if (warpName.containsKey(warpID)) {
                    warpName.replace(warpID, warp[1]);
                    warpComment.replace(warpID, warp[2]);
                    warpTimeStamp.replace(warpID, warp[3]);
                } else {
                    warpName.put(warpID, warp[1]);
                    warpComment.put(warpID, warp[2]);
                    warpTimeStamp.put(warpID, warp[3]);
                }
                warpCountWorking = Math.max(warpCountWorking, warpID);
                return false;
            } else if (message.getString().startsWith("===")) {  // 如果是===开头 -> 说明是传送点列表表头 拦截聊天
                return false;
            } else if (message.getString().startsWith("<上一页>")) {  // 如果是上一页开头 -> 说明是传送点列表翻页 拦截并获取总页数
                shouldListen = false;  // 当监听到翻页时说明已经完成监听

                // 获取当前页和总页数
                Matcher m = patternPageCount.matcher(message.getString());
                if (m.find()) {
                    warpPageCount = Integer.parseInt(m.group(1));
                } else {return true;}

                Matcher m2 = patternCurrentPage.matcher(message.getString());
                int current_page;
                if (m2.find()) {
                    current_page = Integer.parseInt(m2.group(1));
                } else {return true;}

                // 如果不是最后一页 继续运行
                if (current_page < warpPageCount){
                    nextCommand = String.format("warp list %s", current_page + 1);
                } else {
                    isRunning = false;
                    shouldOpenWarp = true;
                    antiSpamTime = System.currentTimeMillis();
                    warpCount = warpCountWorking;
                }
                return false;
            }
            return true;
        });
    }
    public static class TeleportScreen extends Screen {
        public static TeleportList teleportList;

        public TeleportScreen() {
            super(Text.empty());
        }

        @Override
        public void init() {
            super.init();
            teleportList = new TeleportList(client, width, height-50, 25, 25, 10);
            addDrawableChild(teleportList);
        }

    }

    public static class TeleportList extends ElementListWidget<TeleportEntry> {
        public TeleportList(MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);

            for (int id = 1; id <= warpCount; id++) {
                if (warpFavorite.contains(warpName.get(id))) {
                    addEntry(new TeleportEntry(id));
                }
            }
            for (int id = 1; id <= warpCount; id++) {
                if (!warpFavorite.contains(warpName.get(id))) {
                    addEntry(new TeleportEntry(id));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 320;
        }
    }

    public static class TeleportEntry extends ElementListWidget.Entry<TeleportEntry> {
        public int id;
        public final ButtonWidget teleportButton;
        public final ButtonWidget favoriteButton;

        public TeleportEntry(int id) {
            this.id = id;
            this.teleportButton = ButtonWidget.builder(Text.literal("传送"), button -> {
                nextCommand = String.format("warp tp %s", warpName.get(id));
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(0, 0, 50, 20).build();
            if (warpFavorite.contains(warpName.get(id))) {
                this.favoriteButton = ButtonWidget.builder(Text.literal("★"), button -> {
                    warpFavorite.remove(warpName.get(id));
                    MinecraftClient.getInstance().setScreen(new TeleportScreen());
                }).dimensions(0, 0, 20, 20).build();
            } else {
                this.favoriteButton = ButtonWidget.builder(Text.literal("☆"), button -> {
                    warpFavorite.add(warpName.get(id));
                    MinecraftClient.getInstance().setScreen(new TeleportScreen());
                }).dimensions(0, 0, 20, 20).build();
            }
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of();
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.teleportButton, this.favoriteButton);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
            context.drawText(MinecraftClient.getInstance().textRenderer, "§e" + id, x, y, 0xFFFFFFFF, true);
            context.drawText(MinecraftClient.getInstance().textRenderer, "§a" + warpName.get(id), x + 10, y, 0xFFFFFFFF, true);
            if (warpFavorite.contains(warpName.get(id))) {
                context.drawText(MinecraftClient.getInstance().textRenderer, "§b" + warpComment.get(id), x + 50, y, 0xFFFFFFFF, true);
            } else {
                context.drawText(MinecraftClient.getInstance().textRenderer, warpComment.get(id), x + 50, y, 0xFFFFFFFF, true);
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, "§7" + warpTimeStamp.get(id), x + 200, y, 0xFFFFFFFF, true);

            teleportButton.setX(x + 245);
            teleportButton.setY(y - 5);
            teleportButton.render(context, mouseX, mouseY, tickProgress);
            favoriteButton.setX(x + 300);
            favoriteButton.setY(y - 5);
            favoriteButton.render(context, mouseX, mouseY, tickProgress);
        }
    }
}
