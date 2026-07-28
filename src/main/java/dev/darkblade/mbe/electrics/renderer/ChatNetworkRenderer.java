package dev.darkblade.mbe.electrics.renderer;

import dev.darkblade.mbe.api.wiring.NetworkNode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders the multimeter inspection result as structured chat messages.
 * Uses legacy § colour codes consistent with the rest of the codebase.
 */
public final class ChatNetworkRenderer {

    /**
     * Snapshot of energy data for a single node that corresponds to a {@code MultiblockInstance}.
     *
     * @param energy    current energy stored
     * @param maxEnergy maximum energy capacity
     */
    public record EnergyReading(long energy, long maxEnergy) {}

    /**
     * Sends the formatted inspection report to the player.
     *
     * @param player      the player to receive the messages
     * @param path        the ordered list of {@link NetworkNode}s on the BFS path
     * @param energyData  map from node UUID to {@link EnergyReading}; absent entries are cable/unknown nodes
     */
    public void render(Player player, List<NetworkNode> path, Map<UUID, EnergyReading> energyData) {
        if (path.isEmpty()) {
            return;
        }

        NetworkNode first = path.get(0);
        String networkType = first.type().id().toUpperCase();
        String graphPrefix = first.id().toString().substring(0, 4).toUpperCase();

        player.sendMessage("§e--- [Multímetro] Red §6" + networkType + "§e (" + graphPrefix + "...) ---");
        player.sendMessage("§7Nodos en el circuito: §f" + path.size());

        for (NetworkNode node : path) {
            dev.darkblade.mbe.api.wiring.BlockPos pos = node.position();
            EnergyReading reading = energyData.get(node.id());

            if (reading != null) {
                // Multiblock instance node — show energy data
                String shortId = node.id().toString().substring(0, 6);
                player.sendMessage(
                        "§7● §b[MB:" + shortId + "] §f(" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"
                );
                player.sendMessage(
                        "  §a" + reading.energy() + "§7/§a" + reading.maxEnergy() + " §7FE"
                );
            } else {
                // Cable or non-energy node — topology only
                player.sendMessage(
                        "§7● §7[Cable] §f(" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"
                );
            }
        }

        player.sendMessage("§e-----------------------------");
    }
}
