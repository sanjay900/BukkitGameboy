package com.sanjay900.jgbe.bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import com.sanjay900.jgbe.converters.PokemonConverter;
import com.sanjay900.jgbe.converters.PokemonConverter.dataType;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.FHandler;
import com.sanjay900.jgbe.emu.swinggui;

public class CommandHandler implements CommandExecutor{
	swinggui jb = swinggui.getInstance();
	public CommandHandler() {
	}
	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
		if (args.length == 0) {
			return false;
		}
		try {

			switch (args[0]) {
			case "getPokemon":
				if (sender.isOp() && jb.gp != null && jb.gp.conv instanceof PokemonConverter) {
					PokemonConverter pc = (PokemonConverter) jb.gp.conv;
					sender.sendMessage("Pokemon Captured: "+pc.getCapacity());
					for (int i =0; i< pc.getCapacity(); i++) {
						sender.sendMessage("Pokemon "+(i+1)+":");
						sender.sendMessage("Name: "+pc.get(i, dataType.name));
						sender.sendMessage("Species: "+pc.getSpecies(i));
						sender.sendMessage("Health:"+pc.get(i, dataType.hp));
						sender.sendMessage("Max Health:"+pc.get(i, dataType.maxhp));
						sender.sendMessage("Experience:"+pc.get(i, dataType.exp));
					}
				}
				break;
			case "loadrom":
				if (!sender.isOp()) return false;
				if (jb.gp != null) {
					CPU.severLink();
				}
				jb.gp = new GameboyPlayer((Player)sender,args[1]);
				break;



			case "save":
				if (!CPU.canRun()) {
					sender.sendMessage("You need to start a game before you can save one!"); 
					return true;
				
				}
				swinggui.pauseEmulation(false);
				DataOutputStream dostream = FHandler.getDataOutputStream(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+((Player)sender).getUniqueId().toString()+"_"+swinggui.curcartname+".st");
				CPU.saveState(dostream);
				dostream.close();
				sender.sendMessage("Saved State!");
				swinggui.resumeEmulation(false);
				return true;

			case "load":
				if (!CPU.canRun()) {
					sender.sendMessage("You need to start a game before you can load one!"); 
					return true;
				
				}
				swinggui.pauseEmulation(false);
				File f = new File(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/saves/"+((Player)sender).getUniqueId().toString()+"_"+swinggui.curcartname+".st");
				if (!f.exists()) {
					sender.sendMessage("You dont have a save to load!"); 
					return true;
				}
				DataInputStream distream = FHandler.getDataInputStream(jb.getDataFolder().getAbsolutePath()+"/../../../gameboy/roms/"+((Player)sender).getUniqueId().toString()+"_"+swinggui.curcartname+".st");
				CPU.loadState(distream);
				distream.close();
				swinggui.resumeEmulation(false);
				sender.sendMessage("Loaded State!");
				return true;
			case "pause":
				if (!CPU.canRun()) {
					sender.sendMessage("You need to start a game before you can pause one!"); 
					return true;
				
				}
				sender.sendMessage("Paused Game!");
				swinggui.pauseEmulation(false);
				return true;
			case "resume":
				if (!CPU.canRun()) {
					sender.sendMessage("You need to start a game before you can resume one!"); 
					return true;
				
				}
				sender.sendMessage("Resumed Game!");
				swinggui.resumeEmulation(false);
				return true;
			case "reset":
				swinggui.pauseEmulation(false);
				CPU.reset(false);

				swinggui.resumeEmulation(false);
				sender.sendMessage("Reset Game!");
				return true;
			}
			if (args[0].equalsIgnoreCase("accept")) {
				if (jb.socketio.acceptServer == -1) {
					sender.sendMessage(ChatColor.AQUA+"You don't have any Link requests right now.");
					return true;
				}

				sender.sendMessage(ChatColor.AQUA+"Accepted Link Request!");
				JSONObject obj = new JSONObject();
				JSONObject innerObject = new JSONObject();

				innerObject.put("accepted", true);
				innerObject.put("serverid", Bukkit.getServer().getPort()-25580);
				obj.put("eventName", "linkstatus");
				obj.put("server","gba"+jb.socketio.acceptServer);
				obj.put("object", innerObject);

				jb.socketio.client.emit("send", obj);

				jb.socketio.acceptServer = -1;
				return true;
			} else if (args[0].equalsIgnoreCase("deny")) {
				if (jb.socketio.acceptServer == -1) {
					sender.sendMessage(ChatColor.AQUA+"You don't have any Link requests right now.");
					return true;
				}

				sender.sendMessage(ChatColor.AQUA+"Denied Link Request!");
				JSONObject obj = new JSONObject();
				JSONObject innerObject = new JSONObject();
				innerObject.put("accepted", false);
				innerObject.put("serverid", Bukkit.getServer().getPort()-25580);
				obj.put("eventName", "linkstatus");
				obj.put("server","gba"+jb.socketio.acceptServer);
				obj.put("object", innerObject);
				jb.socketio.client.emit("send", obj);

				jb.socketio.acceptServer = -1;
				return true;
			}
			return false;


		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

}

