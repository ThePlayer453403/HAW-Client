package com.haw.client;

import com.haw.client.object.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
            String file1023 = "./xaero/minimap/Multiplayer_" + client.getCurrentServerEntry().address + "/dim%%%s/mw" +
                    (int) Math.floor(worldSpawnPos.getX() / 64.0) + "," +
                    (int) Math.floor( worldSpawnPos.getY() / 64.0) + "," +
                    (int) Math.floor(worldSpawnPos.getZ() / 64.0) + "_1023.txt";

            String file1024 = "./xaero/minimap/Multiplayer_" + client.getCurrentServerEntry().address + "/dim%%%s/mw" +
                    (int) Math.floor(worldSpawnPos.getX() / 64.0) + "," +
                    (int) Math.floor( worldSpawnPos.getY() / 64.0) + "," +
                    (int) Math.floor(worldSpawnPos.getZ() / 64.0) + "_1024.txt";


            // 防止文件不存在
            createFile(Paths.get(String.format(file1023, 0)));
            createFile(Paths.get(String.format(file1023, 1)));
            createFile(Paths.get(String.format(file1023, -1)));
            createFile(Paths.get(String.format(file1024, 0)));
            createFile(Paths.get(String.format(file1024, 1)));
            createFile(Paths.get(String.format(file1024, -1)));
            return file1024;
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

    public static void saveWaypointListsToFile(HashMap<String, Waypoint> warpList, HashMap<String, Waypoint> homeList) {
        if ((warpList == null || warpList.isEmpty()) && (homeList == null || homeList.isEmpty())) {
            return;
        }

        // 获取文件路径模板
        String filePathTemplate = getCurrentWaypointFile();
        if (filePathTemplate.equals("./unknow.txt")) {
            return;
        }

        // 合并两个列表并按维度分组
        HashMap<Integer, HashMap<String, Waypoint>> dimensionGroups = new HashMap<>();

        // 添加warpList
        if (warpList != null) {
            for (Map.Entry<String, Waypoint> entry : warpList.entrySet()) {
                Waypoint waypoint = entry.getValue();
                if (waypoint == null) continue;

                int dimId = getDimensionId(waypoint.dimension);
                dimensionGroups.computeIfAbsent(dimId, k -> new HashMap<>())
                        .put(entry.getKey(), waypoint);
            }
        }

        // 添加homeList
        if (homeList != null) {
            for (Map.Entry<String, Waypoint> entry : homeList.entrySet()) {
                Waypoint waypoint = entry.getValue();
                if (waypoint == null) continue;

                int dimId = getDimensionId(waypoint.dimension);
                dimensionGroups.computeIfAbsent(dimId, k -> new HashMap<>())
                        .put(entry.getKey(), waypoint);
            }
        }

        // 分别保存每个维度的路径点
        for (Map.Entry<Integer, HashMap<String, Waypoint>> group : dimensionGroups.entrySet()) {
            int dimId = group.getKey();
            HashMap<String, Waypoint> dimWaypoints = group.getValue();

            // 替换占位符生成实际文件路径
            String actualFilePath = String.format(filePathTemplate, dimId);
            Path path = Paths.get(actualFilePath);

            // 确保文件存在
            createFile(path);

            // 保存到文件
            saveWaypointsToFile(dimWaypoints, actualFilePath);
        }

    }

    public static int getDimensionId(String dimension) {
        if (dimension == null) return 0;

        if (dimension.contains("overworld") || dimension.contains("minecraft:overworld")) {
            return 0;
        } else if (dimension.contains("the_nether") || dimension.contains("minecraft:the_nether") || dimension.contains("nether")) {
            return -1;
        } else if (dimension.contains("the_end") || dimension.contains("minecraft:the_end") || dimension.contains("end")) {
            return 1;
        }

        return 0;
    }

    public static void saveWaypointsToFile(HashMap<String, Waypoint> waypointList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            for (Map.Entry<String, Waypoint> entry : waypointList.entrySet()) {
                Waypoint waypoint = entry.getValue();
                if (waypoint != null) {
                    String waypointString = waypoint.toWaypointString();
                    writer.write(waypointString);
                    writer.newLine();
                }
            }
            writer.flush();
        } catch (IOException ignored) {}
    }
}
