package com.sanjay900.jgbe.bukkit;

import io.netty.channel.Channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.minecraft.server.v1_8_R3.MapIcon;
import net.minecraft.server.v1_8_R3.PacketPlayOutMap;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.json.JSONException;
import org.json.JSONObject;

import com.sanjay900.jgbe.emu.CPU;
import com.sanjay900.jgbe.emu.VideoController;
import com.sanjay900.jgbe.emu.swinggui;

public class ScreenDrawer implements Runnable{
	public volatile boolean shutdown = false;
	protected static Logger logger = Logger.getLogger("gameboy");
	private final List<MapIcon> empty = Collections.unmodifiableList(new ArrayList<MapIcon>());
	int serverId = Bukkit.getServer().getPort()-25580;
	swinggui jb;
	public ScreenDrawer(swinggui swinggui) {
		this.jb = swinggui;
		MapHelper.removeRenderers(Bukkit.getMap((short) 0));
		MapHelper.removeRenderers(Bukkit.getMap((short) 1));
		MapHelper.removeRenderers(Bukkit.getMap((short) 4));
		MapHelper.removeRenderers(Bukkit.getMap((short) 5));
		Bukkit.getMap((short) 0).addRenderer(new GameboyRenderer());
		Bukkit.getMap((short) 1).addRenderer(new GameboyRenderer());
		Bukkit.getMap((short) 4).addRenderer(new GameboyRenderer());
		Bukkit.getMap((short) 5).addRenderer(new GameboyRenderer());
	}
	//Spacing from sides
	int ysp = 56;
	int xsp = 48;
	//How many pixels the image takes
	int xamt=80;
	int yamt=72;
	//size
	int width = 160;
	int height = 144;
	@Override
	public void run() {
		while (!shutdown){
			if (CPU.cartridge != null && VideoController.getImage() != null) {

				//int[] pixels = new int[width*height];
				//top left
				byte[] data0 = new byte[128 * 128];
				//top right
				byte[] data1 = new byte[128 * 128];
				//bottom left
				byte[] data2 = new byte[128 * 128];
				//bottom right
				byte[] data3 = new byte[128 * 128];
				for (int x = 0; x<128; x++) {
					for (int y = 0; y<128; y++) {
						if (x < xsp) {
							data0[y * 128 + x] = 0;
						} else {
							if (y < ysp) {
								data0[y * 128 + x] = 0;
							} else {
								//int i = VideoController.getImage().getRGB(x-xsp, y-ysp);
								//pixels[(y-ysp)*width+x-xsp] = i;
								data0[y * 128 + x]=MapHelper.matchColor(VideoController.getImage().getRGB(x-xsp, y-ysp));
							}
						}
						if (x >= xamt) {
							data1[y * 128 + x] = 0;
						} else {
							if (y < ysp) {
								data1[y * 128 + x] = 0;
							} else {
								//int i = VideoController.getImage().getRGB(xamt+x, y-ysp);
								//pixels[(y-ysp)*width+xamt+x] = i;
								data1[y * 128 + x]=MapHelper.matchColor(VideoController.getImage().getRGB(xamt+x, y-ysp));
							}
						}
						if (x < xsp) {
							data2[y * 128 + x] = 0;
						} else {
							if (y >= yamt) {
								data2[y * 128 + x] = 0;
							} else {
								//int i = VideoController.getImage().getRGB(x-xsp, y+yamt);
								//pixels[(y+yamt)*width+x-xsp] = i;
								data2[y * 128 + x]=MapHelper.matchColor(VideoController.getImage().getRGB(x-xsp, y+yamt));
							}
						}
						if (x >= xamt || y >= yamt) {
							data3[y * 128 + x] = 0;
						} else {
							//int i = VideoController.getImage().getRGB(xamt+x, yamt+y);
							//pixels[(y+yamt)*width+xamt+x] = i;
							data3[y * 128 + x] =MapHelper.matchColor(VideoController.getImage().getRGB(xamt+x, yamt+y));
						}
					}
				}

				PacketPlayOutMap[] packets = new PacketPlayOutMap[4];
				packets[0] = new PacketPlayOutMap(0, MapView.Scale.FARTHEST.getValue(), empty, data0, 0, 0, 128, 128);
				packets[1] = new PacketPlayOutMap(1, MapView.Scale.FARTHEST.getValue(), empty, data1, 0, 0, 128, 128);
				packets[2] = new PacketPlayOutMap(4, MapView.Scale.FARTHEST.getValue(), empty, data2, 0, 0, 128, 128);
				packets[3] = new PacketPlayOutMap(5, MapView.Scale.FARTHEST.getValue(), empty, data3, 0, 0, 128, 128);
				Bukkit.getOnlinePlayers().forEach(player -> sendPackets(packets,player));
				try {		
					ImageIO.setUseCache(false);
					ByteArrayOutputStream baos = new ByteArrayOutputStream ();
					ImageIO.write(VideoController.getImage(),"gif",baos);
					JSONObject obj = new JSONObject();
					obj.put("pic", Base64.encodeBase64String(baos.toByteArray()));
					jb.socketio.getHandler().sendGlobalMessage("gbpic"+serverId, obj);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try{
				//Thread.sleep(delay);
				Thread.sleep(33);
				// LET'S BE CRAZY! LET'S DO 30/29.97FPS!
			} catch (InterruptedException e){
				logger.log(Level.WARNING, "Something told us to wake up before the animation ended!");
			}
		}
	}
	public static byte[] int2byte(int[]src) {
	    int srcLength = src.length;
	    byte[]dst = new byte[srcLength << 2];
	    
	    for (int i=0; i<srcLength; i++) {
	        int x = src[i];
	        int j = i << 2;
	        dst[j++] = (byte) ((x >>> 0) & 0xff);           
	        dst[j++] = (byte) ((x >>> 8) & 0xff);
	        dst[j++] = (byte) ((x >>> 16) & 0xff);
	        dst[j++] = (byte) ((x >>> 24) & 0xff);
	    }
	    return dst;
	}


	public void sendPackets(PacketPlayOutMap[] packets, Player player) {
		Channel ch = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
		if (ch.isOpen()) {
			ch.flush();
			Arrays.asList(packets).forEach(ch::write);
			ch.flush();
		}
	}





	public void shutdown() {
		this.shutdown = true;
	}

}
