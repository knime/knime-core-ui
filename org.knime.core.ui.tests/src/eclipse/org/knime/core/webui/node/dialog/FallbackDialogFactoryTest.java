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
 *   Aug 6, 2025 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogUIExtension;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Test {@link FallbackDialogFactory}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class FallbackDialogFactoryTest {

    private WorkflowManager m_wfm;

    @SuppressWarnings("javadoc")
    @BeforeEach
    public void setUp() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        FallbackDialogFactory.fallbackDialogEnabled = null;
        System.setProperty(FallbackDialogFactory.FALLBACK_DIALOG_ENABLED_SYS_PROP, "true");
    }

    @SuppressWarnings("javadoc")
    @AfterEach
    public void tearDown() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
        FallbackDialogFactory.fallbackDialogEnabled = null;
        System.clearProperty(FallbackDialogFactory.FALLBACK_DIALOG_ENABLED_SYS_PROP);
    }

    @Test
    void testGetInitialDataAndApplyDataForFallbackDialog() throws InvalidSettingsException {
        // node with a swing-dialog
        var nc = createAndAddNode(m_wfm, new TestNodeFactory());

        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node expected to have a node dialog").isTrue();
        var nodeDialogManager = NodeDialogManager.getInstance();
        assertThat(nodeDialogManager.getPageResourceManager().getPage(NodeWrapper.of(nc)))
            .isSameAs(DefaultNodeDialogUIExtension.PAGE);

        var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();

        var expectedInitialData = """
                  {
                    "result": {
                        "data": {
                            "model": {
                                "string": "foo",
                                "double": 3.14,
                                "int": 42,
                                "float": 2.71,
                                "char": "c",
                                "short": 1,
                                "long": 123456789,
                                "byte": 2,
                                "password": "02BAAAAGMq_QxveL1vZ0EBjyNFzJupmiji",
                                "sub-settings": {
                                    "nested-string": "bar"
                                }
                            }
                        },
                        "schema": {
                            "properties": {
                                "model": {
                                    "properties": {
                                        "string": {
                                            "type": "string",
                                            "title": "string"
                                        },
                                        "double": {
                                            "type": "number",
                                            "title": "double"
                                        },
                                        "int": {
                                            "type": "integer",
                                            "title": "int"
                                        },
                                        "float": {
                                            "type": "number",
                                            "title": "float"
                                        },
                                        "char": {
                                            "type": "string",
                                            "title": "char"
                                        },
                                        "short": {
                                            "type": "integer",
                                            "title": "short"
                                        },
                                        "long": {
                                            "type": "integer",
                                            "title": "long"
                                        },
                                        "byte": {
                                            "type": "integer",
                                            "title": "byte"
                                        },
                                        "password": {
                                            "type": "object",
                                            "title": "password"
                                        },
                                        "sub-settings": {
                                            "properties": {
                                                "nested-string": {
                                                    "type": "string",
                                                    "title": "sub-settings/nested-string"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "ui_schema": {
                            "elements": [
                                {
                                    "type": "Control",
                                    "options": {
                                        "format": "textMessage",
                                        "message": {
                                            "title":"Auto-generated dialog",
                                            "description":"You’re seeing an auto-generated dialog because the original one is not supported here yet.\\nYou can still configure the node as usual. See the node description for more details.\\n",
                                            "type":"INFO"
                                        }
                                    }
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/string"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/double"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/int"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/float"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/char"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/short"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/long"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/byte"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/password",
                                    "options": {
                                        "format": "credentials"
                                    }
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/sub-settings/properties/nested-string"
                                }
                            ]
                        },
                        "flowVariableSettings": {}
                    }
                }
                                """;

        var initialData = dataServiceManager.callInitialDataService(NodeWrapper.of(nc));
        assertThatJson(initialData).isEqualTo(expectedInitialData);

        var applyData = """
                 {
                    "data": {
                        "model": {
                            "string": "bar",
                            "double": 3.14,
                            "int": 43,
                            "float": 2.71,
                            "char": "c",
                            "short": 1,
                            "long": 123456789,
                            "byte": 2,
                            "password": "02BAAAAGMq_QxveL1vZ0EBjyNFzJupmiji",
                            "sub-settings": {
                                "nested-string": "baz"
                            }
                        }
                     }
                  }
                """;
        dataServiceManager.callApplyDataService(NodeWrapper.of(nc), applyData);

        var settings = nc.getNodeSettings().getNodeSettings("model");
        assertThat(settings.getString("string")).isEqualTo("bar");
        assertThat(settings.getInt("int")).isEqualTo(43);
        assertThat(settings.getNodeSettings("sub-settings").getString("nested-string")).isEqualTo("baz");

    }

    class TestNodeFactory extends NodeFactory<NodeModel> {

        @Override
        public NodeModel createNodeModel() {
            return new NodeModel(0, 0) {

                @Override
                protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                    return inSpecs;
                }

                @Override
                protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
                    throws Exception {
                    return inData;
                }

                @Override
                protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                    //
                }

                @Override
                protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                    //
                }

                @Override
                protected void saveSettingsTo(final NodeSettingsWO settings) {
                    settings.addString("string", "foo");
                    settings.addDouble("double", 3.14);
                    settings.addInt("int", 42);
                    settings.addFloat("float", 2.71f);
                    settings.addChar("char", 'c');
                    settings.addShort("short", (short)1);
                    settings.addLong("long", 123456789L);
                    settings.addByte("byte", (byte)2);
                    settings.addPassword("password", "encryption-key", "blub");
                    settings.addNodeSettings("sub-settings").addString("nested-string", "bar");
                }

                @Override
                protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                    //
                }

                @Override
                protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
                    throws InvalidSettingsException {
                    //
                }

                @Override
                protected void reset() {
                    //
                }

            };
        }

        @Override
        protected int getNrNodeViews() {
            return 0;
        }

        @Override
        public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
            return null;
        }

        @Override
        protected boolean hasDialog() {
            return true;
        }

        @Override
        protected NodeDialogPane createNodeDialogPane() {
            return null;
        }

    }

    @Test
    void testWithConfigKeysContainingDots() throws InvalidSettingsException {
        // node with a swing-dialog that has config keys containing dots
        var nc = createAndAddNode(m_wfm, new TestNodeFactoryWithDots());

        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node expected to have a node dialog").isTrue();
        var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();

        var expectedInitialDataWithDots = """
                  {
                    "result": {
                        "data": {
                            "model": {
                                "config<dot>key<dot>with<dot>dots": "value_with_dots",
                                "sub<dot>settings": {
                                    "nested<dot>key": "nested_value"
                                }
                            }
                        },
                        "schema": {
                            "properties": {
                                "model": {
                                    "properties": {
                                        "config<dot>key<dot>with<dot>dots": {
                                            "type": "string",
                                            "title": "config.key.with.dots"
                                        },
                                        "sub<dot>settings": {
                                            "properties": {
                                                "nested<dot>key": {
                                                    "type": "string",
                                                    "title": "sub.settings/nested.key"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "ui_schema": {
                            "elements": [
                                {
                                    "type": "Control",
                                    "options": {
                                        "format": "textMessage",
                                        "message": {
                                            "title":"Auto-generated dialog",
                                            "description":"You’re seeing an auto-generated dialog because the original one is not supported here yet.\\nYou can still configure the node as usual. See the node description for more details.\\n",
                                            "type":"INFO"
                                        }
                                    }
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/config<dot>key<dot>with<dot>dots"
                                },
                                {
                                    "type": "Control",
                                    "scope": "#/properties/model/properties/sub<dot>settings/properties/nested<dot>key"
                                }
                            ]
                        },
                        "flowVariableSettings": {}
                    }
                }
                """;

        var initialData = dataServiceManager.callInitialDataService(NodeWrapper.of(nc));
        assertThatJson(initialData).isEqualTo(expectedInitialDataWithDots);

        var applyDataWithDots = """
                 {
                    "data": {
                        "model": {
                            "config<dot>key<dot>with<dot>dots": "updated_value",
                            "sub<dot>settings": {
                                "nested<dot>key": "updated_nested"
                            }
                        }
                     }
                  }
                """;
        dataServiceManager.callApplyDataService(NodeWrapper.of(nc), applyDataWithDots);

        var settings = nc.getNodeSettings().getNodeSettings("model");
        assertThat(settings.getString("config.key.with.dots")).isEqualTo("updated_value");
        assertThat(settings.getNodeSettings("sub.settings").getString("nested.key")).isEqualTo("updated_nested");
    }

    class TestNodeFactoryWithDots extends NodeFactory<NodeModel> {

        @Override
        public NodeModel createNodeModel() {
            return new NodeModel(0, 0) {

                @Override
                protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                    return inSpecs;
                }

                @Override
                protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
                    throws Exception {
                    return inData;
                }

                @Override
                protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                    //
                }

                @Override
                protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                    //
                }

                @Override
                protected void saveSettingsTo(final NodeSettingsWO settings) {
                    settings.addString("config.key.with.dots", "value_with_dots");
                    settings.addNodeSettings("sub.settings").addString("nested.key", "nested_value");
                }

                @Override
                protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                    //
                }

                @Override
                protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
                    throws InvalidSettingsException {
                    //
                }

                @Override
                protected void reset() {
                    //
                }

            };
        }

        @Override
        protected int getNrNodeViews() {
            return 0;
        }

        @Override
        public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
            return null;
        }

        @Override
        protected boolean hasDialog() {
            return true;
        }

        @Override
        protected NodeDialogPane createNodeDialogPane() {
            return null;
        }

    }

}