package com.haw.client;

import com.example.haw.TeleportPoint;
import com.example.haw.client.HomeAndWarpClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class hawClient implements ClientModInitializer {
    public static final KeyBinding openScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw.screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyBinding.Category.MISC));
    public static List<String> favorite = new ArrayList<>();
    public static boolean type = true;

    public static int i;

    @Override
    public void onInitializeClient() {
        loadFavorites();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new TeleportScreen());
            }
        });
    }

    public static void saveFavorites() {
        try {
            // 获取 Minecraft 配置目录
            Path configPath = Paths.get(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), "config", "haw");

            // 确保目录存在
            Files.createDirectories(configPath);

            // 构建文件路径
            Path filePath = configPath.resolve("haw_favorites.txt");

            // 写入文件
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                for (String name : favorite) {
                    writer.write(name);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static void loadFavorites() {
        try {
            // 获取 Minecraft 配置目录
            Path configPath = Paths.get(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), "config", "haw");

            // 构建文件路径
            Path filePath = configPath.resolve("haw_favorites.txt");

            // 如果文件不存在，直接返回
            if (!Files.exists(filePath)) {
                return;
            }

            // 清空当前列表
            favorite.clear();

            // 读取文件
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        favorite.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load favorites: " + e.getMessage());
        }
    }

    public static class TeleportScreen extends Screen {
        public TeleportListWidget TeleportWidget;
        public ButtonWidget LobbyButton;
        public ButtonWidget TeyvatButton;
        public ButtonWidget ShengdianButton;
        public ButtonWidget SdmirrorButton;
        public ButtonWidget SwitchButton;

        public TeleportScreen() {
            super(Text.empty());
        }

        protected void init() {

            SwitchButton = ButtonWidget.builder(Text.literal(type ? "切换至公共传送点 (warp)" : "切换至个人传送点 (home)"), button -> type = !type).dimensions(this.width - 120, 5, 110, 20).build();
            addDrawableChild(SwitchButton);

            LobbyButton = ButtonWidget.builder(Text.literal("登录服务器"), button -> Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("server lobby")).dimensions(this.width - 100, 40, 90, 20).build();
            addDrawableChild(LobbyButton);
            TeyvatButton = ButtonWidget.builder(Text.literal("提瓦特服务器"), button -> Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("server Teyvat")).dimensions(this.width - 100, 65, 90, 20).build();
            addDrawableChild(TeyvatButton);
            ShengdianButton = ButtonWidget.builder(Text.literal("生电服务器"), button -> Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("server shengdian")).dimensions(this.width - 100, 90, 90, 20).build();
            addDrawableChild(ShengdianButton);
            SdmirrorButton = ButtonWidget.builder(Text.literal("生电镜像服务器"), button -> Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("server sdmirror")).dimensions(this.width - 100, 115, 90, 20).build();
            addDrawableChild(SdmirrorButton);

            TeleportWidget = new TeleportListWidget(client, width, height);
            addDrawableChild(TeleportWidget);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            TeleportWidget.render(context, mouseX, mouseY, deltaTicks);

            SwitchButton.render(context, mouseX, mouseY, deltaTicks);

            LobbyButton.render(context, mouseX, mouseY, deltaTicks);
            TeyvatButton.render(context, mouseX, mouseY, deltaTicks);
            ShengdianButton.render(context, mouseX, mouseY, deltaTicks);
            SdmirrorButton.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    public static class TeleportEntry extends ElementListWidget.Entry<TeleportEntry> {
        public int ID;
        public ButtonWidget TeleportButton;
        public ButtonWidget FavoriteButton;

        public String name;
        public TeleportPoint teleportPoint;

        public TeleportEntry(int ID, String name, TeleportPoint teleportPoint) {
            this.ID = ID;
            this.name = name;
            this.teleportPoint = teleportPoint;

            TeleportButton = ButtonWidget.builder(Text.translatable("teleport.haw.client"), button -> {
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand(String.format("%s tp %s", type ? "warp" : "home", teleportPoint.name));
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(0, 0, 50, 20).build();

            FavoriteButton = ButtonWidget.builder(Text.translatable(favorite.contains(teleportPoint.name) ? "unfavorite.haw.client" : "favorite.haw.client"), button -> {
                if (favorite.contains(teleportPoint.name)) {
                    favorite.remove(teleportPoint.name);
                } else  {
                    favorite.add(teleportPoint.name);
                }
                saveFavorites();
                MinecraftClient.getInstance().setScreen(new TeleportScreen());
            }).dimensions(0, 0, 20, 20).build();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(this.TeleportButton, this.FavoriteButton);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.TeleportButton, this.FavoriteButton);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
//        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
            int x = getX();
            int y = getY() + 10;

            try {
                context.drawText(MinecraftClient.getInstance().textRenderer, "§e" + ID, x, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, "§a" + name, x + 10, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, (favorite.contains(name) ? "§b" : "") + teleportPoint.note, x + 50, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, "§7" + teleportPoint.createdAt, x + 175, y, 0xFFFFFFFF, true);
            } catch (NoSuchMethodError e) {
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§e" + ID, x, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§a" + name, x + 10, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, (favorite.contains(name) ? "§b" : "") + teleportPoint.note, x + 50, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§7" + teleportPoint.createdAt, x + 175, y, 0xFFFFFFFF);
            }
            TeleportButton.setX(x + 245);
            TeleportButton.setY(y - 5);
            TeleportButton.render(context, mouseX, mouseY, deltaTicks);
            FavoriteButton.setX(x + 300);
            FavoriteButton.setY(y - 5);
            FavoriteButton.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    public static class TeleportListWidget extends ElementListWidget<TeleportEntry> {
        public TeleportListWidget(MinecraftClient minecraftClient, int width, int height) {
            super(minecraftClient, width, height - 50, 30, 25);
            i = 1;
            if (type) {
                HomeAndWarpClient.warp.forEach((key, value) -> {
                    if (favorite.contains(key)) {
                        addEntry(new TeleportEntry(i, key, value));
                        i++;
                    }
                });
                HomeAndWarpClient.warp.forEach((key, value) -> {
                    if (!favorite.contains(key)) {
                        addEntry(new TeleportEntry(i, key, value));
                        i++;
                    }
                });
            } else {
                HomeAndWarpClient.home.forEach((key, value) -> {
                    if (favorite.contains(key)) {
                        addEntry(new TeleportEntry(i, key, value));
                        i++;
                    }
                });
                HomeAndWarpClient.home.forEach((key, value) -> {
                    if (!favorite.contains(key)) {
                        addEntry(new TeleportEntry(i, key, value));
                        i++;
                    }
                });
            }
        }

        @Override
        public int getRowWidth() {return 320;}
    }
}