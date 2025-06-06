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
 *   Jun 6, 2025 (hornm): created
 */
package org.knime.core.webui.node.view.table.datavalue.views;

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

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.agentic.tool.ToolValue;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.view.table.datavalue.DataValueView;
import org.knime.core.webui.node.view.table.datavalue.renderer.ToolValueRenderer;
import org.knime.core.webui.page.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

/**
 * {@link DataValueView} for a {@link ToolValue}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ToolValueView implements DataValueView {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CSS_STYLES = """
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

    private final ToolValue m_value;

    @SuppressWarnings("javadoc")
    public ToolValueView(final ToolValue value) {
        m_value = value;
    }

    @Override
    public Page getPage() {
        return Page.builder(() -> getToolValueHTML(m_value), "index.html").build();
    }

    @Override
    public <D> Optional<InitialDataService<D>> createInitialDataService() {
        return Optional.empty();
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        return Optional.empty();
    }

    private static String getToolValueHTML(final ToolValue toolValue) {
        return html( //
            head( //
                title("Tool View"), //
                style(CSS_STYLES) //
            ), //
            body( //
                renderToolDetails(toolValue) //
            ) //
        ).renderFormatted();
    }

    private static ContainerTag renderToolDetails(final ToolValue tool) {
        return //
        div( //
            div( //
                b(tool.getName()), br(), br()), //
            renderDescription(tool.getDescription()), //
            renderParameterSchema(tool.getParameterSchema()), //
            renderPorts("inputs", tool.getInputs()), //
            renderPorts("outputs", tool.getOutputs()) //
        );
    }

    private static DomContent renderDescription(final String htmlContent) {
        return renderDetails(i("description"), isBlank(htmlContent), true,
            div().with(rawHtml(ToolValueRenderer.POLICY.sanitize(htmlContent))));
    }

    private static DomContent renderParameterSchema(final String schema) {
        if (isBlank(schema)) {
            return span();
        }
        try {
            var schemaTree = MAPPER.readTree(schema);
            return renderJson(i("parameters"), schemaTree, true);
        } catch (JsonProcessingException ex) {
            return renderDetails(i("parameters"), isBlank(schema), true, pre().withText(schema));
        }
    }

    private static DomContent renderPorts(final String label, final ToolValue.ToolPort[] ports) {
        if (ports == null || ports.length == 0) {
            return span();
        }
        return renderDetails(i(label), false, true,
            Arrays.stream(ports).map(p -> renderPort(p)).toArray(ContainerTag[]::new));
    }

    private static ContainerTag renderPort(final ToolValue.ToolPort port) {
        return details().with(summary(port.name() + " (" + port.type() + ")"), renderDescription(port.description()),
            renderSpec(port.spec()));
    }

    private static DomContent renderSpec(final String spec) {
        try {
            var specTree = MAPPER.readTree(spec);
            return renderJson(i("table spec"), specTree, false);
        } catch (JsonProcessingException ex) {
            return renderDetails(text("table spec"), isBlank(spec), false, pre().withText(spec));
        }
    }

    private static DomContent renderDetails(final DomContent summary, final boolean empty, final boolean expand,
        final ContainerTag... details) {
        if (empty) {
            return span();
        }
        var res = details().with(ArrayUtils.insert(0, details, summary(summary)));
        if (expand) {
            res.attr("open");
        }
        return res;
    }

    private static DomContent renderJson(final DomContent summary, final JsonNode json, final boolean expand) {
        if (json.isObject()) {
            return renderDetails(summary, false, expand, div( //
                each(json.properties().stream().map(prop -> renderJson(text(prop.getKey()), prop.getValue(), false)))) //
            );
        } else if (json.isArray()) {
            var array = (ArrayNode)json;
            var dom = new DomContent[array.size()];
            for (int i = 0; i < array.size(); i++) {
                dom[i] = renderJson(i(String.valueOf(i + 1)), array.get(i), false);
            }
            return renderDetails(i(summary), false, false, div(dom));
        } else {
            return div(span(i(summary), text(": "), text(json.asText())));
        }
    }

}
