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

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dirty.DirtyTracker;
import org.knime.core.webui.node.dialog.defaultdialog.internal.filesystem.NoLongerSupportedFileSystemParameters.LegacyFileSystemSpec.LegacyFileSystemIsPresent;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.holder.CustomObjectSerializationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.SpaceAware;
import org.knime.filehandling.core.connections.base.hub.HubAccessUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

final class NoLongerSupportedFileSystemParameters implements NodeParameters {

    static final NodeLogger LOGGER = NodeLogger.getLogger(NoLongerSupportedFileSystemParameters.class);

    NoLongerSupportedFileSystemParameters() {
        // For deserialization
    }

    NoLongerSupportedFileSystemParameters(final FSLocationSpec legacyFSLocationSpec) {
        m_legacyFileSystemConfiguration = new LegacyFileSystemSpec(legacyFSLocationSpec);
    }

    static boolean isHubSpaceFSLocationSpec(final FSLocationSpec spec) {
        return FSCategory.HUB_SPACE == spec.getFSCategory();
    }

    static boolean isRelativeToMountpointFSLocationSpec(final FSLocationSpec spec) {
        return FSCategory.RELATIVE == spec.getFSCategory()
            && RelativeTo.MOUNTPOINT.getSettingsValue().equals(spec.getFileSystemSpecifier().orElse(""));
    }

    static boolean isMountpointFSLocationSpec(final FSLocationSpec spec) {
        return FSCategory.MOUNTPOINT == spec.getFSCategory();
    }

    static boolean isNoLongerSupportedFSLocationSpec(final FSLocationSpec spec) {
        return isHubSpaceFSLocationSpec(spec) || isRelativeToMountpointFSLocationSpec(spec)
            || isMountpointFSLocationSpec(spec);
    }

    /**
     * Extra level of nesting to make the @ValueReference work with the custom serializer and deserializer for
     * {@link FSLocationSpec}.
     */
    static final class LegacyFileSystemSpec implements NodeParameters {

        LegacyFileSystemSpec() {
            // For deserialization
        }

        LegacyFileSystemSpec(final FSLocationSpec locationSpec) {
            m_noLongerSupportedFSConfigOrNull = locationSpec;
            m_isPresent = true;
        }

        @JsonSerialize(using = FSLocationSpecSerializer.class)
        @JsonDeserialize(using = FSLocationSpecDeserializer.class)
        FSLocationSpec m_noLongerSupportedFSConfigOrNull;

        static final class FSLocationSpecSerializer
            extends CustomObjectSerializationUtil.CustomObjectSerializer<FSLocationSpec> {
        }

        static final class FSLocationSpecDeserializer
            extends CustomObjectSerializationUtil.CustomObjectDeserializer<FSLocationSpec> {
        }

        @ValueReference(LegacyFileSystemIsPresent.class)
        boolean m_isPresent;

        static final class LegacyFileSystemIsPresent implements BooleanReference {
        }

        boolean isPresent() {
            return m_isPresent;
        }

        FSLocationSpec get() {
            return m_noLongerSupportedFSConfigOrNull;
        }

    }

    @ValueReference(LegacyFileSystemConfigurationRef.class)
    @ValueProvider(ClearOnButtonClick.class)
    LegacyFileSystemSpec m_legacyFileSystemConfiguration = new LegacyFileSystemSpec();

    interface LegacyFileSystemConfigurationRef extends ParameterReference<LegacyFileSystemSpec> {
    }

    @TextMessage(ShowLegacySettingsReadOnlyMessage.class)
    Void m_legacySettingsReadOnlyMessage;

    static final class ShowLegacySettingsReadOnlyMessage implements StateProvider<Optional<TextMessage.Message>> {

        private Supplier<LegacyFileSystemSpec> m_legacyFSConfigSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_legacyFSConfigSupplier = initializer.computeFromValueSupplier(LegacyFileSystemConfigurationRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var legacyFSSpec = m_legacyFSConfigSupplier.get();
            if (!legacyFSSpec.isPresent()) {
                return Optional.empty();
            }
            final var fsSpec = legacyFSSpec.get();
            if (isHubSpaceFSLocationSpec(fsSpec)) {
                Optional<String> spaceName = Optional.empty();
                try {
                    spaceName = HubAccessUtil.createHubAccessViaWorkflowContext().listSpaces().stream()
                        .filter(s -> s.getSpaceId().equals(fsSpec.getFileSystemSpecifier().orElse(""))).findFirst()
                        .map(SpaceAware.Space::getName);
                } catch (final IOException e) {
                    LOGGER.error(
                        "Error while trying to retrieve the name of the currently configured HUB space for the message in the file system selection dialog.",
                        e);
                }
                return Optional.of(new Message(
                    String.format("Configured file system: HUB space %s",
                        spaceName.map(n -> "\"" + n + "\"").orElse("(currently not accessible)")),
                    "", TextMessage.MessageType.INFO));
            }
            if (isMountpointFSLocationSpec(fsSpec)) {
                return Optional.of(new Message(String.format("Configured file system: Mountpoint \"%s\"",
                    fsSpec.getFileSystemSpecifier().orElse("")), "", TextMessage.MessageType.INFO));
            }
            if (isRelativeToMountpointFSLocationSpec(fsSpec)) {
                return Optional.of(new Message("Configured file system: Relative to current mountpoint", "",
                    TextMessage.MessageType.INFO));
            }
            return Optional.empty();
        }

    }

    @TextMessage(NoLongerSupportedMessage.class)
    Void m_noLongerSupportedMessage;

    static final class NoLongerSupportedMessage implements StateProvider<Optional<TextMessage.Message>> {

        private Supplier<LegacyFileSystemSpec> m_legacyFSConfigSupplier;

        private static final String MESSAGE_TITLE = "The currently configured file system is no longer selectable.";

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_legacyFSConfigSupplier = initializer.computeFromValueSupplier(LegacyFileSystemConfigurationRef.class);
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var legacyFSConfig = m_legacyFSConfigSupplier.get();
            if (!legacyFSConfig.isPresent()) {
                return Optional.empty();
            }
            final var fsConfig = legacyFSConfig.get();
            if (isHubSpaceFSLocationSpec(fsConfig)) {
                return Optional.of(new Message(MESSAGE_TITLE,
                    "Selecting another space from the current HUB is no longer supported within this dialog. "
                        + "Consider connecting a \"Space Connector\" node to this node instead.",
                    TextMessage.MessageType.WARNING));
            }
            final var isMountpointRelated =
                isMountpointFSLocationSpec(fsConfig) || isRelativeToMountpointFSLocationSpec(fsConfig);
            if (isMountpointRelated) {
                return Optional.of(new Message(MESSAGE_TITLE,
                    "Selecting a mountpoint is no longer supported within this dialog. "
                        + "Consider connecting a \"Mountpoint Connector\" node to this node instead.",
                    TextMessage.MessageType.WARNING));
            }
            return Optional.empty();
        }

    }

    static final class ClearToReconfigureAvailable implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(LegacyFileSystemIsPresent.class);
        }

    }

    @WidgetInternal(hideControlInNodeDescription = "Only shown if the selected file system is no longer supported.")
    @Widget(title = "Clear to reconfigure", description = "")
    @SimpleButtonWidget(ref = ClearToReconfigureButtonRef.class)
    @Effect(predicate = ClearToReconfigureAvailable.class, type = EffectType.SHOW)
    Void m_clearToReconfigure;

    interface ClearToReconfigureButtonRef extends ButtonReference {
    }

    @DirtyTracker(SetDirtyOnClearButtonClick.class)
    Void m_dirtyAfterClear;

    static final class ClearOnButtonClick implements StateProvider<LegacyFileSystemSpec> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(ClearToReconfigureButtonRef.class);
        }

        @Override
        public LegacyFileSystemSpec computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return new LegacyFileSystemSpec();
        }
    }

    static final class SetDirtyOnClearButtonClick implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(ClearToReconfigureButtonRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return true;
        }
    }

}
