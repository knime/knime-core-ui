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
 */
package org.knime.core.ui.component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.TemplateUpdater;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;

/**
 * Helper to identify components that needs an update.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class CheckForComponentUpdatesUtil {

    private CheckForComponentUpdatesUtil() {
        // utility
    }

    /**
     * Checks whether and which components require an update. It also changes the update-status of the components (i.e.
     * {@link MetaNodeTemplateInformation#getUpdateStatus()}).
     *
     * @param hostWFM
     * @param pluginId the unique identifier of the relevant plug-in; passed to the {@link Status}-object encapsulated
     *            in the returned {@link ComponentUpdatesResult}
     * @param candidateList
     * @param monitor
     * @return the component update result
     * @throws InterruptedException
     * @throws IllegalStateException
     */
    public static ComponentUpdatesResult checkForComponentUpdatesAndSetUpdateStatus(final WorkflowManager hostWFM,
        final String pluginId, final List<NodeID> candidateList, final IProgressMonitor monitor)
        throws InterruptedException, IllegalStateException {
        NodeContext.pushContext(hostWFM);
        try {
            return checkForComponentUpdatesAndSetUpdateStatusWithContext(hostWFM, pluginId, candidateList, monitor);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private static ComponentUpdatesResult checkForComponentUpdatesAndSetUpdateStatusWithContext(
        final WorkflowManager hostWFM, final String pluginId, final List<NodeID> candidateList,
        final IProgressMonitor monitor) throws IllegalStateException, InterruptedException {

        // TODO: Include IProgressMonitor in TemplateOperationContext.
        monitor.beginTask("Checking Link Updates", candidateList.size());

        final var nodeIds = candidateList.toArray(NodeID[]::new);
        // TODO: Think about error notification. Before, this was done via exception messages.
        final var updateStates = TemplateUpdater.forWorkflowManager(hostWFM).withContext()//
            .loadHelper(new WorkflowLoadHelper(true, hostWFM.getContextV2()))//
            .loadResult(new LoadResult("ignored"))//
            .perform(t -> t.checkUpdateForTemplates(nodeIds, false));
        final List<NodeID> updateList = new LinkedList<>();

        // reduce multiple statuses into one MultiStatus
        final var allStatuses = new Status[candidateList.size()];
        var i = 0;
        var overallStatusLevel = IStatus.OK;
        for (Map.Entry<NodeID, UpdateStatus> entry : updateStates.entrySet()) {
            final var nodeId = entry.getKey();
            final var updateStatus = entry.getValue();
            final var tnc = hostWFM.findNodeContainer(nodeId);
            monitor.subTask(tnc.getNameWithID());

            final var status = createTemplateStatus(updateStatus, (NodeContainerTemplate)tnc, updateList, pluginId);
            // if at least one WARNING level status was detected, entire status will be on WARNING level
            if (status.getSeverity() == IStatus.WARNING) {
                overallStatusLevel = IStatus.WARNING;
            }

            if (monitor.isCanceled()) {
                throw new InterruptedException("Update check canceled");
            }
            allStatuses[i] = status;
            i++;
            monitor.worked(1);

        }
        final var overallStatus = new MultiStatus(pluginId, overallStatusLevel, allStatuses, null, null);
        monitor.done();
        return new ComponentUpdatesResult(overallStatus, updateList);
    }

    /**
     * Builds a Status object from the the retrieve UpdateStatus. An UpdateStatus.Error will be masked if a parent node
     * has an update available
     *
     * @param updateStatus status per node template
     * @param tnc node container template
     * @param updateList will be modified!
     * @param pluginId
     * @return status object
     */
    private static Status createTemplateStatus(final UpdateStatus updateStatus, final NodeContainerTemplate tnc,
        final List<NodeID> updateList, final String pluginId) {
        final var nodeId = tnc.getID();
        final var tncName = tnc.getNameWithID();

        switch (updateStatus) {
            case HasUpdate:
                updateList.add(nodeId);
                return new Status(IStatus.OK, pluginId, "Update available for " + tncName);
            case UpToDate:
                return new Status(IStatus.OK, pluginId, "No update available for " + tncName);
            case Error: // NOSONAR
                // if an update for a parent was found, ignore the child's error
                if (!updateableParentExists(nodeId, updateList)) {
                    final var sourceURI = tnc.getTemplateInformation().getSourceURI();
                    Optional<KNIMEURIDescription> d = ResolverUtil.toDescription(sourceURI, new NullProgressMonitor());
                    var s = d.map(KNIMEURIDescription::toDisplayString).orElse(Objects.toString(sourceURI));
                    return new Status(IStatus.WARNING, pluginId,
                        "Unable to check for update of \"" + tncName + "\"; can't read " + s, null);
                } else {
                    return new Status(IStatus.OK, pluginId,
                        "Update error exists, but could be resolved by parent update for " + tncName);
                }
            default:
                return new Status(IStatus.WARNING, pluginId, "Could not resolve update status for " + tncName, null);
        }
    }

    /**
     * Checks whether for a given nodeID a parent already has an update found. Note: this absolutely relies on the node
     * templates being scanned in the correct order, from outer to inner, which will be due to how
     * {@link CheckUpdateMetaNodeLinkAction#getMetaNodesToCheck()} works.
     *
     * @param id NodeID
     * @return does a parent have an update available?
     */
    private static boolean updateableParentExists(final NodeID id, final List<NodeID> updateList) {
        return updateList.stream().anyMatch(id::hasPrefix);
    }

    /**
     * @param status update status
     * @param updateList ids of components that require an update
     */
    public static record ComponentUpdatesResult(Status status, List<NodeID> updateList) {
    }
}
