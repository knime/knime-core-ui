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
 *   May 2, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.knime.core.node.NodeLogger;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.NodeParameters;

/**
 *
 * @author Paul Bärnreuther
 */
public final class InstantiationUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(InstantiationUtil.class);

    private InstantiationUtil() {
        // Utility class
    }

    /**
     *
     * @param <T> the type of the instance
     * @param clazz
     * @param context a settings creation context
     * @return the instance provided by the constructor of clazz which takes a single {@link NodeParametersInput}
     *         as a parameter.
     */
    public static <T extends NodeParameters> T createDefaultNodeSettings(final Class<T> clazz,
        final NodeParametersInput context) {
        @SuppressWarnings("unchecked")
        final var settings = (T)createInstanceWithContext(clazz, context);
        return settings;
    }

    /**
     * @param clazz
     * @param context a settings creation context
     * @return the instance provided by the constructor of clazz which takes a single {@link NodeParametersInput}
     *         as a parameter.
     */
    public static Object createInstanceWithContext(final Class<?> clazz, final NodeParametersInput context) {
        try {
            return createInstance(clazz.getDeclaredConstructor(NodeParametersInput.class), context);
        } catch (NoSuchMethodException ex) { // NOSONAR
        }
        return createInstance(clazz);
    }

    /**
     * @param <T> the type of the instance
     * @param clazz
     * @return the instance provided by the default constructor of clazz
     */
    public static <T> T createInstance(final Class<T> clazz) {
        try {
            return createInstance(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            LOGGER.error(String.format("No default constructor found for class %s.", clazz.getName()), e);
            return null;
        }
    }

    /**
     * Creates a new instance using the given constructor and arguments, trying to make the constructor accessible in
     * the process by invoking {@link Constructor#setAccessible(boolean)}.
     *
     * @param <T> instance type
     * @param constructor constructor
     * @param initArgs arguments for constructor
     * @return instance created by constructor
     */
    public static <T> T createInstance(final Constructor<T> constructor, final Object... initArgs) {
        constructor.setAccessible(true); // NOSONAR
        try {
            return constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.error(String.format("Failed to instantiate class %s.", constructor.getDeclaringClass().getName()),
                e);
            return null;
        }
    }
}
