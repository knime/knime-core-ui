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
 *   Feb 16, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSLocationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree.WorkflowContextInfoProvider;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions.ConnectedFSOptions;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.URIFSConnectionConfig;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.internal.StateProviderInitializerInternal;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;

/**
 * Modern file system chooser. Use a {@link LegacyFileSystemSelection} in case you need to be backwards-compatible to
 * existing file system options.
 *
 * @author Paul Bärnreuther
 */
final class FileSystemSelection implements NodeParameters {
    /**
     * Default constructor that initializes the file system type based on the workflow context.
     *
     * @param parametersInput the parameters input that can be used to check for connected file system ports.
     */
    public FileSystemSelection(final NodeParametersInput parametersInput) {
        m_fileSystemType = getDefaultFileSystem(parametersInput);
    }

    private static FileSystemType getDefaultFileSystem(final NodeParametersInput parametersInput) {
        if (parametersInput != null && hasConnectedFileSystemPort(parametersInput)) {
            return FileSystemType.CONNECTED;
        } else if (FSLocationUtil.isRemoteWorkflowContext()) {
            return FileSystemType.SPACE;
        } else {
            return FileSystemType.LOCAL;
        }
    }

    FileSystemSelection() {
        this((NodeParametersInput)null);
    }

    private FileSystemSelection(final FileSystemType category) {
        m_fileSystemType = category;
    }

    private FileSystemSelection(final RelativeToSpaceOrWorkflow relativeTo) {
        this(FileSystemType.SPACE);
        m_spaceOrWorkflow = relativeTo;
    }

    private FileSystemSelection(final FileSystemType category, final int timeout) {
        this(category);
        m_timeout = timeout;
    }

    @Widget(title = "File system", description = "Select one of the following options for the file system to use:")
    @ChoicesProvider(FileSystemType.AdaptedLabelsChoicesProvider.class)
    @Effect(predicate = NoLongerSupportedFileSystemParameters.LegacyFileSystemSpec.LegacyFileSystemIsPresent.class,
        type = EffectType.HIDE)
    @ValueProvider(SetConnectedIfPortExistsOnLoadOrSetDefaultOnButtonClick.class)
    @ValueReference(FileSystemType.Ref.class)
    FileSystemType m_fileSystemType = FileSystemType.LOCAL;

    /**
     * Hidden setting just to store the specifier of the connected file system in case a port is connected.
     */
    @ValueProvider(SetConnectedFileSystemSpecifier.class)
    @ValueReference(ConnectedFileSystemSpecifierRef.class)
    String m_connectedFileSystemSpecifier;

    interface ConnectedFileSystemSpecifierRef extends ParameterReference<String> {
    }

    static final class SetConnectedIfPortExistsOnLoadOrSetDefaultOnButtonClick
        implements StateProvider<FileSystemType> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            /**
             * We assume that the model is capable of inferring the connected file system as well so we do not want or
             * need to make the dialog dirty here.
             */
            ((StateProviderInitializerInternal)initializer).computeOnParametersLoaded();
        }

        @Override
        public FileSystemType computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (hasConnectedFileSystemPort(parametersInput)) {
                return FileSystemType.CONNECTED;
            }
            throw new StateComputationFailureException();

        }

    }

    static final class SetConnectedFileSystemSpecifier implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (hasConnectedFileSystemPort(parametersInput)) {
                return WorkflowContextInfoProvider.getFirstFileSystemPortIndex(parametersInput)
                    .flatMap(portIndex -> parametersInput.getInPortSpec(portIndex)
                        .map(spec -> ConnectedFSOptions.fromSpec(spec, portIndex)))
                    .map(ConnectedFSOptions::getFileSystemSpecifier).orElse("");
            }
            return "";
        }
    }

    enum FileSystemType {
            @Label(value = "Connected file system", description = "Connect a file system port to this node to use it.")
            CONNECTED, //
            @Label(value = "Local file system", description = "The local file system of the machine running KNIME."
                + " Note that this option is not available when working remotely.")
            LOCAL, //
            @Label(value = "Current space", description = "The space that contains the current workflow.")
            SPACE, //
            @Label(value = "Embedded data", description = "The embedded data of the current workflow.")
            EMBEDDED, //
            @Label(value = "URL", description = "A custom URL.")
            URL;

        interface Ref extends ParameterReference<FileSystemType> {
        }

        static final class AdaptedLabelsChoicesProvider implements EnumChoicesProvider<FileSystemType> {

            @Override
            public List<EnumChoice<FileSystemType>> computeState(final NodeParametersInput context) {
                if (hasConnectedFileSystemPort(context)) {
                    final var fsOptionsOpt =
                        WorkflowContextInfoProvider.getFirstFileSystemPortIndex(context).flatMap(portIndex -> context
                            .getInPortSpec(portIndex).map(spec -> ConnectedFSOptions.fromSpec(spec, portIndex)));
                    final var connectedFSName =
                        fsOptionsOpt.map(ConnectedFSOptions::getFileSystemType).orElse("Connected file system");
                    return List.of(new EnumChoice<>(CONNECTED, connectedFSName));
                } else {
                    final var choices = new ArrayList<EnumChoice<FileSystemType>>();
                    if (WorkflowContextInfoProvider.isLocal().orElse(false)) {
                        choices.add(EnumChoice.fromEnumConst(LOCAL));
                    }
                    choices.add(new EnumChoice<>(SPACE, String.format("Current space (%s)", getSpaceName())));
                    choices.add(EnumChoice.fromEnumConst(EMBEDDED));
                    choices.add(EnumChoice.fromEnumConst(URL));
                    return choices;
                }
            }

        }

    }

    private static boolean hasConnectedFileSystemPort(final NodeParametersInput parametersInput) {
        final var inPortTypes = parametersInput.getInPortTypes();
        return IntStream.range(0, inPortTypes.length)
            .anyMatch(i -> FileSystemPortObjectSpec.class.equals(inPortTypes[i].getPortObjectSpecClass()));
    }

    private static String getSpaceName() {
        return WorkflowContextUtil.getWorkflowContextV2Optional().map(context -> {
            final var locationInfo = context.getLocationInfo();
            if (locationInfo instanceof LocalLocationInfo) {
                return "Local space";
            } else if (locationInfo instanceof HubSpaceLocationInfo hubSpace) {
                // Same logic as for the space names available in the KNIME Explorer (see HubSpace class in knime-server-client)
                return IPath.forPosix(hubSpace.getSpacePath()).lastSegment().toString();
            } else if (locationInfo instanceof ServerLocationInfo server) {
                return server.getDefaultMountId();
            }
            return "Current space";
        }).orElse("Current space");
    }

    static final class IsSpaceAndNotLegacy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileSystemType.Ref.class).isOneOf(FileSystemType.SPACE)
                .and(i
                    .getPredicate(
                        NoLongerSupportedFileSystemParameters.LegacyFileSystemSpec.LegacyFileSystemIsPresent.class)
                    .negate());
        }

    }

    @Widget(title = "Relative to", description = "Choose the base location to which the file path is relative to.")
    @Effect(predicate = IsSpaceAndNotLegacy.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    RelativeToSpaceOrWorkflow m_spaceOrWorkflow = RelativeToSpaceOrWorkflow.SPACE;

    enum RelativeToSpaceOrWorkflow {
            @Label(value = "Space", description = "The root of the space that contains the current workflow.")
            SPACE, //
            @Label(value = "Workflow", description = "The current workflow itself.")
            WORKFLOW;

        interface Ref extends ParameterReference<RelativeToSpaceOrWorkflow> {
        }

    }

    static final class IsUrlAndNotLegacy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileSystemType.Ref.class).isOneOf(FileSystemType.URL)
                .and(i
                    .getPredicate(
                        NoLongerSupportedFileSystemParameters.LegacyFileSystemSpec.LegacyFileSystemIsPresent.class)
                    .negate());
        }

    }

    @Effect(predicate = IsUrlAndNotLegacy.class, type = EffectType.SHOW)
    @Widget(title = "Timeout", description = "The timeout in milliseconds used when reading from or writing to a URL.")
    int m_timeout;

    static final class IsLocalButNotAvailable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileSystemType.Ref.class).isOneOf(FileSystemType.LOCAL)
                .and(i.getConstant(input -> !WorkflowContextInfoProvider.isLocal().orElse(false)));
        }

    }

    static final class IsConnectedButNoLongerAvailable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileSystemType.Ref.class).isOneOf(FileSystemType.CONNECTED)
                .and(not(i.getConstant(FileSystemSelection::hasConnectedFileSystemPort)));
        }

    }

    static final class ClearToReconfigureAvailable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return (i.getPredicate(IsConnectedButNoLongerAvailable.class)
                .or(i.getPredicate(IsLocalButNotAvailable.class)))
                    .and(not(i.getPredicate(NoLongerSupportedFileSystemParameters.ClearToReconfigureAvailable.class)));
        }

    }

    @TextMessage(LocalFileSystemNotAvailableMessageProvider.class)
    @Effect(predicate = IsLocalButNotAvailable.class, type = EffectType.SHOW)
    Void m_localFileSystemNotAvailableMessage;

    @TextMessage(ConnectedFileSystemNoLongerConnectedMessageProvider.class)
    @Effect(predicate = IsConnectedButNoLongerAvailable.class, type = EffectType.SHOW)
    Void m_connectedFileSystemNoLongerAvailableMessage;

    interface VisibleWarning extends TextMessage.SimpleTextMessageProvider {

        @Override
        default boolean showMessage(final NodeParametersInput context) {
            return true; // hidden via effect
        }

        @Override
        default MessageType type() {
            return MessageType.WARNING;
        }

    }

    static final class LocalFileSystemNotAvailableMessageProvider implements VisibleWarning {

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            return true; // hidden via effect
        }

        @Override
        public String title() {
            return "Local file system no longer available";
        }

        @Override
        public String description() {
            return "The node has been configured in a local environment before."
                + " Local file system access is not possible when working remotely.";
        }

        @Override
        public MessageType type() {
            return MessageType.WARNING;
        }

    }

    static final class ConnectedFileSystemNoLongerConnectedMessageProvider implements VisibleWarning {

        @Override
        public String title() {
            return "Connected file system no longer available";
        }

        @Override
        public String description() {
            return "The node has been configured with a file system port before.";
        }

    }

    static FileSystemSelection fromFSLocationSpec(final FSLocationSpec fsLocationSpec) throws InvalidSettingsException {
        return switch (fsLocationSpec.getFSCategory()) {
            case CONNECTED -> new FileSystemSelection(FileSystemType.CONNECTED);
            case LOCAL -> new FileSystemSelection(FileSystemType.LOCAL);
            case RELATIVE -> {
                final var relativeTo = RelativeTo.fromSettingsValue(fsLocationSpec.getFileSystemSpecifier().orElseThrow(
                    () -> new InvalidSettingsException("Missing file system specifier for relative file system.")));
                yield switch (relativeTo) {
                    case SPACE -> new FileSystemSelection(RelativeToSpaceOrWorkflow.SPACE);
                    case WORKFLOW -> new FileSystemSelection(RelativeToSpaceOrWorkflow.WORKFLOW);
                    case WORKFLOW_DATA -> new FileSystemSelection(FileSystemType.EMBEDDED);
                    default -> throw new InvalidSettingsException("Unsupported relative to file system: " + relativeTo);
                };
            }
            case CUSTOM_URL -> new FileSystemSelection(FileSystemType.URL, fsLocationSpec.getFileSystemSpecifier()
                .map(Integer::valueOf).orElse(URIFSConnectionConfig.DEFAULT_TIMEOUT_MILLIS));
            default -> throw new InvalidSettingsException(
                "Unsupported file system category: " + fsLocationSpec.getFSCategory());
        };
    }

    /**
     * Converts the current state of this file system selection to an {@link FSLocationSpec}.
     *
     * @return the {@link FSLocationSpec} corresponding to the current state of this file system selection.
     */
    public FSLocationSpec toFSLocationSpec() {
        return switch (m_fileSystemType) {
            case CONNECTED -> new DefaultFSLocationSpec(FSCategory.CONNECTED, m_connectedFileSystemSpecifier);
            case LOCAL -> new DefaultFSLocationSpec(FSCategory.LOCAL, null);
            case SPACE -> switch (m_spaceOrWorkflow) {
                case SPACE -> new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.SPACE.getSettingsValue());
                case WORKFLOW -> new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.WORKFLOW.getSettingsValue());
            };
            case EMBEDDED -> new DefaultFSLocationSpec(FSCategory.RELATIVE,
                RelativeTo.WORKFLOW_DATA.getSettingsValue());
            case URL -> new DefaultFSLocationSpec(FSCategory.CUSTOM_URL, String.valueOf(m_timeout));
        };
    }

}
