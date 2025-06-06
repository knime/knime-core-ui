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
 *   Jun 3, 2025 (hornm): created
 */
package org.knime.core.webui.node.view.table.datavalue.renderer;

import static j2html.TagCreator.b;
import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.details;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.i;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.style;
import static j2html.TagCreator.summary;
import static j2html.TagCreator.text;
import static j2html.TagCreator.title;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.node.agentic.tool.ToolValue;
import org.knime.core.webui.node.view.table.data.render.DataCellContentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;

/**
 * Renderer for {@link ToolValue}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ToolValueRenderer extends DefaultDataValueRenderer
    implements org.knime.core.webui.node.view.table.data.render.DataValueRenderer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Factory for {@link ToolValueRenderer}.
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        private static final String DESCRIPTION = "Tool";

        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new ToolValueRenderer(DESCRIPTION);
        }
    }

    ToolValueRenderer(final String description) {
        super(description);
    }

    @Override
    public DataCellContentType getContentType() {
        return DataCellContentType.HTML;
    }

    @Override
    protected void setValue(final Object value) {
        final String html;
        if (value instanceof ToolValue toolValue) {
            html = getToolValueHTML(toolValue, name -> div(name));
        } else {
            html = html(//
                body( //
                    span().withStyle("color: red").withText("?") //
                )).render();
        }
        super.setValue(html);
    }

    /**
     * Renders a {@link ToolValue} into a HTML document.
     *
     * @param toolValue
     * @return the HTML document as a string
     */
    public static String getToolValueHTML(final ToolValue toolValue) {
        return getToolValueHTML(toolValue, name -> div(b(name), br(), br()));
    }

    private static String getToolValueHTML(final ToolValue toolValue, final Function<String, Tag> toolNameRenderer) {
        // TODO sanitize?
        return html( //
            head( //
                title("Tool View"), //
                style(getCssStyles()) //
            ), //
            body( //
                renderToolDetails(toolValue, toolNameRenderer.apply(toolValue.getName())) //
            ) //
        ).renderFormatted();
    }

    private static ContainerTag renderToolDetails(final ToolValue tool, final Tag toolName) {
        return //
        div( //
            div( //
                toolName), //
            renderDescription(tool.getDescription()), //
            renderParameterSchema(tool.getParameterSchema()), renderPorts("inputs", tool.getInputs()), //
            renderPorts("outputs", tool.getOutputs()) //
        );
    }

    private static DomContent renderDescription(final String htmlContent) {
        return renderDetails(i("description"), isBlank(htmlContent), div().with(rawHtml(htmlContent)));
    }

    private static DomContent renderParameterSchema(final String schema) {
        try {
            var schemaTree = MAPPER.readTree(schema);
            return renderJson(i("parameter schema"), schemaTree);
        } catch (JsonProcessingException ex) {
            return renderDetails(i("parameter schema"), isBlank(schema), pre().withText(schema));
        }
    }

    private static DomContent renderPorts(final String label, final ToolValue.ToolPort[] ports) {
        var portDetails = details().with(summary(i(label)));
        if (ports == null || ports.length == 0) {
            return span();
        } else {
            for (ToolValue.ToolPort port : ports) {
                portDetails.with(renderPort(port));
            }
        }
        return portDetails;
    }

    private static ContainerTag renderPort(final ToolValue.ToolPort port) {
        return details().with(summary(port.name() + " (" + port.type() + ")"), renderDescription(port.description()),
            renderSpec(port.spec()));
    }

    private static DomContent renderSpec(final String spec) {
        try {
            var specTree = MAPPER.readTree(spec);
            return renderJson(i("table spec"), specTree);
        } catch (JsonProcessingException ex) {
            return renderDetails(text("table spec"), isBlank(spec), pre().withText(spec));
        }
    }

    private static DomContent renderDetails(final DomContent summary, final boolean empty,
        final ContainerTag... details) {
        if (empty) {
            return span();
        }
        return details().with(ArrayUtils.insert(0, details, summary(summary)));
    }

    private static DomContent renderJson(final DomContent summary, final JsonNode json) {
        if (json.isObject()) {
            return renderDetails(summary, false, div(each(json.properties().stream().map(prop -> {
                return renderJson(text(prop.getKey()), prop.getValue());
            }))));
        } else if (json.isArray()) {
            var array = (ArrayNode)json;
            var dom = new DomContent[array.size()];
            for (int i = 0; i < array.size(); i++) {
                dom[i] = renderJson(i(String.valueOf(i + 1)), array.get(i));
            }
            return renderDetails(i(summary), false, div(dom));
        } else {
            return div(span(i(summary), text(": "), text(json.asText())));
        }
    }

    private static String getCssStyles() {
        return """
                    body {
                      font-family: Roboto, sans-serif;
                      line-height: 1.5;
                      margin: 20px;
                    }
                    details {
                      display: list-item;
                      list-style: none;
                      margin-left: 0;
                    }
                    summary {
                      cursor: pointer;
                    }
                    details > *:not(summary) {
                      margin-left: 1em;
                    }
                """;
    }

}
