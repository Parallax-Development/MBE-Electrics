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
        if (fuelTicksObj instanceof Number) {
            fuelTicks = ((Number) fuelTicksObj).intValue();
        }

        if (fuelTicks <= 0) {
            org.bukkit.block.Block block = instance.anchorLocation().getBlock();
            if (block.getState() instanceof org.bukkit.inventory.InventoryHolder holder) {
                org.bukkit.inventory.Inventory inventory = holder.getInventory();
                int[] candidateSlots = new int[]{12, 13, 14, 1};
                for (int slot : candidateSlots) {
                    if (slot < 0 || slot >= inventory.getSize()) continue;
                    org.bukkit.inventory.ItemStack fuel = inventory.getItem(slot);
                    int burnTime = getBurnTime(fuel);
                    if (burnTime > 0) {
                        if (fuel.getAmount() > 1) {
                            fuel.setAmount(fuel.getAmount() - 1);
                        } else {
                            inventory.setItem(slot, null);
                        }
                        fuelTicks = burnTime;
                        break;
                    }
                }
            }
        }

        if (fuelTicks > 0) {
            electricsService.pushEnergy(instance.anchorLocation().getBlock(), generationPerTick);
            fuelTicks--;
            instance.setVariable("fuel_ticks", fuelTicks);
            
            Object energyObj = instance.getVariable("energy");
            long currentEnergy = energyObj instanceof Number ? ((Number) energyObj).longValue() : 0L;
            instance.setVariable("energy", currentEnergy + generationPerTick);
        }
    }

    private int getBurnTime(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return 0;
        switch (item.getType()) {
            case COAL:
            case CHARCOAL:
                return 1600;
            case COAL_BLOCK:
                return 16000;
            case LAVA_BUCKET:
                return 20000;
            case BLAZE_ROD:
                return 2400;
            case DRIED_KELP_BLOCK:
                return 4000;
            default:
                String name = item.getType().name();
                if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_PLANKS")) {
                    return 300;
                }
                return 0;
        }
    }
}
