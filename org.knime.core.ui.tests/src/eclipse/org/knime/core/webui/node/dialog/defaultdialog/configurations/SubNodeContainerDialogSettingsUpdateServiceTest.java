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
 *   May 22, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.configurations;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.data.DataServiceContextTest;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtilImperativeUpdatesTest;
import org.knime.core.webui.node.dialog.defaultdialog.components.SubNodeContainerDialogSettingsUpdateService;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DialogSettingsUpdateService;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.imperative.ImperativeStateProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("static-method")
final class SubNodeContainerDialogSettingsUpdateServiceTest {

    final static ObjectMapper MAPPER = new ObjectMapper();

    final static PortObjectSpec[] PORT_OBJECT_SPECS = new PortObjectSpec[0];

    @BeforeAll
    static void initDataServiceContext() {
        DataServiceContextTest.initDataServiceContext(null, () -> PORT_OBJECT_SPECS);
    }

    @AfterAll
    static void removeDataServiceContext() {
        DataServiceContextTest.removeDataServiceContext();
    }

    private static DialogSettingsUpdateService
        getDataService(final Collection<DialogElementRendererSpec> rendererSpecs) {
        final var modelSettingsRendererSpec =
            rendererSpecs.stream().map(UpdatesUtilImperativeUpdatesTest::setModelSettingsType).toList();
        return new SubNodeContainerDialogSettingsUpdateService(() -> modelSettingsRendererSpec);
    }

    @Test
    void testUiStateProvider() throws ExecutionException, InterruptedException {

        final var dependencyRenderer = UpdatesUtilImperativeUpdatesTest.createTestDependencySpec();
        final var stateProvider = new ImperativeStateProvider<String>() {

            private Supplier<String> m_dependencySupplier;

            @Override
            public void init(final ImperativeStateProviderInitializer initializer) {
                m_dependencySupplier = initializer.computeFromValueSupplier(dependencyRenderer);
            }

            @Override
            public String computeState(final DefaultNodeSettingsContext context)
                throws StateComputationFailureException {
                return "Compute state for dependency:" + m_dependencySupplier.get();
            }

        };
        final var updatedRenderer = UpdatesUtilImperativeUpdatesTest.createControlSpecWithStateProvider(stateProvider);
        final var dataService =
            getDataService(List.of(dependencyRenderer.at("dependency"), updatedRenderer.at("updatedWidget")));

        final String testDepenencyValue = "custom value";
        final var testDependency = List.of(new IndexedValue<String>(List.of(), testDepenencyValue));

        final var valueRefScope = "#/properties/model/properties/dependency";
        final var valueRefTrigger = new Trigger.ValueTrigger(valueRefScope);
        final var resultWrapper =
            dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testDependency));
        final var result = MAPPER.valueToTree(resultWrapper.result());

        assertThatJson(result).inPath("$").isArray().hasSize(1);
        assertThatJson(result).inPath("$[0].scope").isEqualTo("#/properties/model/properties/updatedWidget");
        assertThatJson(result).inPath("$[0].values[0].value")
            .isEqualTo("Compute state for dependency:" + testDepenencyValue);
        assertThatJson(result).inPath("$[0].providedOptionName").isEqualTo("placeholder");
    }

}
