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
 *   11 Nov 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.NodeLogger;

/**
 * A utility class to deal with the data cell creation parameters extension point.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public final class CreateDataCellExtensionsUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateDataCellExtensionsUtil.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.createDataCellParameters";

    private static final Map<DataType, Class<? extends CreateDataCellParameters>> PARAMETER_CLASSES_CORE =
        Map.ofEntries( //
            Map.entry(IntCellFactory.TYPE, CoreCreateDataCellParameters.IntCellParameters.class), //
            Map.entry(DoubleCellFactory.TYPE, CoreCreateDataCellParameters.DoubleCellParameters.class), //
            Map.entry(LongCellFactory.TYPE, CoreCreateDataCellParameters.LongCellParameters.class), //
            Map.entry(BooleanCellFactory.TYPE, CoreCreateDataCellParameters.BooleanCellParameters.class), //
            Map.entry(StringCellFactory.TYPE, CoreCreateDataCellParameters.FromStringCellParameters.class) //
        );

    private static Map<DataType, Class<? extends CreateDataCellParameters>> EXTENSION_PROVIDED_CLASSES;

    private CreateDataCellExtensionsUtil() {
        // utility class
    }

    /**
     * Reads all extensions implementing {@link CreateDataCellParametersFactory} from the extension point
     *
     * @return a map from data type to the corresponding to be preferred parameters class to create a cell of that type
     */
    public static Map<DataType, Class<? extends CreateDataCellParameters>> getCreateDataCellParametersExtensions() {
        return Stream
            .concat(PARAMETER_CLASSES_CORE.entrySet().stream(),
                getOrCreateExtensionProvidedClasses().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    private static Map<DataType, Class<? extends CreateDataCellParameters>> getOrCreateExtensionProvidedClasses() {
        if (EXTENSION_PROVIDED_CLASSES == null) {
            EXTENSION_PROVIDED_CLASSES = createExtensionProvidedClasses();
        }
        return EXTENSION_PROVIDED_CLASSES;
    }

    private static Map<DataType, Class<? extends CreateDataCellParameters>> createExtensionProvidedClasses() {
        final var registry = Platform.getExtensionRegistry();
        final var point = registry.getExtensionPoint(EXT_POINT_ID);
        return Stream.of(point.getExtensions()) //
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
            .map(CreateDataCellExtensionsUtil::readCreateDataCellParametersFactory) //
            .filter(Objects::nonNull) //
            .collect(Collectors.toMap(//
                CreateDataCellParametersFactory::getDataType, //
                CreateDataCellParametersFactory::getNodeParametersClass //
            ));
    }

    /**
     * Use this class to handle data types whose cell factory allows construction from string, i.e. implements
     * {@link FromString}.
     *
     * @return the parameters to use for string-compatible data types.
     */
    public static Class<? extends CreateDataCellParameters> getFromStringCreateDataCellParametersClass() {
        return CoreCreateDataCellParameters.FromStringCellParameters.class;
    }

    private static CreateDataCellParametersFactory
        readCreateDataCellParametersFactory(final IConfigurationElement cfe) {
        try {
            final var createDataCellParametersFactory =
                (CreateDataCellParametersFactory)cfe.createExecutableExtension("factoryClass");
            LOGGER.debugWithFormat("Added data cell creation parameters '%s' from '%s'",
                createDataCellParametersFactory.getClass().getName(), cfe.getContributor().getName());
            return createDataCellParametersFactory;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                CreateDataCellParameters.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

}
