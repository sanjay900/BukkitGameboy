package com.sanjay900.jgbe.bukkit;

/*public class ServerCallback extends IconCallback{

	private int serverId;
	private GameboyPlugin plugin = GameboyPlugin.getInstance();

	public ServerCallback(int serverId) {
		this.serverId = serverId;
	}

	@Override
	public void run(Player player) {
		plugin.socketio.acceptPlayer = player.getUniqueId();
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
		plugin.socketio.client.emit("send", obj);

		
		}

}*/
