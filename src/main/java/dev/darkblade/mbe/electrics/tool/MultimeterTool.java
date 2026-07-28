package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Collection;
import java.util.List;

public final class MultimeterTool implements Tool {

    private final List<ToolMode> modes;

    public MultimeterTool(ToolMode... modes) {
        this.modes = List.of(modes);
    }

    @Override
    public String id() {
        return "multimeter";
    }

    @Override
    public Collection<ToolMode> modes() {
        return modes;
    }
}
