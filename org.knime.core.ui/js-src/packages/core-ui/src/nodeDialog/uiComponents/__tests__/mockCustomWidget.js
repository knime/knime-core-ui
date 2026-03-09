// Mock custom widget for testing

let injectedStyles = null;

export const injectStyles = (shadowRoot) => {
  const style = document.createElement("style");
  style.textContent = `
    .custom-widget-test {
      color: rgb(255, 0, 0);
      font-weight: bold;
    }
  `;
  shadowRoot.appendChild(style);
  injectedStyles = style;
};

export const getInjectedStyles = () => injectedStyles;

export const resetInjectedStyles = () => {
  injectedStyles = null;
};

// Export a simple Vue component
export default {
  name: "MockCustomWidget",
  props: ["control", "schema", "uischema"],
  setup() {
    const { h } = window.Vue;
    return () =>
      h("div", { class: "custom-widget-test" }, [
        h("span", { class: "widget-label" }, "Custom Widget"),
        h("input", {
          type: "text",
          class: "widget-input",
        }),
      ]);
  },
};
