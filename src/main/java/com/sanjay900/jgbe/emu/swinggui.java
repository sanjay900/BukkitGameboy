package com.sanjay900.jgbe.emu;

import java.io.DataOutputStream;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.sanjay900.jgbe.bukkit.CommandHandler;
import com.sanjay900.jgbe.bukkit.GameboyPlayer;
import com.sanjay900.jgbe.bukkit.GameboyRenderer;
import com.sanjay900.jgbe.bukkit.JavaBoyEventHandler;
import com.sanjay900.jgbe.bukkit.MapHelper;
import com.sanjay900.jgbe.bukkit.ScreenDrawer;
import com.sanjay900.jgbe.bukkit.SocketIo;
import com.sanjay900.nmsUtil.NMSUtil;

import lombok.Getter;
class SimpleCPURunner implements CPURunner, Runnable {
	private volatile int threadStatus = 0;
	private Thread cpurunthread;

	public boolean hasThread(Thread t) {
		return cpurunthread.equals(t);
	}

	synchronized public void suspend() {
		while (threadStatus != 0) {
			CPU.keeprunning = false;
			threadStatus = 3;
			while (threadStatus == 3) { { try { Thread.sleep(100); } catch (Exception e) { } }; };
		}
	}

	synchronized public void resume() {
		if (!CPU.canRun()) return;
		if (threadStatus != 2) {
			threadStatus = 1;
			while (threadStatus == 1) { { try { Thread.sleep(100); } catch (Exception e) { } }; };
		}
	}

	public boolean isRunning() {
		return (threadStatus != 0);
	}

	SimpleCPURunner() {
		cpurunthread = new Thread(this);
		cpurunthread.start();
		while (!cpurunthread.isAlive()) { { try { Thread.sleep(100); } catch (Exception e) { } }; };
	}

	public void run() {
		while (true) {
			while (threadStatus == 0) { { try { Thread.sleep(100); } catch (Exception e) { } }; };


			if (threadStatus == 1) threadStatus = 2;
			CPU.runloop();

			if (threadStatus == 2) {
				threadStatus = 3;
				System.out.println("Gameboy: Encountered an invalid instruction, prehaps the ROM is broken?");
			}
			if (threadStatus == 3) threadStatus = 0;

		}
	}
}
public class swinggui extends JavaPlugin {
	@Getter
	static swinggui instance;
	public ScreenDrawer screenthread;
	public SocketIo socketio;
	public NMSUtil nmsutil;
	public JavaBoyEventHandler jb;
	public GameboyPlayer gp = null;
	static Cartridge cart = null; 
	public CPU cpu;
	static public String curcartname;
	static public String biosfilename;
	static public DataOutputStream speedRunPlayWithOutputVideoStream;
	static CPURunner cpuRunner;
	@Override
	public void onEnable() {
		instance = this;
		
		cpu = new CPU();
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		nmsutil = (NMSUtil) Bukkit.getPluginManager().getPlugin("nmsUtils");
		this.socketio = new SocketIo();
		this.screenthread = new ScreenDrawer(this);
		Bukkit.getScheduler().runTaskAsynchronously(this, screenthread);
		this.getCommand("gameboy").setExecutor(new CommandHandler());
		jb = new JavaBoyEventHandler();
		cpuRunner = new SimpleCPURunner();
	    resumeEmulation(false);
	    Bukkit.getWorld("gba").setAutoSave(false);
	}
	@Override
	public void onDisable() {
		cpu.severLink();
		screenthread.shutdown();
	}
	public void tryToLoadROM(String filename) {
		pauseEmulation(false);
		Cartridge tcart=new Cartridge(filename);
		tcart.loadBios("");
		String[] messages = { "[Missing an error message here]" };
		switch(tcart.getStatus(messages)) {
		case Cartridge.STATUS_NONFATAL_ERROR: {
			System.out.println("WARNING:\n"+messages[0]);
		}
		case Cartridge.STATUS_OK: {
			cart = tcart;
			CPU.loadCartridge(cart);
			updateCartName(filename);
			System.out.println("loaded Rom: " + curcartname);
		} break;
		default: {
			System.out.println("There was an error loading this ROM!\n("+messages[0]+")");
		} break;
		}
		cpu.reset(false);
		resumeEmulation(false);
	}
	public void updateCartName(String fname) {
		   int sepPos = fname.lastIndexOf(File.separator);
		   int slashPos = fname.lastIndexOf('/');
		   if (sepPos > slashPos) slashPos = sepPos;

		   int dotPos = fname.lastIndexOf(".");
		   if (dotPos == -1) dotPos = fname.length();
		   curcartname = fname.substring(slashPos+1, dotPos);
		  }
	static public void pauseEmulation(boolean verbose) {
		if(cpuRunner!=null) {
			Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {cpuRunner.suspend();});
			
			System.out.println("Paused gameboy");
		}
	}

	static public void resumeEmulation(boolean verbose) {
		if((cpuRunner!=null)&&(cart!=null)) {
			Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {cpuRunner.resume();});
			System.out.println("Unpaused gameboy");
		}
	}
}
