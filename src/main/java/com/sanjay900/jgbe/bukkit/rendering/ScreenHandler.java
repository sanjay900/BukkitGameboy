package com.sanjay900.jgbe.bukkit.rendering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

import io.netty.channel.Channel;
import net.minecraft.server.v1_9_R1.MapIcon;
import net.minecraft.server.v1_9_R1.Packet;
import net.minecraft.server.v1_9_R1.PacketPlayOutMap;

public class ScreenHandler {
	private final List<MapIcon> empty = Collections.unmodifiableList(new ArrayList<MapIcon>());
	public List<Packet<?>> soundPackets = new ArrayList<>();
	//top left
	byte[] data0 = new byte[128 * 128];
	//top right
	byte[] data1 = new byte[128 * 128];
	//bottom left
	byte[] data2 = new byte[128 * 128];
	//bottom right
	byte[] data3 = new byte[128 * 128];
	//Spacing from sides
	int ysp = 56;
	int xsp = 48;
	//How many pixels the image takes
	int xamt=80;
	int yamt=72;
	//size
	int width = 160;
	int height = 144;
	public ScreenHandler() {
		for (int x = 0; x<128; x++) {
			for (int y = 0; y<128; y++) {
				data0[y * 128 + x] = 0;
				data1[y * 128 + x] = 0;
				data2[y * 128 + x] = 0;
				data3[y * 128 + x] = 0;
			}
		}
		
	}
	public void drawImage(int[][] blit) {
		for (int x = 0; x<128; x++) {
			for (int y = 0; y<128; y++) {
				if (x > xsp && y > ysp) {
					data0[y * 128 + x]=(byte)blit[y-ysp][x-xsp];
				}
				if (x < xamt && y > ysp) {
					data1[y * 128 + x]=(byte)blit[y-ysp][xamt+x];
				}
				if (x > xsp && y < yamt) {
					data2[y * 128 + x]=(byte)blit[y+yamt][x-xsp];
				}
				if (x < xamt && y < yamt) {
					data3[y * 128 + x]=(byte)blit[yamt+y][xamt+x];
				}
			}
		}

		PacketPlayOutMap[] packets = new PacketPlayOutMap[4];
		packets[0] = new PacketPlayOutMap(0, MapView.Scale.FARTHEST.getValue(), false, empty, data0, 0, 0, 128, 128);
		packets[1] = new PacketPlayOutMap(1, MapView.Scale.FARTHEST.getValue(), false, empty, data1, 0, 0, 128, 128);
		packets[2] = new PacketPlayOutMap(2, MapView.Scale.FARTHEST.getValue(), false, empty, data2, 0, 0, 128, 128);
		packets[3] = new PacketPlayOutMap(3, MapView.Scale.FARTHEST.getValue(), false, empty, data3, 0, 0, 128, 128);
		Bukkit.getOnlinePlayers().forEach(player -> sendPackets(packets,player));
	}
	public void sendPackets(PacketPlayOutMap[] packets, Player player) {
		Channel ch = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
		if (ch.isOpen()) {
			ch.flush();
			Arrays.asList(packets).forEach(ch::write);
			Arrays.asList(soundPackets).forEach(ch::write);
			soundPackets.clear();
			ch.flush();
		}
	}
}
