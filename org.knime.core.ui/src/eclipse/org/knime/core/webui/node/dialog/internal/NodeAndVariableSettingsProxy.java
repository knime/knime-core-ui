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
 *   Aug 25, 2023 (hornm): created
 */
package org.knime.core.webui.node.dialog.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.ConfigBase.CopyFromConfigBase;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;

/**
 * Instead of implementing the {@link NodeAndVariableSettingsRO}- and {@link NodeAndVariableSettingsWO}-interface and
 * 'manually' delegating all the methods to {@link NodeSettings}- and {@link VariableSettings}-implementation, this
 * class allows one to create a proxy-instance for those interfaces which 'automatically' (via reflection) delegate the
 * interface-calls to the respective implementation.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeAndVariableSettingsProxy {

    private NodeAndVariableSettingsProxy() {
        // utility
    }

    /**
     * Creates a {@link NodeAndVariableSettingsRO} that combines the given {@link NodeSettings} and {@link VariableSettings}.
     *
     * @param nodeSettingsDelegate the NodeSettings
     * @param variableSettingsDelegate the VariableSettings
     * @return a proxy combining the two provided settings
     */
    public static NodeAndVariableSettingsRO createROProxy(final NodeSettings nodeSettingsDelegate,
        final VariableSettings variableSettingsDelegate) {
        assert nodeSettingsDelegate != null;
        assert variableSettingsDelegate != null;
        return (NodeAndVariableSettingsRO)createProxy(nodeSettingsDelegate, variableSettingsDelegate);
    }

    /**
     * Creates a {@link NodeAndVariableSettingsWO} that combines the given {@link NodeSettings} and {@link VariableSettings}.
     *
     * @param nodeSettingsDelegate the NodeSettings
     * @param variableSettingsDelegate the VariableSettings
     * @return a proxy combining the two provided settings
     */
    public static NodeAndVariableSettingsWO createWOProxy(final NodeSettings nodeSettingsDelegate,
        final VariableSettings variableSettingsDelegate) {
        assert nodeSettingsDelegate != null;
        assert variableSettingsDelegate != null;
        return (NodeAndVariableSettingsWO)createProxy(nodeSettingsDelegate, variableSettingsDelegate);
    }

    private static Object createProxy(final NodeSettings nodeSettingsDelegate,
        final VariableSettings variableSettingsDelegate) {
        Supplier<NodeSettings> nodeSettingsWrapper = () -> nodeSettingsDelegate;
        CopyFromConfigBase copyFromConfigBase = configBase -> configBase.copyTo(nodeSettingsDelegate);
        InvocationHandler invocationHandler = (proxy, method, args) -> { // NOSONAR
            for (Object delegate : new Object[]{copyFromConfigBase, nodeSettingsDelegate, variableSettingsDelegate,
                nodeSettingsWrapper}) {
                try {
                    if (delegate != null) {
                        return method.invoke(delegate, args);
                    }
                } catch (IllegalArgumentException e) { // NOSONAR
                    //
                } catch (InvocationTargetException e) { // NOSONAR
                    throw e.getCause();
                }
            }
            throw new IllegalStateException("Implementation problem - should never end up here");
        };
        return Proxy.newProxyInstance(NodeAndVariableSettingsProxy.class.getClassLoader(),
            new Class[]{NodeAndVariableSettingsWO.class, NodeAndVariableSettingsRO.class, CopyFromConfigBase.class, //
                /*
                 * For testing purposes only. In order to be able to extract the underlying {@link NodeSettings}-class to be able to
                 * read it from json using
                 * {@link JSONConfig#readJSON(org.knime.core.node.config.base.ConfigBaseWO, java.io.Reader)}.
                 */
                Supplier.class},
            invocationHandler);
    }

}
