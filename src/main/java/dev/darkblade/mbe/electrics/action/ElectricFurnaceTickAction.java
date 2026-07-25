package dev.darkblade.mbe.electrics.action;

import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.electrics.service.ElectricsService;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;

public class ElectricFurnaceTickAction implements Action {

    private final ElectricsService electricsService;
    private final long drawPerTick;

    public ElectricFurnaceTickAction(ElectricsService electricsService, long drawPerTick) {
        this.electricsService = electricsService;
        this.drawPerTick = drawPerTick;
    }

    @Override
    public void execute(MultiblockInstance instance) {
        Block block = instance.anchorLocation().getBlock();
        if (!(block.getState() instanceof Furnace furnace)) {
            return;
        }

        FurnaceInventory inv = furnace.getInventory();
        ItemStack input = inv.getSmelting();

        if (input == null || input.getType().isAir()) {
            if (furnace.getCookTime() > 0) {
                furnace.setCookTime((short) 0);
                furnace.update();
                instance.setVariable("smelt_progress", 0);
            }
            return;
        }

        CookingRecipe<?> recipe = null;
        List<Recipe> recipes = Bukkit.getServer().getRecipesFor(input);
        for (Recipe r : recipes) {
            // Prefer BlastingRecipe for Blast Furnace, fallback to FurnaceRecipe
            if (r instanceof BlastingRecipe || r instanceof FurnaceRecipe) {
                recipe = (CookingRecipe<?>) r;
                if (r instanceof BlastingRecipe) {
                    break; // Prioritize blasting recipe
                }
            }
        }

        if (recipe == null) {
            if (furnace.getCookTime() > 0) {
                furnace.setCookTime((short) 0);
                furnace.update();
                instance.setVariable("smelt_progress", 0);
            }
            return;
        }

        ItemStack result = recipe.getResult();
        ItemStack currentOutput = inv.getResult();

        if (currentOutput != null && !currentOutput.getType().isAir()) {
            if (!currentOutput.isSimilar(result) || currentOutput.getAmount() + result.getAmount() > currentOutput.getMaxStackSize()) {
                // Output is full or incompatible, pause processing and energy draw
                return;
            }
        }

        // Output has space, draw energy
        long drawn = electricsService.drawEnergy(block, drawPerTick);
        if (drawn < drawPerTick) {
            // Not enough energy in the network, pause processing
            return;
        }

        short cookTime = furnace.getCookTime();
        cookTime++;
        int totalTime = recipe.getCookingTime();
        furnace.setCookTimeTotal(totalTime);

        if (cookTime >= totalTime) {
            // Finish smelting
            input.setAmount(input.getAmount() - 1);
            if (currentOutput == null || currentOutput.getType().isAir()) {
                inv.setResult(result.clone());
            } else {
                currentOutput.setAmount(currentOutput.getAmount() + result.getAmount());
            }
            furnace.setCookTime((short) 0);
            instance.setVariable("smelt_progress", 0);
        } else {
            furnace.setCookTime(cookTime);
            instance.setVariable("smelt_progress", cookTime);
        }

        furnace.update();
    }
}
