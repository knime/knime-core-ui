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
 *   May 3, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesUpdateHandler;

import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("javadoc")
public final class JsonFormsConsts {

    private JsonFormsConsts() {
        // utility
    }

    /**
     * The version of the designated jsonforms schema object
     */
    public static final SchemaVersion VERSION = SchemaVersion.DRAFT_2019_09;

    public static final String FIELD_NAME_DATA = "data";

    public static final String FIELD_NAME_SCHEMA = "schema";

    public static final String FIELD_NAME_UI_SCHEMA = "ui_schema";

    /**
     * The tags, types and formats used in a jsonforms schema
     *
     * @author Paul Bärnreuther
     */
    public static final class Schema {

        private Schema() {
            // Utility
        }

        public static final String TAG_TYPE = SchemaKeyword.TAG_TYPE.forVersion(VERSION);

        public static final String TAG_FORMAT = SchemaKeyword.TAG_FORMAT.forVersion(VERSION);

        public static final String TYPE_NULL = SchemaKeyword.TAG_TYPE_NULL.forVersion(VERSION);

        public static final String TYPE_ARRAY = SchemaKeyword.TAG_TYPE_ARRAY.forVersion(VERSION);

        public static final String TYPE_OBJECT = SchemaKeyword.TAG_TYPE_OBJECT.forVersion(VERSION);

        public static final String TYPE_BOOLEAN = SchemaKeyword.TAG_TYPE_BOOLEAN.forVersion(VERSION);

        public static final String TYPE_STRING = SchemaKeyword.TAG_TYPE_STRING.forVersion(VERSION);

        public static final String TYPE_INTEGER = SchemaKeyword.TAG_TYPE_INTEGER.forVersion(VERSION);

        public static final String TYPE_NUMBER = SchemaKeyword.TAG_TYPE_NUMBER.forVersion(VERSION);

        public static final String FORMAT_INT = "int32";

        public static final String FORMAT_LONG = "int64";

        public static final String FORMAT_FLOAT = "float";

        public static final String FORMAT_DOUBLE = "double";

        public static final String TAG_PROPERTIES = SchemaKeyword.TAG_PROPERTIES.forVersion(VERSION);

        public static final String TAG_ITEMS = SchemaKeyword.TAG_ITEMS.forVersion(VERSION);

        public static final String TAG_ALLOF = SchemaKeyword.TAG_ALLOF.forVersion(VERSION);

        public static final String TAG_ANYOF = SchemaKeyword.TAG_ANYOF.forVersion(VERSION);

        public static final String TAG_ONEOF = SchemaKeyword.TAG_ONEOF.forVersion(VERSION);

        public static final String TAG_TITLE = SchemaKeyword.TAG_TITLE.forVersion(VERSION);

        public static final String TAG_CONST = SchemaKeyword.TAG_CONST.forVersion(VERSION);

        public static final String TAG_PATTERN = SchemaKeyword.TAG_PATTERN.forVersion(VERSION);

        public static final String TAG_ITEMS_MIN = SchemaKeyword.TAG_ITEMS_MIN.forVersion(VERSION);
    }

    /**
     * The tags, options and types used in the construction of a jsonforms ui-schema
     *
     * @author Paul Bärnreuther
     */
    public static final class UiSchema {

        private UiSchema() {
            // Utility
        }

        /**
         * A schema path to a setting
         */
        public static final String TAG_SCOPE = "scope";

        /**
         * The type of a ui element, rule, etc.
         */
        public static final String TAG_TYPE = Schema.TAG_TYPE;

        /**
         * Additional options for an ui element in the ui-schema
         */
        public static final String TAG_OPTIONS = "options";

        /**
         * Format for an ui element in the ui-schema
         */
        public static final String TAG_FORMAT = "format";

        /**
         * The {@link ButtonActionHandler} of a {@link ButtonWidget}
         */
        public static final String TAG_ACTION_HANDLER = "actionHandler";

        /**
         * The {@link ChoicesUpdateHandler} of a {@link ChoicesWidget}
         */
        public static final String TAG_CHOICES_UPDATE_HANDLER = "choicesUpdateHandler";

        /**
         * The dependencies of a {@link ButtonWidget} or the possible values of a {@link ChoicesWidget}
         */
        public static final String TAG_DEPENDENCIES = "dependencies";

        /**
         * The boolean flag indicator of a file chooser widget whether it has writer capabilities.
         */
        public static final String TAG_IS_WRITER = "isWriter";

        /**
         * Used for providing multiple valid file extension to a file reader widget.
         */
        public static final String TAG_FILE_EXTENSIONS = "fileExtensions";

        /**
         * Used for providing a single valid file extension to a file chooser/writer widget.
         */
        public static final String TAG_FILE_EXTENSION = "fileExtension";

        /**
         * Used for providing a reference to a file extension provider to a file chooser/writer widget.
         */
        public static final String TAG_FILE_EXTENSION_PROVIDER = "fileExtensionProvider";

        /**
         * Used for rich text to adjust validations to incorporate the $$["flowVarName"] - Syntax
         */
        public static final String TAG_USE_FLOW_VAR_TEMPLATES = "useFlowVarTemplates";

        /**
         * Several formats which are written to the options of the ui-schema of an ui element
         *
         * @author Paul Bärnreuther
         */
        public static final class Format {

            private Format() {
                // Utility
            }

            public static final String CHECKBOX = "checkbox";

            public static final String VALUE_SWITCH = "valueSwitch";

            public static final String COLUMN_FILTER = "columnFilter";

            public static final String NAME_FILTER = "nameFilter";

            public static final String COLUMN_SELECTION = "columnSelection";

            public static final String DROP_DOWN = "dropDown";

            public static final String TWIN_LIST = "twinList";

            public static final String COMBO_BOX = "comboBox";

            public static final String SORT_LIST = "sortList";

            public static final String RADIO = "radio";

            public static final String BUTTON = "button";

            public static final String SIMPLE_BUTTON = "simpleButton";

            public static final String DATE_TIME = "date-time";

            public static final String RICH_TEXT_INPUT = "richTextInput";

            public static final String LOCAL_FILE_CHOOSER = "localFileChooser";

            public static final String FILE_CHOOSER = "fileChooser";

            public static final String CREDENTIALS = "credentials";

            public static final String LEGACY_CREDENTIALS = "legacyCredentials";

            public static final String DYNAMIC_VALUE = "dynamicValue";

            public static final String TEXT_MESSAGE = "textMessage";
        }

        /**
         * The option tag for the detail sub ui schema of an array layout
         */
        public static final String TAG_ARRAY_LAYOUT_DETAIL = "detail";

        /**
         * The title of the sub elements in an array layout
         */
        public static final String TAG_ARRAY_LAYOUT_ELEMENT_TITLE = "arrayElementTitle";

        /**
         * The text of the add button of an array layout
         */
        public static final String TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT = "addButtonText";

        /**
         * Whether to add sort buttons to change order of array layout elements
         */
        public static final String TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS = "showSortButtons";

        /**
         * Whether to add edit and reset buttons to array layout elements
         */
        public static final String TAG_ARRAY_LAYOUT_WITH_EDIT_AND_RESET = "withEditAndReset";

        /**
         * The scope controlling a checkbox appearing next to the header of an array layout element
         */
        public static final String TAG_ARRAY_LAYOUT_ELEMENT_CHECKBOX_SCOPE = "elementCheckboxScope";

        /**
         * The id by which dynamic titles for array layout elements can be accessed
         */
        public static final String TAG_ARRAY_LAYOUT_ELEMENT_TITLE_PROVIDER = "elementTitleProvider";

        /**
         * The id by which dynamic sub titles for array layout elements can be accessed
         */
        public static final String TAG_ARRAY_LAYOUT_ELEMENT_SUB_TITLE_PROVIDER = "elementSubTitleProvider";

        /**
         * Whether to hide add and delete buttons such that the size of the array cannot be changed
         */
        public static final String TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE = "hasFixedSize";

        /**
         * The id by which default values for array layout elements can be accessed
         */
        public static final String TAG_ARRAY_LAYOUT_ELEMENT_DEFAULT_VALUE_PROVIDER = "elementDefaultValueProvider";

        /**
         * Rules to show/hide/enable/disable an ui element in the ui-schema
         */
        public static final String TAG_RULE = "rule";

        /**
         * show/hide/enable/disable
         */
        public static final String TAG_EFFECT = "effect";

        /**
         * A condition which has to be fulfilled to trigger a rule
         */
        public static final String TAG_CONDITION = "condition";

        /**
         * Multiple conditions combined by a logical operation
         */
        public static final String TAG_CONDITIONS = "conditions";

        /**
         * For rule conditions on array values
         */
        public static final String TAG_CONTAINS = "contains";

        /**
         * Negation of a condition
         */
        public static final String TAG_NOT = "not";

        /**
         * The title of a section
         */
        public static final String TAG_LABEL = "label";

        /**
         * The description of a section
         */
        public static final String TAG_DESCRIPTION = "description";

        /**
         * The children of a layout part
         */
        public static final String TAG_ELEMENTS = "elements";

        /**
         * Whether the setting/layoutPart is advanced
         */
        public static final String OPTIONS_IS_ADVANCED = "isAdvanced";

        /**
         * Whether the flow variable button for this setting should be hidden
         */
        public static final String OPTIONS_HIDE_FLOW_VARIABLE_BUTTON = "hideFlowVariableButton";

        /**
         * The type of a control
         */
        public static final String TYPE_CONTROL = "Control";

        /**
         * The type of a section
         */
        public static final String TYPE_SECTION = "Section";

        /**
         * The type of a horizontal layout part
         */
        public static final String TYPE_HORIZONTAL_LAYOUT = "HorizontalLayout";
    }
}
