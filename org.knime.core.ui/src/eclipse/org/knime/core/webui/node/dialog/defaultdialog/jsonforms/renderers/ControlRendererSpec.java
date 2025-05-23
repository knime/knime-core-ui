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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers;

import java.util.Map;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.imperative.WithImperativeInitializer;

/**
 * The specification of a control, i.e. a widget that controls the value of one setting.
 *
 * @author Paul Bärnreuther
 */
public interface ControlRendererSpec extends DialogElementRendererSpec<ControlRendererSpec> {

    /**
     * @noimplement only to be implemented by the provided sub-interfaces
     * @return the unique id of this control renderer in case it is detected via the id (in contrast to e.g. the
     *         settings type).
     */
    default Optional<String> getFormat() {
        return Optional.empty();
    }

    /**
     * @noimplement only to be implemented by the provided sub-interfaces
     * @return the required data type of the control
     */
    JsonDataType getDataType();

    /**
     * @return the options for this control renderer
     */
    default Object getOptions() {
        return null;
    }

    /**
     * Some options may be provided dynamically by a state provider. This method defines which options are provided by
     * which state providers.
     *
     * @return a mapping from option names which are usually keys in the options object
     */
    @SuppressWarnings("rawtypes")
    default Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        return Map.of();
    }

    /**
     * Some options may be provided dynamically by a state provider. This method defines which options are provided by
     * which state provider.
     *
     * <p>
     * In contrast to {@link #getStateProviderClasses()} this method returns instances of state providers which enables
     * using it in an imperative way. To also reference other imperatively constructed renderers within such a state
     * provider, let the state provider extend {@link WithImperativeInitializer}!
     * </p>
     *
     * @return a mapping from option names which are usually keys in the options object
     */
    default Map<String, StateProvider<?>> getStateProviders() {
        return Map.of();
    }

    /**
     * @return the title of the control
     */
    String getTitle();

    /**
     * @return the description of the control
     */
    Optional<String> getDescription();

    @Override
    default ControlRendererSpec getNonLocalizedRendererSpec() {
        return this;
    }

}
