package com.haw.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class hawClient implements ClientModInitializer {
    public static final KeyBinding OpenScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.haw.screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyBinding.Category.GAMEPLAY));

    public static Map<String, String> TeleportNote = new HashMap<>();
    public static Map<String, String> TeleportTimestamp = new HashMap<>();

    public static boolean ListenToChat = false;
    public static long LastCommand = 0;
    public static String Mode = "warp";

    public static List<String> FavoriteList = new ArrayList<>();
    public static List<String> DisplayList = new ArrayList<>();
    public static List<String> TeleportList = new ArrayList<>();
    public static List<String> ChatCommandSchedule = new ArrayList<>();

    public static Pattern TeleportInfo = Pattern.compile("(.+?) @ .* (\\d+-\\d+-\\d+ \\d+:\\d+:\\d+) - (.+?)$");
    public static Pattern TeleportInfo_ = Pattern.compile("(.+?) @ .* (\\d+-\\d+-\\d+ \\d+:\\d+:\\d+)");

    private static final Gson GSON = new Gson();
    private static final File CONFIG_FILE = new File("./config/haw-client.json");

    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenScreenKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new TeleportScreen(false));
                getCommandSuggestion("/" + Mode + " look ");
            }
            trySendChatCommand();
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((Text message, boolean overlay) -> {
            System.out.println(message);
            if (ListenToChat) {
                if (message.contains(Text.literal("=== 共享点详情 ===")) || message.contains(Text.literal("=== 传送点详情 ==="))) {
                    return false;
                } else {
                    Matcher matcher = TeleportInfo.matcher(message.getString());

                    if (matcher.find() && Objects.equals(matcher.group(1), ChatCommandSchedule.getFirst())) {
                        TeleportNote.put(ChatCommandSchedule.getFirst(), matcher.group(3));
                        TeleportTimestamp.put(ChatCommandSchedule.getFirst(), matcher.group(2));

                        saveTeleportNoteAndTimestamp();

                        ChatCommandSchedule.removeFirst();

                        if (ChatCommandSchedule.isEmpty()) {
                            ListenToChat = false;
                        }

                        return false;
                    }

                    matcher = TeleportInfo_.matcher(message.getString());

                    if (matcher.find() && Objects.equals(matcher.group(1), ChatCommandSchedule.getFirst())) {
                        TeleportNote.put(ChatCommandSchedule.getFirst(), "");
                        TeleportTimestamp.put(ChatCommandSchedule.getFirst(), matcher.group(2));

                        saveTeleportNoteAndTimestamp();

                        ChatCommandSchedule.removeFirst();

                        if (ChatCommandSchedule.isEmpty()) {
                            ListenToChat = false;
                        }

                        return false;
                    }
                }
            }
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    private static void loadConfig() {
        if (!CONFIG_FILE.exists()) return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type type = new TypeToken<List<Object>>() {}.getType();
            List<Object> data = GSON.fromJson(reader, type);

            if (data != null && data.size() >= 3) {
                TeleportNote = (Map<String, String>) data.get(0);
                TeleportTimestamp = (Map<String, String>) data.get(1);
                FavoriteList = (List<String>) data.get(2);
            }
        } catch (IOException ignored) {}
    }

    public static void saveTeleportNoteAndTimestamp() {
        List<Object> data = new ArrayList<>();
        data.add(TeleportNote);
        data.add(TeleportTimestamp);
        data.add(FavoriteList);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void trySendChatCommand() {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;
        if (ChatCommandSchedule.isEmpty()) return;
        System.out.println("Send");
        if (System.currentTimeMillis() - LastCommand > 1000) {
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand(Mode + " look " + ChatCommandSchedule.getFirst());
            LastCommand = System.currentTimeMillis();
            ListenToChat = true;
        }
    }

    public static void getCommandSuggestionCallback(List<String> message) {
        if (ChatCommandSchedule.isEmpty()) {
            message.forEach(name -> {
                if (!TeleportNote.containsKey(name)) {
                    ChatCommandSchedule.add(name);
                }
            });
        }
        TeleportList = message;
        MinecraftClient.getInstance().setScreen(new TeleportScreen());
    }

    public static void getCommandSuggestion(String command) {
        if (MinecraftClient.getInstance().player != null) {
            int requestID = UUID.randomUUID().hashCode();
            RequestCommandCompletionsC2SPacket requestPacket = new RequestCommandCompletionsC2SPacket(requestID, command);
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().sendPacket(requestPacket);
            }
        }
    }

    public static class TeleportScreen extends Screen {
        public TeleportListWidget TeleportWidget;
        public ButtonWidget LobbyButton;
        public ButtonWidget TeyvatButton;
        public ButtonWidget ShengdianButton;
        public ButtonWidget SdmirrorButton;
        public ButtonWidget SwitchButton;
        public boolean LoadDisplay = true;

        public TeleportScreen(boolean display) {
            super(Text.empty());
            this.LoadDisplay = display;
        }

        public TeleportScreen() {
            super(Text.empty());
        }

        protected void init() {
            DisplayList.clear();
            if (LoadDisplay) {
                TeleportList.forEach(name -> {
                    if (FavoriteList.contains(name)) {
                        DisplayList.add(name);
                    }
                });
                TeleportList.forEach(name -> {
                    if (!FavoriteList.contains(name)) {
                        DisplayList.add(name);
                    }
                });
            }

            SwitchButton = ButtonWidget.builder(Text.literal(Objects.equals(Mode, "home") ? "切换至公共传送点 (warp)" : "切换至个人传送点 (home)"), button -> {
                Mode = Objects.equals(Mode, "home") ? "warp" : "home";
                MinecraftClient.getInstance().setScreen(new TeleportScreen());
                getCommandSuggestion("/" + Mode + " look ");
            }).dimensions(this.width - 120, 5, 110, 20).build();
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
        public TeleportEntry(int id) {
            ID = id;
            TeleportButton = ButtonWidget.builder(Text.translatable("teleport.haw.client"), button -> {
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand(String.format(Mode + " tp %s", DisplayList.get(ID)));
                MinecraftClient.getInstance().setScreen(null);
            }).dimensions(0, 0, 50, 20).build();
            FavoriteButton = ButtonWidget.builder(Text.translatable(FavoriteList.contains(DisplayList.get(ID)) ? "unfavorite.haw.client" : "favorite.haw.client"), button -> {
                if (FavoriteList.contains(DisplayList.get(ID))) {
                    FavoriteList.remove(DisplayList.get(ID));
                } else  {
                    FavoriteList.add(DisplayList.get(ID));
                }
                saveTeleportNoteAndTimestamp();
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

            if (ID >= DisplayList.size()) return;

            String name = DisplayList.get(ID);
            String note = TeleportNote.getOrDefault(name, "Loading...");
            String timestamp = TeleportTimestamp.getOrDefault(name, "Loading...");
            try {
                context.drawText(MinecraftClient.getInstance().textRenderer, "§e" + (ID+1), x, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, "§a" + name, x + 10, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, FavoriteList.contains(name) ? "§b" + note : note, x + 50, y, 0xFFFFFFFF, true);
                context.drawText(MinecraftClient.getInstance().textRenderer, "§7" + timestamp, x + 175, y, 0xFFFFFFFF, true);
            } catch (NoSuchMethodError e) {
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§e" + (ID+1), x, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§a" + name, x + 10, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, FavoriteList.contains(name) ? "§b" + note : note, x + 50, y, 0xFFFFFFFF);
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "§7" + timestamp, x + 175, y, 0xFFFFFFFF);
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
            super(minecraftClient, width, height - 50, 30, 25);//, 10);

            for (int i=0; i<DisplayList.size(); i++) {
                addEntry(new TeleportEntry(i));
            }
        }

        @Override
        public int getRowWidth() {return 320;}
    }
}