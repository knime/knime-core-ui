/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 12, 2016 (hornm): created
 */
package org.knime.core.ui.workflowcoach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeInfo;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeTriple;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.NodeTripleProviderFactory;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;

/**
 * Class that manages the node recommendations. It represents the node recommendations in memory for quick retrieval and
 * provides them accordingly. The {@link #loadRecommendations()}-method updates the statistics, the
 * {@link #getNodeRecommendationFor(NativeNodeContainerUI...)} gives the actual recommendations.
 *
 * @author Martin Horn, University of Konstanz
 * @author Kai Franze, KNIME GmbH
 */
public final class NodeRecommendationManager {

    /**
     * Interface for a listener that gets notified when the recommendations are updated (via
     * {@link NodeRecommendationManager#loadRecommendations()}.
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     */
    @FunctionalInterface
    public interface IUpdateListener {

        /**
         * Called when the recommendations are updated.
         */
        void updated();
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeRecommendationManager.class);

    private static final String SOURCE_NODES_KEY = "<source_nodes>";

    private static final String ALL_NODES_KEY = "<all_nodes>";

    private static final String NODE_NAME_SEP = "#";

    private static final String TRIPLE_PROVIDER_EXTENSION_POINT_ID = "org.knime.core.ui.nodetriples";

    private static final String P_COMMUNITY_NODE_TRIPLE_PROVIDER = "community_node_triple_provider";

    private static final NodeRecommendationManager INSTANCE = new NodeRecommendationManager();

    private final List<IUpdateListener> m_listeners = new ArrayList<>(1);

    private static List<Map<String, RecommendationList>> cachedRecommendations;

    /* modifiable for testing purposes */
    static List<NodeTripleProvider> nodeTripleProviders;

    private Function<String, NodeType> m_nodeTypeSupplier;

    static {
        // Adds preference change listener for community node triple provider
        IPreferenceChangeListener l = event -> {
            if (P_COMMUNITY_NODE_TRIPLE_PROVIDER.equals(event.getKey())) {
                try {
                    INSTANCE.loadRecommendations();
                } catch (IOException ex) {
                    LOGGER.error("Can't load the requested node recommendations: <" + ex.getMessage() + ">", ex);
                }
            }
        };
        InstanceScope.INSTANCE.getNode("org.knime.workbench.workflowcoach").addPreferenceChangeListener(l);
        DefaultScope.INSTANCE.getNode("org.knime.workbench.workflowcoach").addPreferenceChangeListener(l);
    }

    private NodeRecommendationManager() {
        // Singleton class
    }

    /**
     * Returns the singleton instance for this class.
     *
     * @return a singleton instance
     */
    public static NodeRecommendationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the node recommendation manager. I.e. loads the recommendations (if that haven't been loaded, yet)
     * using the 'getNodeType' callback to check for node-presence and determine the node type.
     *
     * @param getNodeType gets the node type for the given node-factory-id; will return {@code null} if there is no node
     *            for the id
     * @return true if {@code loadRecommendations()} recommendations were loaded, false otherwise
     */
    public boolean initialize(final Function<String, NodeType> getNodeType) {
        CheckUtils.checkNotNull(getNodeType);
        m_nodeTypeSupplier = getNodeType;

        // Initially load the recommendations
        try {
            if (cachedRecommendations == null) {
                loadRecommendations();
            } else {
                LOGGER.debug("No need to reload the node recommendations");
            }
            return cachedRecommendations != null;
        } catch (IOException e) {
            LOGGER.error("Failed to initially load the node recommendations", e);
            return false;
        }
    }

    /**
     * Adds a listener that is notified when the recommendations are updated (via {@link #loadRecommendations()}.
     *
     * @param listener a listener
     */
    public void addUpdateListener(final IUpdateListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Removes an update listener.
     *
     * @param listener a listener
     */
    public void removeUpdateListener(final IUpdateListener listener) {
        m_listeners.remove(listener); // NOSONAR: This collection will never be large
    }

    /**
     * (Re-)Loads the recommendations for the node recommendation engine from the currently active node triple
     * providers.
     *
     * @throws IOException if something went wrong while loading the statistics (e.g. a corrupt file)
     * @see #getNodeTripleProviders()
     */
    public void loadRecommendations() throws IOException {
        if (m_nodeTypeSupplier == null) {
            LOGGER.debug("Cannot load recommendations yet. No node type supplier available.");
            return;
        }

        // Read from multiple frequency sources
        List<NodeTripleProvider> providers = getNodeTripleProviders();
        var recommendations = new ArrayList<Map<String, RecommendationList>>(providers.size());

        for (NodeTripleProvider provider : providers) {
            LOGGER.info(String.format("Loading node recommendations from <%s>", provider.getClass().getName()));
            if (provider.isEnabled() && !updateRequired(provider)) {
                Map<String, RecommendationList> recommendationMap = new HashMap<>();
                recommendations.add(recommendationMap);

                provider.getNodeTriples().forEach(
                    nf -> fillRecommendationsMap(recommendationMap, nf, m_nodeTypeSupplier));
                aggregateNodeRecommendations(recommendationMap);
            }
        }

        if (!recommendations.isEmpty()) {
            cachedRecommendations = recommendations; // NOSONAR: This has to be static, but the enclosing method can't
            LOGGER.info("Successfully (re-)loaded all node recommendations available");
        } else {
            cachedRecommendations = null; // NOSONAR: This has to be static, but the enclosing method can't
        }
        m_listeners.stream().forEach(IUpdateListener::updated); // Notify all update listeners after (un-)loading node recommendations
    }

    private record RecommendationList(List<NodeRecommendation> predecessors, List<NodeRecommendation> successors) {
    }

    /*
     * Aggregate multiple occurring id's.
     */
    private static void aggregateNodeRecommendations(final Map<String, RecommendationList> recommendationMap) {
        // apply different aggregation methods for 'independent recommendations' (e.g. source nodes)
        BiConsumer<NodeRecommendation, NodeRecommendation> avgAggr =
            (np1, np2) -> np1.increaseFrequency(np2.getFrequency(), 1);
        BiConsumer<NodeRecommendation, NodeRecommendation> sumAggr =
            (np1, np2) -> np1.increaseFrequency(np2.getFrequency(), 0);
        recommendationMap.entrySet().stream().forEach(entry -> {
            var key = entry.getKey();
            var recommendations = entry.getValue();
            if (key.equals(SOURCE_NODES_KEY) || key.equals(ALL_NODES_KEY)) {
                aggregate(recommendations.successors, sumAggr);
                Collections.sort(recommendations.successors);

                var totalFrequency =
                    recommendations.successors.stream().mapToInt(NodeRecommendation::getFrequency).sum();
                recommendations.successors.forEach(nr -> nr.setTotalFrequency(totalFrequency));
            } else {
                aggregate(recommendations.predecessors, avgAggr);
                aggregate(recommendations.successors, avgAggr);
            }
        });
    }

    private static void fillRecommendationsMap(final Map<String, RecommendationList> recommendationMap,
        final NodeTriple nt, final Function<String, NodeType> getNodeType) {

        var successor = getInternalNodeInfo(nt.getSuccessor(), getNodeType);
        var node = getInternalNodeInfo(nt.getNode().orElse(null), getNodeType);
        var predecessor = getInternalNodeInfo(nt.getPredecessor().orElse(null), getNodeType);

        /* keep track of 'all nodes' to be able to determine the most frequently used nodes */
        if (node != null && node.isKnown()) {
            // place recommendations for ALL_NODES_KEY in successors field by default
            add(recommendationMap, ALL_NODES_KEY, node.id, nt.getCount(), true);
        }

        /* considering the successor only, i.e. for all entries where the predecessor and the node
         * itself is not present
         */
        if (node == null && predecessor == null && successor != null && successor.isSource()) {
         // place recommendations for SOURCE_NODES_KEY in successors field by default
            add(recommendationMap, SOURCE_NODES_KEY, successor.id, nt.getCount(), true);
        }

        /* considering the the node itself as successor, but only for those nodes that don't have a
         * predecessor -> source nodes, i.e. nodes without an input port
         */
        if (predecessor == null && node != null && node.isSource()) {
            add(recommendationMap, SOURCE_NODES_KEY, node.id, nt.getCount(), true);
        }

        /* without predecessor but with the node, if given */
        if (predecessor == null && node != null && successor != null //
            && node.isKnown() && successor.isKnown()) {
            add(recommendationMap, node.id, successor.id, nt.getCount(), true);
            add(recommendationMap, successor.id, node.id, nt.getCount(), false);
        }

        /* considering all triplet, if given */
        if (predecessor != null && node != null && successor != null //
            && predecessor.isKnown() && node.isKnown() && successor.isKnown()) {
            // key: predecessor#node value: successor
            add(recommendationMap, predecessor.id + NODE_NAME_SEP + node.id, successor.id, nt.getCount(), true);
            // key: node#successor value: predecessor
            add(recommendationMap, node.id + NODE_NAME_SEP + successor.id, predecessor.id, nt.getCount(), false);
        }

        /* considering predecessor, regardless of successor */
        if (predecessor != null && node != null //
                && predecessor.isKnown() && node.isKnown()) {
            add(recommendationMap, predecessor.id, node.id, nt.getCount(), true);
            add(recommendationMap, node.id, predecessor.id, nt.getCount(), false);
        }
    }

    private static InternalNodeInfo getInternalNodeInfo(final NodeInfo ni,
        final Function<String, NodeType> getNodeType) {
        if (ni == null) {
            return null;
        }
        String id = ni.getFactoryId();
        NodeType type = null;
        if (id != null) {
            type = getNodeType.apply(id);
        } else {
            // fallback to id-determination prior AP 5.2
            id = ni.getFactory();
            type = getNodeType.apply(id);
            if (type == null) {
                // dynamic node factories before 5.2
                // (doesn't maintain full backwards-compatibility
                //  - new dynamic nodes created for AP >=5.2 won't be found)
                id = id + NODE_NAME_SEP + ni.getName();
                type = getNodeType.apply(id);
            }
        }
        return new InternalNodeInfo(type == null ? null : id, type);
    }

    private static record InternalNodeInfo(String id, NodeType type) {
        boolean isSource() {
            return type == NodeType.Source;
        }

        boolean isKnown() {
            return id != null;
        }
    }

    /**
     * Checks whether the given {@link NodeTripleProvider} requires an update.
     *
     * @param ntp the {@link NodeTripleProvider}
     *
     * @return <code>true</code> if an update is required before the ntp can be used, <code>false</code> otherwise
     */
    private static boolean updateRequired(final NodeTripleProvider ntp) {
        return (ntp instanceof UpdatableNodeTripleProvider untp) && untp.updateRequired();
    }

    /**
     * Adds a new node recommendation to the map. This also updates node recommendations whose names have changed,
     * avoiding duplicated appearances and NPEs.
     */
    private static void add(final Map<String, RecommendationList> recommendation, final String key,
        final String factoryId, final int count, final boolean isSuccesor) {
        RecommendationList p =
            recommendation.computeIfAbsent(key, k -> new RecommendationList(new ArrayList<>(), new ArrayList<>()));
        if (isSuccesor) {
            p.successors.add(new NodeRecommendation(factoryId, count));
        } else {
            p.predecessors.add(new NodeRecommendation(factoryId, count));
        }
    }

    /**
     * Aggregates multiple occurring id's and takes the mean of the frequencies
     *
     * @param l the list is manipulated directly
     * @param aggregationOperation the operation that aggregates the frequency of two node recommendations - important:
     *            only the first node recommendation-object must be manipulated
     */
    private static void aggregate(final List<NodeRecommendation> l,
        final BiConsumer<NodeRecommendation, NodeRecommendation> aggregationOperation) {
        Map<String, NodeRecommendation> aggregates = new HashMap<>();

        for (NodeRecommendation np : l) {
            if (aggregates.containsKey(np.toString())) {
                // aggregate
                NodeRecommendation np2 = aggregates.get(np.toString());
                aggregationOperation.accept(np2, np);
            } else {
                aggregates.put(np.toString(), np);
            }
        }
        l.clear();
        l.addAll(aggregates.values());
    }

    /**
     * Get recommendations for possible successors of the given nodes, see {@link #getNodeRecommendationFor} for more details.
     * @param nnc Array of nodes to get successor recommendations for; if empty, only source nodes will be recommended
     * @return A list of node recommendations
     */
    public List<NodeRecommendation>[] getSuccessorNodeRecommendationFor(final NativeNodeContainerUI... nnc) {
        return getNodeRecommendationFor(true, nnc);
    }

    /**
     * Get recommendations for possible predecessors of the given nodes, see {@link #getNodeRecommendationFor} for more details.
     * @param nnc Array of nodes to get predecessor recommendations for; if empty, only source nodes will be recommended
     * @return A list of node recommendations
     */
    public List<NodeRecommendation>[] getPredecessorNodeRecommendationFor(final NativeNodeContainerUI... nnc) {
        return getNodeRecommendationFor(false, nnc);
    }

    /**
     * Determines lists of node recommendation based on the given nodes (e.g. that are selected in the workflow editor).
     * The {@link NodeRecommendation}s are determined based on the statistics of {@link NodeTriple}s (i.e. predecessor,
     * node, successor, count -> (p,n,s,c)) that are provided by {@link NodeTripleProvider}s.
     *
     * Given the list's of node triples, {(predecessor, node, successor, count/frequency)} = {(p,n,s,c)} and given a
     * selected node 'sn', the recommendations are determined for each node-triple-list as follows:
     *
     * (1) find all node triples (p,n,s,c) where n==sn and add them to the result list; in that case the predecessor is
     * essentially ignored and recommendation are determined only based on n. The recommendation is the successor 's'
     * given by each found triple. Since there will be multiple triples for the same 'n' and therewith successor
     * duplicates (i.e. recommendations), those will be joined by taking the mean of the respective frequencies 'c' (2)
     * determine all current predecessors ('sp') of the selected node 'sn' and find all node triples that match the
     * given predecessor-node pairs ('sp','sn') (i.e. 'sp'='p' and 'sn'='n'). The recommended nodes are the successor
     * nodes 's' given by the found triples. Those are added to the same list as the recommendations of (1). (3)
     * Post-processing: duplicate recommendations are resolved by removing the recommendations with a smaller
     * counts/frequencies
     *
     * If the array of given nodes is empty, all potential source nodes are recommended, i.e. all nodes 'n' in the node
     * triples list that don't have a predecessor 'p'.
     *
     * @param nnc if it's an empty array, source nodes only will be recommended, if more than one node is given, the
     *            node recommendations for different nodes will end up in the same list
     * @return an array of lists of node recommendations, i.e. a list of node recommendations for each used node
     *         {@link NodeTripleProvider}. It will return <code>null</code> if something went wrong with loading the
     *         node statistics!
     */
    @SuppressWarnings("static-method") // Not static to avoid failing initialization
    List<NodeRecommendation>[] getNodeRecommendationFor(final boolean getSuccessors,
        final NativeNodeContainerUI... nnc) {
        if (cachedRecommendations == null) {
            return null; // NOSONAR: Returning null makes sense here
        }
        @SuppressWarnings("unchecked")
        List<NodeRecommendation>[] res = new List[cachedRecommendations.size()]; // NOSONAR: Can't use `var` here
        for (var idx = 0; idx < res.length; idx++) {
            if (nnc.length == 0) {
                // recommendations if no node is given -> source nodes are recommended
                var sourceNodes = cachedRecommendations.get(idx).get(SOURCE_NODES_KEY);
                res[idx] = sourceNodes == null ? Collections.emptyList() : sourceNodes.successors;

            } else if (nnc.length == 1) {
                String nodeID = nnc[0].getNodeFactoryId();
                /* recommendations based on the given node and possible predecessors/successors */
                var set = getSuccessors ? processSucessorNodeRecommendationsForAllPorts(idx, nnc)
                    : processPredecessorNodeRecommendationsForAllPorts(idx, nnc);
                /* recommendation based on the given node only */
                var p1 = cachedRecommendations.get(idx).get(nodeID);
                if (p1 != null) {
                    set.addAll(getSuccessors ? p1.successors : p1.predecessors);
                }
                //add to the result list
                res[idx] = new ArrayList<>(set.size());
                res[idx].addAll(set);
            } else {
                throw new UnsupportedOperationException(
                    "Recommendations for more than one node are not supported, yet.");
            }

            if (nnc.length == 1) {
                /* post-process result */
                Collections.sort(res[idx]);

                // remove the node, the recommendations have been requested for, from the list
                res[idx] = res[idx].stream()//
                    .filter(nr -> !nr.m_factoryId.equals(nnc[0].getNodeFactoryId()))//
                    .collect(Collectors.toList());

                // update the total frequencies
                var totalFrequency = res[idx].stream().mapToInt(NodeRecommendation::getFrequency).sum();
                res[idx].forEach(nr -> nr.setTotalFrequency(totalFrequency));
            }
        }
        return res;
    }

    /**
     * @return list of nodes sorted by their frequency of being used in general
     */
    @SuppressWarnings("static-method") // Not static to avoid failing initialization
    public List<NodeRecommendation>[] getMostFrequentlyUsedNodes() {
        @SuppressWarnings("unchecked")
        List<NodeRecommendation>[] res = new List[cachedRecommendations.size()];
        for (var idx = 0; idx < res.length; idx++) {
            var allNodes = cachedRecommendations.get(idx).get(ALL_NODES_KEY);
            res[idx] = allNodes == null ? Collections.emptyList() : allNodes.successors;
        }
        return res;
    }

    private static Set<NodeRecommendation> processSucessorNodeRecommendationsForAllPorts(final int idx,
        final NativeNodeContainerUI... nnc) {
        Set<NodeRecommendation> set = new HashSet<>();
        for (var i = 0; i < nnc[0].getNrInPorts(); i++) {
            var wfm = nnc[0].getParent();
            var cc = wfm.getIncomingConnectionFor(nnc[0].getID(), i);
            // only take the predecessor if its not leaving the workflow
            // (e.g. the actual predecessor is outside of a metanode)
            if (cc == null || cc.getType() == ConnectionType.WFMIN) {
                return set;
            }
            NodeContainerUI predecessor = wfm.getNodeContainer(cc.getSource());
            if (predecessor instanceof NativeNodeContainerUI nncUI) {
                var map = cachedRecommendations.get(idx);
                var key = nncUI.getNodeFactoryId() + NODE_NAME_SEP + nnc[0].getNodeFactoryId();
                if(map.containsKey(key)) {
                    set.addAll(map.get(key).successors);
                }
            }
        }
        return set;
    }

    private static Set<NodeRecommendation> processPredecessorNodeRecommendationsForAllPorts(final int idx,
        final NativeNodeContainerUI... nnc) {
        Set<NodeRecommendation> set = new HashSet<>();
        for (var i = 0; i < nnc[0].getNrOutPorts(); i++) {
            var wfm = nnc[0].getParent();
            // only take the successors that are not leaving the workflow
            // (e.g. one or more successors are outside of a metanode)
            var cc = wfm.getOutgoingConnectionsFor(nnc[0].getID(), i).stream()
                    .filter(conn -> conn != null || !conn.getType().isLeavingWorkflow()).toList();

            cc.forEach(conn -> {
                NodeContainerUI successor = wfm.getNodeContainer(conn.getDest());
                if (successor instanceof NativeNodeContainerUI nncUI) {
                    var map = cachedRecommendations.get(idx);
                    var key = nnc[0].getNodeFactoryId() + NODE_NAME_SEP + nncUI.getNodeFactoryId();
                    if (map.containsKey(key)) {
                        set.addAll(map.get(key).predecessors);
                    }
                }
            });
        }
        return set;
    }

    /**
     * Returns the number of registered and enabled {@link NodeTripleProvider}s.
     *
     * @return the number of loaded providers
     */
    public static int getNumLoadedProviders() {
        if (cachedRecommendations == null) {
            return 0;
        } else {
            return cachedRecommendations.size();
        }
    }

    /**
     * Checks whether node recommendations can be considered enabled or not.
     *
     * @return True if there are cached recommendations from at least one triple provider loaded, false otherwise.
     */
    public static boolean isEnabled() {
        return getNodeTripleProviders().stream().anyMatch(NodeTripleProvider::isEnabled);
    }

    /**
     * Object representing one node recommendation, including the node template itself and a frequency as a measure of a
     * certainty for the given recommendation.
     *
     * @author Martin Horn, University of Konstanz
     */
    public static class NodeRecommendation implements Comparable<NodeRecommendation> {
        private int m_frequency;

        private String m_factoryId;

        private int m_totalFrequency;

        private int m_num = 1;

        /**
         * Creates a new node recommendation for the given node.
         *
         * @param factoryId
         * @param frequency a frequency of usage
         */
        NodeRecommendation(final String factoryId, final int frequency) {
            m_factoryId = factoryId;
            m_frequency = frequency;
            m_totalFrequency = frequency;
        }

        /**
         * Returns the frequency (in percent), i.e. how often this node recommendation appears in a node triple or pair
         * (given by a {@link NodeTripleProvider}
         *
         * @return the frequency
         */
        public int getFrequency() {
            return (int)Math.round(m_frequency / (double)m_num);
        }

        /**
         * Returns the total frequency summed over all node recommendations that have the SAME predecessor.
         *
         * @return the total frequency
         */
        public int getTotalFrequency() {
            return m_totalFrequency;
        }

        /**
         * Sets the overall frequency that is needed to express the frequency as a probability later on.
         *
         * @param frequency
         */
        private void setTotalFrequency(final int frequency) {
            m_totalFrequency = frequency;
        }

        /**
         * Increases the frequency by the given amount. Needed in order to aggregate frequencies of node recommendations
         * that recommend the same node (e.g. if the selected node only is taken into account and the predecessor
         * ignored). See {@link NodeRecommendationManager#aggregate(List)}.
         *
         * @param freqIncrease the amount of how much to increase the frequency
         * @param countIncrease the amount the count should be increased by which the frequency is in the end divided by
         *            when calling the {@link #getFrequency()}-method. If 1 is passed with every frequency increase, the
         *            mean is essentially taken when finally calling {@link #getFrequency()}. If 0 is passed every time,
         *            {@link #getFrequency()} will return sum of all frequencies provided here.
         */
        private void increaseFrequency(final int freqIncrease, final int countIncrease) {
            m_frequency += freqIncrease;
            m_num += countIncrease;
        }

        /**
         * @return the id as per {@link NodeFactory#getFactoryId()}
         */
        public String getFactoryId() {
            return m_factoryId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final NodeRecommendation o) {
            return -Integer.compare(getFrequency(), o.getFrequency());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_factoryId.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NodeRecommendation other = (NodeRecommendation)obj;
            return Objects.equals(this.m_factoryId, other.m_factoryId);
        }
    }

    /**
     * Returns all available {@link NodeTripleProviderFactory}s. Node triple provider factories can be added via the
     * respective extension point.
     *
     * @return a list of available node triple provider factories
     */
    public static List<NodeTripleProviderFactory> getNodeTripleProviderFactories() {
        List<NodeTripleProviderFactory> l = new ArrayList<>(3);

        // get node triple providers from extension points
        IExtensionPoint extPoint =
            Platform.getExtensionRegistry().getExtensionPoint(TRIPLE_PROVIDER_EXTENSION_POINT_ID);
        assert (extPoint != null) : "Invalid extension point: " + TRIPLE_PROVIDER_EXTENSION_POINT_ID;

        IExtension[] extensions = extPoint.getExtensions();
        for (IExtension ext : extensions) {
            for (IConfigurationElement conf : ext.getConfigurationElements()) {
                try {
                    NodeTripleProviderFactory factory =
                        (NodeTripleProviderFactory)conf.createExecutableExtension("factory-class");
                    l.add(factory);
                } catch (CoreException e) {
                    LOGGER.warn("Could not create factory from " + conf.getAttribute("factory-class"), e);
                }
            }
        }
        return l;
    }

    /**
     * Returns all available {@link NodeTripleProvider}s. Node triple providers are created from their respective
     * {@link NodeTripleProviderFactory} that can be added via the respective extension point. Note that a provider must
     * be enabled to be used in the workflow coach view.
     *
     * @return a list of all available node triple providers
     */
    public static List<NodeTripleProvider> getNodeTripleProviders() {
        if (nodeTripleProviders == null) {
            nodeTripleProviders = getNodeTripleProviderFactories().stream()//
                .flatMap(f -> f.createProviders().stream())//
                .toList();
        }
        return nodeTripleProviders;
    }

    /**
     * Joins the recommendations from multiple sources and removes duplications
     *
     * @param recommendations The original recommendations
     * @return The joined list without recommendations
     */
    public static List<NodeRecommendation[]>
        joinRecommendationsWithoutDuplications(final List<NodeRecommendation>[] recommendations) {
        var recommendationsJoined = joinRecommendations(recommendations);
        return removeDuplicates(recommendationsJoined.stream()).toList();
    }

    /**
     * Removes duplicates from a given stream of recommendations
     *
     * @param recommendations The original recommendations stream
     * @return The stream without duplicates
     */
    public static Stream<NodeRecommendation[]> removeDuplicates(final Stream<NodeRecommendation[]> recommendations) {
        Set<String> duplicates = new HashSet<>();
        return recommendations.filter(nrs -> {
            int idx = getNonNullIdx(nrs);
            return duplicates.add(nrs[idx].toString());
        });
    }

    /**
     * Joins the elements of the recommendation lists by their element ranks, yet taking the possible equality of
     * elements into account.
     *
     * The joining is done as follows:
     *
     * Assume two lists of recommendations, {a1,a2,a3,...} and {b1,b2,b3,...} and, e.g., a2==b2 (i.e. these are the same
     * recommendations) the joined list of arrays is then [a1, null], [null, b1], [a2, b2], [a3, null], [b3, null]
     *
     * @param recommendations n lists of recommendations of possibly different sizes
     *
     * @return a list of recommendation arrays (the array potentially with <code>null</code>-entries) accordingly sorted
     */
    public static List<NodeRecommendation[]> joinRecommendations(final List<NodeRecommendation>[] recommendations) {
        var maxSize = 0;
        for (var l : recommendations) {
            maxSize = Math.max(maxSize, l.size());
        }
        return joinRecommendations(recommendations, maxSize);
    }

    /**
     * Joins the elements of the recommendation lists by their element ranks, yet taking the possible equality of
     * elements into account.
     *
     * The joining is done as follows:
     *
     * Assume two lists of recommendations, {a1,a2,a3,...} and {b1,b2,b3,...} and, e.g., a2==b2 (i.e. these are the same
     * recommendations) the joined list of arrays is then [a1, null], [null, b1], [a2, b2], [a3, null], [b3, null]
     *
     * @param recommendations n lists of recommendations of possibly different sizes
     * @param maxSize the size of the longest list
     *
     * @return a list of recommendation arrays (the array potentially with <code>null</code>-entries) accordingly sorted
     */
    private static List<NodeRecommendation[]> joinRecommendations(final List<NodeRecommendation>[] recommendations,
        final int maxSize) {
        return IntStream.range(0, maxSize)//
            .mapToObj(rank -> joinRecommendationsForRank(recommendations, rank))//
            .flatMap(List::stream)//
            .toList();
    }

    private static List<NodeRecommendation[]>
        joinRecommendationsForRank(final List<NodeRecommendation>[] recommendations, final int rank) {
        List<NodeRecommendation[]> recommendationsForRank = new ArrayList<>();
        if (recommendations.length == 1) {
            recommendationsForRank.add(new NodeRecommendation[]{recommendations[0].get(rank)});
        } else {
            var tuple = new NodeRecommendation[recommendations.length];
            IntStream.range(0, tuple.length).forEach(tripleProviderIdx -> {
                if (rank < recommendations[tripleProviderIdx].size()) {
                    tuple[tripleProviderIdx] = recommendations[tripleProviderIdx].get(rank);
                }
            });
            NodeRecommendation[] same;
            while ((same = getMaxSameElements(tuple)) != null) {
                recommendationsForRank.add(same);
            }
        }
        return recommendationsForRank;
    }

    /**
     * Determines the maximum number of same elements and returns them as a new array where all other elements are set
     * to <code>null</code>. The found elements are also removed from the array passed as parameter (i.e. set to
     * <code>null</code>).
     *
     * @param ar the array to check, it will be manipulated as well!
     * @return array of same length as <code>ar</code>, with the found elements non-null. <code>null</code> will be
     *         returned if <code>ar</code> only consists of <code>null</code>-entries
     */
    private static NodeRecommendation[] getMaxSameElements(final NodeRecommendation[] ar) {
        NodeRecommendation[] res = ar.clone();
        for (var i = ar.length; i > 0; i--) {
            var it = CombinatoricsUtils.combinationsIterator(ar.length, i);
            while (it.hasNext()) {
                int[] indices = it.next();
                NodeRecommendation el = ar[indices[0]];
                if (el == null) {
                    continue;
                }
                for (var j = 1; j < indices.length; j++) {
                    if (ar[indices[j]] == null || !el.equals(ar[indices[j]])) {
                        el = null;
                        break;
                    }
                }
                if (el != null) {
                    Arrays.fill(res, null);
                    for (var j = 0; j < indices.length; j++) {
                        res[indices[j]] = ar[indices[j]];
                        ar[indices[j]] = null;
                    }
                    return res;
                }
            }
        }
        return null; // NOSONAR: null returned on purpose here
    }

    private static int getNonNullIdx(final NodeRecommendation[] arr) {
        for (var i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                return i;
            }
        }
        return -1;
    }

}
