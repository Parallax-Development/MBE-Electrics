package dev.darkblade.mbe.electrics.manager;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.api.util.NamespacedKey;
import dev.darkblade.mbe.electrics.service.ElectricsService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ElectricsManager implements Listener {

    private final ElectricsService electricsService;
    private final dev.darkblade.mbe.api.wiring.NetworkService networkService;
    private final dev.darkblade.mbe.api.ui.binding.PanelBindingLinkService linkService;
    private final Set<Location> electricFurnaces = ConcurrentHashMap.newKeySet();

    public ElectricsManager(ElectricsService electricsService, dev.darkblade.mbe.api.wiring.NetworkService networkService, dev.darkblade.mbe.api.ui.binding.PanelBindingLinkService linkService) {
        this.electricsService = electricsService;
        this.networkService = networkService;
        this.linkService = linkService;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Will handle placing specific components if needed
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Will handle breaking components and unregistering from graph if needed
    }

    public void onMultiblockForm(MultiblockFormEvent event) {
        String typeId = event.getMultiblock().type().id().toString();
        if (typeId.equals("mbe-electrics:electric_forge") || typeId.equals("mbe-electrics:coal_generator")) {
            Location anchor = event.getMultiblock().anchorLocation();
            if (anchor != null) {
                if (typeId.equals("mbe-electrics:electric_forge")) {
                    electricFurnaces.add(anchor);
                    if (linkService != null) {
                        linkService.linkPanelToBlock("electric_forge", anchor.getBlock(), "click");
                    }
                } else if (typeId.equals("mbe-electrics:coal_generator")) {
                    if (linkService != null) {
                        linkService.linkPanelToBlock("coal_generator", anchor.getBlock(), "click");
                    }
                }
                
                electricsService.registerInstance(anchor, event.getMultiblock());
                
                Set<dev.darkblade.mbe.api.wiring.Direction> faces = java.util.EnumSet.allOf(dev.darkblade.mbe.api.wiring.Direction.class);
                dev.darkblade.mbe.api.wiring.NodeDescriptor descriptor = new dev.darkblade.mbe.api.wiring.NodeDescriptor(faces);
                networkService.registerNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, anchor.getBlock(), descriptor);
            }
        }
    }

    public void onMultiblockBreak(MultiblockBreakEvent event) {
        String typeId = event.getMultiblock().type().id().toString();
        if (typeId.equals("mbe-electrics:electric_forge") || typeId.equals("mbe-electrics:coal_generator")) {
            Location anchor = event.getMultiblock().anchorLocation();
            if (anchor != null) {
                if (typeId.equals("mbe-electrics:electric_forge")) {
                    electricFurnaces.remove(anchor);
                }
                
                electricsService.unregisterInstance(anchor);
                
                networkService.findNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, anchor.getBlock()).ifPresent(node -> {
                    networkService.unregisterNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, node);
                });
            }
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        // Prevent vanilla fuel burning if this furnace is part of an electric
        // multiblock
        if (electricFurnaces.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
