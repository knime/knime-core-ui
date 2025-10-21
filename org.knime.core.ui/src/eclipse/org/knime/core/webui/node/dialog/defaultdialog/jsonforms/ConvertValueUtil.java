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
 *   Jan 25, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import java.io.IOException;
import java.lang.reflect.Type;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.PasswordHolder;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.JacksonSerializationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DependencyHandler;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A utility class for converting untyped objects to resolved objects of the correct type by using
 * {@link ObjectMapper#convertValue}.
 *
 * @author Paul Bärnreuther
 */
public final class ConvertValueUtil {
    static final NodeLogger LOGGER = NodeLogger.getLogger(ConvertValueUtil.class);

    private ConvertValueUtil() {
        // Utility
    }

    /**
     *
     * @param objectSettings
     * @param handler
     * @return an object of the generic type of the {@link DependencyHandler}
     */
    public static Object convertDependencies(final Object objectSettings, final DependencyHandler<?> handler) {
        final var settingsType = GenericTypeFinderUtil.getFirstGenericType(handler.getClass(), DependencyHandler.class);
        return convertValue(objectSettings, settingsType, null, null);
    }

    /**
     * @param <T>
     * @param objectSettings
     * @param settingsType
     * @param location to be used to resolve password ids
     * @param specialDeserializer
     * @return an object of the given settings type
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertValue(final Object objectSettings, final Type settingsType, final Location location,
        final JsonDeserializer<?> specialDeserializer) {
        if (specialDeserializer != null) {
            try {
                return (T)JacksonSerializationUtil.deserialize(objectSettings, specialDeserializer);
            } catch (IOException ex) {
                LOGGER.error("Error during manual deserialization of dependency", ex);
            }
        }

        if (location != null) {
            PasswordHolder.setCurrentLocation(location);
        }

        final var mapper = JsonFormsDataUtil.getMapper();
        try {
            return mapper.convertValue(objectSettings, mapper.constructType(settingsType));
        } finally {
            PasswordHolder.removeCurrentLocation();
        }

    }
}
