package com.haw.client.object;

import net.minecraft.util.math.BlockPos;

public class Waypoint {
    public String name;
    public String note;
    public String type;
    public BlockPos position;
    public String dimension;
    public Waypoint(String name, String note, String type, int x, int y, int z, String dimension) {
        this.name = name;
        this.note = note;
        this.position = new BlockPos(x, y, z);
        this.type = type;
        this.dimension = dimension;
        System.out.println(toWaypointString());
    }
    public Waypoint(String name, String type, int x, int y, int z, String dimension) {
        this.name = name;
        this.note = "";
        this.position = new BlockPos(x, y, z);
        this.type = type;
        this.dimension = dimension;
        System.out.println(toWaypointString());
    }
    public String toWaypointString() {
        // 确定要使用的字符
        String charToUse;
        if (note != null && !note.isEmpty()) {
            // 如果note不为空，使用note的第一个字符
            charToUse = String.valueOf(note.charAt(0));
        } else {
            // 如果note为空，使用name的第一个字符并转为大写
            charToUse = name.isEmpty() ? "" : String.valueOf(Character.toUpperCase(name.charAt(0)));
        }

        return String.format("waypoint:%s - %s:%s:%d:%d:%d:11:false:0:%s:%s:0:0:false",
                name,
                note != null ? note : "",
                charToUse,
                position.getX(),
                position.getY(),
                position.getZ(),
                type,
                "home".equals(type)
        );
    }
}
