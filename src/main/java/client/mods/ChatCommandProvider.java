package client.mods;

public interface ChatCommandProvider extends ChatCommand {
    String name();

    @Override
    boolean handle(String args);
}