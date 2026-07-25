package dev.darkblade.mbe.electrics.action;

import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.electrics.service.ElectricsService;

public class BatteryBoxTickAction implements Action {

    private final ElectricsService electricsService;
    private final long capacity;
    private final long transferRate;

    public BatteryBoxTickAction(ElectricsService electricsService, long capacity, long transferRate) {
        this.electricsService = electricsService;
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    @Override
    public void execute(MultiblockInstance instance) {
        // Battery logic
        // Pushes to network if has energy, draws from network if not full.
        long energy = 0;
        Object energyObj = instance.getVariable("energy");
        if (energyObj instanceof Long) {
            energy = (Long) energyObj;
        }

        if (energy > 0) {
            // Push energy to network up to transferRate
            long toPush = Math.min(energy, transferRate);
            // electricsService.pushEnergy(instance.anchorLocation().getBlock(), toPush);
            
            // Assume pushed
            energy -= toPush;
            instance.setVariable("energy", energy);
        }
    }
}
