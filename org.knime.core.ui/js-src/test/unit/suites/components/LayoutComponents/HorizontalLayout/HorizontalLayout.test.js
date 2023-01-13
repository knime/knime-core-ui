import { mountJsonFormsComponent, initializesJsonFormsLayout } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import HorizontalLayout from '@/components/LayoutComponents/HorizontalLayout';

describe('HorizontalLayout.vue', () => {
    let wrapper, defaultPropsData;

    beforeEach(async () => {
        defaultPropsData = {
            layout: {
                cells: [],
                path: 'view.referenceLines',
                schema: {
                    properties: {
                        size: {
                            type: 'integer',
                            title: 'Size'
                        },
                        title: {
                            type: 'string',
                            title: 'title',
                            default: 'title'
                        }
                    }
                },
                uischema: {
                    type: 'HorizontalLayout',
                    elements: [
                        {
                            type: 'Control',
                            scope: '#/properties/size'
                        },
                        {
                            type: 'Control',
                            scope: '#/properties/title'
                        }
                    ]
                },
                visible: true
            }
        };
        wrapper = await mountJsonFormsComponent(HorizontalLayout, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(HorizontalLayout).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsLayout(wrapper);
    });

    it('checks that it is not rendered if it is an advanced setting', async () => {
        defaultPropsData.layout.uischema.options = { isAdvanced: true };
        wrapper = await mountJsonFormsComponent(HorizontalLayout, defaultPropsData);
        expect(wrapper.getComponent(HorizontalLayout).isVisible()).toBe(false);
    });

    it('checks that it is rendered if it is an advanced setting and advanced settings are shown', async () => {
        defaultPropsData.layout.uischema.options = { isAdvanced: true };
        wrapper = await mountJsonFormsComponent(HorizontalLayout, defaultPropsData, true);
        expect(wrapper.getComponent(HorizontalLayout).isVisible()).toBe(true);
    });

    it('adds the property to elements that their description should be teleported', () => {
        expect(wrapper.vm.elements).toEqual([
            {
                type: 'Control',
                scope: '#/properties/size',
                options: {
                    teleportDescription: true
                }
            },
            {
                type: 'Control',
                scope: '#/properties/title',
                options: {
                    teleportDescription: true
                }
            }
        ]);
    });

    it('uses a flex value of 1 for each element when no ratio is specified', () => {
        expect(wrapper.vm.flexValues).toEqual([1, 1]);
    });

    it('uses a given flex values for each element when specified', async () => {
        const ratios = ['1 0 50px', '2 0 100px'];
        defaultPropsData.layout.uischema.options = { ratios };
        wrapper = await mountJsonFormsComponent(HorizontalLayout, defaultPropsData);
        expect(wrapper.vm.flexValues).toEqual(ratios);
    });
});
