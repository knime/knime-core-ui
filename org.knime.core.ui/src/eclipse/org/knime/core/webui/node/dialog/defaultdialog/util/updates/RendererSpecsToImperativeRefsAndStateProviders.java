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
 *   May 21, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.LayoutRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.LocalizedControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

final class RendererSpecsToImperativeRefsAndStateProviders {

    record ImperativeValueRefWrapper(ControlValueReference<?> valueRef, Location fieldLocation) {
    }

    record ImperativeLocationUiStateProviderWrapper(StateProvider<?> stateProvider, Location fieldLocation,
        String providedOptionName) {
    }

    record ImperativeRefsAndStateProviders(Collection<ImperativeValueRefWrapper> valueRefs,
        Collection<ImperativeLocationUiStateProviderWrapper> locationUiStateProviders) {
        static ImperativeRefsAndStateProviders empty() {
            return new ImperativeRefsAndStateProviders(List.of(), List.of());
        }

    }

    private final Collection<ImperativeValueRefWrapper> m_valueRefs = new ArrayList<>();

    private final Collection<ImperativeLocationUiStateProviderWrapper> m_locationUiStateProviders = new ArrayList<>();

    public ImperativeRefsAndStateProviders
        widgetTreesToRefsAndStateProviders(final Collection<DialogElementRendererSpec> rendererSpecs) {
        rendererSpecs.forEach(this::traverseRendererSpec);
        return new ImperativeRefsAndStateProviders(m_valueRefs, m_locationUiStateProviders);
    }

    private void traverseRendererSpec(final DialogElementRendererSpec rendererSpec) {
        if (rendererSpec instanceof LayoutRendererSpec layoutSpec) {
            layoutSpec.getElements().forEach(this::traverseRendererSpec);
        } else if (rendererSpec instanceof LocalizedControlRendererSpec localizedSpec) {
            final var path = localizedSpec.getPathWithinValueJsonObject();
            final var location = rendererPathToLocation(path);
            final var controlSpec = localizedSpec.getControlSpec();
            if (controlSpec instanceof ControlValueReference controlValueReference) {
                m_valueRefs.add(new ImperativeValueRefWrapper(controlValueReference, location));
            }
            if (!controlSpec.getStateProviderClasses().isEmpty()) {
                throw new NotImplementedException(
                    "Declarative state providers are not yet resolved from renderer specs.");
            }
            controlSpec.getStateProviders().entrySet().forEach(entry -> m_locationUiStateProviders
                .add(new ImperativeLocationUiStateProviderWrapper(entry.getValue(), location, entry.getKey())));
        } else {
            throw new IllegalStateException("Unknown renderer spec type: " + rendererSpec.getClass());
        }
    }

    private static Location rendererPathToLocation(final List<String> path) {
        if (path.isEmpty() || !path.get(0).equals(SettingsType.MODEL.getConfigKey())) {
            throw new IllegalStateException("Unexpected path within control renderer: " + path);
        }
        return new Location(List.of(path.subList(1, path.size())), SettingsType.MODEL);
    }

}
