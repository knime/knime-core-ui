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
 *   Feb 13, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.DefaultFieldPersistor;

/**
 * A record representing a temporal format. The format is a string that can be used to format a temporal object using
 * the {@link DateTimeFormatter}. The type of temporal object that the format represents is also stored.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public class TemporalFormat implements PersistableSettings {

    String m_format;

    FormatTemporalType m_temporalType;

    /**
     * Constructor.
     *
     * @param format the format string
     * @param temporalType the type of temporal object that the format represents
     */
    public TemporalFormat(final String format, final FormatTemporalType temporalType) {
        this.m_format = format;
        this.m_temporalType = temporalType;
    }

    /**
     * Returns the format string.
     *
     * @return the format string
     */
    public String format() {
        return m_format;
    }

    /**
     * Returns the type of temporal object that the format represents.
     *
     * @return the type of temporal object
     */
    public FormatTemporalType temporalType() {
        return m_temporalType;
    }

    /**
     * Default constructor for serialization.
     */
    TemporalFormat() {
    }

    /**
     * The type of temporal that the format represents.
     */
    @SuppressWarnings("javadoc")
    public enum FormatTemporalType {

            DATE(LocalDate::from), //
            TIME(LocalTime::from), //
            DATE_TIME(LocalDateTime::from), //
            ZONED_DATE_TIME(ZonedDateTime::from);

        private final TemporalQuery<TemporalAccessor> m_associatedQuery;

        FormatTemporalType(final TemporalQuery<TemporalAccessor> associatedQuery) {
            this.m_associatedQuery = associatedQuery;
        }

        /**
         * Returns the associated query for this temporal type.
         *
         * @return the associated query
         */
        public TemporalQuery<TemporalAccessor> associatedQuery() {
            return m_associatedQuery;
        }
    }

    /**
     * Default persistor for {@link TemporalFormat}.
     */
    public static class TemporalFormatPersistor implements DefaultFieldPersistor<TemporalFormat> {

        private final String m_configKey;

        private static final String CFG_KEY_FORMAT = "format";

        private static final String CFG_KEY_TEMPORAL_TYPE = "temporalType";

        public TemporalFormatPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public void save(final TemporalFormat value, final NodeSettingsWO nodeSettings) {
            var config = nodeSettings.addConfig(m_configKey);
            config.addString(CFG_KEY_FORMAT, value.format());
            config.addString(CFG_KEY_TEMPORAL_TYPE, value.temporalType().name());
        }

        @Override
        public TemporalFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var config = settings.getConfig(m_configKey);
            var format = config.getString(CFG_KEY_FORMAT);
            var temporalType = FormatTemporalType.valueOf(config.getString(CFG_KEY_TEMPORAL_TYPE));
            return new TemporalFormat(format, temporalType);
        }

    }
}
