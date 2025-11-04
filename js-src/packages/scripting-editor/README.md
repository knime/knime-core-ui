# KNIME Scripting Editor Framework

Shared scripting editor components for KNIME Analytics Platform. Use this framework to build dialogs for KNIME nodes that allow users to write scripts.

## Development

### Prerequisites

- Node.js 22.x
- pnpm workspace (install from root: `pnpm install`)

### Standalone Development

Run the demo app for UI development:

```sh
pnpm run demo
```

Opens at [http://localhost:3000](http://localhost:3000) with mocked backend services. Perfect for developing and testing UI components without linking to other projects.

See [demo/README.md](demo/README.md) for detailed documentation.

### Build

Development build with watch mode:

```sh
pnpm run build-watch
```

Production build:

```sh
pnpm run build
```

### Testing

Run unit tests:

```sh
pnpm run test:unit
```

Generate coverage report:

```sh
pnpm run coverage
```

### Code Quality

Lint and format:

```sh
pnpm run lint
pnpm run format
```
