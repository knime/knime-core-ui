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
 *   Nov 5, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.IPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.ConnectedFSOptionsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions.ConnectedFSOptions;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Helper class to provide workflow context information for file chooser renderers. Provides isLocal,
 * connectedFSOptions, and spaceFSOptions based on workflow context and port information.
 *
 * @author Paul Bärnreuther
 */
public final class WorkflowContextInfoProvider {

    private final NodeParametersInput m_nodeParametersInput;

    private final boolean m_allowsConnectedFS;

    private final boolean m_allowsSpaceFS;

    private final boolean m_allowsLocalFS;

    private final boolean m_hasNoConnectedFSOptionsProvider;

    WorkflowContextInfoProvider( //
        final NodeParametersInput nodeParametersInput, //
        final FileSystemOption[] fileSystemOptions, //
        final Class<? extends StateProvider<Optional<ConnectedFSOptions>>> connectedFSOptionsProvider //
    ) {
        m_nodeParametersInput = nodeParametersInput;
        m_allowsConnectedFS =
            fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.CONNECTED);
        m_allowsSpaceFS = fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.SPACE);
        m_allowsLocalFS = fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.LOCAL);
        m_hasNoConnectedFSOptionsProvider = connectedFSOptionsProvider == null //
            || ConnectedFSOptionsProvider.class.equals(connectedFSOptionsProvider);
    }

    /**
     * @return whether the current execution environment is local (which enables local file system access)
     */
    Optional<Boolean> getIsLocal() {
        if (!m_allowsLocalFS) {
            return Optional.empty();
        }
        return isLocal();
    }

    /**
     * Whether the local file system is supported in the current workflow context.
     *
     * @return true if the local file system is supported, false if it is not supported, or empty if it is unknown (e.g.
     *         because there is no workflow context)
     */
    public static Optional<Boolean> isLocal() {
        return WorkflowContextUtil.getWorkflowContextV2Optional()
            .map(context -> context.getExecutorType() == ExecutorType.ANALYTICS_PLATFORM);
    }

    /**
     * @return options for connected file system. Required if file system option CONNECTED is used but can be superseded
     *         if the options are provided by a state provider.
     */
    Optional<FileChooserRendererOptions.ConnectedFSOptions> getConnectedFSOptions() {
        if (m_allowsConnectedFS && m_hasNoConnectedFSOptionsProvider) {
            return getFirstFileSystemPortIndex().flatMap(portIndex -> m_nodeParametersInput.getInPortSpec(portIndex)
                .map(spec -> ConnectedFSOptions.fromSpec(spec, portIndex)));
        }
        return Optional.empty();
    }

    /**
     * @return options for space file system. Required if file system option SPACE is used.
     */
    Optional<FileChooserRendererOptions.SpaceFSOptions> getSpaceFSOptions() {
        if (!m_allowsSpaceFS) {
            return Optional.empty();
        }
        return WorkflowContextUtil.getWorkflowContextV2Optional()
            .flatMap(WorkflowContextInfoProvider::createSpaceFSOptions);
    }

    /**
     * @param nodeParametersInput the node parameters input
     * @return whether any input port is a file system port
     */
    public static boolean hasConnectedFileSystemPort(final NodeParametersInput nodeParametersInput) {
        return getFirstFileSystemPortIndex(nodeParametersInput).isPresent();
    }

    /**
     * @param nodeParametersInput the node parameters input
     * @return the index of the first file system port, if any
     */
    public static Optional<Integer> getFirstFileSystemPortIndex(final NodeParametersInput nodeParametersInput) {
        final var inPortTypes = nodeParametersInput.getInPortTypes();
        return IntStream.range(0, inPortTypes.length)
            .filter(i -> FileSystemPortObjectSpec.class.equals(inPortTypes[i].getPortObjectSpecClass())).boxed()
            .findFirst();
    }

    private Optional<Integer> getFirstFileSystemPortIndex() {
        return getFirstFileSystemPortIndex(m_nodeParametersInput);
    }

    private static Optional<FileChooserRendererOptions.SpaceFSOptions>
        createSpaceFSOptions(final WorkflowContextV2 contextV2) {
        final var locationInfo = contextV2.getLocationInfo();
        if (locationInfo instanceof LocalLocationInfo) {
            return Optional.of(new FileChooserRendererOptions.SpaceFSOptions() {
                @Override
                public String getMountId() {
                    return "Local space";
                }

                @Override
                public String getSpacePath() {
                    return null;
                }

                @Override
                public Optional<String> getRelativeWorkflowPath() {
                    return getLocalCurrentWorkflowPath(contextV2);
                }
            });
        } else if (locationInfo instanceof HubSpaceLocationInfo hubSpace) {
            return Optional.of(new FileChooserRendererOptions.SpaceFSOptions() {
                @Override
                public String getMountId() {
                    return hubSpace.getDefaultMountId();
                }

                @Override
                public String getSpacePath() {
                    return hubSpace.getSpacePath();
                }

                @Override
                public Optional<String> getRelativeWorkflowPath() {
                    final var restCurrentWorkflowPath = getRestCurrentWorkflowPath(hubSpace);
                    final var spacePathString = hubSpace.getSpacePath();
                    if (spacePathString == null || restCurrentWorkflowPath.isEmpty()) {
                        return restCurrentWorkflowPath;
                    }

                    final var relWorkflowPath = IPath.forPosix(restCurrentWorkflowPath.get());
                    final var relSpacePath = IPath.forPosix(spacePathString);
                    return Optional.of(relWorkflowPath.makeRelativeTo(relSpacePath).toString());
                }

            });
        } else if (locationInfo instanceof ServerLocationInfo server) {
            return Optional.of(new FileChooserRendererOptions.SpaceFSOptions() {
                @Override
                public String getMountId() {
                    return server.getDefaultMountId();
                }

                @Override
                public String getSpacePath() {
                    return null;
                }

                @Override
                public Optional<String> getRelativeWorkflowPath() {
                    return getRestCurrentWorkflowPath(server).map(IPath::forPosix) //
                        .map(IPath::makeRelative) //
                        .map(IPath::toString);
                }
            });
        }
        return Optional.empty();
    }

    /**
     * @return the display name for the current space based on the workflow context, or "Current space" as fallback
     */
    public static String getSpaceName() {
        return WorkflowContextUtil.getWorkflowContextV2Optional().map(context -> {
            final var locationInfo = context.getLocationInfo();
            if (locationInfo instanceof LocalLocationInfo) {
                return "Local space";
            } else if (locationInfo instanceof HubSpaceLocationInfo hubSpace) {
                return hubSpace.getDefaultMountId();
            } else if (locationInfo instanceof ServerLocationInfo server) {
                return server.getDefaultMountId();
            }
            return "Current space";
        }).orElse("Current space");
    }

    /**
     * Copied from model of the "Extract Context Properties" node.
     */
    private static Optional<String> getLocalCurrentWorkflowPath(final WorkflowContextV2 contextV2) {
        if (contextV2 == null) {
            return Optional.empty();
        }

        final var executorInfo = (AnalyticsPlatformExecutorInfo)contextV2.getExecutorInfo();
        final var mountPoint = executorInfo.getMountpoint().orElse(null);
        if (mountPoint == null) {
            return Optional.empty();
        }

        final var wfLocation = executorInfo.getLocalWorkflowPath().toAbsolutePath();
        final var mpLocation = mountPoint.getSecond().toAbsolutePath();
        CheckUtils.checkState(wfLocation.startsWith(mpLocation),
            "Workflow '%s' is not contained in mountpoint root '%s'.", wfLocation, mpLocation);
        final var relativeOSPath = mpLocation.relativize(wfLocation).toString();
        return Optional.of(IPath.fromOSString(relativeOSPath).toString());
    }

    private static Optional<String> getRestCurrentWorkflowPath(final RestLocationInfo hubSpace) {
        return Optional.ofNullable(hubSpace.getWorkflowPath());
    }
}
