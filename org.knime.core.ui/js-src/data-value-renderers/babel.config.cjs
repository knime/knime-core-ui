module.exports = {
  presets: [
    [
      "@babel/preset-env",
      {
        corejs: "3",
        useBuiltIns: "entry",
      },
    ],
  ],
  env: {
    test: {
      plugins: ["@babel/plugin-transform-modules-commonjs"],
    },
  },
};
