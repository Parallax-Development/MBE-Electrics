package dev.darkblade.mbe.electrics.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.wiring.NetworkConnection;
import dev.darkblade.mbe.api.wiring.NetworkGraph;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.electrics.renderer.ChatNetworkRenderer;
import dev.darkblade.mbe.electrics.service.ElectricsService;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the multimeter "Inspect" action.
 *
 * <p>Interaction model (two-click closed-circuit):
 * <ol>
 *   <li>First {@code RIGHT_CLICK} on a network node → stores it as the pending endpoint.</li>
 *   <li>Second {@code RIGHT_CLICK} on a second node → performs BFS path-finding between
 *       the two endpoints, collects topology and energy data, and renders the result to chat.</li>
 * </ol>
 */
public final class InspectNetworkAction implements ToolAction {

    private static final String PERMISSION = "mbe.tool.mode.inspect";

    private final NetworkService networkService;
    private final ElectricsService electricsService;
    private final MultimeterSessionService sessionService;
    private final ChatNetworkRenderer renderer;

    public InspectNetworkAction(
            NetworkService networkService,
            ElectricsService electricsService,
            MultimeterSessionService sessionService,
            ChatNetworkRenderer renderer
    ) {
        this.networkService = networkService;
        this.electricsService = electricsService;
        this.sessionService = sessionService;
        this.renderer = renderer;
    }

    @Override
    public ActionId id() {
        return MultimeterActions.INSPECT;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        if (context.player() == null || context.clickedBlock() == null) {
            return WrenchResult.pass();
        }

        if (!context.player().hasPermission(PERMISSION)) {
            return WrenchResult.pass();
        }

        Block clickedBlock = context.clickedBlock();
        Collection<NetworkNode> nodes = resolveNodesAt(clickedBlock);

        if (nodes.isEmpty()) {
            context.player().sendMessage("§7[Multímetro] §cNo hay un nodo de red en ese bloque o en sus alrededores.");
            return WrenchResult.pass();
        }

        // Use the first resolved node (energy type preferred if multiple)
        NetworkNode resolvedNode = resolvePreferredNode(nodes);
        Optional<MultimeterSessionService.PendingSelection> pending = sessionService.get(context.player().getUniqueId());

        if (pending.isEmpty()) {
            // Phase 1: store first endpoint
            sessionService.store(context.player().getUniqueId(), resolvedNode);
            dev.darkblade.mbe.api.wiring.BlockPos pos = resolvedNode.position();
            context.player().sendMessage(
                    "§7[Multímetro] §aPrimer extremo seleccionado §7@ §f("
                            + pos.x() + ", " + pos.y() + ", " + pos.z() + ")§7."
            );
            context.player().sendMessage("§7Haz §aDerecho-Click §7en el segundo extremo para cerrar el circuito.");
            return WrenchResult.success("mbe-electrics.multimeter.first-selected");
        }

        // Phase 2: second endpoint — evaluate the circuit
        NetworkNode nodeA = pending.get().node();
        NetworkNode nodeB = resolvedNode;
        sessionService.clear(context.player().getUniqueId());

        // Reject if same node clicked twice
        if (nodeA.id().equals(nodeB.id())) {
            context.player().sendMessage("§7[Multímetro] §cAmbos extremos son el mismo nodo.");
            return WrenchResult.pass();
        }

        // Resolve graphs
        NetworkGraph graphA = networkService.getGraph(nodeA.type(), nodeA);
        NetworkGraph graphB = networkService.getGraph(nodeB.type(), nodeB);

        if (graphA == null || graphB == null || !graphA.id().equals(graphB.id())) {
            context.player().sendMessage("§7[Multímetro] §cLos nodos pertenecen a redes distintas.");
            return WrenchResult.pass();
        }

        // BFS path-finding
        List<NetworkNode> path = bfsPath(nodeA, nodeB, graphA);
        if (path.isEmpty()) {
            context.player().sendMessage("§7[Multímetro] §cNo hay circuito entre los puntos seleccionados.");
            return WrenchResult.pass();
        }

        // Collect energy data for nodes on the path
        Map<UUID, ChatNetworkRenderer.EnergyReading> energyData = collectEnergyData(path, clickedBlock.getWorld());

        renderer.render(context.player(), path, energyData);
        return WrenchResult.success("mbe-electrics.multimeter.inspect-complete");
    }

    /**
     * Tries to find network nodes at the given block. If none are found at the exact position,
     * falls back to scanning the 6 face-adjacent blocks. This allows the multimeter to work when
     * the player clicks a structural block of a multiblock whose network node is registered at
     * the anchor or at an adjacent port block.
     */
    private Collection<NetworkNode> resolveNodesAt(Block block) {
        Collection<NetworkNode> direct = networkService.findAllNodes(block);
        if (!direct.isEmpty()) {
            return direct;
        }
        // Fallback: check face-adjacent blocks
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            Collection<NetworkNode> adjacent = networkService.findAllNodes(block.getRelative(face));
            if (!adjacent.isEmpty()) {
                return adjacent;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Prefers an ENERGY-type node when multiple nodes exist at the same block.
     * Falls back to the first node in the collection.
     */
    private NetworkNode resolvePreferredNode(Collection<NetworkNode> nodes) {
        for (NetworkNode node : nodes) {
            if (node.type() == dev.darkblade.mbe.api.wiring.NetworkType.ENERGY) {
                return node;
            }
        }
        return nodes.iterator().next();
    }

    /**
     * BFS over the graph's connections to find the shortest path from {@code from} to {@code to}.
     *
     * @param from  starting node
     * @param to    target node
     * @param graph the graph containing both nodes
     * @return ordered list of nodes on the path (inclusive of both endpoints), or empty if no path
     */
    private List<NetworkNode> bfsPath(NetworkNode from, NetworkNode to, NetworkGraph graph) {
        // Build adjacency map from connections (undirected)
        Map<UUID, List<NetworkNode>> adjacency = buildAdjacency(graph);

        Map<UUID, UUID> parentMap = new HashMap<>();
        Deque<NetworkNode> queue = new ArrayDeque<>();

        queue.add(from);
        parentMap.put(from.id(), null); // mark visited with null parent

        while (!queue.isEmpty()) {
            NetworkNode current = queue.poll();

            if (current.id().equals(to.id())) {
                return reconstructPath(parentMap, graph, from.id(), to.id());
            }

            List<NetworkNode> neighbours = adjacency.getOrDefault(current.id(), Collections.emptyList());
            for (NetworkNode neighbour : neighbours) {
                if (!parentMap.containsKey(neighbour.id())) {
                    parentMap.put(neighbour.id(), current.id());
                    queue.add(neighbour);
                }
            }
        }

        return Collections.emptyList(); // no path found
    }

    /**
     * Builds an undirected adjacency list from the graph's connections.
     */
    private Map<UUID, List<NetworkNode>> buildAdjacency(NetworkGraph graph) {
        Map<UUID, NetworkNode> nodeById = new HashMap<>();
        for (NetworkNode node : graph.nodes()) {
            nodeById.put(node.id(), node);
        }

        Map<UUID, List<NetworkNode>> adjacency = new HashMap<>();
        for (NetworkConnection conn : graph.connections()) {
            NetworkNode f = conn.from();
            NetworkNode t = conn.to();
            adjacency.computeIfAbsent(f.id(), k -> new ArrayList<>()).add(t);
            adjacency.computeIfAbsent(t.id(), k -> new ArrayList<>()).add(f);
        }

        return adjacency;
    }

    /**
     * Reconstructs the path from {@code startId} to {@code endId} using the parent map produced by BFS.
     */
    private List<NetworkNode> reconstructPath(
            Map<UUID, UUID> parentMap,
            NetworkGraph graph,
            UUID startId,
            UUID endId
    ) {
        // Index nodes by id for O(1) lookup
        Map<UUID, NetworkNode> nodeById = new HashMap<>();
        for (NetworkNode node : graph.nodes()) {
            nodeById.put(node.id(), node);
        }

        LinkedList<NetworkNode> path = new LinkedList<>();
        UUID current = endId;

        while (current != null) {
            NetworkNode node = nodeById.get(current);
            if (node != null) {
                path.addFirst(node);
            }
            current = parentMap.get(current);
        }

        return path;
    }

    /**
     * Collects energy readings for each node that corresponds to a registered {@code MultiblockInstance}.
     *
     * @param path  the nodes on the BFS path
     * @param world the world used to resolve block positions (may be {@code null} for nodes in other worlds)
     * @return map from node UUID to {@link ChatNetworkRenderer.EnergyReading}
     */
    private Map<UUID, ChatNetworkRenderer.EnergyReading> collectEnergyData(List<NetworkNode> path, World world) {
        Map<UUID, ChatNetworkRenderer.EnergyReading> result = new HashMap<>();

        for (NetworkNode node : path) {
            dev.darkblade.mbe.api.wiring.BlockPos pos = node.position();

            World nodeWorld = world;
            if (pos.worldId() != null) {
                World resolved = org.bukkit.Bukkit.getWorld(pos.worldId());
                if (resolved != null) {
                    nodeWorld = resolved;
                }
            }

            if (nodeWorld == null) {
                continue;
            }

            org.bukkit.Location loc = new org.bukkit.Location(nodeWorld, pos.x(), pos.y(), pos.z());
            dev.darkblade.mbe.core.domain.MultiblockInstance instance = electricsService.getInstance(loc);

            if (instance == null) {
                continue;
            }

            Object energyObj = instance.getVariable("energy");
            Object maxEnergyObj = instance.getVariable("max_energy");

            if (energyObj instanceof Number) {
                long energy = ((Number) energyObj).longValue();
                long maxEnergy = (maxEnergyObj instanceof Number) ? ((Number) maxEnergyObj).longValue() : 0L;
                result.put(node.id(), new ChatNetworkRenderer.EnergyReading(energy, maxEnergy));
            }
        }

        return result;
    }
}
