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
            PortResolutionService portResolutionService
    ) {
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
        String typeId = event.getMultiblock().type().id().toString();
        if (typeId.equals("mbe-electrics:electric_forge") || typeId.equals("mbe-electrics:coal_generator")) {
            Location anchor = event.getMultiblock().anchorLocation();
            if (anchor != null) {
                if (typeId.equals("mbe-electrics:electric_forge")) {
                    electricFurnaces.add(anchor);
                }

                electricsService.registerInstance(anchor, event.getMultiblock());

                Set<dev.darkblade.mbe.api.wiring.Direction> faces = java.util.EnumSet.allOf(dev.darkblade.mbe.api.wiring.Direction.class);
                dev.darkblade.mbe.api.wiring.NodeDescriptor descriptor = new dev.darkblade.mbe.api.wiring.NodeDescriptor(faces);
                networkService.registerNode(dev.darkblade.mbe.api.wiring.NetworkType.ENERGY, anchor.getBlock(), descriptor);
            }
        }

        // Connect all energy-type ports of this multiblock together (internal bus).
        // The MultiblockWiringBridge has already registered a node at each port location;
        // we only need to wire them together so the multimeter (and any traversal) sees
        // them as a single connected component, regardless of which port has cables.
        connectInternalPorts(event.getMultiblock());
    }

    /**
     * Resolves all energy ports of the given multiblock and connects their network nodes
     * to each other, modelling the internal bus of the machine.
     */
    private void connectInternalPorts(dev.darkblade.mbe.core.domain.MultiblockInstance instance) {
        if (portResolutionService == null || instance == null) {
            return;
        }

        List<NetworkNode> energyPortNodes = new ArrayList<>();

        for (PortResolutionService.ResolvedPort resolved : portResolutionService.resolveAll(instance)) {
            PortDefinition def = resolved.definition();
            Location loc = resolved.location();
            if (def == null || loc == null || loc.getWorld() == null) {
                continue;
            }
            // Only wire energy-type ports together
            String portType = def.type() == null ? "" : def.type().trim().toLowerCase(Locale.ROOT);
            if (!portType.equals("energy") && !portType.isEmpty()) {
                continue;
            }
            networkService.findNode(NetworkType.ENERGY, loc.getBlock())
                    .ifPresent(energyPortNodes::add);
        }

        // Connect every energy port to every other energy port (fully-meshed internal bus)
        for (int i = 0; i < energyPortNodes.size(); i++) {
            for (int j = i + 1; j < energyPortNodes.size(); j++) {
                networkService.connect(NetworkType.ENERGY, energyPortNodes.get(i), energyPortNodes.get(j));
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
