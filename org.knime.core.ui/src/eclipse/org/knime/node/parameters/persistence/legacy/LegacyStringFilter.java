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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.TwinList.InclListRef;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.TwinList.TwinListModification;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ChoicesStateProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
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

        private final Class<? extends EffectPredicateProvider> m_alwaysIncludeAllColumnsRefClass;

        private TwinListModification m_twinListModification;

        /**
         * Constructor.
         *
         * @param showKeepAll whether to show the "Keep all columns" checkbox
         * @param columnSelectionDescription the description for the column selection twin list widget, or null to keep
         *            the default description
         */
        protected LegacyStringFilterModification(final boolean showKeepAll, final String columnSelectionDescription) {
            this(showKeepAll, null, columnSelectionDescription, null, null, null, null, null);
        }

        /**
         * Constructor setting the provider classes for the include and exclude list.
         *
         * @param showKeepAll whether to show the "Keep all columns" checkbox
         * @param title the title for the column selection twin list widget, or null to keep the default title
         * @param description the description for the column selection twin list widget, or null to keep the default
         *            description
         * @param inclListTitle the inclList title, or null to keep the default
         * @param exclListTitle the exclList title, or null to keep the default
         * @param choicesProviderClass the choices provider class for the include list, or {@code null} to keep the
         *            default if it is non-{@code null}, the {@code exclListProviderClass} must be non-{@code null} as
         *            well
         * @param exclListProviderClass provider class for the exclude list
         */
        protected LegacyStringFilterModification(final boolean showKeepAll, final String title,
            final String description, final String inclListTitle, final String exclListTitle,
            final Class<? extends ChoicesStateProvider<?>> choicesProviderClass,
            final Class<? extends ExclListProvider<?>> exclListProviderClass) {
            this(showKeepAll, title, description, inclListTitle, exclListTitle, choicesProviderClass, null,
                exclListProviderClass);
        }

        /**
         * Constructor setting the provider classes for the include and exclude list.
         *
         * @param showKeepAll whether to show the "Keep all columns" checkbox
         * @param title the title for the column selection twin list widget, or null to keep the default title
         * @param description the description for the column selection twin list widget, or null to keep the default
         *            description
         * @param inclListTitle the inclList title, or null to keep the default
         * @param exclListTitle the exclList title, or null to keep the default
         * @param choicesProviderClass the choices provider class for the include list, or {@code null} to keep the
         *            default if it is non-{@code null}, the {@code exclListProviderClass} must be non-{@code null} as
         *            well
         * @param inclListProviderClass provider class for the include list, only needed if showKeepAll is true and the
         *            choicesProviderClass is not {@code null}
         * @param exclListProviderClass provider class for the exclude list
         */
        protected LegacyStringFilterModification(final boolean showKeepAll, final String title,
            final String description, final String inclListTitle, final String exclListTitle,
            final Class<? extends ChoicesStateProvider<?>> choicesProviderClass,
            final Class<? extends InclListProvider<?>> inclListProviderClass,
            final Class<? extends ExclListProvider<?>> exclListProviderClass) {
            /**
             * No custom predicate provider, as the "Keep all columns" checkbox is static when using the
             * {@link LegacyStringFilter}.
             */
            this(showKeepAll, title, description, inclListTitle, exclListTitle, choicesProviderClass,
                inclListProviderClass, exclListProviderClass, null);
        }

        /**
         * Constructor setting the provider classes for the include and exclude list.
         *
         * @param showKeepAll whether to show the "Keep all columns" checkbox
         * @param title the title for the column selection twin list widget, or null to keep the default title
         * @param description the description for the column selection twin list widget, or null to keep the default
         *            description
         * @param inclListTitle the inclList title, or null to keep the default
         * @param exclListTitle the exclList title, or null to keep the default
         * @param choicesProviderClass the choices provider class for the include list, or {@code null} to keep the
         *            default if it is non-{@code null}, the {@code exclListProviderClass} must be non-{@code null} as
         *            well
         * @param inclListProviderClass provider class for the include list, only needed if showKeepAll is true and the
         *            choicesProviderClass is not {@code null}
         * @param exclListProviderClass provider class for the exclude list
         * @param alwaysIncludeAllColumnsRefClass reference class for the "Always include all columns" checkbox
         */
        protected LegacyStringFilterModification(final boolean showKeepAll, final String title,
            final String description, final String inclListTitle, final String exclListTitle,
            final Class<? extends ChoicesStateProvider<?>> choicesProviderClass,
            final Class<? extends InclListProvider<?>> inclListProviderClass,
            final Class<? extends ExclListProvider<?>> exclListProviderClass,
            final Class<? extends EffectPredicateProvider> alwaysIncludeAllColumnsRefClass) {
            m_showKeepAll = showKeepAll;
            m_alwaysIncludeAllColumnsRefClass = alwaysIncludeAllColumnsRefClass;
            m_twinListModification = new TwinListModification(showKeepAll, title, description, inclListTitle,
                exclListTitle, choicesProviderClass, inclListProviderClass, exclListProviderClass,
                alwaysIncludeAllColumnsRefClass);
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_showKeepAll) {
                group.find(AlwaysIncludeAllColumns.class).addAnnotation(Widget.class)
                    .withProperty("title", "Always include all columns").withProperty("description",
                        "If checked, node behaves as if all columns were moved to the \"Include\" list.")
                    .modify();
                if (m_alwaysIncludeAllColumnsRefClass != null) {
                    group.find(AlwaysIncludeAllColumns.class).modifyAnnotation(ValueReference.class)
                        .withValue(m_alwaysIncludeAllColumnsRefClass).modify();
                }
            }
            m_twinListModification.modify(group);
        }
    }

    private static final String CFG_KEY_INCL = "InclList";

    private static final String CFG_KEY_EXCL = "ExclList";

    private static final String CFG_KEY_KEEP_ALL = "keep_all_columns_selected";

    private static final class AlwaysIncludeAllColumns implements BooleanReference, Modification.Reference {
    }

    @Persist(configKey = CFG_KEY_KEEP_ALL)
    @ValueReference(AlwaysIncludeAllColumns.class)
    @Modification.WidgetReference(AlwaysIncludeAllColumns.class)
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_keepAllColumnsSelected;

    /**
     * Twin list for include and exclude list.
     */
    @PersistWithin.PersistEmbedded
    public TwinList m_twinList = new TwinList();

    /**
     * Base class for include list providers.
     *
     * @param <T> type of choices provided, must match inclusion list choices provider
     */
    public abstract static class InclListProvider<T> implements StateProvider<String[]> {

        private Supplier<Boolean> m_keepAllColumnsSelectedSupplier;

        /**
         * @return class for the incl list choices, {@code T} must match the type provided by the inclusion list choices
         *         provider
         */
        public abstract Class<? extends ChoicesStateProvider<T>> getChoicesProviderClass();

        abstract String getChoiceID(T choice);

        /**
         *
         * @return reference class for the "Always include all columns" checkbox
         */
        public Class<? extends BooleanReference> getAlwaysIncludeAllColumnsRefClass() {
            return AlwaysIncludeAllColumns.class;
        }

        private Supplier<List<T>> m_choicesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_keepAllColumnsSelectedSupplier =
                initializer.computeFromValueSupplier(getAlwaysIncludeAllColumnsRefClass());
            m_choicesSupplier = initializer.computeFromProvidedState(getChoicesProviderClass());
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_keepAllColumnsSelectedSupplier.get().booleanValue()) {
                return m_choicesSupplier.get().stream().map(this::getChoiceID).toArray(String[]::new);
            }
            throw new StateComputationFailureException();
        }
    }

    /**
     * Base class for include list providers that are based on column choices.
     */
    public abstract static class ColumnBasedInclListProvider extends InclListProvider<TypedStringChoice> {

        @Override
        final String getChoiceID(final TypedStringChoice choice) {
            return choice.id();
        }
    }

    /**
     * Base class for exclude list providers.
     *
     * @param <T> type of choices provided, must match inclusion list provider
     */
    public abstract static class ExclListProvider<T> implements StateProvider<String[]> {

        private Supplier<String[]> m_inclListSupplier;

        private Supplier<List<T>> m_choicesSupplier;

        /**
         * @return class for the excl list choices, {@code T} must match the type provided by the inclusion list
         *         provider
         */
        public abstract Class<? extends ChoicesStateProvider<T>> getChoicesProviderClass();

        abstract String getChoiceID(T choice);

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_inclListSupplier = initializer.computeFromValueSupplier(InclListRef.class);
            m_choicesSupplier = initializer.computeFromProvidedState(getChoicesProviderClass());
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inclSet = Set.of(m_inclListSupplier.get());
            return m_choicesSupplier.get().stream() //
                .map(this::getChoiceID) //
                .filter(Predicate.not(inclSet::contains)).toArray(String[]::new);
        }
    }

    /**
     * Base class for exclude list providers that are based on column choices.
     */
    public abstract static class ColumnBasedExclListProvider extends ExclListProvider<TypedStringChoice> {

        @Override
        final String getChoiceID(final TypedStringChoice choice) {
            return choice.id();
        }
    }

    /**
     * Twin list for include and exclude list.
     *
     * @author Magnus Gohm, KNIME AG, Konstanz, Germany
     */
    public static class TwinList implements NodeParameters {

        /**
         * Constructor.
         */
        public TwinList() {
            this(new String[0], new String[0]);
        }

        /**
         * Constructor.
         *
         * @param inclList initial include list
         * @param exclList initial exclude list
         */
        public TwinList(final String[] inclList, final String[] exclList) {
            m_inclList = inclList;
            m_exclList = exclList;
        }

        /**
         * Modification for {@link TwinList} that allows to change the title, description, incl/excl list title and
         * incl/excl list choices state provider of the column selection twin list widget.
         *
         * @author Magnus Gohm, KNIME GmbH, Berlin, Germany
         */
        public static class TwinListModification implements Modification.Modifier {

            private boolean m_showKeepAll;

            private String m_title;

            private String m_description;

            private String m_inclListTitle;

            private String m_exclListTitle;

            private Class<? extends ChoicesStateProvider<?>> m_choicesProviderClass;

            private Class<? extends InclListProvider<?>> m_inclListProviderClass;

            private Class<? extends ExclListProvider<?>> m_exclListProviderClass;

            private Class<? extends EffectPredicateProvider> m_keepAllPredicateProvider;

            /**
             * Constructor setting the provider classes for the include and exclude list.
             *
             * @param title the title for the column selection twin list widget, or null to keep the default title
             * @param description the description for the column selection twin list widget, or null to keep the default
             *            description
             * @param inclListTitle the inclList title, or null to keep the default
             * @param exclListTitle the exclList title, or null to keep the default
             * @param choicesProviderClass the choices provider class for the include list, or {@code null} to keep the
             *            default if it is non-{@code null}, the {@code exclListProviderClass} must be non-{@code null}
             *            as well
             * @param inclListProviderClass provider class for the include list
             * @param exclListProviderClass provider class for the exclude list
             * @param keepAllPredicateProvider predicate provider for disabling the twin list when "Keep all columns"
             */
            protected TwinListModification(final String title, final String description, final String inclListTitle,
                final String exclListTitle, final Class<? extends ChoicesStateProvider<?>> choicesProviderClass,
                final Class<? extends InclListProvider<?>> inclListProviderClass,
                final Class<? extends ExclListProvider<?>> exclListProviderClass,
                final Class<? extends EffectPredicateProvider> keepAllPredicateProvider) {
                m_title = title;
                m_description = description;
                m_inclListTitle = inclListTitle;
                m_exclListTitle = exclListTitle;
                m_choicesProviderClass = choicesProviderClass;
                m_inclListProviderClass = inclListProviderClass;
                m_exclListProviderClass = exclListProviderClass;
                m_keepAllPredicateProvider = keepAllPredicateProvider;
            }

            private TwinListModification(final boolean showKeepAll, final String title, final String description,
                final String inclListTitle, final String exclListTitle,
                final Class<? extends ChoicesStateProvider<?>> choicesProviderClass,
                final Class<? extends InclListProvider<?>> inclListProviderClass,
                final Class<? extends ExclListProvider<?>> exclListProviderClass,
                final Class<? extends EffectPredicateProvider> keepAllPredicateProvider) {
                this(title, description, inclListTitle, exclListTitle, choicesProviderClass, inclListProviderClass,
                    exclListProviderClass, keepAllPredicateProvider);
                m_showKeepAll = showKeepAll;
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                if (m_keepAllPredicateProvider != null) {
                    group.find(InclListRef.class).modifyAnnotation(Effect.class)
                        .withProperty("predicate", m_keepAllPredicateProvider).modify();
                }
                if (m_title != null) {
                    group.find(InclListRef.class).modifyAnnotation(Widget.class).withProperty("title", m_title)
                        .modify();
                }
                if (m_description != null) {
                    group.find(InclListRef.class).modifyAnnotation(Widget.class)
                        .withProperty("description", m_description).modify();
                }
                if (m_inclListTitle != null) {
                    group.find(InclListRef.class).modifyAnnotation(TwinlistWidget.class)
                        .withProperty("includedLabel", m_inclListTitle).modify();
                }
                if (m_exclListTitle != null) {
                    group.find(InclListRef.class).modifyAnnotation(TwinlistWidget.class)
                        .withProperty("excludedLabel", m_exclListTitle).modify();
                }
                if (m_choicesProviderClass != null) {
                    group.find(InclListRef.class).modifyAnnotation(ChoicesProvider.class)
                        .withProperty("value", m_choicesProviderClass).modify();
                    CheckUtils.checkNotNull(m_exclListProviderClass, """
                            If the inclusion list provider is modified, the exclusion choices provider must be modified
                            with a compatible implementation as well, but it is missing
                            """);
                }
                if (m_exclListProviderClass != null) {
                    group.find(ExclListRef.class).modifyAnnotation(ValueProvider.class)
                        .withProperty("value", m_exclListProviderClass).modify();
                }
                if (m_inclListProviderClass != null) {
                    group.find(InclListRef.class).addAnnotation(ValueProvider.class).withValue(m_inclListProviderClass)
                        .modify();
                } else if (m_showKeepAll) {
                    group.find(InclListRef.class).addAnnotation(ValueProvider.class)
                        .withValue(DefaultInclListProvider.class).modify();
                }
            }
        }

        interface InclListRef extends ParameterReference<String[]>, Modification.Reference {
        }

        interface ExclListRef extends Modification.Reference {
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
                // to be able to control the ExclList via flow vars
                return new String[][]{{CFG_KEY_INCL}, {CFG_KEY_EXCL}};
            }
        }

        static final class DefaultInclListProvider extends ColumnBasedInclListProvider {
            @Override
            public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
                return DoubleColumnsProvider.class;
            }
        }

        /**
         * Include list
         */
        @Persistor(InclListPersistor.class)
        @Widget(title = "Column selection",
            description = "Move the numeric columns of interest to the \"Include\" list.")
        @ChoicesProvider(DoubleColumnsProvider.class) // could at some point be generalized to other types if needed
        @ValueReference(InclListRef.class)
        @Effect(predicate = AlwaysIncludeAllColumns.class, type = Effect.EffectType.DISABLE)
        @Modification.WidgetReference(InclListRef.class)
        @TwinlistWidget
        public String[] m_inclList = new String[0];

        static final class DefaultExclListProvider extends ColumnBasedExclListProvider {
            @Override
            public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
                return DoubleColumnsProvider.class;
            }
        }

        /**
         * Exclude list
         */
        @Persist(configKey = CFG_KEY_EXCL)
        @ValueProvider(DefaultExclListProvider.class)
        @Modification.WidgetReference(ExclListRef.class)
        public String[] m_exclList = new String[0];

    }

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
        m_twinList.m_inclList = inclList;
        m_twinList.m_exclList = exclList;
    }
}
