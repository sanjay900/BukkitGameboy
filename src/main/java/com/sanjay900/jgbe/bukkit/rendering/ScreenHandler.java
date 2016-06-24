package com.sanjay900.jgbe.bukkit.rendering;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.sanjay900.jgbe.bukkit.GameboyRenderer;
import com.sanjay900.jgbe.bukkit.MapHelper;
import com.sanjay900.jgbe.particles.ReflectionUtils;
import io.netty.channel.Channel;
import net.minecraft.server.v1_10_R1.MapIcon;
import net.minecraft.server.v1_10_R1.PacketPlayOutMap;
import net.tangentmc.nmsUtils.utils.ReflectionManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScreenHandler {
    public List<PacketContainer> soundPackets = new ArrayList<>();
    //Spacing from sides
    int ysp = 56;
    int xsp = 48;
    //How many pixels the image takes
    int xamt = 80;
    int yamt = 72;
    byte[][] data = new byte[4][xamt * yamt];
    PacketPlayOutMap[] pcs = new PacketPlayOutMap[4];
    public ScreenHandler() {
        MapHelper.removeRenderers(Bukkit.getMap((short) 0));
        MapHelper.removeRenderers(Bukkit.getMap((short) 1));
        MapHelper.removeRenderers(Bukkit.getMap((short) 2));
        MapHelper.removeRenderers(Bukkit.getMap((short) 3));
        Bukkit.getMap((short) 0).addRenderer(new GameboyRenderer());
        Bukkit.getMap((short) 1).addRenderer(new GameboyRenderer());
        Bukkit.getMap((short) 2).addRenderer(new GameboyRenderer());
        Bukkit.getMap((short) 3).addRenderer(new GameboyRenderer());
        createPacket(0, xsp, ysp);
        createPacket(1, 0, ysp);
        createPacket(2, xsp, 0);
        createPacket(3, 0, 0);
    }

    /**
     * Create a packet, with its data array pointing to data[id];
     * The size is set to xamt,yamt and map icons are disabled
     *
     * @param id   the id in the array and the id of the map
     * @param left the left-most position
     * @param top  the right-most position
     */
    private void createPacket(int id, int left, int top) {
        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MAP);
        StructureModifier<Integer> integers = pc.getIntegers();
        pc.getBytes().write(0, MapView.Scale.FARTHEST.getValue());
        pc.getBooleans().write(0, false);
        //object 3 = private MapIcon[] d;
        pc.getModifier().write(3, new MapIcon[]{});
        integers.write(0, id);
        integers.write(1, left);
        integers.write(2, top);
        integers.write(3, xamt);
        integers.write(4, yamt);
        pc.getByteArrays().write(0, data[id]);
        pcs[id] = (PacketPlayOutMap) pc.getHandle();
    }

    /**
     * Render an image to map and stream
     *
     * @param blit          the backlit image to render on 4 maps
     * @param bufferedImage the bufferedImage to stream
     */
    public void drawImage(byte[][] blit, BufferedImage bufferedImage) {
        //TODO: Handle streaming to socketio here. Think about using zlib or something
        //Copy sections of blit to their respective packets
        for (int y = 0; y < yamt; y++) {
            System.arraycopy(blit[y], 0, data[0], xamt*y, xamt);
            System.arraycopy(blit[y], xamt, data[1], xamt*y, xamt);
            System.arraycopy(blit[y + yamt], 0, data[2], xamt*y, xamt);
            System.arraycopy(blit[y + yamt], xamt, data[3], xamt*y, xamt);
        }
        Bukkit.getOnlinePlayers().forEach(this::sendPackets);
    }

    public void sendPackets(Player player) {
        Channel ch = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        if (ch.isOpen()) {
            ch.flush();
            Arrays.asList(pcs).forEach(ch::write);
            soundPackets.forEach(ch::write);
            soundPackets.clear();
            ch.flush();
        }
    }
}
