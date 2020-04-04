package com.sanjay900.jgbe.bukkit.rendering;

import com.comphenix.packetwrapper.WrapperPlayServerMap;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.sanjay900.jgbe.bukkit.GameboyRenderer;
import com.sanjay900.jgbe.bukkit.MapHelper;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

public class ScreenHandler {
    //Spacing from sides
    int ysp = 56;
    int xsp = 48;
    //How many pixels the image takes
    int xamt = 80;
    int yamt = 72;
    byte[][] data = new byte[4][xamt * yamt];
    PacketContainer[] pcs = new PacketContainer[4];
    Player player;

    public ScreenHandler(Player player) {
        this.player = player;
        for (int i =0; i < 4; i++) {
            MapView m = Bukkit.getMap(i);
            MapHelper.removeRenderers(m);
            m.addRenderer(new GameboyRenderer());
        }
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
        WrapperPlayServerMap wpsm = new WrapperPlayServerMap();
        wpsm.setItemDamage(id);
        wpsm.setX(left);
        wpsm.setZ(top);
        wpsm.setColumns(xamt);
        wpsm.setRows(yamt);
        wpsm.setData(data[id]);
        wpsm.setScale(MapView.Scale.FARTHEST.getValue());
        pcs[id] = wpsm.getHandle();
    }

    /**
     * Render an image to map and stream
     *
     * @param blit          the backlit image to render on 4 maps
     */
    @SneakyThrows
    public void drawImage(byte[][] blit) {
        //Copy sections of blit to their respective packets
        for (int y = 0; y < yamt; y++) {
            System.arraycopy(blit[y], 0, data[0], xamt * y, xamt);
            System.arraycopy(blit[y], xamt, data[1], xamt * y, xamt);
            System.arraycopy(blit[y + yamt], 0, data[2], xamt * y, xamt);
            System.arraycopy(blit[y + yamt], xamt, data[3], xamt * y, xamt);
        }
        for (PacketContainer pc : pcs) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
        }
    }
}
