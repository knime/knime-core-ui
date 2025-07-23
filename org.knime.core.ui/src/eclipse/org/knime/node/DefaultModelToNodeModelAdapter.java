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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.NodeParameters;

/**
 * An interface for adapters that convert a {@link DefaultModel} to a {@link NodeModel}.
 *
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
sealed interface DefaultModelToNodeModelAdapter
    permits StandardDefaultModelToNodeModelAdapter, RearrangeColumnsDefaultModelToNodeModelAdapter {

    Optional<Class<? extends NodeParameters>> getModelParametersClass();

    Optional<Class<? extends NodeParameters>> getViewParametersClass();

    Optional<PortObjectSpec[]> getSpecs();

    NodeParameters getModelParameters();

    void setModelParameters(final NodeParameters modelParameters);

    default void defaultValidateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var modelSettingsClassOptional = getModelParametersClass();
        if (modelSettingsClassOptional.isPresent()) {
            // This can already throw if loading from node settings throws
            final var loadedSettings = NodeParametersUtil.loadSettings(settings, modelSettingsClassOptional.get());
            // Additional custom validation of the settings
            loadedSettings.validate();
        }
    }

    default void defaultValidateViewSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var viewSettingsClassOptional = getViewParametersClass();
        if (viewSettingsClassOptional.isPresent()) {
            // This can already throw if loading from node settings throws
            final var loadedSettings = NodeParametersUtil.loadSettings(settings, viewSettingsClassOptional.get());
            // Additional custom validation of the settings
            loadedSettings.validate();
        }
    }

    default void defaultLoadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var modelSettingsClassOptional = getModelParametersClass();
        if (modelSettingsClassOptional.isPresent()) {
            setModelParameters(NodeParametersUtil.loadSettings(settings, modelSettingsClassOptional.get()));
        }
    }

    default void defaultSaveSettingsTo(final NodeSettingsWO settings) {
        final var modelSettingsClassOptional = getModelParametersClass();
        if (modelSettingsClassOptional.isPresent()) {
            final var modelSettingsClass = modelSettingsClassOptional.get();
            var modelSettings = getModelParameters();
            if (modelSettings == null) {
                // If no model settings are set, save new ones but do not set them in the model.
                modelSettings = NodeParametersUtil.createSettings(modelSettingsClass);
            }
            NodeParametersUtil.saveSettings(modelSettingsClass, modelSettings, settings);
        }
    }

    default NodeParameters setInitialSettingsUsingSpecsIfNecessary(final PortObjectSpec[] inSpecs) {
        final var modelSettingsClassOptional = getModelParametersClass();
        if (modelSettingsClassOptional.isPresent() && getModelParameters() == null) {
            setModelParameters(NodeParametersUtil.createSettings(modelSettingsClassOptional.get(), inSpecs));
        }
        return getModelParameters();
    }

    default void defaultSaveDefaultViewSettingsTo(final NodeSettingsWO settings) {
        final var viewSettingsClassOptional = getViewParametersClass();
        if (viewSettingsClassOptional.isPresent()) {
            final var viewSettingsClass = viewSettingsClassOptional.get();
            final var specs = getSpecs();
            final var viewSettings = specs.isEmpty() ? NodeParametersUtil.createSettings(viewSettingsClass) //
                : NodeParametersUtil.createSettings(viewSettingsClass, specs.get());
            NodeParametersUtil.saveSettings(viewSettingsClass, viewSettings, settings);
        }
    }
}
