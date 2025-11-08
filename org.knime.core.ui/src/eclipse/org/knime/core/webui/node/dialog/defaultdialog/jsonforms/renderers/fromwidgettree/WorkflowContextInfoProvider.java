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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParametersInput;

/**
 * Helper class to provide workflow context information for file chooser renderers. Provides isLocal,
 * connectedFSOptions, and spaceFSOptions based on workflow context and port information.
 *
 * @author Paul Bärnreuther
 */
final class WorkflowContextInfoProvider {

    private final NodeParametersInput m_nodeParametersInput;

    private final boolean m_allowsConnectedFS;

    private final boolean m_allowsSpaceFS;

    private final boolean m_allowsLocalFS;

    WorkflowContextInfoProvider(final NodeParametersInput nodeParametersInput,
        final FileSystemOption[] fileSystemOptions) {
        m_nodeParametersInput = nodeParametersInput;
        m_allowsConnectedFS =
            fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.CONNECTED);
        m_allowsSpaceFS = fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.SPACE);
        m_allowsLocalFS = fileSystemOptions == null || ArrayUtils.contains(fileSystemOptions, FileSystemOption.LOCAL);
    }

    /**
     * @return whether the current execution environment is local (which enables local file system access)
     */
    Optional<Boolean> getIsLocal() {
        if (!m_allowsLocalFS) {
            return Optional.empty();
        }
        return WorkflowContextUtil.getWorkflowContextV2Optional()
            .map(context -> context.getExecutorType() == ExecutorType.ANALYTICS_PLATFORM);
    }

    /**
     * @return options for connected file system. Required if file system option CONNECTED is used.
     */
    Optional<FileChooserRendererOptions.ConnectedFSOptions> getConnectedFSOptions() {
        if (!m_allowsConnectedFS) {
            return Optional.empty();
        }
        return getFirstFileSystemPortIndex().flatMap(portIndex -> m_nodeParametersInput.getInPortSpec(portIndex)
            .map(spec -> toFileSystemPortObjectSpec(spec, portIndex))
            .map(spec -> createConnectedFSOptions(spec, portIndex)));
    }

    /**
     * @return options for space file system. Required if file system option SPACE is used.
     */
    Optional<FileChooserRendererOptions.SpaceFSOptions> getSpaceFSOptions() {
        if (!m_allowsSpaceFS) {
            return Optional.empty();
        }
        return WorkflowContextUtil.getWorkflowContextV2Optional()
            .flatMap(context -> createSpaceFSOptions(context.getLocationInfo()));
    }

    private Optional<Integer> getFirstFileSystemPortIndex() {
        final var inPortTypes = m_nodeParametersInput.getInPortTypes();
        return IntStream.range(0, inPortTypes.length)
            .filter(i -> FileSystemPortObjectSpec.class.equals(inPortTypes[i].getPortObjectSpecClass())).boxed()
            .findFirst();
    }

    private static FileSystemPortObjectSpec toFileSystemPortObjectSpec(final PortObjectSpec spec, final int portIndex) {
        if (spec instanceof FileSystemPortObjectSpec fsSpec) {
            return fsSpec;
        }
        throw new IllegalStateException(String.format("Port at index %s is not a file system port", portIndex));
    }

    private static FileChooserRendererOptions.ConnectedFSOptions
        createConnectedFSOptions(final FileSystemPortObjectSpec spec, final int portIndex) {
        return new FileChooserRendererOptions.ConnectedFSOptions() {
            @Override
            public String getFileSystemType() {
                return spec.getFileSystemType();
            }

            @Override
            public String getFileSystemSpecifier() {
                return spec.getFSLocationSpec().getFileSystemSpecifier().orElse(null);
            }

            @Override
            public boolean isFileSystemConnectionMissing() {
                return spec.getFileSystemConnection().isEmpty();
            }

            @Override
            public int getPortIndex() {
                return portIndex;
            }
        };
    }

    private static Optional<FileChooserRendererOptions.SpaceFSOptions> createSpaceFSOptions(final Object locationInfo) {
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
            });
        }
        return Optional.empty();
    }
}
