package com.sanjay900.jgbe.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import com.sanjay900.jgbe.emu.swinggui;
import com.sanjay900.menus.api.IconCallback;

public class ServerCallback extends IconCallback{

	private int serverId;
	private swinggui jb = swinggui.getInstance();

	public ServerCallback(int serverId) {
		this.serverId = serverId;
	}

	@Override
	public void run(Player player) {
		jb.socketio.acceptPlayer = player.getUniqueId();
		String server = String.valueOf(serverId);
		JSONObject obj = new JSONObject();
		JSONObject innerObject = new JSONObject();
		try {
		innerObject.put("serverid", Bukkit.getServer().getPort()-25580);
		innerObject.put("userName", player.getName());
		obj.put("eventName", "linkrequest");
		obj.put("server","gba"+server);
		obj.put("object", innerObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		jb.socketio.client.emit("send", obj);

		
		}

}
