package com.sanjay900.jgbe.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.Collections;

class KeyListener extends PacketAdapter implements Listener {
    KeyListener() {
        super(GameboyPlugin.getInstance(), ListenerPriority.NORMAL,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.CLIENT_COMMAND,
                PacketType.Play.Client.STEER_VEHICLE,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.ENTITY_ACTION);
        Bukkit.getPluginManager().registerEvents(this, GameboyPlugin.getInstance());
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event) {
        PacketContainer packet = event.getPacket();
//        //This way, normal horses and stuff wont be affected and will function normally
//        if (packet.getType() == PacketType.Play.Client.CLIENT_COMMAND) {
//            if (packet.getClientCommands().read(0) != EnumWrappers.ClientCommand.OPEN_INVENTORY_ACHIEVEMENT) return;
//            PlayerPushedKeyEvent pmEvent = new PlayerPushedKeyEvent(event.getPlayer(), Collections.singletonList(Key.OPEN_INVENTORY));
//            Bukkit.getPluginManager().callEvent(pmEvent);
//            if (pmEvent.isCancelled()) {
//                event.getPlayer().closeInventory();
//            }
//            return;
//        }
        Cancellable pmEvent = null;
        if (packet.getType() == PacketType.Play.Client.BLOCK_DIG) {
            if (packet.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.DROP_ITEM || packet.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.DROP_ALL_ITEMS)
                pmEvent = new PlayerPushedKeyEvent(event.getPlayer(), Collections.singletonList(Key.DROP_ITEM));
            if (packet.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)
                pmEvent = new PlayerPushedKeyEvent(event.getPlayer(), Collections.singletonList(Key.BREAK));
        }
        if (packet.getType() == PacketType.Play.Client.BLOCK_PLACE) {
            pmEvent = new PlayerPushedKeyEvent(event.getPlayer(), Collections.singletonList(Key.PLACE));
        }
        if (packet.getType() == PacketType.Play.Client.STEER_VEHICLE) {
            final float sideMot = packet.getFloat().read(0);
            final float forMot = packet.getFloat().read(1);
            boolean jump = packet.getBooleans().read(0);
            boolean unmount = packet.getBooleans().read(1);
            //avoid unmounting players when sneak is pressed
            if (unmount) event.setCancelled(true);
            pmEvent = new PlayerPushedKeyEvent(event.getPlayer(), forMot, sideMot, jump, unmount);
        }
        if (pmEvent == null) return;
        boolean isUnmount = event.isCancelled();
        Bukkit.getPluginManager().callEvent((Event) pmEvent);
        event.setCancelled(isUnmount || pmEvent.isCancelled());
    }
}