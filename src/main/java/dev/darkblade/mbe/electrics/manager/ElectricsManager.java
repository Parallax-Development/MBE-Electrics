package dev.darkblade.mbe.electrics.manager;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.api.util.NamespacedKey;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.electrics.service.ElectricsService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class ElectricsManager implements Listener {

    private final ElectricsService electricsService;
    private final dev.darkblade.mbe.api.wiring.NetworkService networkService;
    private final PortResolutionService portResolutionService;
    private final Set<Location> electricFurnaces = ConcurrentHashMap.newKeySet();

    public ElectricsManager(
            ElectricsService electricsService,
            dev.darkblade.mbe.api.wiring.NetworkService networkService,
            PortResolutionService portResolutionService) {
        this.electricsService = electricsService;
        this.networkService = networkService;
        this.portResolutionService = portResolutionService;
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
        dev.darkblade.mbe.core.domain.MultiblockInstance instance = event.getMultiblock();
        if (instance == null || instance.type() == null) {
            return;
        }

        String typeId = instance.type().id().toString();
        Location anchor = instance.anchorLocation();

        if (typeId.equals("mbe-electrics:electric_forge")) {
            if (anchor != null) {
                electricFurnaces.add(anchor);
            }
        }

        if (anchor != null) {
            electricsService.registerInstance(anchor, instance);
            if (typeId.equals("mbe-electrics:electric_forge") || typeId.equals("mbe-electrics:coal_generator")) {
                Set<dev.darkblade.mbe.api.wiring.Direction> faces = java.util.EnumSet
                        .allOf(dev.darkblade.mbe.api.wiring.Direction.class);
                dev.darkblade.mbe.api.wiring.NodeDescriptor descriptor = new dev.darkblade.mbe.api.wiring.NodeDescriptor(
                        faces);
                networkService.registerNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, anchor.getBlock(),
                        descriptor);
            }
        }

        // Register all port locations in ElectricsService so getInstance(portLoc) works
        if (portResolutionService != null) {
            for (PortResolutionService.ResolvedPort resolved : portResolutionService.resolveAll(instance)) {
                if (resolved.location() != null) {
                    electricsService.registerInstance(resolved.location(), instance);
                }
            }
        }
    }

    public void onMultiblockBreak(MultiblockBreakEvent event) {
        dev.darkblade.mbe.core.domain.MultiblockInstance instance = event.getMultiblock();
        if (instance == null || instance.type() == null) {
            return;
        }

        String typeId = instance.type().id().toString();
        Location anchor = instance.anchorLocation();

        if (typeId.equals("mbe-electrics:electric_forge")) {
            if (anchor != null) {
                electricFurnaces.remove(anchor);
            }
        }

        if (anchor != null) {
            electricsService.unregisterInstance(anchor);
            networkService.findNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, anchor.getBlock())
                    .ifPresent(node -> {
                        networkService.unregisterNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, node);
                    });
        }

        if (portResolutionService != null) {
            for (PortResolutionService.ResolvedPort resolved : portResolutionService.resolveAll(instance)) {
                if (resolved.location() != null) {
                    electricsService.unregisterInstance(resolved.location());
                }
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
