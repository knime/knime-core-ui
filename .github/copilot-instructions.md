# KNIME Frontend Development Guidelines

## Architecture Overview

This is a **multi-workspace KNIME frontend system** with two main areas:
- `js-src/` - Core UI components (Vue views/dialogs) that embed in KNIME Analytics Platform
- `webapps-common/` - Shared monorepo with reusable packages (@knime/components, @knime/jsonforms, etc.)

### UI Extension Service Pattern
All components communicate through `@knime/ui-extension-service` which provides a service layer between frontend and backend:
- **JsonDataService** - RPC calls to backend (`initialData()`, `data()`, `applyData()`)  
- **SharedDataService** - Cross-component data sharing (dialogs→views)
- **DialogService** - Dialog lifecycle management
- **AlertingService** - User notifications

Example service usage in components:
```javascript
const { jsonDataService, dialogService, sharedDataService } = useServices(getKnimeService());
const initialData = await jsonDataService.initialData();
```

### Component Architecture Layers
1. **Outer wrapper** (e.g., `NodeDialog.vue`) - Handles service communication, data flow
2. **Core component** (e.g., `NodeDialogCore.vue`) - Business logic, can be reused as subcomponent
3. **JsonForms integration** - Schema-driven form rendering with custom KNIME renderers

## Development Workflows

### Building & Testing
- **Views**: `npm run dev:TableView` → `npm run build:TableView` (builds to `/dist`)
- **Dialogs**: `npm run dev:NodeDialog` or `npm run dev:NodeDialog:standalone` (with mocks)
- **Tests**: `npm run test:unit` / `npm run test:integration` (Vitest)
- **Coverage**: `npm run coverage` → `/coverage` folder

### Hot Development Setup
Views/dialogs can be developed live in KNIME Analytics Platform:
```bash
npm run dev:NodeDialog  # Starts on localhost:3333
# Add to Eclipse run config: -Dorg.knime.ui.dev.node.dialog.url=http://localhost:3333/NodeDialog.js
```

### Monorepo Commands (webapps-common)
- `pnpm demo` - Start component demo with all packages built
- `pnpm --filter @knime/jsonforms test:unit` - Run tests for specific package
- `pnpm changeset` - Create changelog entries for packages

## Code Patterns & Conventions

### Testing Structure
- **Unit tests**: `src/**/__tests__/**/*.test.{js,ts}`
- **Integration tests**: `src/**/__integrationTests__/**/*.test.{js,ts}`
- **Mocks**: Use service mocks from `@knime/ui-extension-service` testing utilities
- **Test isolation**: Split integration tests into separate files to avoid module cache issues

### JsonForms Custom Renderers
Located in `packages/jsonforms/src/renderers/` and `js-src/src/nodeDialog/renderers/`:
```javascript
import { controls, layouts, toRenderers } from "@knime/jsonforms";
const renderers = toRenderers([customRenderers], [controls.dropdownRenderer]);
```

### Mock Data & Development
- `js-src/mocks/` - Dialog mock configurations  
- `packages/jsonforms/src/loading/` - Loading state testing patterns
- Use `dev/ParentAppWithDialog.vue` as reference for embedding core components

### Linting & Code Quality
- ESLint config: `@knime/eslint-config/{vue3,typescript}.js` with flat config format
- Stylelint for CSS/Vue files
- TODO comments must include ticket IDs: `TODO: AP-12345`
- Git hooks via husky handle formatting (lint-staged) and commit message formatting

### Flow Variables & Settings
- Flow variables managed through `useFlowVariableSystem` composable
- Settings state tracked via `dialogService.registerSettings()` for dirty detection
- Combined data gathering: `getCombinedData()` merges component data with parent context

## Key Files & Patterns

- `src/nodeDialog/NodeDialog.vue` - Main dialog wrapper with service communication
- `src/nodeDialog/NodeDialogCore.vue` - Reusable core component  
- `src/nodeDialog/composables/nodeDialog/useServices.ts` - Service initialization pattern
- `packages/jsonforms/src/JsonFormsDialog.vue` - Schema-driven form system
- `packages/ui-extension-service/src/services/` - All service implementations
- `vite.config.ts` - Multi-mode build configuration for different components

## Integration Points

- **Backend Communication**: Via UI Extension Service RPC layer (Java↔JavaScript)
- **Cross-Component Data**: SharedDataService for dialog→view communication
- **Mock Development**: Standalone modes with JSON mock data in `/mocks` and `/dev` folders
- **Package Dependencies**: Packages use workspace references (`workspace:*`) in monorepo