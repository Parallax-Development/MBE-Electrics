package dev.darkblade.mbe.electrics.action;

import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.electrics.service.ElectricsService;

public class ElectricFurnaceTickAction implements Action {

    private final ElectricsService electricsService;
    private final long drawPerTick;

    public ElectricFurnaceTickAction(ElectricsService electricsService, long drawPerTick) {
        this.electricsService = electricsService;
        this.drawPerTick = drawPerTick;
    }

    @Override
    public void execute(MultiblockInstance instance) {
        // Furnace draw logic
        // For now we simulate drawing
        // electricsService.drawEnergy(instance.anchorLocation().getBlock(), drawPerTick);
        int progress = 0;
        Object progressObj = instance.getVariable("smelt_progress");
        if (progressObj instanceof Integer) {
            progress = (Integer) progressObj;
        }

        if (progress > 0) {
            progress--;
            instance.setVariable("smelt_progress", progress);
        }
    }
}
