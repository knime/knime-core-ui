import { arrayLayoutRenderer } from "./arrayLayoutRenderer";
import { horizontalLayoutRenderer } from "./horizontalLayoutRenderer";
import { sectionLayoutRenderer } from "./sectionLayoutRenderer";

import { buttonRenderer } from "./buttonRenderer";
import { simpleButtonRenderer } from "./simpleButtonRenderer";
import { checkboxRenderer } from "./checkboxRenderer";
import { checkboxesRenderer } from "./checkboxesRenderer";
import { columnFilterRenderer } from "./columnFilterRenderer";
import { columnSelectRenderer } from "./columnSelectRenderer";
import { comboBoxRenderer } from "./comboBoxRenderer";
import { dateTimeRenderer } from "./dateTimeRenderer";
import { dropdownRenderer } from "./dropdownRenderer";
import { integerRenderer } from "./integerRenderer";
import { numberRenderer } from "./numberRenderer";
import { radioRenderer } from "./radioRenderer";
import { richTextInputRenderer } from "./richTextInputRenderer";
import { twinlistRenderer, simpleTwinlistRenderer } from "./twinlistRenderer";
import { sortListRenderer } from "./sortListRenderer";
import { valueSwitchRenderer } from "./valueSwitchRenderer";
import { textAreaRenderer } from "./textAreaRenderer";
import { credentialsRenderer } from "./credentialsRenderer";
import { localFileChooserRenderer } from "./localFileChooserRenderer";
import { fileChooserRenderer } from "./fileChooserRenderer";
import { legacyCredentialsRenderer } from "./legacyCredentialsRenderer";

export const defaultRenderers = [
  /* layout renderers */
  arrayLayoutRenderer,
  horizontalLayoutRenderer,
  sectionLayoutRenderer,

  /* component renderers */
  buttonRenderer,
  simpleButtonRenderer,
  checkboxRenderer,
  checkboxesRenderer,
  columnFilterRenderer,
  columnSelectRenderer,
  comboBoxRenderer,
  dateTimeRenderer,
  dropdownRenderer,
  integerRenderer,
  numberRenderer,
  radioRenderer,
  richTextInputRenderer,
  simpleTwinlistRenderer,
  twinlistRenderer,
  sortListRenderer,
  valueSwitchRenderer,
  textAreaRenderer,
  credentialsRenderer,
  legacyCredentialsRenderer,
  localFileChooserRenderer,
  fileChooserRenderer,
];
