import { afterEach, beforeEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore }
    from '@@/test-setup/utils/jsonFormsTestUtils';
import CredentialsInput from '../CredentialsInput.vue';
import LabeledInput from '../LabeledInput.vue';
import InputField from 'webapps-common/ui/components/forms/InputField.vue';

describe('CredentialsInput.vue', () => {
    let defaultProps, wrapper, onChangeSpy, component;

    beforeAll(() => {
        onChangeSpy = vi.spyOn(CredentialsInput.methods, 'onChange');
    });
    
    beforeEach(async () => {
        defaultProps = {
            control: {
                path: 'test',
                enabled: true,
                visible: true,
                label: 'defaultLabel',
                schema: { properties: {
                    enterCredentials: {
                        type: 'object',
                        label: 'username',
                        password: {
                            type: 'string'
                        },
                        username: {
                            type: 'string',
                            default: ''
                        },
                        description: 'Test credentials field'
                    }
                } },
                uischema: {
                    type: 'Control',
                    scope: '#/properties/view/properties/enterCredentials',
                    label: 'Enter credentials',
                    options: {
                        format: 'credentials',
                        handleMagicPassword: true
                    },
                    password: {
                        type: 'string'
                    },
                    username: {
                        type: 'string'
                    }
                },
                rootSchema: {
                    hasNodeView: true,
                    flowVariablesMap: {}
                }
            }
        };

        component = await mountJsonFormsComponent(CredentialsInput, defaultProps);
        wrapper = component.wrapper;
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(CredentialsInput).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findAllComponents(InputField)).toHaveLength(2);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsControl(component);
    });

    //
    
    // have one test: password text input and one for the username input when changed - copy below 2 times use class name
    // it('calls onChange when text input is changed', () => {
    //         const dirtySettingsMock = vi.fn();
    //         const { wrapper, updateData } = mountJsonFormsComponentWithStore(TextInput, defaultProps, {
    //             'pagebuilder/dialog': {
    //                 actions: { dirtySettings: dirtySettingsMock },
    //                 namespaced: true
    //             }
    //         });
    //         const changedTextInput = 'Shaken not stirred';
    //         wrapper.findComponent(InputField).vm.$emit('update:modelValue', changedTextInput);
    //         expect(onChangeSpy).toHaveBeenCalledWith(changedTextInput);
    //         expect(updateData).toHaveBeenCalledWith(
    //             expect.anything(), defaultProps.control.path, changedTextInput
    //         );
    //         expect(dirtySettingsMock).not.toHaveBeenCalled();
    //     });

    //
    // same as in checkboxes -----
    // it('indicates model settings change when model setting is changed', () => {
    //         const dirtySettingsMock = vi.fn();
    //         const { wrapper, updateData } = mountJsonFormsComponentWithStore(
    //             TextInput,
    //             {
    //                 ...defaultProps,
    //                 control: {
    //                     ...defaultProps.control,
    //                     uischema: {
    //                         ...defaultProps.control.schema,
    //                         scope: '#/properties/model/properties/yAxisColumn'
    //                     }
    //                 }
    //             },
    //             {
    //                 'pagebuilder/dialog': {
    //                     actions: { dirtySettings: dirtySettingsMock },
    //                     namespaced: true
    //                 }
    //             }
    //         );
    //         const changedTextInput = 'Shaken not stirred';
    //         wrapper.findComponent(InputField).vm.$emit('update:modelValue', changedTextInput);
    //         expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
    //         expect(updateData).toHaveBeenCalledWith(
    //             expect.anything(), defaultProps.control.path, changedTextInput
    //         );
    //     });


    it('sets correct label', () => {
        expect(wrapper.find('label').text()).toBe(defaultProps.control.label);
    });

    it('disables input when controlled by a flow variable', () => {
        const localDefaultProps = JSON.parse(JSON.stringify(defaultProps));
        localDefaultProps.control.rootSchema
            .flowVariablesMap[defaultProps.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };
        const { wrapper } = mountJsonFormsComponent(CredentialsInput, localDefaultProps);
        expect(wrapper.vm.disabled).toBeTruthy();
    });

    it('does not render content of TextInput when visible is false', async () => {
        wrapper.setProps({ control: { ...defaultProps.control, visible: false } });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(false);
    });

    it('checks that it is not rendered if it is an advanced setting', () => {
        defaultProps.control.uischema.options.isAdvanced = true;
        const { wrapper } = mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.getComponent(CredentialsInput).isVisible()).toBe(false);
    });

    it('checks that it is rendered if it is an advanced setting and advanced settings are shown', () => {
        defaultProps.control.rootSchema = { showAdvancedSettings: true };
        defaultProps.control.uischema.options.isAdvanced = true;
        const { wrapper } = mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.getComponent(CredentialsInput).isVisible()).toBe(true);
    });
});
