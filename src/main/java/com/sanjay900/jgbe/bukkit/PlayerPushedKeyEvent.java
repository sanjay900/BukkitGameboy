package com.sanjay900.jgbe.bukkit;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.Setter;
@Getter
public class PlayerPushedKeyEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private List<Key> buttons;
    private Player player;
    @Setter
    private boolean cancelled = false;
    private float forMot = 0;
    private float sideMot = 0;

    public PlayerPushedKeyEvent(Player player,
                                List<Key> buttons) {
        super(true);
        this.player = player;
        this.buttons = buttons;
    }
    public PlayerPushedKeyEvent(Player player, float forMot,
                                float sideMot, boolean jump, boolean unmount) {
        super(true);
        this.player = player;
        this.forMot = forMot;
        this.sideMot = sideMot;
        buttons = new ArrayList<>();
        if (jump) {
            buttons.add(Key.JUMP);
        }
        if (unmount) {
            buttons.add(Key.UNMOUNT);
        }
        if(sideMot > 0) {
            buttons.add(Key.LEFT);
        }

        if(sideMot < 0) {
            buttons.add(Key.RIGHT);
        }
        if(forMot > 0) {
            buttons.add(Key.UP);
        }
        if(forMot < 0) {
            buttons.add(Key.DOWN);
        }
    }
    /**
     * Get a list of pushed {@link Key}
     * @return a <List> of Buttons
     */
    public List<Key> getPushed() {
        return buttons;
    }
    /**
     * Get the raw forward momentum
     * @return a float representing the raw forward momentum
     * if no momentum was used in this event, will return 0
     */
    public float getForMot() {
        return forMot;
    }
    /**
     * Get the raw sideways momentum
     * @return a float representing the raw sideways momentum
     * if no momentum was used in this event, will return 0
     */
    public float getSideMot() {
        return sideMot;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
