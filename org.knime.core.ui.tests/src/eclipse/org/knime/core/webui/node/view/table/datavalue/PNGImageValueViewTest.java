/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Oct 29, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.view.table.datavalue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.webui.node.view.table.datavalue.views.image.ImageInitialData;
import org.knime.core.webui.node.view.table.datavalue.views.image.PNGImageValueView;
import org.mockito.Mockito;

/**
 * @author Paul Bärnreuther
 */
public class PNGImageValueViewTest {

    /**
     * Test that the initial data of the {@link PNGImageValueView} is the base64 encoded image.
     */
    @Test
    public void testPNGImageValueView() {
        final var pngImageValue = Mockito.mock(PNGImageValue.class);
        final var pngContent = Mockito.mock(PNGImageContent.class);
        Mockito.when(pngImageValue.getImageContent()).thenReturn(pngContent);
        final var imageBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Mockito.when(pngContent.getByteArray()).thenReturn(imageBytes);

        ImageInitialData initialData = new PNGImageValueView(pngImageValue).getInitialData();
        assertThat(initialData.data()).isEqualTo(imageBytes);
        assertThat(initialData.mimeType()).isEqualTo("image/png");
    }
}
