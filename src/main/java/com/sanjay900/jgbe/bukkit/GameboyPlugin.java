package com.sanjay900.jgbe.bukkit;

import com.sanjay900.jgbe.bukkit.rendering.ScreenHandler;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.CPURunner;
import com.sanjay900.jgbe.emu.Cartridge;
import lombok.Getter;
import net.tangentmc.nmsUtils.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class GameboyPlugin extends JavaPlugin {
    @Getter
    static GameboyPlugin instance;
    public ScreenHandler screenthread;
    public SocketIo socketio;
    public NMSUtils nmsutil;
    public JavaBoyEventHandler plugin;
    public GameboyPlayer gp = null;
    static Cartridge cart = null;
    public CPU cpu;
    static public String curcartname;
    static public String biosfilename;
    static CPURunner cpuRunner;
    @Getter
    String version = "1.0";

    @Override
    public void onEnable() {
        instance = this;

        screenthread = new ScreenHandler();
        cpu = new CPU();
        cpu.init();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        nmsutil = NMSUtils.getInstance();
        this.socketio = new SocketIo();
        this.getCommand("gameboy").setExecutor(new CommandHandler());
        plugin = new JavaBoyEventHandler();
        cpuRunner = new SimpleCPURunner();
    }

    @Override
    public void onDisable() {
        cpu.severLink();
    }

    public void tryToLoadROM(String filename) {
        Cartridge tcart = new Cartridge(filename);
        tcart.loadBios("");
        String[] messages = {"[Missing an error message here]"};
        switch (tcart.getStatus(messages)) {
            case Cartridge.STATUS_NONFATAL_ERROR: {
                System.out.println("WARNING:\n" + messages[0]);
            }
            case Cartridge.STATUS_OK: {
                cart = tcart;
                cpu.loadCartridge(cart);
                updateCartName(filename);
                System.out.println("loaded Rom: " + curcartname);
            }
            break;
            default: {
                System.out.println("There was an error loading this ROM!\n(" + messages[0] + ")");
            }
            break;
        }
        cpu.reset(false);
    }

    public void updateCartName(String fname) {
        int sepPos = fname.lastIndexOf(File.separator);
        int slashPos = fname.lastIndexOf('/');
        if (sepPos > slashPos) slashPos = sepPos;

        int dotPos = fname.lastIndexOf(".");
        if (dotPos == -1) dotPos = fname.length();
        curcartname = fname.substring(slashPos + 1, dotPos);
    }

    static public void pauseEmulation(boolean verbose) {
        if (cpuRunner != null) {
            Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {
                cpuRunner.suspend();
            });

            System.out.println("Paused gameboy");
        }
    }

    static public void resumeEmulation(boolean verbose) {
        if ((cpuRunner != null) && (cart != null)) {
            Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {
                cpuRunner.resume();
            });
            System.out.println("Unpaused gameboy");
        }
    }
}

class SimpleCPURunner implements CPURunner, Runnable {
    private volatile int threadStatus = 0;
    private Thread cpurunthread;

    public boolean hasThread(Thread t) {
        return cpurunthread.equals(t);
    }

    synchronized public void suspend() {
        while (threadStatus != 0) {
            GameboyPlugin.getInstance().cpu.keeprunning = false;
            threadStatus = 3;
            while (threadStatus == 3) {
                {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    synchronized public void resume() {
        if (!GameboyPlugin.getInstance().cpu.canRun()) return;
        if (threadStatus != 2) {
            threadStatus = 1;
            while (threadStatus == 1) {
                {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public boolean isRunning() {
        return (threadStatus != 0);
    }

    SimpleCPURunner() {
        cpurunthread = new Thread(this);
        cpurunthread.start();
        while (!cpurunthread.isAlive()) {
            {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void run() {
        while (true) {
            while (threadStatus == 0) {
                {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                }
            }


            if (threadStatus == 1) threadStatus = 2;
            GameboyPlugin.getInstance().cpu.runloop();
            if (threadStatus == 2) {
                threadStatus = 3;
                System.out.println("Gameboy: Encountered an invalid instruction, prehaps the ROM is broken?");
            }
            if (threadStatus == 3) threadStatus = 0;

        }
    }
}
