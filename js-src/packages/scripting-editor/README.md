# KNIME Scripting Editor Framework

This package contains the Scripting Editor framework for KNIME Analytics Platform. Use this framework to build dialogs for KNIME nodes that allow the user to write scripts.

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Project Setup

```sh
npm install
```

### Run Demo for UI Development

```sh
npm run demo
```

Opens a standalone demo at [http://localhost:3000](http://localhost:3000) with mocked backend services. Perfect for developing and testing UI components without needing to link to other projects or rebuild constantly.

See [demo/README.md](demo/README.md) for detailed documentation.

### Compile and Hot-Reload for Development

```sh
npm run build-watch
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
