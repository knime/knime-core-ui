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
 *   Dec 5, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.persistance;

import java.lang.reflect.InvocationTargetException;

/**
 * Must provide an empty constructor for instantiation, the empty default constructor does not suffice.
 * CustomNodeSettingsPersistors must be immutable.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of object the persistor operates on
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface CustomNodeSettingsPersistor<T> extends NodeSettingsPersistor<T> {

    /**
     * Creates a new instance from the provided CustomNodeSettingsPersistor class by calling its empty constructor.
     *
     * @param <S> the type CustomNodeSettingsPersistor
     * @param persistorClass the class of CustomNodeSettingsPersistor
     * @return a new instance of the provided class
     * @throws IllegalStateException if the class does not have an empty constructor, is abstract, or the constructor
     *             raises an exception
     */
    static <S extends CustomNodeSettingsPersistor<?>> S createInstance(final Class<S> persistorClass) {
        try {
            var constructor = persistorClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Coding issue: The persistor '" + persistorClass.getName()
                + "' does not provide an empty constructor.", ex);
        } catch (IllegalAccessException ex) {
            // not reachable because we use black-magic to ensure accessibility
            throw new IllegalStateException(
                String.format("Can't access the empty constructor of '%s'.", persistorClass));
        } catch (InstantiationException ex) {
            throw new IllegalStateException(
                String.format("Can't instantiate persistors of abstract class '%s'.", persistorClass), ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException(
                String.format("The empty constructor of '%s' raised an exception.", persistorClass), ex);
        }
    }
}
