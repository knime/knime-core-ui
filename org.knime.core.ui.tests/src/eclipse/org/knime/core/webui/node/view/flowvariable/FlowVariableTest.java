/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Jan 02, 2024 (carlwitt): created
 */
package org.knime.core.webui.node.view.flowvariable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.workflow.CredentialsStore;

/**
 * Tests {@link FlowVariable#create}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class FlowVariableTest {

    /**
     * AP-21680: Flow variable view should show the login instead of the flow variable name again
     */
    @ParameterizedTest
    @MethodSource("expectedCredentialsVariableFormat")
    void credentialsFlowVariableWithUserName(final String username, final String password, final String expected) {
        // given a credentials variable
        final var credentialsVar =
            CredentialsStore.newCredentialsFlowVariable("variableName", username, password, "fact");
        final var gatewayVar = FlowVariable.create(credentialsVar);
        // when extracting the value for display
        String value = gatewayVar.getValue();
        // then we get the expected format
        assertThat(value).isEqualTo(expected);
    }

    private static Stream<Arguments> expectedCredentialsVariableFormat() {
        return Stream.of( //
            Arguments.of("usern", "passw", "Username: \"usern\", Password: ******"), //
            Arguments.of("usern", null, "Username: \"usern\", Password: not provided"), //
            Arguments.of("usern", "", "Username: \"usern\", Password: not provided"), //
            Arguments.of("", "passs", "Username: \"\", Password: ******"));
    }
}
