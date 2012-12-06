package com.sk89q.craftbook.gates.world.weather;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.BukkitUtil;
import com.sk89q.craftbook.ic.AbstractIC;
import com.sk89q.craftbook.ic.AbstractICFactory;
import com.sk89q.craftbook.ic.ChipState;
import com.sk89q.craftbook.ic.IC;
import com.sk89q.craftbook.ic.ICFactory;
import com.sk89q.craftbook.ic.RestrictedIC;
import com.sk89q.craftbook.ic.SelfTriggeredIC;
import com.sk89q.craftbook.util.LocationUtil;

/**
 * @author Me4502
 */
public class TimeFaker extends AbstractIC implements SelfTriggeredIC {

    public TimeFaker(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    private ArrayList<String> players = new ArrayList<String>();

    @Override
    public String getTitle() {

        return "Time Faker";
    }

    @Override
    public String getSignTitle() {

        return "TIME FAKER";
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new TimeFaker(getServer(), sign, this);
        }

        @Override
        public String getDescription() {

            return "Radius based fake time.";
        }

        @Override
        public String[] getLineHelp() {

            String[] lines = new String[] {
                    "radius",
                    "time"
            };
            return lines;
        }
    }

    int dist;
    long time;

    @Override
    public void load() {

        dist = Integer.parseInt(getSign().getLine(2));
        time = Long.parseLong(getSign().getLine(3));
    }

    @Override
    public boolean isActive() {

        return true;
    }

    @Override
    public void trigger(ChipState chip) {

    }

    @Override
    public void think(ChipState chip) {

        if (chip.getInput(0)) {
            for(Player p : Bukkit.getOnlinePlayers()) {

                if(!players.contains(p.getName()) && LocationUtil.isWithinRadius(p.getLocation(), BukkitUtil.toSign(getSign()).getLocation(), dist)) {
                    p.setPlayerTime(time, false);
                    players.add(p.getName());
                }
                else if(players.contains(p.getName())) {
                    p.resetPlayerTime();
                    players.remove(p.getName());
                }
            }
        }
    }
}