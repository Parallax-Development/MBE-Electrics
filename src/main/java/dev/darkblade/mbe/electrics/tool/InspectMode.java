package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Map;

public final class InspectMode implements ToolMode {

    private static final Map<ActionTrigger, ActionId> BINDINGS = Map.of(
            ActionTrigger.RIGHT_CLICK, MultimeterActions.INSPECT,
            ActionTrigger.SHIFT_RIGHT_CLICK, MultimeterActions.RESET_SELECTION
    );

    @Override
    public String id() {
        return "multimeter_inspect";
    }

    @Override
    public Map<ActionTrigger, ActionId> bindings() {
        return BINDINGS;
    }
}
