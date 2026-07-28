package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;

/**
 * Clears any pending multimeter endpoint selection for the player.
 * Bound to {@link ActionTrigger#SHIFT_RIGHT_CLICK} in {@link InspectMode}.
 */
public final class ResetSelectionAction implements ToolAction {

    private final MultimeterSessionService sessionService;

    public ResetSelectionAction(MultimeterSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public ActionId id() {
        return MultimeterActions.RESET_SELECTION;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        if (context.player() == null) {
            return WrenchResult.pass();
        }

        boolean hadSelection = sessionService.clear(context.player().getUniqueId());
        if (hadSelection) {
            context.player().sendMessage("§7[Multímetro] §cSelección cancelada.");
            return WrenchResult.success("mbe-electrics.multimeter.reset");
        }

        return WrenchResult.pass();
    }
}
