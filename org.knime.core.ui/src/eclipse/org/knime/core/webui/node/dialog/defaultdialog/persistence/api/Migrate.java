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
 *   Dec 23, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines how a field is migrated from a pervious version of settings.
 *
 * <p>
 * A migration - in contrast to a {@link Persistor} - is needed when the structure of the settings has changed, i.e.
 * there can exist versions of the node saved with settings that characterize the state of the node to before a certain
 * point in time when the saved settings structure was changed. Defining a migration is necessary for two reasons:
 * <ul>
 * <li>To be able to still load from these old settings.</li>
 * <li>To not break flow variables set for old configs that are not saved to again.</li>
 * </ul>
 * Note that the first point could be achieved by using a {@link Persistor} but the second one cannot.
 *
 *
 * <p>
 * This is it the simple alternative to {@link Migration @Migration} and only one of the two can be used. Use
 * {@link Migration} instead for more complex migrations or for migrations on class level. Every field of this
 * annotation can be achieved by using {@link Migration @Migration} as well as described in the individual javadocs.
 * </p>
 *
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Migrate {

    /**
     * Use this annotation for a field that has been added to the settings after the initial release of the node.
     *
     * If it isn't present, during load, then the default value of this field from the declaring DefaultNodeSettings
     * class is used.
     *
     * In case a custom {@link Persistor} is used, being present means that any of the specified config paths in
     * {@link NodeSettingsPersistor#getConfigPaths()} is present.
     *
     * <h5>@Migration Alternative:</h5>
     * <p>
     * This annotation might not suffice because
     * <ol>
     * <li>a different value than the field's default value is needed (in case the default changed with the
     * migration)</li>
     * <li>a second migration is required additionally.</li>
     * </ol>
     * For 1. one can use a {@link Migration @Migration} with a {@link NodeSettingsMigration} implementing
     * {@link DefaultProvider}. For 2. the same effect can be achieved by using a {@link NodeSettingsMigration}
     * including the following configs deprecation (usually as the last in the provided list):
     *
     * <pre>
     * {@code
     * new ConfigsDeprecation.builder(settings -> myDefaultValue).build()
     * }
     * </pre>
     *
     * @return true if the default value should be used if the field does not exist in the saved settings.
     */
    boolean loadDefaultIfAbsent() default false;

}
