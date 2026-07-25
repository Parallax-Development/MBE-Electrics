package dev.darkblade.mbe.electrics.action;

import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.electrics.service.ElectricsService;

public class CoalGeneratorTickAction implements Action {

    private final ElectricsService electricsService;
    private final long generationPerTick;

    public CoalGeneratorTickAction(ElectricsService electricsService, long generationPerTick) {
        this.electricsService = electricsService;
        this.generationPerTick = generationPerTick;
    }

    @Override
    public void execute(MultiblockInstance instance) {
        int fuelTicks = 0;
        Object fuelTicksObj = instance.getVariable("fuel_ticks");
        if (fuelTicksObj instanceof Integer) {
            fuelTicks = (Integer) fuelTicksObj;
        }

        if (fuelTicks > 0) {
            // Push energy to the network
            electricsService.pushEnergy(instance.anchorLocation().getBlock(), generationPerTick);
            
            fuelTicks--;
            instance.setVariable("fuel_ticks", fuelTicks);
        } else {
            // Need logic to consume coal from an inventory if present.
            // For now, we simulate fuel being added externally.
        }
    }
}
