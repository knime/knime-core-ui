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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CredentialsRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.PasswordWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.UsernameWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopBooleanProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

class CredentialsRenderer extends WidgetTreeControlRendererSpec implements CredentialsRendererSpec {

    CredentialsRenderer(final TreeNode<WidgetGroup> node) {
        super(node);
    }

    @Override
    public Optional<CredentialsRendererOptions> getOptions() {
        final var credentialsWidget = m_node.getAnnotation(CredentialsWidget.class);
        final var passwordWidget = m_node.getAnnotation(PasswordWidget.class);
        final var usernameWidget = m_node.getAnnotation(UsernameWidget.class);
        if (Stream.of(credentialsWidget, passwordWidget, usernameWidget).flatMap(Optional::stream).count() > 1) {
            throw new UiSchemaGenerationException(String.format(
                "@%s, @%s and @%s should not be used together in one place.", UsernameWidget.class.getSimpleName(),
                PasswordWidget.class.getSimpleName(), CredentialsWidget.class.getSimpleName()));
        }
        if (credentialsWidget.isPresent()) {
            return Optional.of(getOptionsFromCredentialsWidget(credentialsWidget.get()));
        }
        if (passwordWidget.isPresent()) {
            return Optional.of(getOptionsFromPasswordWidget(passwordWidget.get()));
        }
        if (usernameWidget.isPresent()) {
            return Optional.of(getOptionsFromUsernameWidget(usernameWidget.get()));
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        final Map<String, Class<? extends StateProvider>> stateProviderClasses = new HashMap<>();
        final var credentialsWidget = m_node.getAnnotation(CredentialsWidget.class);
        final var hasUsernameProvider = credentialsWidget.map(CredentialsWidget::hasUsernameProvider)
            .filter(prov -> !prov.equals(NoopBooleanProvider.class));
        hasUsernameProvider.ifPresent(provider -> stateProviderClasses.put(HAS_USERNAME, provider));
        final var hasPasswordProvider = credentialsWidget.map(CredentialsWidget::hasPasswordProvider)
            .filter(prov -> !prov.equals(NoopBooleanProvider.class));
        hasPasswordProvider.ifPresent(provider -> stateProviderClasses.put(HAS_PASSWORD, provider));
        return stateProviderClasses;
    }

    private static CredentialsRendererOptions
        getOptionsFromCredentialsWidget(final CredentialsWidget credentialsWidget) {
        return new CredentialsRendererOptions() {

            @Override
            public Optional<String> getUsernameLabel() {
                return Optional.of(credentialsWidget.usernameLabel());
            }

            @Override
            public Optional<String> getPasswordLabel() {
                return Optional.of(credentialsWidget.passwordLabel());
            }

            @Override
            public Optional<Boolean> getShowSecondFactor() {
                if (credentialsWidget.hasSecondAuthenticationFactor()) {
                    return Optional.of(true);
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> getSecondFactorLabel() {
                if (credentialsWidget.hasSecondAuthenticationFactor()) {
                    return Optional.of(credentialsWidget.secondFactorLabel());
                }
                return Optional.empty();
            }

        };
    }

    private static CredentialsRendererOptions getOptionsFromPasswordWidget(final PasswordWidget passwordWidget) {
        return new CredentialsRendererOptions() {

            @Override
            public Optional<Boolean> getHasUsername() {
                return Optional.of(false);
            }

            @Override
            public Optional<String> getPasswordLabel() {
                return Optional.of(passwordWidget.passwordLabel());
            }

            @Override
            public Optional<Boolean> getShowSecondFactor() {
                if (passwordWidget.hasSecondAuthenticationFactor()) {
                    return Optional.of(true);
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> getSecondFactorLabel() {
                if (passwordWidget.hasSecondAuthenticationFactor()) {
                    return Optional.of(passwordWidget.secondFactorLabel());
                }
                return Optional.empty();
            }

        };
    }

    private static CredentialsRendererOptions getOptionsFromUsernameWidget(final UsernameWidget usernameWidget) {
        return new CredentialsRendererOptions() {

            @Override
            public Optional<Boolean> getHasPassword() {
                return Optional.of(false);
            }

            @Override
            public Optional<String> getUsernameLabel() {
                return Optional.of(usernameWidget.value());
            }

        };
    }

}
