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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil.jsonToString;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.keepNodeSpecificSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.map;

import java.util.Map;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersNativeNodeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersPersistUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettingsImpl;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.PasswordHolder;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.NodeSettingsToDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link NodeSettingsService} that translates {@link NodeParameters}-implementations into
 * {@link NodeSettings}-objects (on data apply) and vice-versa (initial data).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class DefaultNodeSettingsService implements NodeSettingsService {

    private final Map<SettingsType, Class<? extends NodeParameters>> m_settingsClasses;

    private final DefaultTextToNodeSettingsConverter m_textToNodeSettingsConverter;

    /**
     * @param settingsClasses map that associates a {@link NodeParameters} class-with a {@link SettingsType}
     */
    public DefaultNodeSettingsService(final Map<SettingsType, Class<? extends NodeParameters>> settingsClasses) {
        m_settingsClasses = settingsClasses;
        m_textToNodeSettingsConverter = new DefaultTextToNodeSettingsConverter(settingsClasses);
    }

    @Override
    public void toNodeSettings(final String textSettings,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {
        m_textToNodeSettingsConverter.toNodeSettings(textSettings, previousSettings, settings);

    }

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final var nodeSpecificSettings = keepNodeSpecificSettings(settings);
        var context = createContext(specs);
        final var loadedSettings = new NodeSettingsToDefaultNodeSettings(context, m_settingsClasses)
            .nodeSettingsToDefaultNodeSettingsOrDefault(map(nodeSpecificSettings));

        final var widgetTreeFactory = new WidgetTreeFactory();
        final var widgetTrees = map(loadedSettings, (type, s) -> widgetTreeFactory.createTree(s.getClass(), type));

        JsonFormsSettings jsonFormsSettings = new JsonFormsSettingsImpl(loadedSettings, context, widgetTrees);

        final Optional<String> nonDefaultJobManagerId = settings.containsKey(SettingsType.JOB_MANAGER)
            ? JobManagerParametersNativeNodeUtil.getNonDefaultJobManagerId(settings.get(SettingsType.JOB_MANAGER))
            : Optional.empty();
        if (nonDefaultJobManagerId.isPresent()) {
            jsonFormsSettings = JobManagerParametersNativeNodeUtil.addJobManagerSelection(//
                jsonFormsSettings, nonDefaultJobManagerId.get());
        }

        final var root = new DefaultNodeDialogDataServiceUtil.InitialDataBuilder(jsonFormsSettings)
            .withUpdates(
                (rootJson, dataJson) -> UpdatesUtil.addUpdates(rootJson, widgetTrees.values(), dataJson, context))
            .withFlowVariables(map(nodeSpecificSettings), context).buildJson();

        addPersist(root, loadedSettings);
        if (nonDefaultJobManagerId.isPresent()) {
            JobManagerParametersPersistUtil
                .setPersistSchema(((ObjectNode)((ObjectNode)root.get("persist")).get("properties")));
        }
        return jsonToString(root);
    }

    private static void addPersist(final ObjectNode root, final Map<SettingsType, NodeParameters> loadedSettings) {
        final var persistTreeFactory = new PersistTreeFactory();
        final var persistTrees = map(loadedSettings, (type, s) -> persistTreeFactory.createTree(s.getClass(), type));
        PersistUtil.addPersist(root, persistTrees);
    }

    @Override
    public void validateNodeSettingsAndVariables(final Map<SettingsType, NodeAndVariableSettingsRO> settings)
        throws InvalidSettingsException {
        for (var entry : settings.entrySet()) {
            final var key = entry.getKey();
            if (key != SettingsType.JOB_MANAGER) {
                NodeParametersUtil.loadSettings(entry.getValue(), m_settingsClasses.get(key));
            }
        }
    }

    private static NodeParametersInput createContext(final PortObjectSpec[] specs) {
        return NodeParametersUtil.createDefaultNodeSettingsContext(specs);
    }

    /**
     * {@inheritDoc}
     *
     * We need to clean up the passwords which were stored during serialization in {@link #fromNodeSettings}
     */
    @Override
    public void deactivate() {
        final var nodeId = NodeContext.getContext().getNodeContainer().getID();
        PasswordHolder.removeAllPasswordsOfDialog(nodeId);
    }

}
