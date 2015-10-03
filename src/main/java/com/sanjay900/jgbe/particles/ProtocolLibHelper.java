package com.sanjay900.jgbe.particles;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Particle;

public class ProtocolLibHelper
{
    private final JavaPlugin plugin;
    private final boolean protocolLibInstalled;
    private ProtocolManager protocolManager;
    
    public ProtocolLibHelper(final JavaPlugin plugin) {
        super();
        this.plugin = plugin;
        this.protocolLibInstalled = (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null);
        if (this.canUseProtocolLib()) {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
        }
    }
    
    public void sendParticle(final Player player, final Location loc, Color c) {
        final PacketContainer particle = this.protocolManager.createPacket(PacketType.Play.Server.WORLD_PARTICLES);
        particle.getParticles().write(0, Particle.REDSTONE);
        particle.getBooleans().write(0, true);
        particle.getFloat().write(0, (float)loc.getX());
        particle.getFloat().write(1, (float)loc.getY());
        particle.getFloat().write(2, (float)loc.getZ());
        particle.getFloat().write(3, c.getRed()/255f);
        particle.getFloat().write(4, c.getGreen()/255f);
        particle.getFloat().write(5, c.getBlue()/255f);
        particle.getFloat().write(6, 0.1f);
        try {
            this.protocolManager.sendServerPacket(player, particle);
        }
        catch (InvocationTargetException e) {
            this.plugin.getLogger().warning("Failed to send particle.");
        }
    }
    
    public boolean canUseProtocolLib() {
        return this.protocolLibInstalled;
    }
    
    public boolean isProtocolLibInstalled() {
        return this.protocolLibInstalled;
    }
}
