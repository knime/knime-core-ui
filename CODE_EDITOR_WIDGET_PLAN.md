# Code Editor Widget — Implementation Plan

## Goal

Add a new `@CodeEditorWidget` annotation for `String` settings that renders a Monaco-based code editor in the node dialog. Supports syntax highlighting via a configurable language (e.g. `"json"`, `"yaml"`, `"sql"`).

Future extensions (not in scope now):
- JSON Schema validation
- Static auto-completion items
- Language Server Protocol (LSP) integration

---

## Architecture Summary

The pattern follows the existing `@TextAreaWidget` → `textArea` format as a reference:

```
@CodeEditorWidget (Java annotation)
  → CodeEditorRenderer (reads annotation, implements CodeEditorRendererSpec)
    → CodeEditorRendererSpec (defines format="codeEditor", options with language)
      → RendererToJsonFormsUtil serializes to JSON UI schema
        → codeEditorRenderer.ts (tester matches format="codeEditor")
          → CodeEditorControl.vue (renders Monaco editor)
```

---

## Phase 1 — Backend (Java, `knime-core-ui`)

### 1.1 Format constant

**File:** `org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/JsonFormsConsts.java`

Add to `UiSchema.Format`:
```java
public static final String CODE_EDITOR = "codeEditor";
```

### 1.2 Annotation

**New file:** `org.knime.core.ui/src/eclipse/org/knime/node/parameters/widget/text/CodeEditorWidget.java`

```java
@Retention(RUNTIME)
@Target(FIELD)
public @interface CodeEditorWidget {
    /** Syntax highlighting language, e.g. "json", "yaml", "sql", "toml" */
    String language() default "";
}
```

### 1.3 Renderer Spec

**New file:** `org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/renderers/CodeEditorRendererSpec.java`

- Extends `ControlRendererSpec`
- `getFormat()` returns `Optional.of(UiSchema.Format.CODE_EDITOR)`
- `getDataType()` returns `JsonDataType.STRING`
- Nested `CodeEditorRendererOptions` interface with `String getLanguage()`

### 1.4 Renderer (from widget tree)

**New file:** `org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/renderers/fromwidgettree/CodeEditorRenderer.java`

- Extends `WidgetTreeControlRendererSpec`, implements `CodeEditorRendererSpec`
- Constructor takes `TreeNode<WidgetGroup>` and `CodeEditorWidget` annotation
- `getOptions()` returns an `Optional` of `CodeEditorRendererOptions` populated from the annotation

### 1.5 Wire up in WidgetTreeRenderers

**File:** `org.knime.core.ui/src/eclipse/org/knime/core/webui/node/dialog/defaultdialog/jsonforms/renderers/fromwidgettree/WidgetTreeRenderers.java`

Add a new `WidgetTreeNodeTester` entry matching `@CodeEditorWidget` on `String` fields → creates a `CodeEditorRenderer`.

---

## Phase 2 — Frontend (`packages/core-ui`, `knime-core-ui`)

### 2.1 Format constant

**File:** `packages/core-ui/src/nodeDialog/constants/inputFormats.ts`

```ts
codeEditor: "codeEditor",
```

### 2.2 Vue control component

**New file:** `packages/core-ui/src/nodeDialog/uiComponents/CodeEditorControl.vue`

- Props: `VueControlPropsForLabelContent<string>` (from `@knime/jsonforms`)
- Reads `control.uischema.options?.language` to configure Monaco
- Mounts a `monaco.editor.IStandaloneCodeEditor` in the component container
- Emits value changes on model change via `changeValue`
- Handles dispose on `onUnmounted`
- Uses raw `monaco-editor` (already a direct dependency in `packages/core-ui`)

### 2.3 Renderer

**New file:** `packages/core-ui/src/nodeDialog/renderers/codeEditorRenderer.ts`

```ts
export const codeEditorRenderer: VueControlRenderer = {
  name: "CodeEditorControl",
  control: CodeEditorControl,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.codeEditor)),
};
```

Wrapped with `withLabel()` from `@knime/jsonforms` so it gets the standard label.

### 2.4 Register renderer

**File:** `packages/core-ui/src/nodeDialog/renderers/index.ts`

Add `codeEditorRenderer` to the `coreUIControls` map.

---

## Phase 3 — Future: Move to `@knime/jsonforms` (`webapps-common`)

When the widget is needed outside of `core-ui` (e.g. in other KNIME web apps):

1. Add `monaco-editor` (or a lightweight wrapper) as a dependency in `packages/jsonforms/package.json`
2. Move `CodeEditorControl.vue` → `packages/jsonforms/src/uiComponents/`
3. Move `codeEditorRenderer.ts` → `packages/jsonforms/src/renderers/`
4. Add `codeEditor` to `packages/jsonforms/src/constants/inputFormats.ts`
5. Register in `packages/jsonforms/src/renderers/defaultRenderers.ts`

The format string `"codeEditor"` and all backend Java code remain unchanged.

---

## Data Flow (UI Schema)

The backend serializes the annotation into this JSONForms UI schema structure:

```json
{
  "type": "Control",
  "scope": "#/properties/myCodeSetting",
  "options": {
    "format": "codeEditor",
    "language": "sql"
  }
}
```

The frontend tester matches `options.format === "codeEditor"` and renders `CodeEditorControl.vue`, which passes `options.language` to Monaco.
