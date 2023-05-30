<script>
import { defineComponent } from 'vue';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue';
import { isModelSettingAndHasNodeView, getFlowVariablesMap } from '../utils';
import RichtTextEdtior from './RichTextEditor.vue';


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
        return { ...useJsonFormsControl(props) };
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
        this.editor.on('update', () => {
            this.html = this.editor.getHTML();
            this.onChange(this.html);
        });
    },
    methods: {
        onChange(event) {
            this.handleChange(this.control.path, event);
            if (this.isModelSettingAndHasNodeView) {
                this.$store.dispatch('pagebuilder/dialog/dirtySettings', true);
            }
        }
    }
        
});
export default MarkdownInput;
</script>

<template>
  <RichtTextEdtior
    :id="test"
    editable
    :initial-value="control.data"
    @change="onChange"
  />
</template>
