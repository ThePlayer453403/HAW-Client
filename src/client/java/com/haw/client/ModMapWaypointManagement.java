package com.haw.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModMapWaypointManagement {
    /*
    需要自动设置的XaeroMap设置
    ./xaero/world-map/Multiplayer_xxx/server_config.txt
    multiworldType:2

    ./xaero/minimap/Multiplayer_xxx/config.txt
    usingDefaultTeleportCommand:false
    serverTeleportCommandFormat:/warp tp {name}
    serverTeleportCommandRotationFormat:/home tp {name}
     */
    //TODO: 自动配置地图

    public static String getCurrentWaypointFile() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null && client.world != null) {
            BlockPos worldSpawnPos = client.world.getSpawnPoint().getPos();
            // 当XaeroMap使用世界出生点区分世界时，路径点将存储在出生点坐标处以64并向下取整的文件中
            // 这也就是为什么地图区分不出lobby和shengdian，它们除完都是0,1,0
            String file = "./xaero/minimap/Multiplayer_" + client.getCurrentServerEntry().address + "/dim%%%s/mw_" +
                    (int) Math.floor(worldSpawnPos.getX() / 64.0) + "," +
                    (int) Math.floor( worldSpawnPos.getY() / 64.0) + "," +
                    (int) Math.floor(worldSpawnPos.getZ() / 64.0) + "_1024.txt";

            // 防止文件不存在
            createFile(Paths.get(String.format(file, 0)));
            createFile(Paths.get(String.format(file, 1)));
            createFile(Paths.get(String.format(file, -1)));
            return file;
        }
        return "./unknow.txt";
    }

    public static void createFile(Path path) {
        try {
            if (path.getParent() != null) {Files.createDirectories(path.getParent());}
            if (!Files.exists(path)) {Files.createFile(path);}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
