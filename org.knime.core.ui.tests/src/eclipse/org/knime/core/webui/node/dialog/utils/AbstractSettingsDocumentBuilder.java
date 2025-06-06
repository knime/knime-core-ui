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
 *   Jun 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.internal.VariableSettings;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Used to construct settings JSON or {@link NodeSettings} for several default dialog test cases.
 *
 * Construct JSON to test apply behavior. Construct {@link NodeSettings} to test initial data behavior.
 *
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("javadoc")
public abstract class AbstractSettingsDocumentBuilder<T extends AbstractSettingsDocumentBuilder<T>> {

    private List<CustomSettingAt> m_customSettingsAt;

    public record CustomSettingAt(SettingsAtPath value, SettingsType type, List<String> path) {
    }

    protected AbstractSettingsDocumentBuilder() {
        m_customSettingsAt = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    protected T thisAsT() {
        return (T)this;
    }

    public T modifyAt(final SettingsType type, final List<String> path,
        final Function<SettingsAtPath.Builder, SettingsAtPath> subBuilder) {
        final var customSettings = subBuilder.apply(SettingsAtPath.builder());
        getCustomSettingsAt().add(new CustomSettingAt(customSettings, type, path));
        return thisAsT();
    }

    public T modifyAt(final SettingsType type, final String settingsKey,
        final Function<SettingsAtPath.Builder, SettingsAtPath> subBuilder) {
        return modifyAt(type, List.of(settingsKey), subBuilder);
    }

    /**
     * @return the customSettingsAt
     */
    public List<CustomSettingAt> getCustomSettingsAt() {
        return m_customSettingsAt;
    }

    @SuppressWarnings("javadoc")
    public static final class SettingsAtPath {

        String m_exposedFlowVariableName;

        String m_invalidValue;

        String m_controllingFlowVariableName;

        boolean m_isFlawed;

        static Builder builder() {
            return new Builder();
        }

        /**
         * @return the invalidValue
         */
        public String getInvalidValue() {
            return m_invalidValue;
        }

        /**
         * @return the controllingFlowVariableName
         */
        public String getControllingFlowVariableName() {
            return m_controllingFlowVariableName;
        }

        /**
         * @return the exposedFlowVariableName
         */
        public String getExposedFlowVariableName() {
            return m_exposedFlowVariableName;
        }

        /**
         * @return the isFlawed
         */
        public boolean isFlawed() {
            return m_isFlawed;
        }

        @SuppressWarnings("javadoc")
        public static final class Builder {

            private String m_exposedFlowVariableName;

            private String m_invalidValue;

            private String m_controllingFlowVariableName;

            private boolean m_isFlawed;

            public Builder setStringValue(final String invalidValue) {
                m_invalidValue = invalidValue;
                return this;
            }

            public Builder withExposedFlowVariableName(final String name) {
                m_exposedFlowVariableName = name;
                return this;
            }

            public Builder withControllingFlowVariableName(final String name) {
                m_controllingFlowVariableName = name;
                return this;
            }

            public Builder asFlawedFlowVariable() {
                checkControllingFlowVariableSet();
                m_isFlawed = true;
                return this;
            }

            private void checkControllingFlowVariableSet() {
                Objects.requireNonNull(m_controllingFlowVariableName, "Set a controlling flow variable first");
            }

            public SettingsAtPath build() {
                final var built = new SettingsAtPath();
                built.m_controllingFlowVariableName = m_controllingFlowVariableName;
                built.m_exposedFlowVariableName = m_exposedFlowVariableName;
                built.m_invalidValue = m_invalidValue;
                built.m_isFlawed = m_isFlawed;
                return built;
            }

        }

    }

    @SuppressWarnings("javadoc")
    public static final class NodeSettingsBuilderUtils {

        private NodeSettingsBuilderUtils() {
            // utility class
        }

        public static void resolveCustomSettingsAt(final NodeSettings nodeSettings,
            final List<CustomSettingAt> customSettingsAt) {
            customSettingsAt.forEach(customSettingAt -> {
                try {

                    if (customSettingAt.value().getInvalidValue() != null) {
                        addInvalidValue(nodeSettings, customSettingAt);
                    }
                    if (customSettingAt.value().getControllingFlowVariableName() != null
                        || customSettingAt.value().getExposedFlowVariableName() != null) {
                        addFlowVariable(nodeSettings, customSettingAt);
                    }
                } catch (InvalidSettingsException ex) {
                    throw new RuntimeException(ex); // NOSONAR
                }
            });
        }

        private static void addInvalidValue(final NodeSettings nodeSettings, final CustomSettingAt customSettingAt)
            throws InvalidSettingsException {
            final var path = customSettingAt.path().stream().toArray(String[]::new);
            var config = nodeSettings.getNodeSettings(customSettingAt.type().getConfigKey());
            for (int i = 0; i < path.length - 1; i++) {
                config = config.getNodeSettings(path[i]);
            }
            config.addString(path[path.length - 1], customSettingAt.value().getInvalidValue());
        }

        private static void addFlowVariable(final NodeSettings nodeSettings, final CustomSettingAt customSettingAt)
            throws InvalidSettingsException {
            final var path = customSettingAt.path().stream().toArray(String[]::new);
            var variableConfig = getOrCreateVariableTree(nodeSettings, customSettingAt.type());
            for (int i = 0; i < path.length; i++) {
                variableConfig = getOrCreateNodeSettings(variableConfig, path[i]);
            }
            variableConfig.addString(VariableSettings.EXPOSED_VARIABLE_CFG_KEY,
                customSettingAt.value().getExposedFlowVariableName());
            variableConfig.addString(VariableSettings.USED_VARIABLE_CFG_KEY,
                customSettingAt.value().getControllingFlowVariableName());
            variableConfig.addBoolean(VariableSettings.USED_VARIABLE_FLAWED_CFG_KEY,
                customSettingAt.value().isFlawed());
        }

        private static NodeSettings getOrCreateVariableTree(final NodeSettings nodeSettings, final SettingsType type)
            throws InvalidSettingsException {
            final var key = type.getVariablesConfigKey();
            if (nodeSettings.containsKey(key)) {
                return nodeSettings.getNodeSettings(key).getNodeSettings("tree");
            }
            final var variableSettings = nodeSettings.addNodeSettings(key);
            variableSettings.addString("version", "V_2019_09_13");
            return (NodeSettings)variableSettings.addNodeSettings("tree");
        }

        private static NodeSettings getOrCreateNodeSettings(final NodeSettings nodeSettings, final String key)
            throws InvalidSettingsException {
            if (nodeSettings.containsKey(key)) {
                return nodeSettings.getNodeSettings(key);
            }
            return (NodeSettings)nodeSettings.addNodeSettings(key);
        }
    }

    @SuppressWarnings("javadoc")
    public static final class JsonBuilderUtils {
        private JsonBuilderUtils() {
            // utility class
        }

        public static void resolveCustomSettingsAt(final ObjectNode root,
            final List<CustomSettingAt> customSettingsAtList) {
            final var data = (ObjectNode)root.get("data");
            final var flowVariableSettings = (ObjectNode)root.get("flowVariableSettings");
            customSettingsAtList.forEach(customSettingAt -> {
                if (customSettingAt.value().getInvalidValue() != null) {
                    addInvalidValue(data, customSettingAt);
                }
                if (customSettingAt.value().getControllingFlowVariableName() != null
                    || customSettingAt.value().getExposedFlowVariableName() != null) {
                    addFlowVariable(flowVariableSettings, customSettingAt);
                }
            });
        }

        private static void addFlowVariable(final ObjectNode flowVariableSettings,
            final CustomSettingAt customSettingAt) {
            final var path = toJsonPath(customSettingAt.type(), customSettingAt.path());
            final var value = customSettingAt.value();
            flowVariableSettings.putObject(path)//
                .put("exposedFlowVariableName", value.getExposedFlowVariableName())
                .put("controllingFlowVariableName", value.getControllingFlowVariableName())
                .put("controllingFlowVariableFlawed", value.isFlawed());
        }

        private static String toJsonPath(final SettingsType settingsType, final List<String> path) {
            return settingsType.getConfigKey() + "." + String.join(".", path);
        }

        private static void addInvalidValue(final ObjectNode data, final CustomSettingAt customSettingAt) {
            final var lastIndex = customSettingAt.path().size() - 1;
            final var pathToParent = customSettingAt.path().subList(0, lastIndex);
            final var settingsKey = customSettingAt.path().get(lastIndex);
            final var parent = (ObjectNode)data.at(toJsonPointer(customSettingAt.type(), pathToParent));
            parent.put(settingsKey, customSettingAt.value().getInvalidValue());
        }

        private static String toJsonPointer(final SettingsType settingsType, final List<String> path) {
            final var segments = Stream.concat(Stream.of(settingsType.getConfigKey()), path.stream()).toList();
            return "/" + String.join("/", segments);
        }
    }
}