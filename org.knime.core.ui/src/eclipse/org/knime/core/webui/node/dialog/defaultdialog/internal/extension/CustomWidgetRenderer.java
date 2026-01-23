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
 *   Jan 20, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogUIExtension;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.JsonDataType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.WidgetRendererSpec;
import org.knime.node.parameters.updates.StateProvider;

/**
 * To be used for an implementation of {@link DefaultNodeDialogWidget}.
 *
 * @author Paul Bärnreuther
 */
public final class CustomWidgetRenderer implements WidgetRendererSpec<CustomWidgetRenderer> {

    /**
     * Use {@link Builder} to create an instance of {@link CustomWidgetRenderer}.
     */
    private CustomWidgetRenderer(final JsonDataType dataType, final Map<String, Object> options,
        final Map<String, Class<? extends StateProvider>> stateProviders, final String widgetResourceName,
        final String rpcServiceName) {
        m_dataType = dataType;
        m_options = options;
        m_stateProviders = stateProviders;
        m_widgetResourceName = widgetResourceName;
        m_rpcServiceName = rpcServiceName;

    }

    final JsonDataType m_dataType;

    final Map<String, Object> m_options;

    final Map<String, Class<? extends StateProvider>> m_stateProviders;

    final String m_widgetResourceName;

    final String m_rpcServiceName;

    @Override
    public final JsonDataType getDataType() {
        return m_dataType;
    }

    @Override
    public Optional<String> getFormat() {
        return Optional.of("custom");
    }

    @Override
    public final Object getOptions() {
        final Map<String, Object> options = new HashMap<>();
        options.put("widgetResource", m_widgetResourceName);
        options.put("rpcServiceName", m_rpcServiceName);
        options.putAll(m_options);
        return options;
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        return m_stateProviders;
    }

    /**
     * Builder to be used within {@link DefaultNodeDialogWidget#createRenderOptions}.
     */
    public static final class Builder {
        private JsonDataType m_dataType = JsonDataType.OBJECT;

        private final String m_widgetResourceName;

        private final String m_rpcServiceName;

        private final Map<String, Object> m_options = new HashMap<>();

        private final Map<String, Class<? extends StateProvider>> m_stateProviders = new HashMap<>();

        /**
         * A builder for {@link CustomWidgetRenderer}.
         *
         * @param extensionClass the class of the extension
         */
        public Builder(final Class<? extends DefaultNodeDialogWidget> extensionClass) {
            m_widgetResourceName = DefaultNodeDialogUIExtension.getResourceNameForWidget(extensionClass);
            m_rpcServiceName = DefaultNodeDialog.toRpcServiceName(extensionClass);
        }

        /**
         * Set the data type required by this custom widget. Per default {@link JsonDataType#OBJECT} is used.
         *
         * @param dataType the data type
         * @return this
         */
        public Builder withDataType(final JsonDataType dataType) {
            m_dataType = dataType;
            return this;
        }

        /**
         * Add an option to the options object of this custom widget.
         *
         * @param key the option key
         * @param value the option value. It has to be serializable to JSON and will appear in the frontend as property
         *            of `props.control.uischema.options`.
         * @return this
         */
        public Builder withOption(final String key, final Object value) {
            m_options.put(key, value);
            return this;
        }

        /**
         * Add an option to be provided by a state provider. It can be accessed in the frontend using the
         * `useProvidedState` composable.
         *
         * @param optionName the option name.
         * @param stateProviderClass the currently used state provider class for this option.
         * @return this
         */
        public Builder withStateProvider(final String optionName,
            final Class<? extends StateProvider> stateProviderClass) {
            m_stateProviders.put(optionName, stateProviderClass);
            return this;
        }

        /**
         * Build the {@link CustomWidgetRenderer}.
         *
         * @return the built {@link CustomWidgetRenderer}
         */
        public CustomWidgetRenderer build() {
            return new CustomWidgetRenderer(m_dataType, m_options, m_stateProviders, m_widgetResourceName,
                m_rpcServiceName);
        }
    }

    /**
     * Create a builder for {@link CustomWidgetRenderer}.
     *
     * @param extensionClass the class of the extension
     * @return the builder
     */
    public static Builder builder(final Class<? extends DefaultNodeDialogWidget> extensionClass) {
        return new Builder(extensionClass);
    }

}
