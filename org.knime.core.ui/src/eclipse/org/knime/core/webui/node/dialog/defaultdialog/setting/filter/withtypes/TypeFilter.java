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
 *   16 Jan 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.TypedStringChoice.PossibleTypeValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The sub-settings of the column filter which hold the information for filtering by column type.
 *
 * @author Paul Bärnreuther
 */
public abstract class TypeFilter implements PersistableSettings {

    /**
     * Additional information are necessary to display the types in the dialog in order to display previously selected
     * types (and more importantly, their text) stored in {@link #m_selectedTypes} which are not present anymore.
     */
    static final class Serializer extends JsonSerializer<String[]> {

        @Override
        public void serialize(final String[] value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            serializers.defaultSerializeValue(value, gen);
            final var typeFilter = (TypeFilter)gen.getCurrentValue();
            gen.writeObjectField("typeDisplays", typeFilter.getTypeDisplays());
        }

    }

    /**
     * A list of string representations of types of columns which are used in case of m_mode = "TYPE"
     */
    @JsonSerialize(using = TypeFilter.Serializer.class)
    public String[] m_selectedTypes; //NOSONAR

    /**
     * Filter with no selected Types
     */
    protected TypeFilter() {
        this(new String[0]);
    }

    /**
     * Filter with selected types
     *
     * @param selectedTypes
     */
    protected TypeFilter(final String[] selectedTypes) {
        m_selectedTypes = selectedTypes;
    }

    /**
     * For a given persisted/previous type id, return the corresponding {@link PossibleTypeValue} if it still can be
     * determined.
     *
     * @param typeId the persisted/previous type id
     * @return the corresponding {@link PossibleTypeValue} if it still can be determined
     */
    protected abstract Optional<PossibleTypeValue> fromTypeId(final String typeId);

    private PossibleTypeValue[] getTypeDisplays() {
        return Stream.of(m_selectedTypes)//
            .map(this::fromTypeId)//
            .flatMap(Optional::stream)//
            .toArray(PossibleTypeValue[]::new);

    }

    /**
     * @return whether the typeId is part of the selected ids.
     */
    protected Predicate<String> getIsSelectedPredicate() {
        final var selectedTypesSet = Set.of(m_selectedTypes);
        return selectedTypesSet::contains;
    }

}
