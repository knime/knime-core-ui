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
 *   Dec 11, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import org.knime.core.webui.node.dialog.defaultdialog.internal.additionalsave.SaveAdditional;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.ParametersSaver;

/**
 * Utility class to create instances of {@link NodeParametersPersistor} and {@link NodeParametersMigration} classes.
 *
 * @author Paul Bärnreuther
 */
final class InitializeWithDefaultConstructorUtil {

    private InitializeWithDefaultConstructorUtil() {
        // utility class
    }

    /**
     * Create an instance of a {@link NodeParametersPersistor}. We use the empty constructor as per contract.
     *
     * @throws IllegalStateException if the class does not have a suitable constructor, is abstract, or the constructor
     *             raises an exception
     */
    static <P extends NodeParametersPersistor<?>> P createPersistor(final Class<P> persistorClass) {
        return ReflectionUtil.createInstance(persistorClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format("The provided persistor class '%s' does not provide a default constructor ",
                    persistorClass.getCanonicalName())));
    }

    /**
     * Create an instance of a {@link NodeParametersMigration}. We use the empty constructor as per contract.
     *
     * @throws IllegalStateException if the class does not have a suitable constructor, is abstract, or the constructor
     *             raises an exception
     */
    @SuppressWarnings("rawtypes")
    static NodeParametersMigration createMigrator(final Migration migration) {
        final var migratorClass = migration.value();
        return ReflectionUtil.createInstance(migratorClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format("The provided migrator class '%s' does not provide an empty constructor.",
                    migratorClass.getCanonicalName())));
    }

    /**
     * Creates an instance of a {@link ParametersSaver}. We use the empty constructor as per contract.
     *
     * @throws IllegalStateException if the class does not have a suitable constructor, is abstract, or the constructor
     *             raises an exception
     */
    @SuppressWarnings("rawtypes")
    static ParametersSaver createSaver(final SaveAdditional saveAdditional) {
        final var saverClass = saveAdditional.value();
        return ReflectionUtil.createInstance(saverClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format("The provided saver class '%s' does not provide an empty constructor.",
                    saverClass.getCanonicalName())));
    }

}
