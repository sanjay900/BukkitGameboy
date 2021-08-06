package com.sanjay900.jgbe.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.sanjay900.jgbe.bukkit.rendering.ScreenHandler;
import com.sanjay900.jgbe.converters.*;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.Cartridge;
import com.sanjay900.jgbe.emu.FHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class GameboyPlayer {

    public ScreenHandler screenthread;
    GameboyPlugin plugin = GameboyPlugin.getInstance();
    UUID player;
    @Getter
    String playerName;
    public Converter conv;
    public Scoreboard board;
    @Getter
    private String curcartname;
    private boolean running;

    @Getter
    private CPU cpu;
    private int asid;

    private void stealPlayerControls(Location loc, Player who) {
        if (loc != null) {
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc.add(0, -0.9875, 0), EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setSmall(true);
            as.addPassenger(who);
            as.setGravity(false);
            asid = as.getEntityId();
        }
    }


    public void updateCartName(String fname) {
        int sepPos = fname.lastIndexOf(File.separator);
        int slashPos = fname.lastIndexOf('/');
        if (sepPos > slashPos) slashPos = sepPos;

        int dotPos = fname.lastIndexOf(".");
        if (dotPos == -1) dotPos = fname.length();
        curcartname = fname.substring(slashPos + 1, dotPos);
    }

    private void tryToLoadROM(String filename) {
        Cartridge tcart = new Cartridge(filename, false);
        tcart.loadBios("");
        String[] messages = {"[Missing an error message here]"};
        switch (tcart.getStatus(messages)) {
            case Cartridge.STATUS_NONFATAL_ERROR: {
                System.out.println("WARNING:\n" + messages[0]);
            }
            case Cartridge.STATUS_OK: {
                cpu.loadCartridge(tcart);
            }
            break;
            default: {
                System.out.println("There was an error loading this ROM!\n(" + messages[0] + ")");
            }
            break;
        }
        cpu.reset(false);
    }

    GameboyPlayer(final Player p, final String rom) {
        updateCartName(rom);
        player = p.getUniqueId();
        playerName = p.getName();
        screenthread = new ScreenHandler(p);
        cpu = new CPU(this, p, screenthread);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setGameMode(GameMode.SURVIVAL);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player = p.getUniqueId();
            giveItems(p,false);
            p.sendMessage(ChatColor.YELLOW + "Controls: Jump for A, Sneak for B, Hold the clock and Mine for start, Interact for select, and move for directionals.");
            Location loc = new Location(Bukkit.getWorld("gba"), 47, 46, 46);
            loc = loc.setDirection(new Vector(0, 0, 1));
            p.teleport(loc);
            stealPlayerControls(loc, p);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                try {
                    //TODO stop this being hardcoded.
                    String filename = plugin.getDataFolder().getAbsolutePath() + "/roms/" + rom;
                    tryToLoadROM(filename);
                    loadSave();

                    Bukkit.getScheduler().runTaskLater(GameboyPlugin.getInstance(), () -> {
                        board = Bukkit.getScoreboardManager().getNewScoreboard();

                        p.setScoreboard(board);
                        if (rom.contains("red") || rom.contains("blue"))
                            conv = new RedBlueConverter(GameboyPlayer.this);
                        if (rom.contains("yellow"))
                            conv = new YellowConverter(GameboyPlayer.this);
                        if (rom.contains("crystal"))
                            conv = new CrystalConverter(GameboyPlayer.this);
                        if (rom.contains("land.gb"))
                            conv = new MarioLand1Converter(GameboyPlayer.this);
                        if (rom.contains("bomb"))
                            conv = new BombermanConverter(GameboyPlayer.this);
                    }, 5L);
                    startGame();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, 7L);
        }, 20L);


    }

    public static void giveItems(Player p, boolean onlyExit) {
        ItemStack item = new ItemStack(Material.CLOCK,
                1);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GOLD
                + "Start/Select");
        item.setItemMeta(itemMeta);

        ItemStack stuck = new ItemStack(
                Material.REDSTONE, 1);
        ItemMeta stuckMeta = item.getItemMeta();
        stuckMeta.setDisplayName(ChatColor.GOLD
                + "Connect to another Gameboy");
        stuck.setItemMeta(stuckMeta);
        ItemStack exit = new ItemStack(
                Material.BARRIER, 1);
        ItemMeta exitMeta = item.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RED
                + "Quit Game");
        exit.setItemMeta(exitMeta);
        p.getInventory().clear();
        if (!onlyExit) {
            p.getInventory().setItem(0, item);
            p.getInventory().setItem(2, stuck);
        }
        p.getInventory().setItem(8, exit);
        p.getInventory().setHeldItemSlot(0);
    }

    private void startGame() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, cpu::runloop);
    }

    private File getSaveFile() {
        return new File(plugin.getDataFolder().getAbsolutePath() + "/saves/" + player.toString() + "_" + curcartname + ".st");
    }

    public void pause() {
        cpu.runlooponce();
    }

    public void resume() {
        if (!cpu.isRunning()) {
            startGame();
        }
    }

    public void reset() {
        cpu.runlooponce();
        cpu.reset(false);
        startGame();
    }

    @SneakyThrows
    public boolean loadSave() {
        File f = getSaveFile();
        if (f.exists()) {
            boolean running = cpu.isRunning();
            if (running) {
                cpu.runlooponce();
            }
            DataInputStream distream = FHandler.getDataInputStream(f.getAbsolutePath());
            cpu.loadState(distream);
            distream.close();
            if (running) {
                startGame();
            }
        }
        return f.exists();
    }

    @SneakyThrows
    public void save() {
        boolean resume = cpu.isRunning();
        if (resume) {
            cpu.runlooponce();
        }
        DataOutputStream dostream = FHandler.getDataOutputStream(getSaveFile().getAbsolutePath());
        cpu.saveState(dostream);
        dostream.close();
        if (resume) {
            Bukkit.getPlayer(player).sendMessage("Saved State!");
            startGame();
        }
    }

    public void press(boolean b, int i) {
        if (b) {
            cpu.pressButton(keyMasks[i]);
        } else {
            cpu.releaseButton(keyMasks[i]);
        }
    }

    public void keyToggled(Key key, boolean pressed) {
        if (cpu.cartridge == null) return;
        switch (key) {
            case UP:
                press(pressed, 0);
                break;
            case DOWN:
                press(pressed, 1);
                break;
            case LEFT:
                press(pressed, 2);
                break;
            case RIGHT:
                press(pressed, 3);
                break;
            case JUMP:
                press(pressed, 4);
                break;
            case UNMOUNT:
//                1.16 is stupid. Is this whole thing actually necessary anymore though, i think you can listen for all this stuff without the armorstand these dayscepti
                if (pressed) {
                    Bukkit.getScheduler().runTaskLater(GameboyPlugin.getInstance(), ()->{
                        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MOUNT);
                        pc.getIntegers().write(0, asid);
                        pc.getIntegerArrays().write(0, new int[]{Bukkit.getPlayer(player).getEntityId()});
                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(Bukkit.getPlayer(player), pc);
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    },5);
                }
                press(pressed, 5);
                break;
            case OPEN_INVENTORY:
                press(pressed, 7);
                break;
            case DROP_ITEM:
                press(pressed, 6);
                break;
            case BREAK:
                break;
            case PLACE:
                break;
            default:
                break;

        }
    }

    public void stopEmulation() {
        //Kill the connection and pause emulation. Your not going to restore a connected snapshot
        cpu.severLink();
        cpu.runlooponce();
        Bukkit.getScheduler().runTaskLater(plugin, this::save, 20L);
        plugin.stopPlayer(this);
    }

    static final int[] keyMasks = {
            (1 << 2),
            (1 << 3),
            (1 << 1),
            (1 << 0),
            (1 << 4),
            (1 << 5),
            (1 << 7),
            (1 << 6),
    };

    public boolean canBeLinked() {
        return !cpu.isConnected();
    }


    public void link(GameboyPlayer game) {
        cpu.serveLink();
        game.cpu.clientLink(cpu);
    }
}
