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

const mountApp = (initialData: any = null) => {
  const AppComponent = defineComponent({
    props: { initialData: { type: Object, default: null } },
    template:
      "<div :initialData='initialData && JSON.stringify(initialData)'>The App</div>",
  });

  const exportedAppFunction = createShadowRootApp(
    AppComponent,
    Boolean(initialData),
  );

  const shadowRoot = document
    .createElement("div")
    .attachShadow({ mode: "open" });
  const knimeService = {} as UIExtensionService;

  const app = exportedAppFunction(shadowRoot, knimeService, initialData);

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

  it("passes initialData if given", () => {
    const { app, shadowRoot } = mountApp({ someData: 3 });
    expect(shadowRoot.innerHTML).toBe(
      `${heightCSS}<style>.someCssCode { color: red; }</style><div id="holder" data-v-app="">` +
        '<div initialdata="{&quot;someData&quot;:3}">The App</div></div>',
    );

    expect(app.teardown).toBeInstanceOf(Function);
  });
});
