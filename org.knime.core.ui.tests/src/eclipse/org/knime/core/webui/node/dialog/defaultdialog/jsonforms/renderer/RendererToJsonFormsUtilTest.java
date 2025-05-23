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
 *   Apr 9, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderer;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.SectionRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The implementation of the individual renderers is (to be) tested in the ui schema generation tests for default node
 * dialogs. This test is just a superficial test for the renderer framework.
 *
 * @author Paul Bärnreuther
 */
class RendererToJsonFormsUtilTest {

    static final DialogElementRendererSpec TEST_TEXT_RENDERER = new TextRendererSpec() {

        @Override
        public String getTitle() {
            return "My Text";
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.of("My description");
        }

        @Override
        public Optional<TextRendererOptions> getOptions() {
            return Optional.of(new TextRendererOptions() {
                @Override
                public Optional<String> getPlaceholder() {
                    return Optional.of("My placeholder");
                }
            });
        }
    }.at("view", "my", "path");

    /**
     * Test dialog with a text renderer with options inside a section.
     */
    static final DialogElementRendererSpec TEST_DIALOG = new SectionRendererSpec() {

        @Override
        public Collection<DialogElementRendererSpec> getElements() {
            return List.of(TEST_TEXT_RENDERER);
        }

        @Override
        public String getTitle() {
            return "My Section";
        }
    };

    static final UnaryOperator<ObjectNode> TEST_SCHEMA_CONSTRUCTOR =
        RendererToJsonFormsUtil.toSchemaConstructor(TEST_DIALOG);

    static final UnaryOperator<ObjectNode> TEST_SCHEMA_PROPERTIES_ADDER =
        RendererToJsonFormsUtil.toSchemaPropertiesAdder(TEST_DIALOG);

    @Test
    void testUiSchema() {
        final var uiSchema = RendererToJsonFormsUtil.toUiSchemaElement(TEST_DIALOG);
        assertThatJson(uiSchema).inPath("$.type").isEqualTo("Section");
        assertThatJson(uiSchema).inPath("$.label").isEqualTo("My Section");
        assertThatJson(uiSchema).inPath("$.elements").isArray().hasSize(1);
        assertThatJson(uiSchema).inPath("$.elements[0].type").isEqualTo("Control");
        assertThatJson(uiSchema).inPath("$.elements[0].scope")
            .isEqualTo("#/properties/view/properties/my/properties/path");
        assertThatJson(uiSchema).inPath("$.elements[0].options.placeholder").isEqualTo("My placeholder");

    }

    @Test
    void testSchemaPropertiesAdder() {
        final var schemaWithoutProperties = JsonFormsDataUtil.getMapper().createObjectNode();
        schemaWithoutProperties.putObject("properties").putObject("view").putObject("properties").putObject("my")
            .putObject("properties").putObject("path").put("title", "will be overwritten");

        final var filledSchema = TEST_SCHEMA_PROPERTIES_ADDER.apply(schemaWithoutProperties);

        assertSchemaWithProperties(filledSchema);

    }

    @Test
    void testSchemaConstructor() {
        final var emptySchema = JsonFormsDataUtil.getMapper().createObjectNode();
        final var constructedSchema = TEST_SCHEMA_CONSTRUCTOR.apply(emptySchema);

        assertSchemaWithProperties(constructedSchema);

    }

    @Test
    void testConstructSchema() {
        assertSchemaWithProperties(RendererToJsonFormsUtil.constructSchema(TEST_DIALOG));
    }

    private static void assertSchemaWithProperties(final ObjectNode schema) {
        assertThatJson(schema).inPath("$.properties.view.properties.my.properties.path.title").isEqualTo("My Text");
        assertThatJson(schema).inPath("$.properties.view.properties.my.properties.path.description")
            .isEqualTo("My description");
    }

    @Test
    void testSchemaConstructorThrowsOnWrongExistingValues() {
        final var schemaWithExistingProperties = JsonFormsDataUtil.getMapper().createObjectNode();
        final var subSchema = schemaWithExistingProperties.putObject("properties").putObject("view")
            .putObject("properties").putObject("my").putObject("properties").putObject("path");
        subSchema.put("title", "My Text");
        subSchema.put("description", "My different description");

        assertThat(
            assertThrows(IllegalStateException.class, () -> TEST_SCHEMA_CONSTRUCTOR.apply(schemaWithExistingProperties))
                .getMessage()).contains("description", "My different description", "My description");
    }

    @ParameterizedTest
    @MethodSource("schemaConstructorProvider")
    void testSchemaThrowsOnNonObjectExistingValues(final UnaryOperator<ObjectNode> schemaConstructor) {
        final var schemaWithWrongStructure = JsonFormsDataUtil.getMapper().createObjectNode();
        schemaWithWrongStructure.put("properties", "wrong structure");

        assertThat(assertThrows(IllegalStateException.class, () -> schemaConstructor.apply(schemaWithWrongStructure))
            .getMessage()).contains("properties", "object");
    }

    static Stream<Arguments> schemaConstructorProvider() {
        return Stream.of(Arguments.of(TEST_SCHEMA_CONSTRUCTOR), Arguments.of(TEST_SCHEMA_PROPERTIES_ADDER));
    }

}
