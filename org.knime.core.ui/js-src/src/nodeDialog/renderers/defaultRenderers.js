import { arrayLayoutRenderer } from "./arrayLayoutRenderer";
import { horizontalLayoutRenderer } from "./horizontalLayoutRenderer";
import { verticalLayoutRenderer } from "./verticalLayoutRenderer";
import { sectionLayoutRenderer } from "./sectionLayoutRenderer";

import { buttonRenderer } from "./buttonRenderer";
import { simpleButtonRenderer } from "./simpleButtonRenderer";
import { checkboxRenderer } from "./checkboxRenderer";
import { checkboxesRenderer } from "./checkboxesRenderer";
import { columnFilterRenderer } from "./columnFilterRenderer";
import { nameFilterRenderer } from "./nameFilterRenderer";
import { columnSelectRenderer } from "./columnSelectRenderer";
import { comboBoxRenderer } from "./comboBoxRenderer";
import { dateTimeRenderer } from "./dateTimeRenderer";
import { dropdownRenderer } from "./dropdownRenderer";
import { integerRenderer } from "./integerRenderer";
import { numberRenderer } from "./numberRenderer";
import { radioRenderer } from "./radioRenderer";
import { richTextRenderer } from "./richTextRenderer";
import { twinlistRenderer, simpleTwinlistRenderer } from "./twinlistRenderer";
import { sortListRenderer } from "./sortListRenderer";
import { valueSwitchRenderer } from "./valueSwitchRenderer";
import { textAreaRenderer } from "./textAreaRenderer";
import { credentialsRenderer } from "./credentialsRenderer";
import { localFileChooserRenderer } from "./localFileChooserRenderer";
import { fileChooserRenderer } from "./fileChooserRenderer";
import { legacyCredentialsRenderer } from "./legacyCredentialsRenderer";
import { vennDiagramLayoutRenderer } from "./vennDiagramRenderer";
import { dynamicValueRenderer } from "./dynamicValueRenderer";
import { editResetButtonRenderer } from "./editResetButtonRenderer";
import { elementCheckboxRenderer } from "./elementCheckboxRenderer";
import { defineComponent, h, Suspense } from "vue";
import LoadingDialog from "../loading/LoadingDialog.vue";
import DialogComponentWrapper from "../uiComponents/DialogComponentWrapper.vue";

const wrapInSuspense = (component, fallback = null) =>
  defineComponent({
    render() {
      return h(
        Suspense,
        {
          suspensible: true,
          timeout: 1000,
          onPending: () => console.log("pending"),
          onFallback: () => console.log("fallback"),
          onResolve: () => console.log("resolve"),
        },
        {
          default: () => h(component, { ...this.$attrs }),
          fallback: () => h(LoadingDialog),
        },
      );
    },
  });

const toDialogComponent = (component, { fill } = { fill: false }) => {
  const suspendedComponent = wrapInSuspense(component);
  return defineComponent({
    render() {
      return h(DialogComponentWrapper, { fill, control: this.$attrs }, [
        h(suspendedComponent, { ...this.$attrs }),
      ]);
    },
  });
};

export const defaultRenderers = [
  /* layout renderers */
  arrayLayoutRenderer,
  horizontalLayoutRenderer,
  verticalLayoutRenderer,
  sectionLayoutRenderer,
  vennDiagramLayoutRenderer,

  /* component renderers */
  buttonRenderer,
  simpleButtonRenderer,
  checkboxRenderer,
  checkboxesRenderer,
  columnFilterRenderer,
  nameFilterRenderer,
  columnSelectRenderer,
  comboBoxRenderer,
  dateTimeRenderer,
  dropdownRenderer,
  integerRenderer,
  numberRenderer,
  radioRenderer,
  richTextRenderer,
  simpleTwinlistRenderer,
  twinlistRenderer,
  sortListRenderer,
  valueSwitchRenderer,
  textAreaRenderer,
  credentialsRenderer,
  legacyCredentialsRenderer,
  localFileChooserRenderer,
  fileChooserRenderer,
  dynamicValueRenderer,
  /**
   * Internal synchronous renderers
   */
  editResetButtonRenderer,
  elementCheckboxRenderer,
].map(({ name, renderer, tester }) => ({
  name,
  tester,
  renderer: toDialogComponent(renderer),
}));
