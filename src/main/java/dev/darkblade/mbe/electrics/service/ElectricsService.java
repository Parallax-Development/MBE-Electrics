package dev.darkblade.mbe.electrics.service;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkType;
import org.bukkit.block.Block;

import java.util.Optional;

public class ElectricsService implements MBEService {
    
    private final NetworkService networkService;

    public ElectricsService(NetworkService networkService) {
        this.networkService = networkService;
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
            NetworkNode node = nodeOpt.get();
            // Pushing energy to graph logic will be implemented here.
            // mbe-wiring handles graph connections, we will need to traverse and find consumers/storages.
        }
    }
}
