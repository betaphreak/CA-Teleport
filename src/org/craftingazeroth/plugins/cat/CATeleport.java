/*
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

package org.craftingazeroth.plugins.cat;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class CATeleport extends JavaPlugin implements Listener
{
    // TODO: config file
    
    static final long teleport_cooldown = 60;
    
    static final int world_height = 256;
    static final int altitude_border = 32;
    static final double altitude_padding = 1.5;
    static final double zone_padding = 5;

    static final int grid_spacing = 51200;
    static final int max_altitude_level = 999;
    static final int min_altitude_level = -999;

    static final ChatColor broadcast_color = ChatColor.YELLOW;
    static final ChatColor status_color = ChatColor.AQUA;

    static final int bound_above = world_height - altitude_border;
    static final int bound_below = altitude_border;
    static final int offset = world_height - altitude_border * 2;

    boolean teleport_player = true;
    boolean sync_blocks = true;

    Map<Player, PlayerProfile> playerProfiles;
    Map<ZoneCoordinate, ZoneProfile> zoneProfiles;

    Logger log;

    public CATeleport()
    {
        log = Logger.getLogger("Minecraft");
        playerProfiles = new HashMap<Player, PlayerProfile>();
        zoneProfiles = new HashMap<ZoneCoordinate, ZoneProfile>();
    }

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);

        ZoneProfile[] azeroth = {
                new ZoneProfile("Azeroth-1", 0, -1),
                new ZoneProfile("Azeroth", 0, 0),
                new ZoneProfile("Azeroth+1", 0, 1),
                new ZoneProfile("Azeroth+2", 0, 2),
                new ZoneProfile("Azeroth+3", 0, 3),
                new ZoneProfile("Azeroth+4", 0, 4),
                new ZoneProfile("Azeroth+5", 0, 5)
        };

        ZoneProfile[] outland = {
                new ZoneProfile("Outland-1", -1, -1),
                new ZoneProfile("Outland", -1, 0),
                new ZoneProfile("Outland+1", -1, 1),
                new ZoneProfile("Outland+2", -1, 2)
        };
        
        ZoneProfile instance = new ZoneProfile("Instance", 1, 0);

        for(ZoneProfile z : azeroth) 
            zoneProfiles.put(z.getCoordinate(), z);
        for(ZoneProfile z : outland) 
            zoneProfiles.put(z.getCoordinate(), z);
        zoneProfiles.put(instance.getCoordinate(), instance);
        
        double psi = Math.PI * 0.213;
        
        azeroth[1].addTeleport(new ZoneTeleport(instance, 
                new Circle(3173, 8700, 430), 
                new Circle(2727-96, 8550, 332),
                76, -50, psi, false));
        
        // inverse of above
        instance.addTeleport(new ZoneTeleport(azeroth[1], 
                new Circle(76, -50, 430 + zone_padding), 
                new Circle(-256, -503, 332 - zone_padding),
                3173, 8700, -psi, true));
    }

    public void onDisable()
    {
        playerProfiles.clear();
        zoneProfiles.clear();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if(args.length < 1)
            return false;
        
        String method = args[0];
        String option = cmd.getName();
        String target = args.length > 1 ? args[1] : null;
                
        if(method == null)
            return false;

        if(method.equalsIgnoreCase("status"))
            return displayStatus(sender, option, target);
        else if(method.equalsIgnoreCase("enable") || method.equalsIgnoreCase("start"))
            return setOptionStatus(sender, option, target, true);
        else if(method.equalsIgnoreCase("disable") || method.equalsIgnoreCase("stop"))
            return setOptionStatus(sender, option, target, false);

        return false;
    }

    protected boolean setOptionStatus(CommandSender sender, String option, String target, boolean enabled)
    {
        if(target != null && (target.equals("-global") || target.equals("-all") || target.equals("*")))
        {
            if(option.equalsIgnoreCase("tp"))
            {
                teleport_player = enabled;
                getServer().broadcastMessage(broadcast_color
                        + "CA-Teleport: player teleportation " 
                        + (enabled ? "enabled" : "disabled")
                        + " globally by " + sender.getName());
            }
            else if(option.equalsIgnoreCase("sync"))
            {
                sync_blocks = enabled;
                getServer().broadcastMessage(broadcast_color
                        + "CA-Teleport: block synchronization " 
                        + (enabled ? "enabled" : "disabled")
                        + " globally by " + sender.getName());
            }
            else
                return false;
        }
        else
        {
            Player player;
            
            if(target == null || target.isEmpty() || target.equals("-self"))
                player = getServer().getPlayer(sender.getName());
            else
                player = getServer().getPlayer(target);
            
            if(player == null)
                return false;

            PlayerProfile profile = fetchProfile(player);

            if(option.equalsIgnoreCase("catp"))
            {
                profile.setTeleportEnabled(enabled);                
                if(target != null && target.equalsIgnoreCase(sender.getName()))
                {
                    sender.sendMessage(status_color 
                            + "CA-Teleport: player teleportation "  
                            + (enabled ? "enabled" : "disabled"));
                }
                else
                {
                    sender.sendMessage(status_color 
                            + "CA-Teleport: player teleportation " 
                            + (enabled ? "enabled" : "disabled") 
                            + " for " + target);
                    player.sendMessage(status_color 
                            + "CA-Teleport: player teleportation " 
                            + (enabled ? "enabled" : "disabled") 
                            + " by " + sender.getName());
                }
            }
            else if(option.equalsIgnoreCase("casync"))
            {
                profile.setSyncEnabled(enabled);
                if(target != null && target.equalsIgnoreCase(sender.getName()))
                {
                    sender.sendMessage(status_color 
                            + "CA-Teleport: block synchronization "  
                            + (enabled ? "enabled" : "disabled"));
                }
                else
                {
                    sender.sendMessage(status_color 
                            + "CA-Teleport: block synchronization " 
                            + (enabled ? "enabled" : "disabled")
                            + " for " + target);
                    player.sendMessage(status_color 
                            + "CA-Teleport: block synchronization " 
                            + (enabled ? "enabled" : "disabled") 
                            + " by " + sender.getName());
                }
            }
            else
                return false;
        }

        return true;
    }

    protected boolean displayStatus(CommandSender sender, String arg1, String arg2)
    {
        String message;
        boolean mode;

        if(arg1.equalsIgnoreCase("catp"))
        {
            mode = teleport_player;
            message = status_color + "AltitudePlugin: player teleportation";
        }
        else if(arg1.equalsIgnoreCase("casync"))
        {
            mode = sync_blocks;
            message = status_color + "AltitudePlugin: block synchronization";
        }
        else
            return false;

        if(mode)
            message += " is globally enabled";
        else
            message += " is globally disabled";

        sender.sendMessage(message);

        return true;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if(!sync_blocks)
            return;

        Player player = event.getPlayer();
        PlayerProfile profile = fetchProfile(player);

        if(!profile.isSyncEnabled())
            return;
        
        editBlock(event.getBlock(), false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        if(!sync_blocks)
            return;

        Player player = event.getPlayer();
        PlayerProfile profile = fetchProfile(player);

        if(!profile.isSyncEnabled())
            return;
        
        editBlock(event.getBlock(), true);
    }

    protected void editBlock(Block block, boolean delete)
    {
        double y = block.getLocation().getY();

        if(y < bound_below + altitude_border - 1)
            editBlock(block, -1, delete);
        else if(y > bound_above - altitude_border + 1)
            editBlock(block, 1, delete);
    }

    protected boolean editBlock(Block block, int delta, boolean delete)
    {
        Location locationDelta = getLocationDelta(block.getLocation(), delta);
        
        if(locationDelta == null)
            return false;
        
        Block blockDelta = locationDelta.getWorld().getBlockAt(locationDelta);

        if(delete)
            blockDelta.setType(Material.AIR);
        else
        {
            blockDelta.setType(block.getType());
            blockDelta.setData(block.getData());
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        createProfile(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        removeProfile(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event)
    {
        removeProfile(event.getPlayer());
    }

    protected PlayerProfile fetchProfile(Player player)
    {
        PlayerProfile profile = playerProfiles.get(player);
        if(profile == null)
            profile = createProfile(player);

        return profile;
    }

    protected PlayerProfile createProfile(Player player)
    {
        PlayerProfile profile = new PlayerProfile(player);
        profile.setZoneProfile(getZoneProfile(player.getLocation()));
        playerProfiles.put(player, profile);
        return profile;
    }

    protected PlayerProfile removeProfile(Player player)
    {
        PlayerProfile profile = playerProfiles.remove(player);

        if(profile == null)
            return null;

        return profile;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if(!teleport_player)
            return;

        Player player = event.getPlayer();
        PlayerProfile profile = fetchProfile(player);

        if(!profile.isTeleportEnabled() || profile.isTeleportOnCooldown())
            return;

        Location loc = player.getLocation();
                
        double y = loc.getY();
        Location tele = null;

        if(y < bound_below - altitude_padding)
            tele = getLocationDelta(loc, -1);
        else if(y > bound_above + altitude_padding)
            tele = getLocationDelta(loc, 1);

        if(tele == null && profile.getZoneProfile() != null && profile.getZoneProfile().hasTeleports())
            tele = profile.getZoneProfile().getTeleportLocation(loc);
        
        if(tele != null)
        {
            teleportPlayer(player, tele);
            profile.startTeleportCooldown();
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {
        PlayerProfile player = fetchProfile(event.getPlayer());
        player.setZoneProfile(getZoneProfile(event.getTo()));
    }
    
    protected ZoneProfile getZoneProfile(Location loc)
    {
        int x = loc.getBlockX() / grid_spacing;
        int z = loc.getBlockZ() / grid_spacing;        
        return zoneProfiles.get(new ZoneCoordinate(x, z));
    }
    
    protected void teleportPlayer(Player player, Location loc)
    {
        // move up one block, in case of collision
        if(!(loc.getBlock().isEmpty() || loc.getBlock().isLiquid()))
            loc.subtract(0, -1, 0);
        
        // get player velocity and fall distance
        Vector velocity = player.getVelocity();
        float fallDistance = player.getFallDistance();
        boolean flying = player.isFlying();

        // teleport player to destination
        player.teleport(loc);

        // restore velocity and fall distance
        player.setFlying(flying);
        player.setVelocity(velocity);

        // repeat, in case the player drifted during load
        player.teleport(loc);
        player.setFlying(flying);
        player.setVelocity(velocity);
        player.setFallDistance(fallDistance);
    }

    protected Location getLocationDelta(Location to, int delta)
    {
        // return null if no change in altitude
        if(delta == 0)
            return null;

        Location locationDelta = to.clone();
        locationDelta.subtract(0, offset * delta, -grid_spacing * delta);

        // return null of target zone is not defined
        ZoneProfile zone = getZoneProfile(locationDelta);
        if(zone == null)
            return null;
        
        return locationDelta;
    }
}