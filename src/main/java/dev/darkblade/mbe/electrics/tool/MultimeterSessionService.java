package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Maintains per-player pending endpoint selections for the multimeter Inspect action.
 * Implements {@link Listener} to clean up sessions when players disconnect.
 */
public final class MultimeterSessionService implements Listener {

    /**
     * Captures the first endpoint selected by a player.
     *
     * @param node the resolved {@link NetworkNode} at the clicked block
     * @param pos  the position of the block for feedback messages
     */
    public record PendingSelection(NetworkNode node, BlockPos pos) {}

    private final Map<UUID, PendingSelection> sessions = new ConcurrentHashMap<>();

    /**
     * Stores a pending selection for the given player.
     *
     * @param playerId the UUID of the player
     * @param node     the {@link NetworkNode} selected as the first endpoint
     */
    public void store(UUID playerId, NetworkNode node) {
        sessions.put(playerId, new PendingSelection(node, node.position()));
    }

    /**
     * Returns the pending selection for the given player, if any.
     *
     * @param playerId the UUID of the player
     * @return an {@link Optional} containing the pending selection, or empty if none
     */
    public Optional<PendingSelection> get(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    /**
     * Removes the pending selection for the given player.
     *
     * @param playerId the UUID of the player
     * @return {@code true} if a selection was present and cleared, {@code false} otherwise
     */
    public boolean clear(UUID playerId) {
        return sessions.remove(playerId) != null;
    }

    /**
     * Cleans up the session when a player disconnects.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}
