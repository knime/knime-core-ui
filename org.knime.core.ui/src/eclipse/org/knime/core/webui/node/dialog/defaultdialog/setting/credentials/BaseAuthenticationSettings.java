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
 *   Apr 3, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;

/**
 * Provides a base for authentication settings in which an {@link AuthenticationType} can be selected.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
public abstract class BaseAuthenticationSettings implements WidgetGroup, Persistable {

    BaseAuthenticationSettings() {
        this.m_type = AuthenticationType.NONE;
    }

    BaseAuthenticationSettings(final AuthenticationType type) {
        this.m_type = type;
    }

    @Widget(title = "Authentication type", description = "The type of the used authentication.")
    @ValueReference(AuthenticationTypeRef.class)
    @Modification.WidgetReference(AuthenticationTypeRef.class)
    AuthenticationType m_type;

    /**
     * Can be overwritten and used to limit the available authentication types.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public abstract static class AuthenticationTypeModification implements Modification.Modifier {

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(AuthenticationTypeRef.class).addAnnotation(ChoicesProvider.class)
                .withProperty("value", getAuthenticationTypeChoicesProvider()).modify();
        }

        /**
         * @return choices provider for available authentication types
         */
        protected abstract Class<? extends EnumChoicesProvider<AuthenticationType>>
            getAuthenticationTypeChoicesProvider();

    }

    static final class AuthenticationTypeRef implements ParameterReference<AuthenticationType>, Modification.Reference {
        static class RequiresCredentials implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationTypeRef.class).isOneOf(AuthenticationType.REQUIRE_CREDENTIALS);
            }

        }
    }

}
