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
 */
/**
 * This package contains implementations of {@link org.knime.node.parameters.persistence.NodeParametersPersistor} that
 * enable backwards-compatibility for settings models that were used in legacy swing dialogs.
 *
 * <h2> Simple settings models don't require a custom persistor </h2>
 *
 * For simple settings models like integers, doubles or strings, using the appropriate Java type for fields in the node
 * parameters is sufficient (together with an appropriate {@link Persist @Persist}
 * annotation for setting the correct legacy key).
 *
 * <h2> Complex settings models require a custom persistor </h2>
 *
 * For more complex settings models a custom persistor is required and can be attached to a field using
 * {@link Persistor @Persistor}). Note that all persistors in this package are
 * abstract classes where the legacy config key of the settings model is required as constructor argument (do not use
 * the @Persist annotation in this case).
 *
 * <h2> Contribute new implementations </h2>
 *
 * The list of legacy persistors within this package might not be complete. In this case feel free to contribute a
 * new implementation here to avoid duplication.
 *
 * <h2> Examples </h2>
 *
 * Find a list of examples {@link ExampleNodeParameters here}.
 *
 */
package org.knime.node.parameters.persistence.legacy;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 *
 * The following examples illustrate how to migrate legacy settings models to fields in NodeParameters. There is one
 * example for each of the following cases:
 * <ul>
 * <li>Simple settings model that can be replaced by a primitive type and a @Persist annotation</li>
 * <li>Settings model that requires a custom persistor provided within this package</li>
 * <li>Settings model that requires a custom persistor that needs to be implemented for this one node.</li>
 * </ul>
 *
 *
 * Given the following excerpt of a node model setup of a to be migrated node:
 *
 * <code>
 *
 * static final String OPTION_APPEND = "Append selected columns";
 *
 * static final String OPTION_REPLACE = "Replace selected columns";
 *
 * private final SettingsModelBoolean m_cancelOnFail = createCancelOnFailModel();
 *
 * private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();
 *
 * private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();
 *
 * static SettingsModelBoolean createCancelOnFailModel() {
 *     return new SettingsModelBoolean("cancel_on_fail", true);
 * }
 *
 * static SettingsModelString createReplaceAppendStringBool() {
 *     return new SettingsModelString("replace_or_append", OPTION_REPLACE);
 * }
 *
 * static SettingsModelColumnFilter2 createColSelectModel() {
 *     return new SettingsModelColumnFilter2("col_select", StringValue.class);
 * }
 * </code>
 *
 * @author Paul Bärnreuther
 */
class ExampleNodeParameters implements NodeParameters {

    /**
     * The node previously used a SettingsModelBoolean with key "cancel_on_fail". This is now automatically persisted by
     * using a boolean field with the appropriate @Persist.
     */
    @Widget(title = "Cancel execution on failure",
        description = "Whether the node should fail if a selected column does not exist.")
    @Persist(configKey = "cancel_on_fail")
    boolean m_cancelOnFail = true; // default value previously set in the settings model

    /**
     * The node previously used a SettingsModelColumnFilter2 with key "col_select". This requires a custom persistor for
     * backwards compatibility.
     */
    @Persistor(SelectedColumnsLegacyPersistor.class)
    /**
     * Settings models were not only responsible for persistence but also for providing various other information. E.g.
     * here the column filter model set StringValue.class as compatible column type.
     */
    @ChoicesProvider(StringColumnsProvider.class)
    @Widget(title = "Select columns", description = "The columns to be processed by the node.")
    ColumnFilter m_selectedColumns = new ColumnFilter();

    static /* static, since otherwise an error is thrown on runtime */ final class SelectedColumnsLegacyPersistor
        extends LegacyColumnFilterPersistor {

        /**
         * package-scoped constructor, otherwise a runtime error is thrown
         */
        SelectedColumnsLegacyPersistor() {
            super("col_select");
        }

    }

    /**
     * Since the dialog element previously used radio buttons, we need to use an enum, not a string. So a custom
     * persistor is now used to translate the legacy string values into enum values.
     */
    @Persistor(LegacyReplaceAppendPersistor.class)
    @Widget(title = "Column handling",
        description = "Whether the selected columns should replace existing columns or be appended to the data table.")
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE; // default value previously set in the settings model

    enum ReplaceOrAppend {
            // Legacy key (OPTION_REPLACE). Import from model if visible or reference by a comment.
            @Label("Replace selected columns") //
            REPLACE("Replace selected columns"), //
            @Label("Append selected columns") //
            APPEND("Append selected columns");

        private final String m_legacyValue;

        ReplaceOrAppend(final String legacyValue) {
            m_legacyValue = legacyValue;
        }

        String getLegacyValue() {
            return m_legacyValue;
        }

        static ReplaceOrAppend fromLegacyValue(final String legacyValue) {
            for (final ReplaceOrAppend roa : values()) {
                if (roa.getLegacyValue().equals(legacyValue)) {
                    return roa;
                }
            }
            throw new IllegalArgumentException("No enum constant for value: " + legacyValue);
        }

    }

    static final class LegacyReplaceAppendPersistor implements NodeParametersPersistor<ReplaceOrAppend> {

        private static final String LEGACY_CFG_KEY = "replace_or_append";

        @Override
        public ReplaceOrAppend load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String legacyValue = settings.getString(LEGACY_CFG_KEY);
            return ReplaceOrAppend.fromLegacyValue(legacyValue);
        }

        @Override
        public void save(final ReplaceOrAppend param, final NodeSettingsWO settings) {
            settings.addString(LEGACY_CFG_KEY, param.getLegacyValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{LEGACY_CFG_KEY}};
        }
    }

    /**
     * Look at other persistors in this package for more examples.
     */

}
