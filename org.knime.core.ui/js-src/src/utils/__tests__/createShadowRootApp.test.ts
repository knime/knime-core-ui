import { describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";

import type { UIExtensionService } from "@knime/ui-extension-service";

import createShadowRootApp from "../createShadowRootApp";

const heightCSS = `<style>
      @media screen {
        #holder {
          height: 100%;
        }
      }
    </style>`;

vi.stubGlobal("__INLINE_CSS_CODE__", ".someCssCode { color: red; }");

const mountApp = () => {
  const AppComponent = defineComponent({
    props: { initialData: { type: Object, default: null } },
    template: "<div>The App</div>",
  });

  const exportedAppFunction = createShadowRootApp(AppComponent);

  const shadowRoot = document
    .createElement("div")
    .attachShadow({ mode: "open" });
  const knimeService = {} as UIExtensionService;

  const app = exportedAppFunction(shadowRoot, knimeService);

  return { app, shadowRoot };
};

describe("createShadowRootApp", () => {
  it("create shadow root app", () => {
    const { app, shadowRoot } = mountApp();
    expect(shadowRoot.innerHTML).toBe(
      `${heightCSS}<style>.someCssCode { color: red; }</style><div id="holder" data-v-app=""><div>The App</div></div>`,
    );

    expect(app.teardown).toBeInstanceOf(Function);
  });
});
