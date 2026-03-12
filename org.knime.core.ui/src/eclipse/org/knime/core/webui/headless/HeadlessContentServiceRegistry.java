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
 *
 * ------------------------------------------------------------------------
 */
package org.knime.core.webui.headless;

import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Registry that loads {@link HeadlessContentService} implementations from the Eclipse extension point.
 *
 * <p>When multiple implementations are registered, the one with the highest priority is used.
 * The priority attribute is optional (defaults to 0). A remote implementation (e.g. in the executor)
 * can register with a higher priority to take precedence over the local CEF implementation.
 *
 * @since 5.12
 */
public final class HeadlessContentServiceRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(HeadlessContentServiceRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.HeadlessContentService";

    private static final String ATTR_CLASS = "serviceClass";

    private static final String ATTR_PRIORITY = "priority";

    private static HeadlessContentServiceRegistry instance;

    private HeadlessContentService m_service;

    private boolean m_initialized;

    private HeadlessContentServiceRegistry() {
        // singleton
    }

    /**
     * @return the singleton instance
     */
    public static synchronized HeadlessContentServiceRegistry getInstance() {
        if (instance == null) {
            instance = new HeadlessContentServiceRegistry();
        }
        return instance;
    }

    /**
     * Returns the highest-priority {@link HeadlessContentService} implementation, if any is registered.
     *
     * @return the service, or empty if no implementation is available
     */
    public synchronized Optional<HeadlessContentService> getService() {
        if (!m_initialized) {
            m_initialized = true;
            m_service = loadService();
        }
        return Optional.ofNullable(m_service);
    }

    /**
     * Convenience method that returns the service or throws if none is available.
     *
     * @return the service
     * @throws IllegalStateException if no HeadlessContentService implementation is registered
     */
    public HeadlessContentService getServiceOrThrow() {
        return getService().orElseThrow(
            () -> new IllegalStateException("No HeadlessContentService implementation is registered. "
                + "Make sure a bundle providing this extension point is installed (e.g. org.knime.js.cef)."));
    }

    private static HeadlessContentService loadService() {
        final var registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error("Extension point not found: " + EXT_POINT_ID);
            return null;
        }

        HeadlessContentService bestService = null;
        int bestPriority = Integer.MIN_VALUE;

        for (final IConfigurationElement element : point.getConfigurationElements()) {
            try {
                final int priority = parsePriority(element);
                if (priority > bestPriority) {
                    final var service = (HeadlessContentService)element.createExecutableExtension(ATTR_CLASS);
                    bestService = service;
                    bestPriority = priority;
                    LOGGER.debugWithFormat("Loaded HeadlessContentService: %s (priority=%d)",
                        service.getClass().getName(), priority);
                }
            } catch (final Exception e) {
                LOGGER.error("Failed to load HeadlessContentService from extension: " + element.getContributor(), e);
            }
        }

        if (bestService == null) {
            LOGGER.debug("No HeadlessContentService implementation found");
        }
        return bestService;
    }

    private static int parsePriority(final IConfigurationElement element) {
        final String val = element.getAttribute(ATTR_PRIORITY);
        if (val == null || val.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (final NumberFormatException e) {
            LOGGER.warnWithFormat("Invalid priority value '%s' for HeadlessContentService from %s, defaulting to 0",
                val, element.getContributor());
            return 0;
        }
    }
}
