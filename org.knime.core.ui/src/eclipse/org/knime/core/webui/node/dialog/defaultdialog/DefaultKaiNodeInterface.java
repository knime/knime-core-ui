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
 *   Jan 17, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.util.InputPortUtil;
import org.knime.core.webui.node.dialog.NodeSettingsDataServiceProviderAdapter;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Default implementation of a KaiNodeInterface for nodes that use {@link DefaultNodeSettings}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultKaiNodeInterface implements KaiNodeInterface {

    private final NodeSettingsService m_settingsService;

    // TODO only needed because the settingsService doesn't allow to infer which types it supports
    private final Set<SettingsType> m_settingsTypes;

    /**
     * Constructor for nodes that are based on {@link DefaultNodeSettings}.
     * @param settingsClasses the classes of settings of the node
     */
    public DefaultKaiNodeInterface(final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses) {
        this(new DefaultNodeSettingsService(settingsClasses, null, true), settingsClasses.keySet());
    }

    /**
     * Constructor for nodes that provide a NodeSettingsService that produces JSON forms dialog representations.
     * @param settingsService used to extract the dialog representation and apply generated settings
     * @param settingsTypes the types of settings required by the settingsService
     */
    public DefaultKaiNodeInterface(final NodeSettingsService settingsService, final Set<SettingsType> settingsTypes) {
        m_settingsService = settingsService;
        m_settingsTypes = settingsTypes;
    }

    @Override
    public ConfigureSpec getConfigureSpec() {
        // TODO evaluate what the correct inputs for getConfigureAPI are
        var nnc = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();

        var dialogRepresentation = getJsonDialogRepresentation(nnc);

        var currentSettings = dialogRepresentation.get(JsonFormsConsts.FIELD_NAME_DATA);
        var schema = dialogRepresentation.get(JsonFormsConsts.FIELD_NAME_SCHEMA);

        var specs = InputPortUtil.getInputSpecsIncludingVariablePort(nnc);

        var systemMessage = constructSystemMessage(currentSettings.toPrettyString(), specs);

        try {
            return new ConfigureSpec(systemMessage, JsonFormsDataUtil.getMapper().writeValueAsString(schema));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write schema as string.", ex);
        }
    }

    private JsonNode getJsonDialogRepresentation(final NativeNodeContainer nnc) {
        var adapter = getDataServiceProvider(nnc);
        var initialDataService = adapter.createInitialDataService().orElseThrow();

        var mapper = JsonFormsDataUtil.getMapper();

        var initialData = initialDataService.getInitialData();
        try {
            return mapper.readTree(initialData).get("result");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private DataServiceProvider getDataServiceProvider(final NativeNodeContainer nnc) {
        return NodeSettingsDataServiceProviderAdapter.adaptNodeSettingsService(nnc,
            m_settingsTypes, m_settingsService, null);
    }

    private static String constructSystemMessage(final String currentSettings, final PortObjectSpec[] specs) {
        return new StringBuilder(
            "You are a KNIME AI assistant that configures nodes using JSON based on the given instruction.")//
                .append("\n\n")//
                .append("# Current settings\n\n")//
                .append(currentSettings)//
                .append("\n\n# Inputs\n\n")//
                .append(stringify(specs))//
                .toString();
    }

    private static String stringify(final PortObjectSpec[] specs) {
        var nc = NodeContext.getContext().getNodeContainer();
        return IntStream.range(0, nc.getNrInPorts())//
            .mapToObj(i -> stringify(nc.getInPort(i).getPortType(), specs[i]))//
            .collect(joining("\n"));
    }

    private static String stringify(final PortType portType, final PortObjectSpec spec) {
        if (spec instanceof DataTableSpec tableSpec) {
            return tableSpec.stream()//
                    .map(c -> "(%s, %s)".formatted(c.getName(), c.getType().getName()))//
                    .collect(joining("[", ", ", "]"));
        }

        return portType.getName();

    }

    @Override
    public void applyResponse(final String jsonResponse) throws InvalidSettingsException {
        // TODO pull out?
        var nnc = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
        var applyService = getDataServiceProvider(nnc).createApplyDataService().orElseThrow();
        try {
            applyService.applyData(jsonResponse);
        } catch (IOException ex) {
            throw new InvalidSettingsException("Failed to apply settings.", ex);
        }

    }


}
