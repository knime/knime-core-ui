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
package org.knime.core.webui.node.dialog.defaultdialog.setting.choices.single;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;

/**
 * Use this class whenever a combination of static/special and dynamic/special choices is required. If no special
 * choices are required, use a String field instead. If no dynamic choices are required, use an enum field instead.
 *
 * Use a {@link ChoicesWidget} annotation to define the dynamic choices.
 *
 *
 * @param <E> an enum representing the fixed/special choices of this selection. The same mechanism as when the enum is
 *            used directly as field type applies here.
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class SingleSelection<E extends Enum<E>> implements PersistableSettings {

    String m_regularChoice;

    /**
     * If set to true, the special choice is enforced, even if a regular choice is set. This option is only exposed to
     * the user in the flow variable tab. It is needed to be able to dynamically switch between regular and special
     * choices.
     */
    boolean m_enforceSpecialChoice;

    E m_specialChoice;

    SingleSelection() {
        // for serialization
    }

    /**
     *
     * @param regularChoice one of the dynamic choices as defined in the associated provider, which should be selected
     *            initially,
     */
    public SingleSelection(final String regularChoice) {
        m_regularChoice = regularChoice;
    }

    /**
     * @param specialChoice one of the special choices, which should be selected initially.
     */
    public SingleSelection(final E specialChoice) {
        m_specialChoice = specialChoice;
    }

    /**
     * @return the selected special choice if a special choice is selected.
     */
    public Optional<E> getSpecialChoice() {
        if (m_regularChoice == null || m_enforceSpecialChoice) {
            return Optional.of(m_specialChoice);
        }
        return Optional.empty();
    }
    /**
     * Call this method only when {@link #getSpecialChoice} returned an empty optional in order to guarantee that the returned value is non-null.
     * @return the selected regular choices.
     */
    public String getRegularChoice() {
        return m_enforceSpecialChoice ? null : m_regularChoice;
    }
}
