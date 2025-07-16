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
 *   May 16, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.util.function.Supplier;

import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.StateProvider.TypeReference;

/**
 * This interface is used within {@link DefaultStateProviderInitializer} and
 * {@link DefaultImperativeStateProviderInitializer} to define how to reference and materialize dependencies.
 *
 * @param <T> the type of the reference to be injected. For a normal state provider this is a class (declarative), for
 *            an imperative state provider, this is an instance.
 * @author Paul Bärnreuther
 */
@SuppressWarnings("javadoc")
sealed interface DependencyInjector<T> {
    <V> Supplier<V> setValueTriggerAndGetValueSupplier(final T reference);

    <V> Supplier<V> getValueSupplier(final T reference);

    <V> Supplier<V> getValueSupplier(T ref, TypeReference<V> typeRef);

    void setValueTrigger(final T reference);

    /**
     * Used during construction time, i.e. when the graph of dependencies/triggers/states/updates is built. State
     * providers "initialized" with this dependency injector are not prepared for a {@link StateProvider#computeState}
     * call since returned value suppliers are null.
     */
    abstract non-sealed class DependencyCollector<T> implements DependencyInjector<T> {

        /**
         * Each reference is associated to a value, i.e. to a location within the data. The value also has a certain
         * type.
         */
        protected abstract LocationAndType locate(T reference);

        /**
         * @see {@link StateProvider.StateProviderInitializer#getValueSupplier(Class, TypeReference)}.
         */
        protected abstract LocationAndType locate(T reference, TypeReference<?> typeRef);

        protected abstract void setDependency(LocationAndType location, final T reference);

        protected abstract void setTrigger(Location location);

        @Override
        public final <V> Supplier<V> setValueTriggerAndGetValueSupplier(final T reference) {
            final var typeAndLocation = locate(reference);
            setTrigger(typeAndLocation.location());
            setDependency(typeAndLocation, reference);
            return null;
        }

        @Override
        public final <V> Supplier<V> getValueSupplier(final T reference) {
            final var typeAndLocation = locate(reference);
            setDependency(typeAndLocation, reference);
            return null;
        }

        @Override
        public final <V> Supplier<V> getValueSupplier(final T ref, final TypeReference<V> typeRef) {
            final var typeAndLocation = locate(ref, typeRef);
            setDependency(typeAndLocation, ref);
            return null;
        }

        @Override
        public final void setValueTrigger(final T reference) {
            final var typeAndLocation = locate(reference);
            setTrigger(typeAndLocation.location());
        }

    }

    /**
     * Used during invocation time, i.e. when a graph of dependencies/triggers/states/updates is already available and
     * it is just about handing the correct value suppliers to state providers. After this, the state providers
     * {@link StateProvider#computeState} method can be called.
     */
    abstract non-sealed class DependencyProvider<T> implements DependencyInjector<T> {

        @Override
        public final void setValueTrigger(final T reference) {
            // Nothing to be provided
        }

        @Override
        public final <V> Supplier<V> setValueTriggerAndGetValueSupplier(final T reference) {
            return getValueSupplier(reference);
        }

        @Override
        public final <V> Supplier<V> getValueSupplier(final T reference, final TypeReference<V> typeRef) {
            return getValueSupplier(reference);
        }

    }

}
