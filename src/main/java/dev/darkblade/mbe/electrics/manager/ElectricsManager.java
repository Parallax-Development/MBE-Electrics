package dev.darkblade.mbe.electrics.manager;

import dev.darkblade.mbe.electrics.service.ElectricsService;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class ElectricsManager implements Listener {
    
    private final ElectricsService electricsService;

    public ElectricsManager(ElectricsService electricsService) {
        this.electricsService = electricsService;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Will handle placing specific components if needed
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Will handle breaking components and unregistering from graph if needed
    }
}
