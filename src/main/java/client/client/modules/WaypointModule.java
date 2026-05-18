package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import java.util.ArrayList;
import java.util.List;

public class WaypointModule extends Module {
    public static class Waypoint {
        public String name;
        public double x, y, z;
        public Waypoint(String name, double x, double y, double z) {
            this.name = name; this.x = x; this.y = y; this.z = z;
        }
    }

    public static final List<Waypoint> waypoints = new ArrayList<>();

    public WaypointModule() {
        super("Waypoints", "Save and display named positions", Category.MISC);
        setKeybind(org.lwjgl.input.Keyboard.KEY_P);
    }

    public static void addCurrent(String name) {
        if (Minecraft.mc != null && Minecraft.mc.localPlayer != null) {
            waypoints.add(new Waypoint(name,
                Minecraft.mc.localPlayer.x,
                Minecraft.mc.localPlayer.y,
                Minecraft.mc.localPlayer.z));
        }
    }
}
