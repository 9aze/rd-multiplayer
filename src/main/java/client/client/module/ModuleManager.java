package client.client.module;

import client.client.modules.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Movement
        register(new SpeedModule());
        register(new BunnyHopModule());
        register(new AutoSprintModule());
        register(new NoClipModule());
        register(new TowerModule());
        // Render
        register(new ESPModule());
        register(new TracersModule());
        register(new ChamsModule());
        register(new RadarModule());
        // Player / Build
        register(new ScaffoldModule());
        register(new AutoHighwayModule());
        register(new MineAuraModule());
        register(new FlattenModule());
        register(new SpamPlaceModule());
        register(new NukeModule());
        // Misc
        register(new AnnoyModule());
        register(new WaypointModule());
    }

    private void register(Module m) { modules.add(m); }
    public List<Module> getModules() { return modules; }

    public Module getByName(String name) {
        for (Module m : modules)
            if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public void onKey(int key) {
        for (Module m : modules)
            if (m.getKeybind() == key) m.toggle();
    }

    public void onTick() {
        for (Module m : modules)
            if (m.isEnabled()) m.onTick();
    }

    public void onRender(int width, int height) {
        for (Module m : modules)
            if (m.isEnabled()) m.onRender(width, height);
    }
}
