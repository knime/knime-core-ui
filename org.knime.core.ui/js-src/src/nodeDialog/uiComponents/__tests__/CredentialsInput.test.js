import { afterEach, beforeEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore }
    from '@@/test-setup/utils/jsonFormsTestUtils';
import CredentialsInput from '../CredentialsInput.vue';
import LabeledInput from '../LabeledInput.vue';
import InputField from 'webapps-common/ui/components/forms/InputField.vue';

describe('CredentialsInput.vue', () => {
    let wrapper, onChangeSpy, defaultProps;

    beforeAll(() => {
        onChangeSpy = vi.spyOn(CredentialsInput.methods, 'onChange');
    });
    
    beforeEach(async () => {
        defaultProps = {
            control: {
                path: 'test',
                enabled: true,
                visible: true,
                data: { username: 'knime', password: 'test' },
                label: 'defaultLabel',
                schema: {
                    type: 'object',
                    title: 'Credentials input',
                    description: 'Some description.',
                    username: { default: 'knime' },
                    password: { default: 'test' }
                },
                uischema: {
                    type: 'Control',
                    scope: '#/properties/view/properties/credentials',
                    options: {
                        format: 'credentials',
                        handleMagicPassword: false
                    }
                },
                rootSchema: {
                    hasNodeView: true,
                    flowVariablesMap: {
                        test: {
                            controllingFlowVariableAvailable: true,
                            controllingFlowVariableName: 'knime.test',
                            exposedFlowVariableName: 'test',
                            leaf: true
                        }
                    }
                }
            }
        };
        wrapper = await mountJsonFormsComponent(CredentialsInput, defaultProps);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(CredentialsInput).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findAllComponents(InputField).length).toBe(2);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsControl(wrapper);
    });

    it('calls onChange when input is changed', async () => {
        const dirtySettingsMock = vi.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(CredentialsInput, defaultProps, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        const changedUsernameInput = 'knimeChanged';
        localWrapper.findAllComponents(InputField)[0].vm.$emit('update:modelValue', changedUsernameInput);
        expect(onChangeSpy).toHaveBeenCalledWith(changedUsernameInput, 'username');
        expect(localWrapper.vm.handleChange).toHaveBeenCalledWith(defaultProps.control.path,
            { username: changedUsernameInput, password: 'test' });
        expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it('indicates model settings change when model setting is changed', async () => {
        const dirtySettingsMock = vi.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(
            CredentialsInput,
            {
                ...defaultProps,
                control: {
                    ...defaultProps.control,
                    uischema: {
                        ...defaultProps.control.schema,
                        scope: '#/properties/model/properties/credentials'
                    }
                }
            },
            {
                'pagebuilder/dialog': {
                    actions: { dirtySettings: dirtySettingsMock },
                    namespaced: true
                }
            }
        );
        const changedUsernameInput = 'Shaken not stirred';
        localWrapper.findAllComponents(InputField)[0].vm.$emit('update:modelValue', changedUsernameInput);
        expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
        expect(localWrapper.vm.handleChange).toHaveBeenCalledWith(defaultProps.control.path,
            { username: changedUsernameInput, password: 'test' });
    });

    it('sets correct initial username value', () => {
        expect(wrapper.findAllComponents(InputField)[0].vm.modelValue).toBe(defaultProps.control.data.username);
    });

    it('sets correct initial password value', () => {
        expect(wrapper.findAllComponents(InputField)[1].vm.modelValue).toBe(defaultProps.control.data.password);
    });

    it('sets correct label', () => {
        expect(wrapper.find('label').text()).toBe(defaultProps.control.label);
    });

    it('checks that placeholder text is correctly set if there is no username present', async () => {
        defaultProps.control.data.username = '';
        const localWrapper = await mountJsonFormsComponentWithStore(
            CredentialsInput,
            defaultProps
        );
        expect(localWrapper.findAllComponents(InputField)[0].vm.placeholder).toBe('Username');
    });

    it('checks that placeholder text is correctly set if there is no password present', async () => {
        defaultProps.control.data.password = '';
        const localWrapper = await mountJsonFormsComponentWithStore(
            CredentialsInput,
            defaultProps
        );
        expect(localWrapper.findAllComponents(InputField)[1].vm.placeholder).toBe('Password');
    });

    it('disables InputField when controlled by a flow variable', () => {
        expect(wrapper.vm.disabled).toBeTruthy();
        expect(wrapper.findAllComponents(InputField)[0].vm.disabled).toBeTruthy();
        expect(wrapper.findAllComponents(InputField)[1].vm.disabled).toBeTruthy();
    });

    it('does not disable InputField when not controlled by a flow variable', async () => {
        delete defaultProps.control.rootSchema.flowVariablesMap;
        wrapper = await mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.vm.disabled).toBeFalsy();
        expect(wrapper.findAllComponents(InputField)[0].vm.disabled).toBeFalsy();
        expect(wrapper.findAllComponents(InputField)[1].vm.disabled).toBeFalsy();
    });

    it('does not render content of CredentialsInput when visible is false', async () => {
        defaultProps.control.visible = false;
        wrapper = await mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(false);
    });

    it('checks that it is not rendered if it is an advanced setting', async () => {
        defaultProps.control.uischema.options.isAdvanced = true;
        wrapper = await mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.getComponent(CredentialsInput).isVisible()).toBe(false);
    });

    it('checks that it is rendered if it is an advanced setting and advanced settings are shown', async () => {
        defaultProps.control.rootSchema = { showAdvancedSettings: true };
        defaultProps.control.uischema.options.isAdvanced = true;
        wrapper = await mountJsonFormsComponent(CredentialsInput, defaultProps);
        expect(wrapper.getComponent(CredentialsInput).isVisible()).toBe(true);
    });
});
