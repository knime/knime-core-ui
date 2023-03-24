<script>
import { defineComponent } from 'vue';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue';
import { optionsMapper, getFlowVariablesMap, isModelSettingAndHasNodeView } from '../utils';
import RadioButtons from 'webapps-common/ui/components/forms/RadioButtons.vue';
import ValueSwitch from 'webapps-common/ui/components/forms/ValueSwitch.vue';
import LabeledInput from './LabeledInput.vue';
import DialogComponentWrapper from './DialogComponentWrapper.vue';

const RadioInputBase = defineComponent({
    name: 'RadioInputBase',
    components: {
        RadioButtons,
        ValueSwitch,
        LabeledInput,
        DialogComponentWrapper
    },
    props: {
        ...rendererProps(),
        type: {
            type: String,
            required: true,
            default: 'radio'
        }
    },
    setup(props) {
        return useJsonFormsControl(props);
    },
    data() {
        return {
            options: null
        };
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
        },
        alignment() {
            return this.control.uischema.options?.radioLayout;
        },
        uiComponent() {
            switch (this.type) {
                case 'valueSwitch':
                    return ValueSwitch;
                case 'radio':
                    return RadioButtons;
                default:
                    return RadioButtons;
            }
        }
    },
    mounted() {
        this.options = this.control?.schema?.oneOf?.map(optionsMapper);
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
export default RadioInputBase;
</script>

<template>
  <DialogComponentWrapper :control="control">
    <LabeledInput
      :text="control.label"
      :show-reexecution-icon="isModelSettingAndHasNodeView"
      :scope="control.uischema.scope"
      :flow-settings="flowSettings"
      :description="control.description"
    >
      <component
        :is="uiComponent"
        v-if="options"
        :possible-values="options"
        :alignment="alignment"
        :disabled="disabled"
        :model-value="control.data"
        @update:model-value="onChange"
      />
    </LabeledInput>
  </DialogComponentWrapper>
</template>

<style lang="postcss" scoped>
.labeled-input {
  margin-bottom: 10px;
}
</style>