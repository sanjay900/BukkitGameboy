package com.sanjay900.jgbe.bukkit;


public class RemoteServerLink
{
    public String player;
    public boolean isConnected;
    public String cart;
    
    public RemoteServerLink(final String player, final String cart, final Boolean isConnected) {
        super();
        this.player = player;
        this.cart = cart;
        this.isConnected = isConnected;
    }
}
