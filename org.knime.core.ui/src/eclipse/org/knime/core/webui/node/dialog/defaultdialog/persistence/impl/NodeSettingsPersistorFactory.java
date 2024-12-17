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
 *   Dec 2, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;

/**
 * Creates and caches NodeSettingsPersistors for DefaultNodeSettings.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class NodeSettingsPersistorFactory {

    private static final Map<Class<? extends DefaultNodeSettings>, NodeSettingsPersistor<?>> CACHE =
        new ConcurrentHashMap<>();

    private NodeSettingsPersistorFactory() {

    }

    /**
     * Gets the persistor for the provided settingsClass. Persistors are cached i.e. subsequent calls to
     * {@link #getPersistor(Class)} will return the same persistor instance.
     *
     * @param <S> the type of the settings
     * @param settingsClass class of the settings
     * @return the persistor for the settings
     */
    @SuppressWarnings("unchecked")
    public static <S extends DefaultNodeSettings> NodeSettingsPersistor<S> getPersistor(final Class<S> settingsClass) {
        return (NodeSettingsPersistor<S>)CACHE.computeIfAbsent(settingsClass,
            NodeSettingsPersistorFactory::createPersistor);
    }

    /**
     * Creates the {@link NodeSettingsPersistor persistor} for a {@link DefaultNodeSettings settings} class. <br>
     * <br>
     * If the {@link DefaultNodeSettings settings} are annotated with a {@link Persistor}, then an instance of the
     * {@link Persistor#value()} is created.<br>
     * Otherwise the existing reflection based persistence is used for backwards compatibility.
     *
     * @param <S> the type of {@link DefaultNodeSettings} the persistor is for
     * @param settingsClass the class of {@link DefaultNodeSettings} to create a persistor for
     * @return the persistor for the provided settingsClass
     * @throws IllegalArgumentException if the provided class references an unsupported persistor (e.g. a custom
     *             persistor that extends NodeSettingsPersistor directly)
     */
    public static <S extends PersistableSettings> NodeSettingsPersistor<S>
        createPersistor(final Class<S> settingsClass) {
        var persistor = Optional.ofNullable(settingsClass.getAnnotation(Persistor.class));
        return createPersistor(settingsClass, persistor, () -> new PersistTreeFactory().createTree(settingsClass));
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    @SuppressWarnings({"javadoc", "unchecked"})
    public static <S extends PersistableSettings> NodeSettingsPersistor<S>
        createPersistor(final Tree<PersistableSettings> tree) {
        return createPersistor((Class<S>)tree.getType(), tree.getTypeAnnotation(Persistor.class), () -> tree);
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    @SuppressWarnings("javadoc")
    public static <S extends PersistableSettings> NodeSettingsPersistor<S> createPersistor(final Class<S> settingsClass,
        final Optional<Persistor> persistor, final Supplier<Tree<PersistableSettings>> tree) {
        if (persistor.isEmpty()) {
            return createDefaultPersistor(tree.get());
        } else {
            return createFromPersistorAnnotation(persistor.get(), settingsClass,
                () -> createDefaultPersistor(tree.get()));
        }
    }

    /**
     * no annotation means we use field based persistence
     */
    private static <S extends PersistableSettings> NodeSettingsPersistor<S>
        createDefaultPersistor(final Tree<PersistableSettings> tree) {
        return new FieldBasedNodeSettingsPersistor<>(tree);
    }

    /**
     * @param <S> the settings type
     * @param persistorAnnotation an annotation
     * @param settingsClass the settings class
     * @param defaultClassPersistor
     * @return the persistor instance
     */
    public static <S> NodeSettingsPersistor<S> createFromPersistorAnnotation(final Persistor persistorAnnotation,
        final Class<S> settingsClass, final Supplier<NodeSettingsPersistor<S>> defaultClassPersistor) {
        var persistorClass = persistorAnnotation.value();
        @SuppressWarnings("unchecked")
        NodeSettingsPersistor<S> customPersistor =
            CreateNodeSettingsPersistorUtil.createInstance(persistorClass, settingsClass);
        return CustomPersistorUtil.prepareCustomPersistor(customPersistor, defaultClassPersistor);

    }

}
