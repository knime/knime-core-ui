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
 *   Jun 28, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.choices;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.SettingsCreationContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DialogDataServiceHandler;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DialogDataServiceHandlerResult;

/**
 *
 * @author Paul Bärnreuther
 * @param <S> the supplier of the column name whose domain should be used. Other settings can be referenced by using the
 *            same name/paths for fields in this class as described in {@link DialogDataServiceHandler}.
 */
public class DomainChoicesUpdateHandler<S extends ColumnNameSupplier> implements ChoicesUpdateHandler<S> {

    @Override
    public Future<DialogDataServiceHandlerResult<ChoicesWidgetChoice[]>> invoke(final String state, final S settings,
        final SettingsCreationContext context) {
        final var spec = context.getDataTableSpec(0);
        if (spec.isEmpty()) {
            return getEmptyResult();
        }
        final var columnName = settings.columnName();
        final var colSpec = spec.get().getColumnSpec(columnName);
        if (colSpec == null) {
            return getEmptyResult();
        }
        final var domainValues = getDomainValues(colSpec);
        if (domainValues.isEmpty()) {
            return wrapToError(String.format(
                "No column domain values present for column \"%s\". Consider using a Domain Calculator node.",
                columnName));
        }
        final var choices =
            domainValues.get().stream().map(ChoicesWidgetChoice::fromId).toArray(ChoicesWidgetChoice[]::new);
        return wrapToResult(choices);
    }

    private static Future<DialogDataServiceHandlerResult<ChoicesWidgetChoice[]>> getEmptyResult() {
        return wrapToResult(new ChoicesWidgetChoice[0]);
    }

    private static Future<DialogDataServiceHandlerResult<ChoicesWidgetChoice[]>>
        wrapToResult(final ChoicesWidgetChoice[] choices) {
        return CompletableFuture.supplyAsync(() -> DialogDataServiceHandlerResult.succeed(choices));
    }

    private static Future<DialogDataServiceHandlerResult<ChoicesWidgetChoice[]>> wrapToError(final String message) {
        return CompletableFuture.supplyAsync(() -> DialogDataServiceHandlerResult.fail(message));
    }

    private static Optional<List<String>> getDomainValues(final DataColumnSpec colSpec) {
        var colDomain = colSpec.getDomain().getValues();
        if (colDomain == null) {
            return Optional.empty();
        }
        return Optional.of(colDomain.stream().map(cell -> ((StringCell)cell).getStringValue()).toList());
    }

}