package com.sanjay900.jgbe.bukkit;

import com.sanjay900.jgbe.emu.Cartridge;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class GameboyPlugin extends JavaPlugin {
    @Getter
    static GameboyPlugin instance;
    public JavaBoyEventHandler plugin;
    private Map<UUID, GameboyPlayer> games = new HashMap<>();
    @Getter
    String version = "1.0";

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getCommand("gameboy").setExecutor(new CommandHandler());
        plugin = new JavaBoyEventHandler();
    }

    @Override
    public void onDisable() {
        games.values().forEach(GameboyPlayer::stopEmulation);
    }

    public GameboyPlayer getPlayer(Player player) {
        return games.get(player.getUniqueId());
    }

    public void startPlaying(Player sender, String cart) {
        games.put(sender.getUniqueId(), new GameboyPlayer(sender, cart));
    }

    public void listRoms(Player player) {
        String filename = getDataFolder().getAbsolutePath()+"/roms/";
        for (String file : new File(filename).list()) {
            Cartridge tcart = new Cartridge(filename+file, true);
            TextComponent t = new TextComponent( new ComponentBuilder("Click to play: "+tcart.getTitle()).create());
            t.setUnderlined(true);
            t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gameboy loadrom "+file));
            t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Play "+tcart.getTitle()).create()));
            player.spigot().sendMessage(t);
        }
    }

    public void stopPlayer(GameboyPlayer p) {
        games.remove(p.player);
    }
    public Collection<GameboyPlayer> listGames() {
        return games.values();
    }
}