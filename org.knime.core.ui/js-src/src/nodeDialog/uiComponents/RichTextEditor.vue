<script setup lang="ts">
import { onMounted, nextTick, toRefs, watch, ref, computed } from 'vue';
import { EditorContent, useEditor } from '@tiptap/vue-3';
import TextAlign from '@tiptap/extension-text-align';
import UnderLine from '@tiptap/extension-underline';
import StarterKit from '@tiptap/starter-kit';

import { useFloating, offset,
    flip,
    shift,
    autoPlacement } from '@floating-ui/vue';


import RichTextEditorToolbar from './RichTextEditorToolbar.vue';

interface Props {
  id: string;
  editable: boolean;
  initialValue: string;
}

const reference = ref(null);
const floating = ref(null);
const { floatingStyles } = useFloating(reference, floating, {
    placement: 'top',
    middleware: [offset(20), autoPlacement()]
});

const props = defineProps<Props>();
const { initialValue } = toRefs(props);

// eslint-disable-next-line func-call-spacing
const emit = defineEmits<{
  (e: 'editStart'): void;
  (e: 'change', content: string | undefined): void;
}>();

const editor = useEditor({
    content: props.initialValue,
    editable: props.editable,
    extensions: [
        StarterKit,
        UnderLine,
        TextAlign.configure({
            types: ['heading', 'paragraph']
        })
    ],
    onUpdate: () => emit('change', editor.value?.getHTML())
});

onMounted(() => {
    if (props.editable) {
        nextTick(() => {
            editor.value?.commands.focus();
        });
    }
});
</script>

<template>
  <div
    class="annotation-editor-wrapper"
    @pointerdown="editable && $event.stopPropagation()"
  >
    <div
      v-if="editable && editor"
      ref="reference"
      to="annotation-editor-toolbar"
    >
      <RichTextEditorToolbar
        ref="floating"
        :style="floatingStyles"
        :editor="editor"
      />
    </div>
    <EditorContent
      class="annotation-editor"
      :editor="editor"
      :class="{
        editable,
      }"
      @dblclick="!editable && emit('editStart')"
    />
  </div>
</template>

<style lang="postcss" scoped>
.annotation-editor-wrapper {
  height: 100%;
  background: var(--knime-white);
}

.toolbar-wrapper {
  height: 100%;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: initial;
}

.annotation-editor {
  --border-width: 2px;

  height: 100%;
  overflow-y: auto;
  border: var(--border-width) solid red;

  &.editable {
    cursor: text;
  }

  /* stylelint-disable-next-line selector-class-pattern */
  & :deep(.ProseMirror) {
    height: 100%;
    font-size: 12px;
    padding: 16px;
    color: var(--knime-black);

    &:focus-visible,
    &:focus {
      outline: transparent;
    }

    & p {
      margin: 0 0 6px;
      padding: 0;
      line-height: 1.44;
    }

    & blockquote {
      margin: 0 0 6px 12px;
      position: relative;

      &::before {
        position: absolute;
        content: "";
        left: -12px;
        height: 100%;
        width: 4px;
        background-color: var(--knime-silver-sand);
        border-radius: 4px;
      }

      & p:last-child {
        padding-bottom: 0;
      }
    }

    & h1 {
      font-size: 48px;
      margin: 32px 0 16px;
    }

    & h2 {
      font-size: 36px;
      margin: 24px 0 12px;
    }

    & h3 {
      font-size: 30px;
      margin: 20px 0 10px;
    }

    & h4 {
      font-size: 24px;
      margin: 16px 0 8px;
    }

    & h5 {
      font-size: 18px;
      margin: 12px 0 6px;
    }

    & h6 {
      font-size: 15px;
      margin: 10px 0 5px;
    }

    & h1:first-child,
    & h2:first-child,
    & h3:first-child,
    & h4:first-child,
    & h5:first-child,
    & h6:first-child {
      margin-top: 0;
    }

    & hr {
      border: none;
      border-top: 1px solid var(--knime-silver-sand);
      margin: 6px 0;
    }

    & :not(pre) > code {
      padding: 0 2px;
      font-family: "Roboto Mono", monospace;
      border: 1px solid var(--knime-silver-sand);
      background: var(--knime-gray-light-semi);
      box-decoration-break: clone;
    }

    & pre {
      background: var(--knime-gray-light-semi);
      border: 1px solid var(--knime-silver-sand);
      font-family: "Roboto Mono", monospace;
      padding: 8px 12px;
      line-height: 1.44;

      & > code {
        color: inherit;
        padding: 0;
        background: none;
      }
    }

    & ul,
    & ol {
      padding-left: 20px;

      &:first-child {
        margin-top: 0;
      }
    }

    & a {
      color: var(--knime-cornflower);
    }
  }
}
</style>
