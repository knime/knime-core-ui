<script>
import { defineComponent, ref } from 'vue';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue';
import { isModelSettingAndHasNodeView, getFlowVariablesMap } from '../utils';
import RichtTextEdtior from 'webapps-common/ui/components/RichTextEditor/RichTextEditor.vue';
import { onClickOutside } from '@vueuse/core';

const MarkdownInput = defineComponent({
    name: 'MarkdownInput',
    components: {
        RichtTextEdtior
    },
    props: {
        ...rendererProps()
    },
    emits: ['update'],
    setup(props) {
        const target = ref(false);
        const editable = ref(false);
        onClickOutside(target, () => {
            editable.value = false;
        });
        return { ...useJsonFormsControl(props), target, editable };
    },
    computed: {
        isModelSettingAndHasNodeView() {
            return isModelSettingAndHasNodeView(this.control);
        },
        flowSettings() {
            return getFlowVariablesMap(this.control);
        },
        disabled() {
            return !this.control.enabled || this.flowSettings?.controllingFlowVariableAvailable;
        }
    },
    mounted() {
        this.initialValue = this.control.data;
    },
    methods: {
        onChange(event) {
            this.handleChange(this.control.path, event);
            if (this.isModelSettingAndHasNodeView) {
                this.$store.dispatch('pagebuilder/dialog/dirtySettings', true);
            }
        },
        handleClick() {
            this.editable = true;
        }
    }
        
});
export default MarkdownInput;
</script>

<template>
  <RichtTextEdtior
    ref="target"
    v-on-click-outside="closeEditor"
    class="editor"
    :min-height="400"
    compact
    :editable="editable"
    :model-value="control.data"
    :enabled-tools="{
      all: true
    }"
    @click="handleClick"
    @update:model-value="onChange"
  />
</template>

<style lang="postcss" scoped>
.editor {
    --rich-text-editor-font-size: 13;
    font-weight: 300;
    background-color: white;
    border: 1px solid var(--knime-silver-sand);

    &:deep(.rich-text-editor) {
        & h1 {
            font-size: 36px;
            margin: 32px 0 16px;
            font-weight: bold;
        }

        & h2 {
            font-size: 30px;
            margin: 24px 0 12px;
            font-weight: bold;
        }

        & h3 {
            font-size: 26px;
            margin: 20px 0 10px;
            font-weight: bold;
        }

        & h4 {
            font-size: 22px;
            margin: 16px 0 8px;
            font-weight: bold;
        }

        & h5 {
            font-size: 18px;
            margin: 12px 0 6px;
            font-weight: bold;
        }

        & h6 {
            font-size: 16px;
            margin: 10px 0 5px;
            font-weight: bold;
        }
    }
}
</style>
