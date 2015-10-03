package com.sanjay900.jgbe.bukkit;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.xml.bind.DatatypeConverter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.json.JSONException;
import org.json.JSONObject;

import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.VideoController;
import com.sanjay900.jgbe.emu.swinggui;

public class ScreenDrawer implements Runnable{
	public volatile boolean shutdown = false;
	swinggui plugin;
	ArmorStand[] holograml = new ArmorStand[144];
	ArmorStand[] hologramr= new ArmorStand[144];
	public ScreenDrawer(swinggui swinggui) {
		
		this.plugin = swinggui;
		Location l = new Location(Bukkit.getWorld("gba"), 77.0, 150.0, 30.0);
		Location r = new Location(Bukkit.getWorld("gba"), 96.2, 150.0, 30.0);
		for (int i = 0; i< 144; i++)  {
			holograml[i]=(ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
			hologramr[i]=(ArmorStand) r.getWorld().spawnEntity(r, EntityType.ARMOR_STAND);
			holograml[i].setGravity(false);
			hologramr[i].setGravity(false);
			holograml[i].setCustomNameVisible(true);
			hologramr[i].setCustomNameVisible(true);
			l = l.subtract(0, 0.25, 0);
			r = r.subtract(0, 0.25, 0);
		}
	}
	@Override
	public void run() {
		if (!shutdown && CPU.cartridge != null && VideoController.getImage() != null) {
			//System.out.println(VideoController.averageFPS);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				DataOutputStream ds = new DataOutputStream(os);			
				

				ImageMessage msg = new ImageMessage(VideoController.getImage(), ds);
				for (int i = 0; i < msg.getLines().linesl.length; ++i) {
					holograml[i].setCustomName(msg.getLines().linesl[i]);
					hologramr[i].setCustomName(msg.getLines().linesr[i]);
				}
				int serverId = Bukkit.getServer().getPort()-25580;
				JSONObject obj = new JSONObject();
				JSONObject innerObject = new JSONObject();
				innerObject.put("pic", DatatypeConverter.printBase64Binary(os.toByteArray()));
				obj.put("eventName", "gbpic"+serverId);
				obj.put("object", innerObject);
				//plugin.socketio.client.emit("replayObject", obj);
				ds.close();
				os.close();
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}

		}


	}






	public void shutdown() {
		for (ArmorStand s: holograml) {
			s.remove();
		}
		for (ArmorStand s: hologramr) {
			s.remove();
		}
		this.shutdown = true;
	}

}
