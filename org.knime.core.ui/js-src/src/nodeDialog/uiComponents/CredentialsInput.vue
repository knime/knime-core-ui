<script>
import { defineComponent } from 'vue';
import { rendererProps, useJsonFormsControl } from '@jsonforms/vue';
import { getFlowVariablesMap, isModelSettingAndHasNodeView } from '../utils';
import InputField from 'webapps-common/ui/components/forms/InputField.vue';
import LabeledInput from './LabeledInput.vue';
import DialogComponentWrapper from './DialogComponentWrapper.vue';

const MAGIC_PASSWORD = '*************';

const CredentialsInput = defineComponent({
    name: 'CredentialsInput',
    components: {
        InputField,
        LabeledInput,
        DialogComponentWrapper
    },
    props: {
        ...rendererProps()
    },
    setup(props) {
        return useJsonFormsControl(props);
    },
    data() {
        return {
            credentials: {
                username: '',
                password: ''
            }
        };
    },
    computed: {
        handleMagicPassword() {
            return this.control.uischema.options?.handleMagicPassword ?? false;
        },
        defaultUsername() {
            return this.control.schema.username?.default ?? '';
        },
        defaultPassword() {
            return this.control.schema.password?.default ?? '';
        },
        isModelSettingAndHasNodeView() {
            return isModelSettingAndHasNodeView(this.control);
        },
        flowSettings() {
            return getFlowVariablesMap(this.control);
        },
        disabled() {
            return (
                !this.control.enabled ||
        this.flowSettings?.controllingFlowVariableAvailable
            );
        },
        passwordChanged() {
            return this.credentials.password !== this.defaultPassword;
        }
    },
    created() {
        this.credentials.username = this.defaultUsername;
        this.credentials.password = this.defaultPassword;
    },
    methods: {
        onChange(event, key) {
            // The magic password is a 'decrypted' string sent from the backend if the password is prefilled.
            // Since the password is always sent back to the server, we have to delete it, if the username changed
            // to prevent sending the magic password to the backend.
            if (this.handleMagicPassword) {
                if (this.defaultPassword === MAGIC_PASSWORD) {
                    if (
                        key === 'username' &&
            this.defaultPassword === MAGIC_PASSWORD &&
            !this.passwordChanged
                    ) {
                        this.credentials.password = '';
                    }
                }
            }
            this.credentials[key] = event;
            
            this.handleChange(this.control.path, this.credentials);
            if (this.isModelSettingAndHasNodeView) {
                this.$store.dispatch('pagebuilder/dialog/dirtySettings', true);
            }
        }
    }
});
export default CredentialsInput;
</script>

<template>
  <DialogComponentWrapper
    :control="control"
    style="min-width: 0"
  >
    <LabeledInput
      :text="control.label"
      :description="control.description"
      :errors="[control.errors]"
      :show-reexecution-icon="isModelSettingAndHasNodeView"
      :scope="control.uischema.scope"
      :flow-settings="flowSettings"
    >
      <InputField
        placeholder="Username"
        :model-value="credentials.username"
        :disabled="disabled"
        type="text"
        @update:model-value="(event) => onChange(event, 'username')"
      />
      <InputField
        class="password"
        placeholder="Password"
        :model-value="credentials.password"
        :disabled="disabled"
        type="password"
        @update:model-value="(event) => onChange(event, 'password')"
      />
    </LabeledInput>
  </DialogComponentWrapper>
</template>

<style lang="postcss" scoped>
.password {
  margin-top: 20px;
}
</style>
