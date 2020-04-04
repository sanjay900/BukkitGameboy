package com.sanjay900.jgbe.bukkit;

import com.sanjay900.jgbe.converters.CrystalConverter;
import com.sanjay900.jgbe.converters.RedBlueConverter;
import com.sanjay900.jgbe.converters.RedBlueConverter.dataType;
import com.sanjay900.jgbe.converters.YellowConverter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandHandler implements CommandExecutor {
    GameboyPlugin plugin = GameboyPlugin.getInstance();

    public CommandHandler() {
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (!(sender instanceof Player)) return false;
        GameboyPlayer gp = plugin.getPlayer((Player) sender);
        if (args.length == 0) {
            String filename = plugin.getDataFolder().getAbsolutePath() + "/roms/";
            plugin.listRoms((Player) sender);
            return true;
        }
        if (!args[0].equals("loadrom") && gp == null) {
            sender.sendMessage("You need to start a game before you can interact with it!");
            return true;
        }
        try {

            switch (args[0]) {
                case "getPokemon":
                    if (gp.conv instanceof RedBlueConverter) {
                        RedBlueConverter pc = (RedBlueConverter) gp.conv;
                        sender.sendMessage("Pokemon Captured: " + pc.getCapacity());
                        for (int i = 0; i < pc.getCapacity(); i++) {
                            sender.sendMessage("Pokemon " + (i + 1) + ":");
                            sender.sendMessage("Name: " + pc.get(i, dataType.name));
                            sender.sendMessage("Species: " + pc.getSpecies(i));
                            sender.sendMessage("Health:" + pc.get(i, dataType.hp));
                            sender.sendMessage("Max Health:" + pc.get(i, dataType.maxhp));
                            sender.sendMessage("Experience:" + pc.get(i, dataType.exp));
                        }
                    }
                    if (gp.conv instanceof YellowConverter) {
                        YellowConverter pc = (YellowConverter) gp.conv;
                        sender.sendMessage("Pokemon Captured: " + pc.getCapacity());
                        for (int i = 0; i < pc.getCapacity(); i++) {
                            sender.sendMessage("Pokemon " + (i + 1) + ":");
                            sender.sendMessage("Name: " + pc.get(i, YellowConverter.dataType.name));
                            sender.sendMessage("Species: " + pc.getSpecies(i));
                            sender.sendMessage("Health:" + pc.get(i, YellowConverter.dataType.hp));
                            sender.sendMessage("Max Health:" + pc.get(i, YellowConverter.dataType.maxhp));
                            sender.sendMessage("Experience:" + pc.get(i, YellowConverter.dataType.exp));
                        }
                    }
                    if (gp.conv instanceof CrystalConverter) {
                        CrystalConverter pc = (CrystalConverter) gp.conv;
                        sender.sendMessage("Pokemon Captured: " + pc.getCapacity());
                        for (int i = 0; i < pc.getCapacity(); i++) {
                            sender.sendMessage("Pokemon " + (i + 1) + ":");
                            sender.sendMessage("Name: " + pc.get(i, CrystalConverter.dataType.name));
                            sender.sendMessage("Species: " + pc.getSpecies(i));
                            sender.sendMessage("Health:" + pc.get(i, CrystalConverter.dataType.hp));
                            sender.sendMessage("Max Health:" + pc.get(i, CrystalConverter.dataType.maxhp));
//						sender.sendMessage("Experience:"+pc.get(i, CrystalConverter.dataType.));
                        }
                    }
                    break;
                case "loadrom":
                    plugin.startPlaying((Player) sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                    return true;


                case "save":
                    gp.save();
                    return true;

                case "load":
                    if (gp.loadSave()) {
                        sender.sendMessage("Loaded State!");
                    } else {
                        sender.sendMessage("You dont have a save to load!");
                    }
                    return true;
                case "pause":
                    gp.pause();
                    sender.sendMessage("Paused Game!");
                    return true;
                case "resume":
                    gp.resume();
                    sender.sendMessage("Resumed Game!");
                    return true;
                case "reset":
                    gp.reset();
                    sender.sendMessage("Reset Game!");
                    return true;
            }
            return false;


        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

}

