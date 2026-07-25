package dev.darkblade.mbe.electrics.item;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;

import java.util.Map;

public class BasicItemDefinition implements ItemDefinition {
    private final ItemKey key;
    private final String displayName;
    private final Map<String, Object> properties;

    public BasicItemDefinition(String keyString, String displayName, String material, int customModelData) {
        this.key = ItemKeys.of(keyString, 1);
        this.displayName = displayName;
        this.properties = Map.of(
                "material", material,
                "custom-model-data", customModelData,
                "unstackable", false
        );
    }

    @Override
    public ItemKey key() {
        return key;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Map<String, Object> properties() {
        return properties;
    }
}
