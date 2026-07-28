package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.tool.ActionId;

public final class MultimeterActions {

    public static final String NS = "mbe-electrics";

    public static final ActionId INSPECT = ActionId.of(NS, "inspect_network");
    public static final ActionId RESET_SELECTION = ActionId.of(NS, "reset_multimeter");

    private MultimeterActions() {}
}
