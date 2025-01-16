import { setUpRendererTest } from "@knime/jsonforms/testing";

import { initializeRenderers } from "..";

export const determineRenderer = setUpRendererTest(
  initializeRenderers(),
).getRendererName;
