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
 *   30 Jul 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jobmanager;

import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

/**
 * The job manager settings (a single dropdown) shown for native nodes. The dropdown will only be shown if the factory
 * id of a job manager (which is not the default) exists in the node settings.
 *
 * @author Robin Gerling
 */
public final class JobManagerNodeParameters implements NodeParameters {

    static final class JobManagerFactoryIdPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.containsKey(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get())
                ? settings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get()) : "";
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            settings.addString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(), param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][];
        }

    }

    static class HasNoJobManagerSpecified implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getString(FactoryIdRef.class).isEqualTo("");
        }

    }

    static class FactoryIdRef implements ParameterReference<String> {

    }

    static class JobManagerProvider implements StringChoicesProvider {
        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            return List
                .of(new StringChoice(DEFAULT_JOB_MANAGER_FACTORY.getID(), DEFAULT_JOB_MANAGER_FACTORY.getLabel()));
        }
    }

    @Section(title = "Job manager")
    public interface JobManagerSection {
    }

    @Widget(title = "Job manager selection", description = "")
    @Effect(type = EffectType.HIDE, predicate = HasNoJobManagerSpecified.class)
    @ValueReference(FactoryIdRef.class)
    @Persistor(JobManagerFactoryIdPersistor.class)
    @ChoicesProvider(JobManagerProvider.class)
//    @Layout(JobManagerSection.class)
    String m_jobManagerFactoryId = "";
}
