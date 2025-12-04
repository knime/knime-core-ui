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
 *   Aug 27, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell.CreateDataCellParameters.SpecificTypeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Dynamically loaded parameters core data types.
 *
 * @author Paul Bärnreuther
 */
final class CoreCreateDataCellParameters {

    private CoreCreateDataCellParameters() {
        // utility class containing nested types.
    }

    /**
     * Just a string input. Can be used for all data types whose cell factories implement {@link FromString} and do not
     * provide a specific other parameters class.
     */
    static final class FromStringCellParameters implements CreateDataCellParameters {

        @Widget(title = CUSTOM_VALUE_TITLE, description = CUSTOM_VALUE_DESCRIPTION)
        String m_value = "";

        @Override
        public DataCell createDataCell(final DataType type, final ExecutionContext ctx)
            throws InvalidSettingsException {
            return createDataCellFromString(m_value, type, ctx);
        }

        @Override
        public String toString() {
            return m_value;
        }
    }

    static final class IntCellParameters implements SpecificTypeParameters {

        @Widget(title = CUSTOM_VALUE_TITLE, description = CUSTOM_VALUE_DESCRIPTION)
        int m_value;

        @Override
        public DataCell createSpecificCell() {
            return IntCellFactory.create(m_value);
        }

        @Override
        public String toString() {
            return String.valueOf(m_value);
        }

        @Override
        public DataType getSpecificType() {
            return IntCellFactory.TYPE;
        }

    }

    static final class DoubleCellParameters implements SpecificTypeParameters {

        @Widget(title = CUSTOM_VALUE_TITLE, description = CUSTOM_VALUE_DESCRIPTION)
        double m_value;

        @Override
        public DataCell createSpecificCell() {
            return DoubleCellFactory.create(m_value);
        }

        @Override
        public String toString() {
            return String.valueOf(m_value);
        }

        @Override
        public DataType getSpecificType() {
            return DoubleCellFactory.TYPE;
        }

    }

    static final class LongCellParameters implements SpecificTypeParameters {
        @Widget(title = CUSTOM_VALUE_TITLE, description = CUSTOM_VALUE_DESCRIPTION)
        long m_value;

        @Override
        public DataType getSpecificType() {
            return LongCellFactory.TYPE;
        }

        @Override
        public DataCell createSpecificCell() {
            return LongCellFactory.create(m_value);
        }

        @Override
        public String toString() {
            return String.valueOf(m_value);
        }

    }

    static final class BooleanCellParameters implements SpecificTypeParameters {
        @Widget(title = CUSTOM_VALUE_TITLE, description = CUSTOM_VALUE_DESCRIPTION)
        @ValueSwitchWidget
        @Persistor(FalseTruePersistor.class)
        FalseTrue m_value = FalseTrue.FALSE;

        static final class FalseTruePersistor extends EnumBooleanPersistor<FalseTrue> {

            FalseTruePersistor() {
                super("value", FalseTrue.class, FalseTrue.TRUE);
            }
        }

        enum FalseTrue {
                @Label("false")
                FALSE, //
                @Label("true")
                TRUE
        }

        @Override
        public DataType getSpecificType() {
            return BooleanCellFactory.TYPE;
        }

        @Override
        public DataCell createSpecificCell() {
            return BooleanCellFactory.create(m_value == FalseTrue.TRUE);
        }

        @Override
        public String toString() {
            return String.valueOf(m_value == FalseTrue.TRUE);
        }

    }

}
