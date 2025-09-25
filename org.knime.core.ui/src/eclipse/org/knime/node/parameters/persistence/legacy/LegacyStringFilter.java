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
 *   Sep 24, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.persistence.legacy;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;

/**
 * NodeParameters class for legacy {@link SettingsModelFilterString} that should be displayed as twin list with numeric
 * column choices. If this class is to be used for columns other than double columns, the
 * {@link LegacyStringFilterModification} must be extended in a way that allows it to change the choices provider at the
 * include list as well as the value provider at the exclude list accordingly.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class LegacyStringFilter implements NodeParameters {

    /**
     * Modification for {@link LegacyStringFilter} that allows to show/hide the "Keep all columns" checkbox and to
     * change the description of the column selection twin list widget.
     *
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     */
    public abstract static class LegacyStringFilterModification implements Modification.Modifier {

        private boolean m_showKeepAll;

        private String m_columnSelectionDescription;

        /**
         * Constructor.
         *
         * @param showKeepAll whether to show the "Keep all columns" checkbox
         * @param columnSelectionDescription the description for the column selection twin list widget, or null to keep
         *            the default description
         */
        protected LegacyStringFilterModification(final boolean showKeepAll, final String columnSelectionDescription) {
            m_showKeepAll = showKeepAll;
            m_columnSelectionDescription = columnSelectionDescription;
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_showKeepAll) {
                group.find(KeepAllColumnsSelectedRef.class).addAnnotation(Widget.class)
                    .withProperty("title", "Always include all columns").withProperty("description",
                        "If checked, node behaves as if all columns were moved to the \"Include\" list.")
                    .modify();
            }
            if (m_columnSelectionDescription != null) {
                group.find(InclListRef.class).modifyAnnotation(Widget.class)
                    .withProperty("description", m_columnSelectionDescription).modify();
            }
        }
    }

    private static final String CFG_KEY_INCL = "InclList";

    private static final String CFG_KEY_EXCL = "ExclList";

    interface KeepAllColumnsSelectedRef extends ParameterReference<Boolean>, Modification.Reference {
    }

    @Persist(configKey = "keep_all_columns_selected")
    @ValueReference(KeepAllColumnsSelectedRef.class)
    @Modification.WidgetReference(KeepAllColumnsSelectedRef.class)
    boolean m_keepAllColumnsSelected;

    static final class KeepAllColumnsSelectedIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(KeepAllColumnsSelectedRef.class).isTrue();
        }
    }

    interface InclListRef extends ParameterReference<String[]>, Modification.Reference {
    }

    static final class InclListPersistor implements NodeParametersPersistor<String[]> {

        @Override
        public String[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getStringArray(CFG_KEY_INCL);
        }

        @Override
        public void save(final String[] param, final NodeSettingsWO settings) {
            settings.addStringArray(CFG_KEY_INCL, param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY_INCL}, {CFG_KEY_EXCL}}; // to be able to control the ExclList via flow vars
        }
    }

    @Persistor(InclListPersistor.class)
    @Widget(title = "Column selection", description = "Move the numeric columns of interest to the \"Include\" list.")
    @ChoicesProvider(DoubleColumnsProvider.class) // could at some point be generalized to other types if needed
    @ValueReference(InclListRef.class)
    @Modification.WidgetReference(InclListRef.class)
    @TwinlistWidget
    @Effect(predicate = KeepAllColumnsSelectedIsTrue.class, type = Effect.EffectType.DISABLE)
    String[] m_inclList = new String[0];

    static final class ExclListProvider implements StateProvider<String[]> {

        private Supplier<String[]> m_inclListSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnValueChange(InclListRef.class);
            m_inclListSupplier = initializer.getValueSupplier(InclListRef.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inclSet = Set.of(m_inclListSupplier.get());
            return parametersInput.getInTableSpec(0).map(ColumnSelectionUtil::getDoubleColumns).orElse(List.of())
                .stream().map(DataColumnSpec::getName).filter(Predicate.not(inclSet::contains)).toArray(String[]::new);
        }
    }

    @Persist(configKey = CFG_KEY_EXCL)
    @ValueProvider(ExclListProvider.class)
    String[] m_exclList = new String[0];

    LegacyStringFilter() {
        this(new String[0], new String[0]);
    }

    /**
     * Constructor.
     *
     * @param inclList initial include list
     * @param exclList initial exclude list
     */
    public LegacyStringFilter(final String[] inclList, final String[] exclList) {
        m_inclList = inclList;
        m_exclList = exclList;
    }
}
