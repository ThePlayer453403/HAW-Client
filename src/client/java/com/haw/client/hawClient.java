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
    public static KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.haw"));
    public static boolean debug = false;

    public static String nextCommand = "";
    public static long cooldown = 0;
    public static boolean globalLock = false;
    public static boolean type = false;  // true -> msg false -> home
    public static boolean listen = false;
    public static int counter = 0;

    public static int warpCount = 0;
    public static HashMap<Integer, String> warpName = new HashMap<>();
    public static HashMap<Integer, String> warpComment = new HashMap<>();
    public static HashMap<Integer, String> warpTimeStamp = new HashMap<>();
    public static List<String> warpFavorite = new ArrayList<>();

    public static int homeCount = 0;
    public static HashMap<Integer, String> homeName = new HashMap<>();
    public static HashMap<Integer, String> homeComment = new HashMap<>();
    public static HashMap<Integer, String> homeTimeStamp = new HashMap<>();
    public static List<String> homeFavorite = new ArrayList<>();

    public static Pattern patternCurrentPage = Pattern.compile("第(\\d)页");
    public static Pattern patternPageCount = Pattern.compile("共(\\d)页");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {return;}  // 如果没有玩家 不进行处理

            if (keyBinding.isPressed()) {  // 当打开公共传送点列表 且 没有正在加载的列表 -> 开始加载公共传送点列表
                openScreen();
                if (System.currentTimeMillis() - cooldown > 5000 && !globalLock) {
                    nextCommand = String.format("%s list", type ? "warp" : "home" );
                    globalLock = true;
                    counter = 0;
                }
            }

            if (!Objects.equals(nextCommand, "")) {  // 如果要执行的指令不为空 -> 执行指令
                if (nextCommand.contains("list")) {
                    listen = true;}  // 如果指令为获取列表 -> 开始监听聊天
                client.player.networkHandler.sendChatCommand(nextCommand);
                nextCommand = "";
            }
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!listen) {return true;}  // 如果不监听聊天 直接返回
            
            String messageText = message.getString();
                
            if (messageText.startsWith("ID:")) {  // 如果以ID开头 -> 说明是传送点信息 拦截聊天
                String[] msg = messageText.substring(3).split("\\s");
                int id = Integer.parseInt(msg[0]);
                if (type) {
                    if (warpName.containsKey(id)) {
                        warpName.replace(id, msg[1]);
                        warpComment.replace(id, msg[2]);
                        warpTimeStamp.replace(id, msg[3]);
                    } else {
                        warpName.put(id, msg[1]);
                        warpComment.put(id, msg[2]);
                        warpTimeStamp.put(id, msg[3]);
                    }
                } else {
                    if (homeName.containsKey(id)) {
                        homeName.replace(id, msg[1]);
                        homeComment.replace(id, msg[2]);
                        homeTimeStamp.replace(id, msg[3]);
                    } else {
                        homeName.put(id, msg[1]);
                        homeComment.put(id, msg[2]);
                        homeTimeStamp.put(id, msg[3]);
                    }
                }
                counter = Math.max(counter, id);
                return debug;
            } else if (messageText.startsWith("===")) {  // 如果是===开头 -> 说明是传送点列表表头 拦截聊天
                return debug;
            } else if (messageText.startsWith("<上一页>")) {  // 如果是上一页开头 -> 说明是传送点列表翻页 拦截并获取总页数

                // 获取当前页和总页数
                int current_page, max_page;
                Matcher m = patternPageCount.matcher(messageText);
                if (m.find()) {max_page = Integer.parseInt(m.group(1));} else {return true;}
                Matcher m2 = patternCurrentPage.matcher(messageText);
                if (m2.find()) {current_page = Integer.parseInt(m2.group(1));} else {return true;}

                listen = false;  // 当监听到翻页时说明已经完成监听
                // 如果不是最后一页 继续运行
                if (current_page < max_page){
                    nextCommand = String.format("%s list %s", type ? "warp" : "home", current_page + 1);
                } else {
                    globalLock = false;
                    cooldown = System.currentTimeMillis();
                    if (type) {
                        warpCount = counter;
                    } else {
                        homeCount = counter;
                    }
                    openScreen();
                }
                return debug;
            } else if (messageText.startsWith("You cannot execute") || messageText.startsWith("未知或不完整的命令") || messageText.startsWith("Unknow or incomplete command")) {
                return debug;
            } else if (messageText.contains("<--[") || messageText.startsWith("您需要先通过验证")) {
                globalLock = false;
                listen = false;
                return debug;
            }
            return true;
        });
    }
    public static void openScreen() {
        MinecraftClient.getInstance().setScreen(new TeleportScreen());
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
                    type = false;
                    counter = 0;
                    nextCommand = "home list";
                    openScreen();
                }).dimensions(this.width - 120, 5, 110, 20).build();
            } else {
                buttonWidget = ButtonWidget.builder(Text.literal("切换至公共传送点 (warp)"), button -> {
                    type = true;
                    counter = 0;
                    nextCommand = "warp list";
                    openScreen();
                }).dimensions(this.width - 120, 5, 110, 20).build();
            }
            addDrawableChild(buttonWidget);
        }

    }

    public static class TeleportList extends ElementListWidget<TeleportEntry> {
        public TeleportList(MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);

            if (type) {
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
            } else {
                for (int id = 1; id <= homeCount; id++) {
                    if (homeFavorite.contains(homeName.get(id))) {
                        addEntry(new TeleportEntry(id));
                    }
                }
                for (int id = 1; id <= homeCount; id++) {
                    if (!homeFavorite.contains(homeName.get(id))) {
                        addEntry(new TeleportEntry(id));
                    }
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
                nextCommand = String.format("%s tp %s", type ? "warp" : "home", (type ? warpName : homeName).get(id));
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(0, 0, 50, 20).build();
            if (type) {
                if (warpFavorite.contains(warpName.get(id))) {
                    this.favoriteButton = ButtonWidget.builder(Text.literal("★"), button -> {
                        warpFavorite.remove(warpName.get(id));
                        openScreen();
                    }).dimensions(0, 0, 20, 20).build();
                } else {
                    this.favoriteButton = ButtonWidget.builder(Text.literal("☆"), button -> {
                        warpFavorite.add(warpName.get(id));
                        openScreen();
                    }).dimensions(0, 0, 20, 20).build();
                }
            } else {
                if (homeFavorite.contains(homeName.get(id))) {
                    this.favoriteButton = ButtonWidget.builder(Text.literal("★"), button -> {
                        homeFavorite.remove(homeName.get(id));
                        openScreen();
                    }).dimensions(0, 0, 20, 20).build();
                } else {
                    this.favoriteButton = ButtonWidget.builder(Text.literal("☆"), button -> {
                        homeFavorite.add(homeName.get(id));
                        openScreen();
                    }).dimensions(0, 0, 20, 20).build();
                }
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
            HashMap<Integer, String> name;
            HashMap<Integer, String> comment;
            HashMap<Integer, String> timeStamp;
            List<String> favorite;
            if (type) {
                name = warpName;
                comment = warpComment;
                timeStamp = warpTimeStamp;
                favorite = warpFavorite;
            } else {
                name = homeName;
                comment = homeComment;
                timeStamp = homeTimeStamp;
                favorite = homeFavorite;
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, "§e" + id, x, y, 0xFFFFFFFF, true);
            context.drawText(MinecraftClient.getInstance().textRenderer, "§a" + name.get(id), x + 10, y, 0xFFFFFFFF, true);
            if (favorite.contains(name.get(id))) {
                context.drawText(MinecraftClient.getInstance().textRenderer, "§b" + comment.get(id), x + 50, y, 0xFFFFFFFF, true);
            } else {
                context.drawText(MinecraftClient.getInstance().textRenderer, comment.get(id), x + 50, y, 0xFFFFFFFF, true);
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, "§7" + timeStamp.get(id), x + 200, y, 0xFFFFFFFF, true);

            teleportButton.setX(x + 245);
            teleportButton.setY(y - 5);
            teleportButton.render(context, mouseX, mouseY, tickProgress);
            favoriteButton.setX(x + 300);
            favoriteButton.setY(y - 5);
            favoriteButton.render(context, mouseX, mouseY, tickProgress);
        }
    }
}
