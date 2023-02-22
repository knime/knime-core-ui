import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';

import LabeledInput from '@/nodeDialog/UIComponents/LabeledInput.vue';
import ErrorMessage from '@/nodeDialog/UIComponents/ErrorMessage.vue';
import FlowVariableIcon from '@/nodeDialog/UIComponents/FlowVariableIcon.vue';
import DescriptionPopover from '@/nodeDialog/UIComponents/DescriptionPopover.vue';
import ReexecutionIcon from 'webapps-common/ui/assets/img/icons/reexecution.svg';
import BothFlowVariables from 'webapps-common/ui/assets/img/icons/both-flow-variables.svg';
import OnlyFlowVariable from 'webapps-common/ui/assets/img/icons/only-flow-variables.svg';
import ExposeFlowVariable from 'webapps-common/ui/assets/img/icons/expose-flow-variables.svg';

describe('LabeledInput.vue', () => {
    it('renders', () => {
        const wrapper = mount(LabeledInput);
        expect(wrapper.getComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(ErrorMessage).exists()).toBe(true);
        const icon = wrapper.findComponent(ReexecutionIcon);
        expect(icon.exists()).toBe(false);
    });

    it('visually displays model settings', () => {
        const wrapper = mount(LabeledInput, { props: { showReexecutionIcon: true } });
        expect(wrapper.vm.showReexecutionIcon).toBe(true);
        const icon = wrapper.findComponent(ReexecutionIcon);
        expect(icon.exists()).toBe(true);
    });

    it('renders the description popover', async () => {
        const wrapper = mount(LabeledInput);
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(false);
        wrapper.setProps({ description: 'foo' });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(true);
    });

    // FIXME: UIEXT-253 - this needs to be added again once errors are properly passed and displayed
    /* it('renders error message on error', () => {
        const wrapper = mount(LabeledInput, { props: { errors: ['test error'] } });
        expect(wrapper.getComponent(ErrorMessage).props().errors).toStrictEqual(['test error']);
    }); */
    it('renders both icons rendered when controlled and exposed by a flow variable', () => {
        const wrapper = mount(LabeledInput, {
            props: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(BothFlowVariables);
        expect(icon.exists()).toBe(true);
    });

    it('renders exposedFlowVariable icon when exposed flow variable exists', () => {
        const wrapper = mount(LabeledInput, {
            props: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: null,
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(false);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(ExposeFlowVariable);
        expect(icon.exists()).toBe(true);
    });

    it('renders onlyFlowVariable icon when controlled by flow variable', () => {
        const wrapper = mount(LabeledInput, {
            props: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: null,
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(false);
        const icon = wrapper.findComponent(OnlyFlowVariable);
        expect(icon.exists()).toBe(true);
    });
});
