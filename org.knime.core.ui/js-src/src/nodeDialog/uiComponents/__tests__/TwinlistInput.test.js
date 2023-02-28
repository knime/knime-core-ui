import { afterEach, beforeEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore }
    from '@@/test-setup/utils/jsonFormsTestUtils';
import TwinlistInput from '../TwinlistInput.vue';
import LabeledInput from '../LabeledInput.vue';
import MultiModeTwinlist from 'webapps-common/ui/components/forms/MultiModeTwinlist.vue';
import Twinlist from 'webapps-common/ui/components/forms/Twinlist.vue';
import { mergeDeep } from '@/nodeDialog/utils';

describe('TwinlistInput.vue', () => {
    let props;

    beforeEach(() => {
        props = {
            control: {
                path: 'test',
                enabled: true,
                visible: true,
                label: 'defaultLabel',
                data: {
                    selected: ['test_1'],
                    manualFilter: {
                        manuallySelected: ['test_1'],
                        manuallyDeselected: ['test_2', 'test_3'],
                        includeUnknownColumns: true
                    },
                    patternFilter: {
                        isCaseSensitive: false,
                        isInverted: false,
                        pattern: ''
                    },
                    typeFilter: {
                        selectedTypes: ['StringValue', 'IntValue'],
                        typeDisplays: [{ id: 'StringValue', text: 'String' }]
                    },
                    mode: 'MANUAL'
                },
                schema: {
                    type: 'object',
                    properties: {
                        patternFilter: {
                            type: 'object',
                            properties: {
                                isCaseSensitive: {
                                    type: 'boolean'
                                },
                                isInverted: {
                                    type: 'boolean'
                                },
                                pattern: {
                                    type: 'string'
                                }
                            }
                        },
                        manualFilter: {
                            type: 'object',
                            properties: {
                                manuallySelected: {
                                    items: {
                                        type: 'string'
                                    },
                                    type: 'array'
                                },
                                manuallyDeselected: {
                                    item: {
                                        type: 'string'
                                    },
                                    type: 'array'
                                },
                                includeUnknownColumns: {
                                    type: 'boolean'
                                }
                            }
                        },
                        typeFilter: {
                            type: 'object',
                            properties: {
                                selectedTypes: {
                                    items: {
                                        type: 'string'
                                    },
                                    type: 'array'
                                },
                                typeDisplays: {
                                    items: {
                                        type: 'object',
                                        properties: {
                                            id: {
                                                type: 'string'
                                            },
                                            text: {
                                                type: 'string'
                                            }
                                        }
                                    },
                                    type: 'array'
                                }
                            }
                        },
                        mode: {
                            oneOf: [
                                {
                                    const: 'MANUAL',
                                    title: 'Manual'
                                },
                                {
                                    const: 'REGEX',
                                    title: 'Regex'
                                },
                                {
                                    const: 'WILDCARD',
                                    title: 'Wildcard'
                                },
                                {
                                    const: 'TYPE',
                                    title: 'Type'
                                }
                            ]
                        },
                        selected: {
                            anyOf:
                                [{
                                    const: 'test_1',
                                    title: 'test_1',
                                    columnType: 'StringValue',
                                    columnTypeDisplayed: 'String'
                                },
                                {
                                    const: 'test_2',
                                    title: 'test_2',
                                    columnType: 'DoubleValue',
                                    columnTypeDisplayed: 'Double'
                                },
                                {
                                    const: 'test_3',
                                    title: 'test_3',
                                    columnType: 'StringValue',
                                    columnTypeDisplayed: 'String'
                                }]
                        }
                    }
                },
                uischema: {},
                rootSchema: {
                    hasNodeView: true,
                    flowVariablesMap: {}
                }
            }
        };
    });


    let wrapper, onChangeSpy, handleChangeSpy;

    beforeAll(() => {
        onChangeSpy = vi.spyOn(TwinlistInput.methods, 'onChange');
        handleChangeSpy = vi.fn();
        TwinlistInput.methods.handleChange = handleChangeSpy;
    });

    beforeEach(() => {
        wrapper = mountJsonFormsComponent(TwinlistInput, props);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(TwinlistInput).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(MultiModeTwinlist).exists()).toBe(true);
    });

    it('initializes jsonforms', () => {
        initializesJsonFormsControl(wrapper);
    });

    it('calls onChange when twinlist input is changed', async () => {
        const dirtySettingsMock = vi.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(TwinlistInput, props, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        await localWrapper.findComponent(Twinlist).find({ ref: 'moveAllRight' }).trigger('click');
        expect(onChangeSpy).toBeCalled();
        expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it('indicates model settings change when model setting is changed', async () => {
        const dirtySettingsMock = vi.fn();
        props.control.uischema.scope = '#/properties/model/properties/yAxisColumn';
        const localWrapper = await mountJsonFormsComponentWithStore(TwinlistInput, props, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        await localWrapper.findComponent(Twinlist).find({ ref: 'moveAllRight' }).trigger('click');
        expect(onChangeSpy).toBeCalled();
        expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
    });

    describe('handles changes', () => {
        it('handles selected values change', () => {
            const selected = ['A', 'B', 'C'];
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('input', { selected, isManual: false });
            expect(handleChangeSpy)
                .toHaveBeenNthCalledWith(3, props.control.path, expect.objectContaining({ selected }));
        });

        it('handles selected values change', () => {
            const selected = ['A', 'B', 'C'];
            const deselected = ['E', 'F', 'G'];
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('input', { selected, isManual: true, deselected });
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({
                    selected,
                    manualFilter: expect.objectContaining({
                        manuallySelected: selected,
                        manuallyDeselected: deselected
                    })
                })
            );
        });

        it('handles includeUnknownColumns change', () => {
            const includeUnknownColumns = false;
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('includeUnknownValuesInput', includeUnknownColumns);
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({ manualFilter: expect.objectContaining({ includeUnknownColumns }) })
            );
        });

        it('handles pattern change', () => {
            const pattern = 'abc';
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('patternInput', pattern);
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({ patternFilter: expect.objectContaining({ pattern }) })
            );
        });

        it('handles isInverted change', () => {
            const isInverted = true;
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('inversePatternInput', isInverted);
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({ patternFilter: expect.objectContaining({ isInverted }) })
            );
        });

        it('handles isCaseSensitive change', () => {
            const isCaseSensitive = true;
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('caseSensitivePatternInput', isCaseSensitive);
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({ patternFilter: expect.objectContaining({ isCaseSensitive }) })
            );
        });

        it('handles type selection change', () => {
            const selectedTypes = ['A', 'B', 'C'];
            const typeDisplays = [
                { id: 'A', text: 'Text A' },
                { id: 'B', text: 'Text B' },
                { id: 'C', text: 'Text C' }
            ];
            wrapper.findComponent(MultiModeTwinlist).vm.$emit('typesInput', selectedTypes, typeDisplays);
            expect(handleChangeSpy).toHaveBeenNthCalledWith(
                3,
                props.control.path,
                expect.objectContaining({ typeFilter: expect.objectContaining({ selectedTypes, typeDisplays }) })
            );
        });
    });

    it('correctly transforms the data into possible values', () => {
        expect(wrapper.findComponent(MultiModeTwinlist).props().possibleValues).toEqual(
            [{
                id: 'test_1',
                text: 'test_1',
                type: {
                    id: 'StringValue',
                    text: 'String'
                }
            },
            {
                id: 'test_2',
                text: 'test_2',
                type: {
                    id: 'DoubleValue',
                    text: 'Double'
                }
            },
            {
                id: 'test_3',
                text: 'test_3',
                type: {
                    id: 'StringValue',
                    text: 'String'
                }
            }]
        );
    });

    it('correctly determines previously selected types', () => {
        expect(wrapper.findComponent(MultiModeTwinlist).props().additionalPossibleTypes).toEqual([
            {
                id: 'StringValue',
                text: 'String'
            },
            {
                id: 'IntValue',
                text: 'IntValue'
            }
        ]);
    });

    it('transforms empty anyof into empty possible values', async () => {
        props.control.schema.properties.selected.anyOf = [{ const: '', title: '' }];
        const localWrapper = await mountJsonFormsComponentWithStore(
            TwinlistInput,
            props
        );
        expect(localWrapper.findComponent(Twinlist).props().possibleValues).toEqual([]);
    });

    describe('unknown columns', () => {
        it('excludes unknown columns', () => {
            const localProps = mergeDeep(props, {
                control: {
                    data: {
                        manualFilter: {
                            includeUnknownColumns: false,
                            manuallySelected: ['A', 'B'],
                            manuallyDeselected: ['C', 'D']
                        }
                    },
                    schema: {
                        properties: {
                            selected: {
                                anyOf: [
                                    { const: 'B', title: 'B' },
                                    { const: 'D', title: 'D' },
                                    { const: 'E', title: 'E' }
                                ]
                            }
                        }
                    }
                }
            });

            mountJsonFormsComponent(TwinlistInput, localProps);
            expect(handleChangeSpy).toHaveBeenCalledWith(localProps.control.path, expect.objectContaining({
                manualFilter: expect.objectContaining({
                    manuallySelected: ['A', 'B'],
                    manuallyDeselected: ['D', 'E']
                })
            }));
        });

        it('includes unknown columns', () => {
            const localProps = mergeDeep(props, {
                control: {
                    data: {
                        manualFilter: {
                            includeUnknownColumns: true,
                            manuallySelected: ['A', 'B'],
                            manuallyDeselected: ['C', 'D']
                        }
                    },
                    schema: {
                        properties: {
                            selected: {
                                anyOf: [
                                    { const: 'B', title: 'B' },
                                    { const: 'D', title: 'D' },
                                    { const: 'E', title: 'E' }
                                ]
                            }
                        }
                    }
                }
            });
            mountJsonFormsComponent(TwinlistInput, localProps);
            expect(handleChangeSpy).toHaveBeenCalledWith(localProps.control.path, expect.objectContaining({
                manualFilter: expect.objectContaining({
                    manuallySelected: ['A', 'B', 'E'],
                    manuallyDeselected: ['D']
                })
            }));
        });
    });

    it('sets correct initial value', () => {
        expect(wrapper.findComponent(MultiModeTwinlist).vm.initialPattern).toBe(
            props.control.data.patternFilter.pattern
        );
        expect(wrapper.findComponent(MultiModeTwinlist).vm.initialSelectedTypes).toStrictEqual(
            props.control.data.typeFilter.selectedTypes
        );
        expect(wrapper.findComponent(MultiModeTwinlist).vm.initialManuallySelected).toStrictEqual(
            props.control.data.manualFilter.manuallySelected
        );
        expect(wrapper.findComponent(MultiModeTwinlist).vm.initialMode).toBe('manual');
    });


    it('sets correct label', () => {
        expect(wrapper.find('label').text()).toBe(props.control.label);
    });

    it('disables twinlist when controlled by a flow variable', () => {
        const localprops = JSON.parse(JSON.stringify(props));
        localprops.control.rootSchema
            .flowVariablesMap[props.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };
        const localWrapper = mountJsonFormsComponent(TwinlistInput, localprops);
        expect(localWrapper.vm.disabled).toBeTruthy();
    });

    it('moves missing values correctly', async () => {
        const dirtySettingsMock = vi.fn();
        const localProps = {
            ...props,
            control: {
                ...props.control,
                data: { ...props.control.data,
                    manualFilter: {
                        manuallySelected: ['missing'],
                        manuallyDeselected: ['test_1', 'test_2', 'test_3'],
                        includeUnknownColumns: false
                    } }
            }
        };
        const localWrapper = await mountJsonFormsComponentWithStore(TwinlistInput, localProps, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        expect(localWrapper.props().control.data.manualFilter).toMatchObject({ manuallySelected: ['missing'] });
        await localWrapper.findComponent(Twinlist).find({ ref: 'moveAllLeft' }).trigger('click');
        expect(onChangeSpy).toBeCalledWith({
            manualFilter: { manuallySelected: [], manuallyDeselected: ['test_1', 'test_2', 'test_3'] },
            selected: []
        });
        await localWrapper.findComponent(Twinlist).find({ ref: 'moveAllRight' }).trigger('click');
        expect(onChangeSpy).toBeCalledWith({
            manualFilter: { manuallySelected: ['test_1', 'test_2', 'test_3'], manuallyDeselected: [] },
            selected: ['test_1', 'test_2', 'test_3']
        });
    });

    it('does not render content of TwinlistInput when visible is false', async () => {
        wrapper.setProps({ control: { ...props.control, visible: false } });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(false);
    });
});
