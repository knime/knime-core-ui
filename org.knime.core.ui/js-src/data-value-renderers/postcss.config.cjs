const { preset, plugins } = require("@knime/styles/config/postcss.config.cjs");

module.exports = {
  plugins: Object.assign({}, plugins, {
    "postcss-preset-env": preset,
  }),
};
