import type { MaybeRef } from "vue";

import {
  type NamedRenderer,
  type PerformExternalValidation,
  type VueControlRenderer,
  type VueLayout,
  type VueLayoutRenderer,
  controls,
  getAsyncSetupMethod,
  layouts,
  mapControls,
  toRenderers,
} from "@knime/jsonforms";

import {
  handleIsAdvanced,
  handleIsAdvancedLayout,
  handleIsAdvancedRenderer,
  mapDirty,
  mapWithFlowVariables,
  withDescriptionButton,
  withDescriptionButtonLayout,
  withReexecutionIcon,
} from "../higherOrderComponents";

import { arrayLayoutRenderer } from "./arrayLayoutRenderer";
import { buttonRenderer } from "./buttonRenderer";
import { credentialsRenderer } from "./credentialsRenderer";
import { dynamicValueRenderer } from "./dynamicValueRenderer";
import { editResetButtonRenderer } from "./editResetButtonRenderer";
import { elementCheckboxRenderer } from "./elementCheckboxRenderer";
import {
  fileChooserForMultiFileRenderer,
  fileChooserRenderer,
} from "./fileChooserRenderer";
import { legacyCredentialsRenderer } from "./legacyCredentialsRenderer";
import { localFileChooserRenderer } from "./localFileChooserRenderer";
import { multiFileChooserRenderer } from "./multiFileChooserRenderer";
import { vennDiagramLayoutRenderer } from "./vennDiagramRenderer";

const coreUIControls: Record<string, VueControlRenderer> = {
  buttonRenderer, // since it will be removed soon and is based on the old update mechanism
  localFileChooserRenderer, // since it would require api calls
  fileChooserRenderer,
  multiFileChooserRenderer,
  fileChooserForMultiFileRenderer,
  dynamicValueRenderer, // since it will change a lot and is no use to the hub
  credentialsRenderer, // since it contains flowSettings logic (that a flow variable is set is important for the backend to resolve the value properly, since we do not load the password in the frontend in this case)
  legacyCredentialsRenderer,
  elementCheckboxRenderer,
};

const otherRenderers = [
  /* layout renderers */
  arrayLayoutRenderer, // since it contains dirty-state - and update-logic. Note that we just remove the renderer from here but e.g. the data can still contain arrays
  vennDiagramLayoutRenderer, // probably never used anywhere else than in NodeDialogs

  /**
   * Sub renderers for array layout
   */
  editResetButtonRenderer,
] satisfies NamedRenderer[];

export const allControls: typeof controls & typeof coreUIControls = {
  ...controls,
  ...coreUIControls,
};

type InitializeRenderersProps = {
  hasNodeView: boolean;
  showAdvancedSettings: MaybeRef<boolean>;
};

type ExternalValidationInitializeRendererProps = {
  performExternalValidation: PerformExternalValidation<unknown>;
};

const processControls = ({
  hasNodeView,
  showAdvancedSettings,
}: InitializeRenderersProps) => {
  let processedControls = allControls;
  processedControls = mapWithFlowVariables(
    mapControls(withDescriptionButton)(processedControls),
  );
  processedControls = mapDirty(processedControls);
  processedControls = mapControls(handleIsAdvanced(showAdvancedSettings))(
    processedControls,
  );
  if (hasNodeView) {
    processedControls = mapControls(withReexecutionIcon)(processedControls);
  }
  return processedControls;
};

const allLayouts: VueLayoutRenderer[] = [...Object.values(layouts)];

export const initializeRenderers = (
  props: InitializeRenderersProps &
    ExternalValidationInitializeRendererProps = {
    hasNodeView: false,
    showAdvancedSettings: false,
    performExternalValidation: () => Promise.resolve(null),
  },
) =>
  toRenderers({
    renderers: otherRenderers.map(({ name, tester, renderer }) => ({
      name,
      tester,
      renderer: handleIsAdvancedRenderer(props.showAdvancedSettings)(
        renderer,
        getAsyncSetupMethod(renderer),
      ),
    })),
    controls: Object.values(processControls(props)),
    layouts: allLayouts.map(({ name, tester, layout }) => ({
      name,
      tester,
      layout: handleIsAdvancedLayout(props.showAdvancedSettings)(
        withDescriptionButtonLayout(layout as VueLayout),
      ),
      __asyncSetup: getAsyncSetupMethod(layout),
    })),
    config: {
      performExternalValidation: props.performExternalValidation,
    },
  });
