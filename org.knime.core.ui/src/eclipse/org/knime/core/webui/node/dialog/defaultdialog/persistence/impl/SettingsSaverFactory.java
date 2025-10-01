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
 *   Dec 19, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.ParametersSaver;
import org.knime.node.parameters.persistence.Persistable;

/**
 *
 * Factory responsible for saving settings.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class SettingsSaverFactory extends PersistenceFactory<ParametersSaver> {

    static final NodeLogger LOGGER = NodeLogger.getLogger(SettingsSaverFactory.class);

    static final String CLASS_ID_CFG_KEY = "@class";

    private static final SettingsSaverFactory INSTANCE = new SettingsSaverFactory();

    private SettingsSaverFactory() {
        // use getInstance
    }

    static SettingsSaverFactory getInstance() {
        return INSTANCE;
    }

    /**
     * @param settings to be saved
     * @param nodeSettings to save to
     * @param <S> the type of the to be saved settings.
     */
    public static <S extends Persistable> void saveSettings(final S settings, final NodeSettingsWO nodeSettings) {
        createSettingsSaver((Class<S>)settings.getClass()).save(settings, nodeSettings);
    }

    /**
     * @param settingsClass
     * @return a settings saver for the given class.
     * @param <S> the type of the settings to save.
     */
    public static <S extends Persistable> ParametersSaver<S> createSettingsSaver(final Class<S> settingsClass) {
        return getInstance().extractFromSettings(settingsClass);
    }

    @Override
    protected ParametersSaver getForLeaf(final LeafNode<Persistable> node) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        final var defaultPersistor = DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey);
        return (obj, settings) -> uncheckedSave(defaultPersistor, obj, settings);
    }

    private static <T> void uncheckedSave(final ParametersSaver<T> saver, final Object value,
        final NodeSettingsWO nodeSettings) {
        saver.save((T)value, nodeSettings);
    }

    @Override
    protected ParametersSaver getFromCustomPersistor(final NodeParametersPersistor<?> nodeSettingsPersistor,
        final TreeNode<Persistable> node) {
        return (obj, settings) -> uncheckedSave(nodeSettingsPersistor, obj, settings);
    }

    @Override
    protected ParametersSaver getForTree(final Tree<Persistable> tree,
        final Function<TreeNode<Persistable>, ParametersSaver> childProperty) {
        return (obj, settings) -> {
            if (tree.isDynamic()) {
                if (obj == null) {
                    settings.addString(CLASS_ID_CFG_KEY, null);
                } else {
                    final var dynamicParametersProvider = InstantiationUtil
                        .createInstance(tree.getAnnotation(DynamicParameters.class).orElseThrow().value());
                    settings.addString(CLASS_ID_CFG_KEY,
                        dynamicParametersProvider.getClassIdStrategy().toIdentifier(obj.getClass()));
                    SettingsSaverFactory.saveSettings((Persistable)obj, settings);
                }
            }
            for (final var child : tree.getChildren()) {
                final var childValue = child.getFromParentValue(obj);
                childProperty.apply(child).save(childValue, settings);
            }
        };
    }

    @Override
    protected ParametersSaver getForArray(final ArrayParentNode<Persistable> arrayNode,
        final ParametersSaver elementProperty) {
        return (obj, settings) -> SettingsSaverArrayParentUtil.save(obj,
            (i, objAtI) -> elementProperty.save(objAtI, settings.addNodeSettings(Integer.toString(i))));
    }

    @Override
    protected ParametersSaver getNested(final TreeNode<Persistable> node, final ParametersSaver property) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        final var doNotNest = SettingsLoaderFactory.hasPersistWithinGoingUp(node);
        return (obj, settings) -> property.save(obj, doNotNest ? settings : settings.addNodeSettings(configKey));
    }

    @Override
    protected ParametersSaver reroute(final String[] relativePath, final ParametersSaver property,
        final TreeNode<Persistable> node) {
        return (obj, settings) -> {
            NodeSettingsWO current = settings;
            for (String pathElement : relativePath) {
                current = getOrAddNodeSettings(current, pathElement);
            }
            property.save(obj, current);
        };
    }

    /**
     * Since the first ".." key is already handled in {@link #getNested}, we need to ignore it here.
     */
    @Override
    protected ParametersSaver rerouteForType(final String[] relativePath, final ParametersSaver property,
        final Tree<Persistable> node) {
        var relativePathCorrected = SettingsLoaderFactory.correctRelativePath(relativePath);
        return reroute(relativePathCorrected, property, node);
    }

    static NodeSettingsWO getOrAddNodeSettings(final NodeSettingsWO settings, final String key) {
        if (key.equals("..")) {
            return (NodeSettings)((NodeSettings)settings).getParent();
        } else {
            try {
                return (((NodeSettings)settings).getNodeSettings(key));
            } catch (final InvalidSettingsException | ClassCastException e) {
                LOGGER.debug("Could not get existing node settings for key " + key + ", creating new one.", e);
                return settings.addNodeSettings(key);
            }
        }

    }

}
