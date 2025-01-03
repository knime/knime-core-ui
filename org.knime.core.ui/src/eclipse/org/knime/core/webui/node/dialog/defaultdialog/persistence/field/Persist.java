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
 *   Nov 14, 2022 ("Adrian Nembach, KNIME GmbH, Konstanz, Germany"): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allows to define the persistence of individual fields to NodeSettings if field based persistence is used.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Persist {

    /**
     * Optional argument that defines the key under which to store the field in the NodeSettings. The key is generated
     * from the field name by stripping any leading 'm_' prefix if this argument is left empty (the default) or consists
     * only of whitespaces.
     *
     * @return the key under which to store the field in the NodeSettings.
     */
    String configKey() default "";

    /**
     * Optional argument that allows to specify a custom persistor for a field. For accessing the {@link #configKey()}
     * within this persistor use a subclass of {@link NodeSettingsPersistorWithConfigKey}. Otherwise the
     * {@link #configKey()} is ignored whenever a customPersistor is defined.
     *
     * @return the class of the customPersistor
     */
    @SuppressWarnings("rawtypes") // annotations and generics don't mix well
    Class<? extends FieldNodeSettingsPersistor> customPersistor() default FieldNodeSettingsPersistor.class;

    /**
     * Optional argument that allows to hide a setting in the flow variable tab.
     *
     * @return true if the setting should be hidden in the flow variable tab.
     */
    boolean hidden() default false;

    /**
     * Optional argument that allows to make a setting optional. An optional settings is a settings that may be in the
     * NodeSettings when loading but doesn't have to be. If it isn't present, then the default value from the declaring
     * DefaultNodeSettings class is used. The typical use-case for this are settings that are added in a later release.
     * This argument is overruled by {@link #defaultProvider()} if provided. Does not apply to {@link #customPersistor()
     * custom persistors}.
     *
     * @return true if the setting is optional
     */
    boolean optional() default false;

    /**
     * Optional argument that allows to provide a {@link DefaultProvider} that provides the default value for a setting
     * that may not be part of the node settings e.g. because it was added in a later release. <br>
     * If the default for newly created settings is the same as for loaded settings, use {@link #optional()} instead. If
     * provided, a defaultProvider overrules {@link #optional()}. Does not apply to {@link #customPersistor() custom
     * persistors}.
     *
     * @return the class of {@link DefaultProvider}
     * @see #optional()
     */
    @SuppressWarnings("rawtypes")
    Class<? extends DefaultProvider> defaultProvider() default DefaultProvider.class;
}
