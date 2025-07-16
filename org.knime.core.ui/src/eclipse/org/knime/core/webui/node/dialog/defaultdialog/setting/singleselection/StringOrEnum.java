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
 *   Feb 5, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection;

import java.util.Objects;
import java.util.Optional;

import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.choices.ChoicesProvider;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Use this class whenever a combination of static/special and dynamic/regular choices is required. If no special
 * choices are required, use a String field instead. If no dynamic choices are required, use an enum field instead.
 *
 * Use a {@link ChoicesProvider} annotation to define the dynamic string choices.
 *
 *
 * @param <E> an enum representing the fixed/special choices of this selection. The same mechanism as when the enum is
 *            used directly as field type applies here.
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class StringOrEnum<E extends Enum<E>> implements Persistable {

    @JsonProperty("regularChoice")
    String m_regularChoice;

    @JsonProperty("specialChoice")
    @Persist(hidden = true)
    E m_specialChoice;

    StringOrEnum() {
        // for serialization
    }

    /**
     * String constructor.
     *
     * @param regularChoice one of the dynamic choices as defined in the associated provider, which should be selected
     *            initially,
     */
    public StringOrEnum(final String regularChoice) {
        m_regularChoice = regularChoice;
    }

    /**
     * Enum constructor.
     *
     * @param specialChoice one of the special choices, which should be selected initially.
     */
    public StringOrEnum(final E specialChoice) {
        m_specialChoice = specialChoice;
    }

    /**
     * This method returns an empty optional, if no special choice is selected. Use {@link #getStringChoice()} to get
     * the selected choice instead in this case.
     *
     * @return the selected special choice if a special choice is selected.
     */
    public Optional<E> getEnumChoice() {
        if (m_regularChoice == null) {
            return Optional.ofNullable(m_specialChoice);
        }
        return Optional.empty();
    }

    /**
     * Call this method only when {@link #getEnumChoice} returned an empty optional in order to guarantee that the
     * returned value is non-null.
     *
     * @return the selected regular choices.
     */
    public String getStringChoice() {
        return m_regularChoice;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StringOrEnum<?> other = (StringOrEnum<?>)obj;
        return Objects.equals(m_regularChoice, other.m_regularChoice)
            && Objects.equals(m_specialChoice, other.m_specialChoice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_regularChoice, m_specialChoice);
    }
}
