package com.haw.client;


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
    public static boolean debug = true;
    public static boolean type = true;
    public static long lastRequestTime = 0;
    public static int antiSpam = 5000;
    public static HashMap<Integer, String> warpName = new HashMap<>();
    public static HashMap<Integer, String> warpComment = new HashMap<>();
    public static HashMap<Integer, String> warpCreateTime = new HashMap<>();
    public static List<String> warpFavorite = new ArrayList<>();
    public static HashMap<Integer, String> homeName = new HashMap<>();
    public static HashMap<Integer, String> homeComment = new HashMap<>();
    public static HashMap<Integer, String> homeCreateTime = new HashMap<>();
    public static List<String> homeFavorite = new ArrayList<>();
    public static HashMap<Integer, String> name = new HashMap<>();
    public static HashMap<Integer, String> comment = new HashMap<>();
    public static HashMap<Integer, String> createTime = new HashMap<>();
    public static Pattern patternCurrentPage = Pattern.compile("第(\\d)页");
    public static Pattern patternPageCount = Pattern.compile("共(\\d)页");
    public static KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw-client.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.haw-client"));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(hawClient::tick);
        ClientReceiveMessageEvents.ALLOW_GAME.register(hawClient::message);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {return;}
        
        if (keyBinding.isPressed()) {
            client.setScreen(new TeleportScreen());
            if (System.currentTimeMillis() - antiSpam > lastRequestTime) {
                Objects.requireNonNull(client.getNetworkHandler()).sendChatCommand(String.format("%s list", type ? "warp" : "home"));
                lastRequestTime = -1;
            }
        }
    }

    public static boolean message(Text message, boolean overlay) {
        if (lastRequestTime != -1) {return true;}
        String messageContent = message.getString();

        if (messageContent.startsWith("===")) {
            return debug;
        } else if (messageContent.startsWith("ID:")) {
            String[] messageContentPart = messageContent.substring(3).split("\\s");
            int key = Integer.parseInt(messageContentPart[0]);
            name.put(key, messageContentPart[1]);
            comment.put(key, messageContentPart[2]);
            createTime.put(key, messageContentPart[3]);
            return debug;
        } else if (messageContent.startsWith("<上一页>")) {
            System.out.println(messageContent);
            int currentPage, maxPage;
            Matcher currentPageMatcher = patternCurrentPage.matcher(messageContent);
            if (currentPageMatcher.find()){
                currentPage = Integer.parseInt(currentPageMatcher.group(1));
            } else {
                return debug;
            }
            Matcher maxPageMatcher = patternPageCount.matcher(messageContent);
            if (maxPageMatcher.find()){
                maxPage = Integer.parseInt(maxPageMatcher.group(1));
            } else {
                return debug;
            }

            if (currentPage < maxPage) {
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand(String.format("%s list %s", type ? "warp" : "home", currentPage + 1));
            } else {
                lastRequestTime = System.currentTimeMillis();
                if (type) {
                    if (!(name.equals(warpName) && comment.equals(warpComment) && createTime.equals(warpCreateTime))) {
                        warpName = name;
                        warpComment = comment;
                        warpCreateTime = createTime;
                        if (MinecraftClient.getInstance().currentScreen instanceof TeleportScreen) {
                            MinecraftClient.getInstance().setScreen(new TeleportScreen());
                        }
                    }
                } else {
                    if (!(name.equals(homeName) && comment.equals(homeComment) && createTime.equals(homeCreateTime))) {
                        homeName = name;
                        homeComment = comment;
                        homeCreateTime = createTime;
                        if (MinecraftClient.getInstance().currentScreen instanceof TeleportScreen) {
                            MinecraftClient.getInstance().setScreen(new TeleportScreen());
                        }
                    }
                }
                name = new HashMap<>();
                comment = new HashMap<>();
                createTime = new HashMap<>();
            }
            return debug;
        } else if (messageContent.startsWith("You cannot execute") || messageContent.startsWith("未知或不完整的命令") || messageContent.startsWith("Unknow or incomplete command")) {
            return debug;
        } else if (messageContent.contains("<--[") || messageContent.startsWith("您需要先通过验证")) {
            lastRequestTime = System.currentTimeMillis();
            return debug;
        }
        return true;
    }

    public static class TeleportScreen extends Screen {
        public static TeleportList teleportList;
        public static ButtonWidget buttonWidget;

        public TeleportScreen() {
            super(Text.empty());
        }

        @Override
        public void init() {
            super.init();
            teleportList = new TeleportList(client, width, height-50, 30, 25, 10);
            addDrawableChild(teleportList);

            if (type) {
                buttonWidget = ButtonWidget.builder(Text.literal("切换至个人传送点 (home)"), button -> {
                    if (System.currentTimeMillis() - antiSpam > lastRequestTime) {
                        if (client != null) {Objects.requireNonNull(client.getNetworkHandler()).sendChatCommand("home list");}
                        lastRequestTime = -1;
                    }
                    type = false;
                    MinecraftClient.getInstance().setScreen(new TeleportScreen());
                }).dimensions(this.width - 120, 5, 110, 20).build();
            } else {
                buttonWidget = ButtonWidget.builder(Text.literal("切换至公共传送点 (warp)"), button -> {
                    if (System.currentTimeMillis() - antiSpam > lastRequestTime) {
                        if (client != null) {Objects.requireNonNull(client.getNetworkHandler()).sendChatCommand("warp list");}
                        lastRequestTime = -1;
                    }
                    type = true;
                    MinecraftClient.getInstance().setScreen(new TeleportScreen());
                }).dimensions(this.width - 120, 5, 110, 20).build();
            }
            addDrawableChild(buttonWidget);
        }

    }

    public static class TeleportList extends ElementListWidget<TeleportEntry> {
        public TeleportList(MinecraftClient client, int i, int j, int k, int l, int m) {
            super(client, i, j, k, l, m);
            System.out.println(type ? warpName : homeName);
            (type ? warpName : homeName).forEach((key, value) -> {
                if ((type ? warpFavorite : homeFavorite).contains(value)) {
                    addEntry(new TeleportEntry(key));
                }
            });
            (type ? warpName : homeName).forEach((key, value) -> {
                if (!(type ? warpFavorite : homeFavorite).contains(value)) {
                    addEntry(new TeleportEntry(key));
                }
            });
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
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand(String.format("%s tp %s", type ? "warp" : "home", (type ? warpName : homeName).get(id)));
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(0, 0, 50, 20).build();
            if ((type ? warpFavorite : homeFavorite).contains((type ? warpName : homeName).get(id))) {
                this.favoriteButton = ButtonWidget.builder(Text.literal("★"), button -> {
                    (type ? warpFavorite : homeFavorite).remove((type ? warpName : homeName).get(id));
                    MinecraftClient.getInstance().setScreen(new TeleportScreen());
                }).dimensions(0, 0, 20, 20).build();
            } else {
                this.favoriteButton = ButtonWidget.builder(Text.literal("☆"), button -> {
                    (type ? warpFavorite : homeFavorite).add((type ? warpName : homeName).get(id));
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
            context.drawText(MinecraftClient.getInstance().textRenderer, "§a" + (type ? warpName : homeName).get(id), x + 10, y, 0xFFFFFFFF, true);
            if ((type ? warpFavorite : homeFavorite).contains((type ? warpName : homeName).get(id))) {
                context.drawText(MinecraftClient.getInstance().textRenderer, "§b" + (type ? warpComment : homeComment).get(id), x + 50, y, 0xFFFFFFFF, true);
            } else {
                context.drawText(MinecraftClient.getInstance().textRenderer, (type ? warpComment : homeComment).get(id), x + 50, y, 0xFFFFFFFF, true);
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, "§7" + (type ? warpCreateTime : homeCreateTime).get(id), x + 200, y, 0xFFFFFFFF, true);

            teleportButton.setX(x + 245);
            teleportButton.setY(y - 5);
            teleportButton.render(context, mouseX, mouseY, tickProgress);
            favoriteButton.setX(x + 300);
            favoriteButton.setY(y - 5);
            favoriteButton.render(context, mouseX, mouseY, tickProgress);
        }
    }
}