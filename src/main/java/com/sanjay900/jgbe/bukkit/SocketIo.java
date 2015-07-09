package com.sanjay900.jgbe.bukkit;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.sanjay900.eyrePlugin.EyrePlugin;
import com.sanjay900.eyrePlugin.sockets.SocketIOHandler;
import com.sanjay900.eyrePlugin.utils.Cooldown;
import com.sanjay900.jgbe.converters.PokemonConverter;
import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.swinggui;

public class SocketIo {

	public int acceptServer = -1;
	public UUID acceptPlayer = null;
	public HashMap<Integer,RemoteServerLink> servers = new HashMap<Integer, RemoteServerLink>();
	public Socket client;
	public swinggui plugin = swinggui.getInstance();
	public GameboyPlayer gp;
	private boolean broadcasting = false;
	@Getter
	private SocketIOHandler handler;
	public SocketIo() {

		int serverId = Bukkit.getServer().getPort()-25580;
		EyrePlugin pl = (EyrePlugin) Bukkit.getPluginManager().getPlugin("EyrePlugin");
		handler = pl.getHandler();
		client = pl.getHandler().client;
		if (client.connected()) {
			gameboyBroadcaster();
		} else {
			client.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					gameboyBroadcaster();
				}
			});
			
		}
		client.on("gbstatus", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject obj = (JSONObject)args[0];
				try {
					servers.put(obj.getInt("serverid"), new RemoteServerLink(obj.getString("userName"),obj.getString("cartName"),obj.getBoolean("isConnected")));
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
				if (plugin.jb != null)
					plugin.jb.updateLinkMenu();
			}
		});
		client.on("gba"+serverId, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				plugin.jb.cart = (String) args[0];
			}
		});
		client.on("linkrequest", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				if (CPU.player != null && Cooldown.tryCooldown(Bukkit.getPlayer(CPU.player), "tellraw", 100)) {
					JSONObject obj = (JSONObject)args[0];
					try {
						acceptServer = obj.getInt("serverid");
						String command = "tellraw {eplayername}  {\"text\":\"\",\"extra\":[{\"text\":\"Click to \",\"color\":\"aqua\"},{\"text\":\"Accept \",\"color\":\"green\",\"bold\":\"true\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/gameboy accept\"}},{\"text\":\"(/gb accept) \",\"color\":\"green\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/gb accept\"}},{\"text\":\"or \",\"color\":\"aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/gameboy deny\"}},{\"text\":\"Deny \",\"color\":\"red\",\"bold\":\"true\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/gameboy deny\"}},{\"text\":\"(/gb deny) \",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/gb deny\"}},{\"text\":\"a request to link with your gameboy from {playername} on {servername}\",\"color\":\"aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/gameboy deny\"}}]}";
						command = command.replace("{eplayername}", (Bukkit.getPlayer(CPU.player)).getName());
						command = command.replace("{playername}", obj.getString("userName"));
						command = command.replace("{servername}", "Server "+obj.getInt("serverid"));

						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
					} catch (JSONException ex) {

					}
				}
			}
		});
		client.on("linkstatus", new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				JSONObject obj = (JSONObject)args[0];
				try {
					if (obj.getBoolean("accepted")) {
						Bukkit.getPlayer(acceptPlayer).sendMessage("Your request was accepted by the other player!");
						if (CPU.isConnected())
							CPU.severLink();

						CPU.clientLink(obj.getInt("serverid"), Bukkit.getPlayer(acceptPlayer));
					} else {
						Bukkit.getPlayer(acceptPlayer).sendMessage("Your request was denied by the other player.");
					}
				} catch (JSONException | IOException ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	public void disconnect() {

		client.disconnect();
	}
	private void gameboyBroadcaster() {
		if (broadcasting) return;
		this.broadcasting = true;
		System.out.print("Connected to Socket.IO. Setting up Gameboy.");

		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				String game = "null";
				String player = "null";
				int serverId = Bukkit.getServer().getPort()-25580;
				int rmServerId = -1;
				boolean isConnected = false;
					player = CPU.player==null?"null":(Bukkit.getPlayer(CPU.player)).getName();
					isConnected = CPU.isConnected();
					if (CPU.isServer()) {
						rmServerId = CPU.clientN-35580;
					} else if (isConnected) {
						rmServerId = CPU.serverN-35580;
					}
				
				if (CPU.cartridge == null) {
					game = "null";
				} else {
					game = plugin.cpu.cartridge.getTitle();
				}
				try {
					JSONObject obj = new JSONObject();
					JSONObject obj2 = new JSONObject();
					JSONObject innerObject = new JSONObject();
					innerObject.put("serverid", serverId);
					innerObject.put("userName", player);
					innerObject.put("cartName", game);
					innerObject.put("isConnected", isConnected);
					innerObject.put("rmServerId", rmServerId);
					obj.put("eventName", "serverStatus"+serverId);
					obj.put("object", innerObject);
					obj2.put("eventName", "gbstatus");
					obj2.put("object", innerObject);
					client.emit("replayObject", obj);
					client.emit("replayObject", obj2);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (plugin.gp != null && plugin.gp.conv != null && plugin.gp.conv instanceof PokemonConverter){
					PokemonConverter conv = (PokemonConverter) plugin.gp.conv;
					//TODO: SOCKETIO ALL THE POKEMON THINGS!
				}

			}
		}, 5l, 20l);

	}
}
