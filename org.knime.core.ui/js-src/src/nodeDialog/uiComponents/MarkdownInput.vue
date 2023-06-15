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
    @click="handleClick"
    @update:model-value="onChange"
  />
</template>

<style scoped>
.editor {
    --rich-text-editor-font-size: 16px;
    background-color: white;
    border: 1px solid var(--knime-silver-sand);
}
</style>
