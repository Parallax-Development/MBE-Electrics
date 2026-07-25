package dev.darkblade.mbe.electrics;

import dev.darkblade.mbe.api.addon.AddonContext;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.addon.MultiblockAddon;

public final class ElectricsAddon implements MultiblockAddon {
    public static final String ADDON_ID = "mbe-electrics";
    private static final String VERSION = "1.0.0";

    private AddonContext context;

    @Override
    public String getId() {
        return ADDON_ID;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void onLoad(AddonContext context) throws AddonException {
        this.context = context;
        context.getLogger().info("ElectricsAddon loaded");
    }

    @Override
    public void onEnable() throws AddonException {
        dev.darkblade.mbe.api.wiring.NetworkService networkService = context.getService(dev.darkblade.mbe.api.wiring.NetworkService.class);
        if (networkService != null) {
            dev.darkblade.mbe.electrics.service.ElectricsService electricsService = new dev.darkblade.mbe.electrics.service.ElectricsService(networkService);
            context.registerService(dev.darkblade.mbe.electrics.service.ElectricsService.class, electricsService);
            context.getLogger().info("ElectricsService registered");

            dev.darkblade.mbe.electrics.manager.ElectricsManager electricsManager = new dev.darkblade.mbe.electrics.manager.ElectricsManager(electricsService);
            context.registerListener(electricsManager);
        }

        dev.darkblade.mbe.api.item.ItemService itemService = context.getService(dev.darkblade.mbe.api.item.ItemService.class);
        if (itemService != null) {
            dev.darkblade.mbe.api.item.ItemRegistry itemRegistry = itemService.registry();
            itemRegistry.register(new dev.darkblade.mbe.electrics.item.BasicItemDefinition("mbe-electrics:copper_ingot", "§6Copper Ingot", "IRON_INGOT", 100));
            itemRegistry.register(new dev.darkblade.mbe.electrics.item.BasicItemDefinition("mbe-electrics:circuit", "§aCircuit", "REPEATER", 101));
            itemRegistry.register(new dev.darkblade.mbe.electrics.item.BasicItemDefinition("mbe-electrics:casing", "§7Machine Casing", "IRON_BLOCK", 102));
        }
        
        context.getLogger().info("ElectricsAddon enabled");
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.getLogger().info("ElectricsAddon disabled");
        }
    }
}
