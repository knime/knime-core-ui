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

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.img;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.title;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.node.agentic.tool.ToolValue;
import org.knime.core.webui.node.view.table.data.render.DataCellContentType;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

/**
 * Renderer for {@link ToolValue}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ToolValueRenderer extends DefaultDataValueRenderer
    implements org.knime.core.webui.node.view.table.data.render.DataValueRenderer {

    /**
     * Policy for sanitizing HTML content.
     */
    public static final PolicyFactory POLICY = new HtmlPolicyBuilder() //
        .allowCommonInlineFormattingElements() //
        .allowStandardUrlProtocols() //
        .allowCommonBlockElements() //
        .allowStyling() //
        .allowElements("a") //
        .allowAttributes("href", "target").onElements("a") //
        .toFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DESCRIPTION_ICON =
        """
                data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2032%2032%22%3E%3Cpath%20stroke%3D%22%23000%22%20stroke-linejoin%3D%22round%22%20stroke-miterlimit%3D%2210%22%20d%3D%22M25.428%2017.175h-22m9.404%208H3.157m26.271-4h-26m26-8h-26m26-4h-26m26-4h-26%22%2F%3E%3C%2Fsvg%3E
                """;

    private static final String NO_DESCRIPTION_ICON =
        """
                data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20stroke%3D%22%23000%22%20stroke-linejoin%3D%22round%22%20viewBox%3D%220%200%2032%2032%22%3E%3Cpath%20d%3D%22M3%2016.5h-.5v1H3v-1Zm8.6.5v-.5H3v1h8.6V17ZM2.729%2024.5h-.5v1h.5v-1Zm.871.5v-.5h-.871v1H3.6V25ZM3%2020.5h-.5v1H3v-1Zm4.6.5v-.5H3v1h4.6V21ZM3%2012.5h-.5v1H3v-1Zm12.6.5v-.5H3v1h12.6V13ZM3%208.5h-.5v1H3v-1Zm16.6.5v-.5H3v1h16.6V9ZM3%204.5h-.5v1H3v-1Zm20.6.5v-.5H3v1h20.6V5Z%22%2F%3E%3Cpath%20d%3D%22m1.293%2026.793-.354.353.707.707L2%2027.5l-.707-.707Zm.353.353L2%2027.5%2027.284%202.216l-.353-.354-.354-.353L1.293%2026.793l.353.353ZM28.5%204.5a.5.5%200%200%200%200%201v-1Zm.5.5v-.5h-.5v1h.5V5ZM25.5%208.5a.5.5%200%200%200%200%201v-1ZM29%209v-.5h-3.5v1H29V9ZM21.5%2012.5a.5.5%200%200%200%200%201v-1Zm7.5.5v-.5h-7.5v1H29V13ZM17.5%2016.5a.5.5%200%200%200%200%201v-1ZM29%2017v-.5H17.5v1H29V17ZM13.5%2020.5a.5.5%200%200%200%200%201v-1ZM29%2021v-.5H13.5v1H29V21ZM9.5%2024.5a.5.5%200%200%200%200%201v-1Zm2.904.5v-.5H9.5v1h2.904V25Z%22%2F%3E%3C%2Fsvg%3E
                                """;

    private static final String PARAMETERS_ICON =
        """
                data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%20stroke%3D%22%23000%22%20fill%3D%22none%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M7%2013.5c-1.4%200-2.5%201.1-2.5%202.5s1.1%202.5%202.5%202.5%20%202.5-1.1%202.5-2.5-1.2-2.5-2.5-2.5zM16%2018.6c-1.4%200-2.5%201.1-2.5%202.5s1.1%202.5%202.5%202.5%202.5-1.1%202.5-2.5-1.1-2.5-2.5-2.5zM25%20%2011.8c-1.4%200-2.5%201.1-2.5%202.5s1.1%202.5%202.5%202.5%202.5-1.1%202.5-2.5-1.1-2.5-2.5-2.5zM16%2023.3v4M16%204.7v14.1M7%2018.3v9M7%204.7v9M25%20%20%2016.5v10.8M25%204.7V12%22%2F%3E%3C%2Fsvg%3E
                                              """;

    private static final String INPUTS_ICON =
        """
                data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%20%20%20%20fill%3D%22none%22%20stroke%3D%22%23000%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M16.224%206.514c-5.239-.02-9.503%204.21-9.523%209.449s4.21%209.503%209.449%209.523l6.113-.053.073-18.896-6.112-.023z%20%20M22.313%2012.569l7.873.031M22.286%2019.478l7.873.031M6.701%2015.963l-4.887.035M14.855%2012.749L18.106%2016l-3.251%203.251M18.106%20%2016h-7.175%22%2F%3E%3C%2Fsvg%3E
                                """;

    private static final String OUTPUTS_ICON =
        """
                data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%20%20%20%20fill%3D%22none%22%20stroke%3D%22%23000%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M15.776%2025.486c5.239.02%209.503-4.21%209.523-9.449.02-5.239-4.21-9.503-9.449-9.523l-6.113.053-.074%2018.896%206.113%20%20.023zM9.687%2019.431L1.814%2019.4M9.714%2012.522l-7.873-.031M25.299%2016.037l4.887-.035M16.818%2012.749L20.069%2016l-3.251%203.251%20%20M20.069%2016h-7.175%22%2F%3E%3C%2Fsvg%3E
                                """;

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
            html = getToolValueHTML(toolValue);
        } else {
            html = html(//
                body( //
                    span().withStyle("color: red").withText("?") //
                )).render();
        }
        super.setValue(html);
    }

    private static String getToolValueHTML(final ToolValue toolValue) {
        return html( //
            head( //
                title("Tool Value") //
            ), //
            body( //
                renderToolDetails(toolValue) //
            ) //
        ).renderFormatted();
    }

    private static ContainerTag renderToolDetails(final ToolValue tool) {
        var root = span();
        var header = div(span(tool.getName()) //
            .withStyle("""
                    display: inline-block;
                    width: 120px;
                    text-overflow: ellipsis;
                    overflow: hidden;
                    """));
        var hasDescription = isNotBlank(tool.getDescription());
        if (hasDescription) {
            header.with(renderIcon(null, DESCRIPTION_ICON, "Indicates that the tool has a description"));
        } else {
            header.with(renderIcon(null, NO_DESCRIPTION_ICON, "Indicates that the tool does not have a description"));
        }
        if (isBlank(tool.getParameterSchema())) {
            header.with(emptySpace());
        } else {
            String numParams;
            try {
                var tree = MAPPER.readTree(tool.getParameterSchema());
                numParams = String.valueOf(tree.size());
            } catch (JsonProcessingException ex) {
                numParams = "?";
            }
            header.with(renderIcon(numParams, PARAMETERS_ICON, "The number of tool parameters"));
        }
        var numInputs = tool.getInputs().length;
        if (numInputs > 0) {
            header.with(renderIcon(String.valueOf(numInputs), INPUTS_ICON, "The number of tool inputs"));
        } else {
            header.with(emptySpace());
        }
        var numOutputs = tool.getOutputs().length;
        if (numOutputs > 0) {
            header.with(renderIcon(String.valueOf(numOutputs), OUTPUTS_ICON, "The number of tool outputs"));
        } else {
            header.with(emptySpace());
        }
        root.with(div(header));
        if (hasDescription) {
            root.with(div(rawHtml(ToolValueRenderer.POLICY.sanitize(tool.getDescription()))));
        }
        return root;
    }

    private static DomContent emptySpace() {
        return span() //
            .withStyle("display: inline-block; width: 30px;") //
            .withText(" ");
    }

    private static DomContent renderIcon(final String precedingText, final String iconDataUri, final String title) {
        var res = span();
        if (precedingText == null) {
            res.withStyle("display: inline-block; width: 25px; vertical-align: top;");
        } else {
            res.withStyle("display: inline-block; width: 30px; vertical-align: top;").withText(precedingText);
        }
        return res.with(img().withSrc(iconDataUri).withTitle(title).withWidth("16px").withHeight("16px"));
    }

}
