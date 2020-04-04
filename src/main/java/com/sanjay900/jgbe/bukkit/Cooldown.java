package com.sanjay900.jgbe.bukkit;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class Cooldown {
    private static Table<UUID, String, Long> cooldowns = HashBasedTable.create();

    /**
     * Retrieve the number of milliseconds left until a given cooldown expires.
     * <p>
     * Check for a negative value to determine if a given cooldown has expired. <br>
     * Cooldowns that have never been defined will return {@link Long#MIN_VALUE}.
     * @param entity - the entity.
     * @param key - cooldown to locate.
     * @return Number of milliseconds until the cooldown expires.
     */
    public static long getCooldown(Entity entity, String key) {
        return calculateRemainder(cooldowns.get(entity.getUniqueId(), key));
    }

    /**
     * Update a cooldown for the specified player.
     * @param entity - the entity.
     * @param key - cooldown to update.
     * @param delay - number of milliseconds until the cooldown will expire again.
     * @return The previous number of milliseconds until expiration.
     */
    public static long setCooldown(Entity entity, String key, long delay) {
        return calculateRemainder(
                cooldowns.put(entity.getUniqueId(), key, System.currentTimeMillis() + delay));
    }

    /**
     * Determine if a given cooldown has expired. If it has, refresh the cooldown. If not, do nothing.
     * @param entity - the entity.
     * @param key - cooldown to update.
     * @param delay - number of milliseconds until the cooldown will expire again.
     * @return TRUE if the cooldown was expired/unset and has now been reset, FALSE otherwise.
     */
    public static boolean tryCooldown(Entity entity, String key, long delay) {
        if (getCooldown(entity, key) <= 0) {
            setCooldown(entity, key, delay);
            return true;
        }
        return false;
    }

    private static long calculateRemainder(Long expireTime) {
        return expireTime != null ? expireTime - System.currentTimeMillis() : Long.MIN_VALUE;
    }
}