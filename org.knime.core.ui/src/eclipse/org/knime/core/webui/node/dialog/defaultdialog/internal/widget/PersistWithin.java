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
 *   Sep 30, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.widget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.knime.core.node.NodeSettings;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.persistence.Persistable;

/**
 * Use this annotation to be able to persist a field or class relative to a different parent than the one determined by
 * its position in the {@link NodeParameters}. This is e.g. useful if two fields need to access the same sub-config of
 * the parent config (which leads to collisions when saving) or if a nested {@link NodeParameters} class should be
 * persisted top-level. E.g.
 *
 * <pre>
 * public class MyParameters extends NodeParameters {
 *     &#64;PersistWithin("mySubConfig")
 *     private String m_field1; // saved within root > mySubConfig > field1
 *
 *     &#64;PersistWithin("mySubConfig")
 *     private String m_field2; // saved within root > mySubConfig > field2
 *
 *     private NestedParameters m_nestedParams; // will be persisted top-level (i.e. no config named nestedParameter)
 *
 *     &#64;PersistWithin("..")
 *     static final class NestedParameters extends NodeParameters {
 *
 *         String m_field3; // saved within root > field3
 *     }
 * }
 * </pre>
 *
 *
 * Note that attaching the annotation to a class has a different meaning than attaching it to a field of that class:
 * <ul>
 * <li>Attaching to a field means that the parent config for the field is navigated according to the given relative
 * path.</li>
 * <li>Attaching to a class means that the parent config for all fields within the class is navigated according to the
 * given relative path. In case, in addition, one of these fields has another {@link PersistWithin} annotation attached,
 * it is also resolved in addition.</li>
 * </ul>
 * I.e. using path "foo" to the m_nestedParameters field in the example above would lead to foo > nestedParams while
 * attaching it to the class leads to nestedParams > foo.
 *
 *
 * @author Paul Bärnreuther
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface PersistWithin {

    /**
     * The relative path to the parent config in the {@link NodeSettings} where this field or class should be persisted.
     *
     * @return a non-empty list containing ".." to go up one level and/or names of sub-configs to go down
     */
    String[] value();

    /**
     * Adding this to a field has the same effect as adding {@code @PersistWithin({".."})} to its class.
     *
     * It needs to be added to fields of a type extending {@link Persistable}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface PersistEmbedded {

    }

}
