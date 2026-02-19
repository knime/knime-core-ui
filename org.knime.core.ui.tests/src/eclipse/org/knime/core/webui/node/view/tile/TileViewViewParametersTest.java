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
 *   Feb 20, 2026 (gerling): created
 */
package org.knime.core.webui.node.view.tile;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.data.property.ColorModelRange2.SpecialColorType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

final class TileViewViewParametersTest extends DefaultNodeSettingsSnapshotTest {

    TileViewViewParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) //
            .testJsonFormsForView(TileViewViewParameters.class) //
            .testJsonFormsWithInstance(SettingsType.VIEW, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static TileViewViewParameters readSettings() {
        try {
            var path = getSnapshotPath(TileViewViewParameters.class).getParent().resolve("node_settings")
                .resolve("TileViewViewParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.VIEW.getConfigKey()),
                    TileViewViewParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        return new PortObjectSpec[]{createDefaultTestTableSpec()};
    }

    private static DataTableSpec createDefaultTestTableSpec() {
        final var colSpecCreator = new DataColumnSpecCreator("String1", StringCell.TYPE);
        colSpecCreator.setColorHandler(new ColorHandler(createTestColorModelNominal()));
        final var colSpecCreator2 = new DataColumnSpecCreator("String2", StringCell.TYPE);
        final var colSpecCreator3 = new DataColumnSpecCreator("Double1", DoubleCell.TYPE);
        final var colSpecCreator4 = new DataColumnSpecCreator("Double2", DoubleCell.TYPE);
        colSpecCreator4.setColorHandler(new ColorHandler(createTestColorModelRange()));
        final var colSpecCreator5 = new DataColumnSpecCreator("BinaryObject1", BinaryObjectDataCell.TYPE);
        final var colSpecCreator6 =
            new DataColumnSpecCreator("NominalDistribution1", DataType.getType(NominalDistributionCell.class));
        final var colSpecCreator7 = new DataColumnSpecCreator("List1", ListCell.getCollectionType(StringCell.TYPE));

        return new DataTableSpec(colSpecCreator.createSpec(), colSpecCreator2.createSpec(),
            colSpecCreator3.createSpec(), colSpecCreator4.createSpec(), colSpecCreator5.createSpec(),
            colSpecCreator6.createSpec(), colSpecCreator7.createSpec());
    }

    private static ColorModelRange2 createTestColorModelRange() {
        final Map<SpecialColorType, Color> specialColorsMap = Map.of( //
            SpecialColorType.MISSING, Color.BLUE, //
            SpecialColorType.NAN, Color.CYAN, //
            SpecialColorType.NEGATIVE_INFINITY, Color.MAGENTA, //
            SpecialColorType.POSITIVE_INFINITY, Color.ORANGE, //
            SpecialColorType.BELOW_MIN, Color.PINK, //
            SpecialColorType.ABOVE_MAX, Color.LIGHT_GRAY);
        return new ColorModelRange2(specialColorsMap, new double[]{0, 10}, new Color[]{Color.WHITE, Color.BLACK}, true);
    }

    private static ColorModelNominal createTestColorModelNominal() {
        final var map = Map.<DataCell, ColorAttr> of( //
            new StringCell("value 1"), ColorAttr.getInstance(Color.GREEN), //
            new StringCell("value 2"), ColorAttr.getInstance(Color.RED), //
            new StringCell("value 3"), ColorAttr.getInstance(Color.BLUE), //
            new StringCell("value 4"), ColorAttr.getInstance(Color.PINK));
        final var palette = new ColorAttr[]{ //
            ColorAttr.getInstance(Color.GREEN), //
            ColorAttr.getInstance(Color.RED), //
            ColorAttr.getInstance(Color.BLUE)};
        final var customColorValues = Set.<DataCell> of(new StringCell("value 3"), new StringCell("value 4"));

        return new ColorModelNominal(map, palette, customColorValues);
    }

}
