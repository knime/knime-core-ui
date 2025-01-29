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
 *   Aug 5, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeFactory;

/**
 * For creating a persist tree from a {@link PersistableSettings} class.
 *
 * @author Paul Bärnreuther
 */
public final class PersistTreeFactory extends TreeFactory<PersistableSettings> {

    private static final Collection<Class<? extends Annotation>> POSSIBLE_TREE_ANNOTATIONS =
        List.of(Persist.class, Migrate.class, Persistor.class, Migration.class);

    /**
     * Peristors and backwards compatible loaders on classes are interpreted differently than the same annotation on a
     * containing field. The level of the {@link NodeSettings} that the persistor operates on is different.
     */
    static final Collection<ClassAnnotationSpec> POSSIBLE_TREE_CLASS_ANNOTATIONS =
        List.of(new ClassAnnotationSpec(Persistor.class, false), new ClassAnnotationSpec(Migration.class, false));

    private static final Collection<Class<? extends Annotation>> POSSIBLE_LEAF_ANNOTATIONS =
        List.of(Persist.class, Migrate.class, Persistor.class, Migration.class);

    private static final Collection<Class<? extends Annotation>> POSSIBLE_ARRAY_ANNOTATIONS = POSSIBLE_LEAF_ANNOTATIONS;

    /**
     * Create a new factory. This factory is non-static since it implements an abstract factory, but it does not hold
     * any state.
     */
    public PersistTreeFactory() {
        super(POSSIBLE_TREE_ANNOTATIONS, POSSIBLE_TREE_CLASS_ANNOTATIONS, POSSIBLE_LEAF_ANNOTATIONS,
            POSSIBLE_ARRAY_ANNOTATIONS);
    }

    @Override
    protected Class<? extends PersistableSettings> getTreeSettingsClass() {
        return PersistableSettings.class;
    }

}
