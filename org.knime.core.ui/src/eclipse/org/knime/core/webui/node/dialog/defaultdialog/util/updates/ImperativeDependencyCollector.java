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
 *   May 26, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.DependencyInjector.DependencyCollector;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.RendererSpecsToImperativeRefsAndStateProviders.ImperativeValueRefWrapper;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider.TypeReference;

final class ImperativeDependencyCollector extends DependencyCollector<ControlValueReference<?>> {

    private final Collection<ImperativeValueRefWrapper> m_valueRefs;

    private final BiConsumer<ControlValueReference<?>, DependencyVertex> m_setDependency;

    private final Consumer<TriggerVertex> m_setTrigger;

    ImperativeDependencyCollector(final Collection<ImperativeValueRefWrapper> valueRefs,
        final BiConsumer<ControlValueReference<?>, DependencyVertex> setDependency,
        final Consumer<TriggerVertex> setTrigger) {
        m_valueRefs = valueRefs;
        m_setDependency = setDependency;
        m_setTrigger = setTrigger;
    }

    @Override
    protected LocationAndType locate(final ControlValueReference<?> reference) {
        return new LocationAndType(findValueRefWrapper(reference).fieldLocation(), () -> getSettingsType(reference));
    }

    @Override
    protected LocationAndType locate(final ControlValueReference<?> reference, final TypeReference<?> typeRef) {
        return new LocationAndType(findValueRefWrapper(reference).fieldLocation(),
            () -> RefsAndValueProvidersAndUiStateProvidersToDependencyTree.getSettingsType(typeRef));
    }

    private ImperativeValueRefWrapper findValueRefWrapper(final ControlValueReference<?> valueRef) {
        return m_valueRefs.stream().filter(wrapper -> wrapper.valueRef().equals(valueRef)).findAny()
            .orElseThrow(() -> new RuntimeException(
                "A control value reference used within an imperatively defined state provider could not be found."
                    + " Make sure that the referenced object is part of the dialog. "));
    }

    @Override
    protected void setDependency(final LocationAndType location, final ControlValueReference<?> reference) {
        m_setDependency.accept(reference, new DependencyVertex(location));
    }

    @Override
    protected void setTrigger(final Location location) {
        m_setTrigger.accept(new ValueTriggerVertex(location));
    }

    private static Type getSettingsType(final ControlValueReference<?> valueRef) {
        return GenericTypeFinderUtil.getFirstGenericType(valueRef.getClass(), ControlValueReference.class);
    }

}
