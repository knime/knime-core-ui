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
 *   Apr 4, 2025 (benjaminwilhelm): created
 */
package org.knime.core.webui.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The {@link DataServiceDependencies} allow to run data services with additional external dependencies. Wrap the call
 * to the data service into a {@link DependencyContextRunnable} and call it with the dependencies via
 * {@link #runWithDependencies}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @noreference client code must not set data service dependencies. They are set by the framework. Use
 *              {@link DataServiceContext#getOtherDependency(Class)} to access the dependencies.
 */
public final class DataServiceDependencies {

    private DataServiceDependencies() {
    }

    private static final ThreadLocal<Map<Class<?>, Object>> DEPENDENCIES = ThreadLocal.withInitial(Map::of);

    /**
     * Functional interface to define a runnable that calls the data service with dependencies.
     *
     * @param <V> return value
     * @param <E> exception type
     */
    @FunctionalInterface
    public interface DependencyContextRunnable<V, E extends Exception> {
        /**
         * Method that calls a data service which will have access to the dependencies.
         *
         * @return anything
         * @throws E anything
         */
        V run() throws E;
    }

    /**
     * Utility to create a map of one dependency.
     *
     * @param <A> the type of the dependency
     * @param key the class
     * @param value the dependency implementation (can be {@code null})
     * @return a map that contains the dependency and can be used in {@link #runWithDependencies}
     */
    public static <A> Map<Class<?>, Object> dependencies(final Class<A> key, final A value) {
        var d = new HashMap<Class<?>, Object>(1);
        d.put(key, value);
        return d;
    }

    /**
     * Utility to create a map of two dependencies.
     *
     * @param <A> the type of the first dependency
     * @param <B> the type of the second dependency
     * @param keyA the class of the first dependency
     * @param valueA the first dependency implementation (can be {@code null})
     * @param keyB the class of the second dependency
     * @param valueB the second dependency implementation (can be {@code null})
     * @return a map that contains the dependencies and can be used in {@link #runWithDependencies}
     */
    public static <A, B> Map<Class<?>, Object> dependencies(final Class<A> keyA, final A valueA, final Class<B> keyB,
        final B valueB) {
        var d = new HashMap<Class<?>, Object>(2);
        d.put(keyA, valueA);
        d.put(keyB, valueB);
        return d;
    }

    /**
     * Run the call to a data service with the given dependencies.
     *
     * @param <V> the return type
     * @param <E> the type of the exception of thrown by {@code runWithDependencies}
     * @param dependencies the dependencies which will use the class of the object as the key
     * @param runWithDependencies a runnable that calls the data service
     * @return the return value of {@code runWithDependencies}
     * @throws E if {@code runWithDependencies} throws
     * @noreference client code must not set data service dependencies. They are set by the framework. Use
     *              {@link DataServiceContext#getOtherDependency(Class)} to access the dependencies.
     */
    public static <V, E extends Exception> V runWithDependencies(final List<Object> dependencies,
        final DependencyContextRunnable<V, E> runWithDependencies) throws E {
        return runWithDependencies( //
            dependencies.stream().collect(Collectors.toMap(Object::getClass, Function.identity())), //
            runWithDependencies //
        );
    }

    /**
     * Run the call to a data service with the given dependencies.
     *
     * @param <V> the return type
     * @param <E> the type of the exception of thrown by {@code runWithDependencies}
     * @param dependencies the dependencies
     * @param runWithDependencies a runnable that calls the data service
     * @return the return value of {@code runWithDependencies}
     * @throws E if {@code runWithDependencies} throws
     * @noreference client code must not set data service dependencies. They are set by the framework. Use
     *              {@link DataServiceContext#getOtherDependency(Class)} to access the dependencies.
     */
    public static <V, E extends Exception> V runWithDependencies(final Map<Class<?>, Object> dependencies,
        final DependencyContextRunnable<V, E> runWithDependencies) throws E {
        try {
            DEPENDENCIES.set(dependencies);
            return runWithDependencies.run();
        } finally {
            DEPENDENCIES.remove();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T getDependency(final Class<T> clazz) {
        // Note that `DEPENDENCIES.get()` will never be null because we set an initial value
        return (T)DEPENDENCIES.get().get(clazz);
    }
}
