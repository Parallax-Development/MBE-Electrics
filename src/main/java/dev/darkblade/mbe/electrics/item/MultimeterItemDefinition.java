package dev.darkblade.mbe.electrics.item;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;

import java.util.Map;

public final class MultimeterItemDefinition implements ItemDefinition {

    private final ItemKey key;
    private final String displayName;
    private final Map<String, Object> properties;

    public MultimeterItemDefinition() {
        this.key = ItemKeys.of("mbe-electrics:multimeter", 1);
        this.displayName = "§eMultímetro";
        this.properties = Map.of(
                "material", "CLOCK",
                "custom-model-data", 200,
                "unstackable", true
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
