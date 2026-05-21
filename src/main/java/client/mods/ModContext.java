package client.mods;

import java.util.function.Consumer;

public interface ModContext {
    String modId();

    client.FontRenderer font();

    // chat    
    // registers a / (slash) command
    void registerCommand(String name, ChatCommand handler);
    // prints to client-side chat
    void chatLocal(String message);

    // HUD
    void registerHud(HudRenderer renderer);

    // ticks
    void registerTick(Runnable onTick);

    // keybinds
    void registerKeybind(int key, Runnable onPress);

    // events
    <E extends ModEvent> void addListener(Class<E> type, Consumer<E> listener);
}