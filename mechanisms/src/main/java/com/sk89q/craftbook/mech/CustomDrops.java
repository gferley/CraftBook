package com.sk89q.craftbook.mech;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.craftbook.bukkit.MechanismsPlugin;

public class CustomDrops extends MechanismsPlugin implements Listener{

    MechanismsPlugin plugin;
    
    public CustomDrops(MechanismsPlugin plugin) {
	this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
	if(!event.getPlayer().hasPermission("craftbook.mech.drops")) return;
	int oldId = event.getBlock().getTypeId();
	byte oldData = event.getBlock().getData();
	for(String s : config.customDropSettings.blockData) {
	    int newId = Integer.parseInt(s.split("->")[0].split(":")[0]);
	    if(oldId!=newId) continue; //Wrong ID
	    byte newData = 0;
	    if(s.split("->")[0].split(":").length>1) {
		newData = Byte.parseByte(s.split("->")[0].split(":")[1]);
		if(newData!=oldData) continue; //Wrong data
	    }
	    
	    //Clear normal drops
	    event.getBlock().getDrops().clear();
	    
	    //We have the correct block, now we just need to work out what it should drop.
	    String[] drops = s.split("->")[1].split(",");
	    for(String drop : drops) {
		String[] dropInfo = drop.split(":");
		int dropID = Integer.parseInt(dropInfo[0]);
		byte dropData = 0;
		int dropCount = 1;
		if(dropInfo.length>1)
		    dropData = Byte.parseByte(dropInfo[1]);
		if(dropInfo.length>2) {
		    if(dropInfo[2].contains("-")) {
			String[] ranges = dropInfo[2].split("-");
			int min = Integer.parseInt(ranges[0]);
			int max = Integer.parseInt(ranges[1]);
			dropCount = min + (int)(Math.random() * ((max - min) + 1));
		    }
		    else
			dropCount = Integer.parseInt(dropInfo[2]);
		}
		
		//Add the new drops :)
		event.getBlock().getDrops().add(new ItemStack(dropID,dropCount,dropData));
	    }
	}
    }
}
