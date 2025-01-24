import { type Component, createApp } from "vue";

import type { UIExtensionService } from "@knime/ui-extension-service";

export default (component: Component) => {
  return (shadowRoot: ShadowRoot, knimeService: UIExtensionService) => {
    // create a app holder in the shadow root
    const holder = document.createElement("div");

    // the table requires all wrappers to have 100% height to enlarge itself
    // since 100% does not mix well with flex-box, it is only set on media screen
    // (https://issues.chromium.org/issues/365922171)
    holder.setAttribute("id", "holder");
    const fitContentStyle = document.createElement("style");
    fitContentStyle.innerHTML = `
      @media screen {
        #holder {
          height: 100%;
        }
      }
    `;
    shadowRoot.appendChild(fitContentStyle);

    // inject styles in the shadow root
    const style = document.createElement("style");
    // @ts-ignore - will be replaced by the build tool see vite.config.ts
    style.innerHTML = __INLINE_CSS_CODE__;

    // elements attach to the shadow root
    shadowRoot.appendChild(style);
    shadowRoot.appendChild(holder);

    const app = createApp(component);
    app.provide("getKnimeService", () => knimeService);
    app.provide("shadowRoot", shadowRoot);
    app.mount(holder);

    return {
      teardown: () => {
        app.unmount();
        shadowRoot.replaceChildren();
      },
    };
  };
};
