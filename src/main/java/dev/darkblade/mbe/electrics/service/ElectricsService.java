package dev.darkblade.mbe.electrics.service;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkType;
import org.bukkit.block.Block;

import java.util.Optional;

public class ElectricsService implements MBEService {
    
    private final NetworkService networkService;
    private final java.util.Map<org.bukkit.Location, dev.darkblade.mbe.core.domain.MultiblockInstance> instances = new java.util.concurrent.ConcurrentHashMap<>();

    public ElectricsService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void registerInstance(org.bukkit.Location location, dev.darkblade.mbe.core.domain.MultiblockInstance instance) {
        instances.put(location, instance);
    }

    public void unregisterInstance(org.bukkit.Location location) {
        instances.remove(location);
    }

    public dev.darkblade.mbe.core.domain.MultiblockInstance getInstance(org.bukkit.Location location) {
        return instances.get(location);
    }

    @Override
    public String getServiceId() {
        return "mbe-electrics";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public void pushEnergy(Block source, long amount) {
        Optional<NetworkNode> nodeOpt = networkService.findNode(NetworkType.ENERGY, source);
        if (nodeOpt.isPresent()) {
            NetworkNode sourceNode = nodeOpt.get();
            dev.darkblade.mbe.api.wiring.NetworkGraph graph = networkService.getGraph(NetworkType.ENERGY, sourceNode);
            if (graph != null) {
                long remaining = amount;
                for (NetworkNode node : graph.nodes()) {
                    if (remaining <= 0) break;
                    if (node.id().equals(sourceNode.id())) continue;

                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(node.position().worldId());
                    if (world == null) continue;
                    
                    Block nodeBlock = world.getBlockAt(node.position().x(), node.position().y(), node.position().z());
                    dev.darkblade.mbe.core.domain.MultiblockInstance instance = getInstance(nodeBlock.getLocation());
                    
                    if (instance != null) {
                        Object energyObj = instance.getVariable("energy");
                        Object maxEnergyObj = instance.getVariable("max_energy");
                        if (energyObj instanceof Number && maxEnergyObj instanceof Number) {
                            long energy = ((Number) energyObj).longValue();
                            long maxEnergy = ((Number) maxEnergyObj).longValue();
                            
                            long capacity = maxEnergy - energy;
                            if (capacity > 0) {
                                long toGive = Math.min(remaining, capacity);
                                instance.setVariable("energy", energy + toGive);
                                remaining -= toGive;
                            }
                        }
                    }
                }
            }
        }
    }

    public long drawEnergy(Block source, long requested) {
        Optional<NetworkNode> nodeOpt = networkService.findNode(NetworkType.ENERGY, source);
        if (nodeOpt.isPresent()) {
            NetworkNode sourceNode = nodeOpt.get();
            dev.darkblade.mbe.api.wiring.NetworkGraph graph = networkService.getGraph(NetworkType.ENERGY, sourceNode);
            if (graph != null) {
                long remainingToDraw = requested;
                for (NetworkNode node : graph.nodes()) {
                    if (remainingToDraw <= 0) break;
                    if (node.id().equals(sourceNode.id())) continue;

                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(node.position().worldId());
                    if (world == null) continue;

                    Block nodeBlock = world.getBlockAt(node.position().x(), node.position().y(), node.position().z());
                    dev.darkblade.mbe.core.domain.MultiblockInstance instance = getInstance(nodeBlock.getLocation());
                    
                    if (instance != null) {
                        Object energyObj = instance.getVariable("energy");
                        if (energyObj instanceof Number) {
                            long energy = ((Number) energyObj).longValue();
                            
                            if (energy > 0) {
                                long toDraw = Math.min(remainingToDraw, energy);
                                instance.setVariable("energy", energy - toDraw);
                                remainingToDraw -= toDraw;
                            }
                        }
                    }
                }
                return requested - remainingToDraw;
            }
        }
        return 0;
    }
}
