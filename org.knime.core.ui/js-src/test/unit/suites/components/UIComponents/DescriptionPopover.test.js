import { mount } from '@vue/test-utils';

import DescriptionPopover from '@/components/UIComponents/DescriptionPopover.vue';

describe('DescriptionPopover.vue', () => {
    let toggleSpy, closeSpy;

    beforeAll(() => {
        toggleSpy = jest.spyOn(DescriptionPopover.methods, 'toggle');
        closeSpy = jest.spyOn(DescriptionPopover.methods, 'close');
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the default state', () => {
        const wrapper = mount(DescriptionPopover);

        expect(wrapper.find('.popover').exists()).toBeTruthy();
        expect(wrapper.find('.button').exists()).toBeTruthy();
        expect(wrapper.find('svg').exists()).toBeTruthy();
        expect(wrapper.find('.box').exists()).toBeTruthy();
        expect(wrapper.find('.box').element.style.display).toBe('none');
    });

    it('renders the content box when expanded', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: false, html: 'foo' } });
        wrapper.setData({ expanded: true });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved

        expect(wrapper.find('.popover').exists()).toBeTruthy();
        expect(wrapper.find('.button').exists()).toBeTruthy();
        expect(wrapper.find('svg').exists()).toBeTruthy();
        expect(wrapper.find('.box').exists()).toBeTruthy();
        expect(wrapper.find('.box').element.style.display).not.toBe('none');
        expect(wrapper.find('.content').exists()).toBeTruthy();
        expect(wrapper.find('.content').text()).toContain('foo');
    });

    it('shows description button when hovering', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: false } });
        
        const descriptionIcon = wrapper.find('.button');
        expect(descriptionIcon.exists()).toBeTruthy();
        expect(descriptionIcon.isVisible()).toBeFalsy();
        
        wrapper.setProps({ hover: true }); // set hover to true
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(descriptionIcon.isVisible()).toBeTruthy();
    });

    it('expands and closes on mouseup', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.box').element.style.display).toBe('none');

        // opens content
        wrapper.find('.button').trigger('mouseup');
        expect(toggleSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).not.toBe('none');

        // closes content
        wrapper.find('.button').trigger('mouseup');
        expect(toggleSpy).toHaveBeenCalledTimes(2);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).toBe('none');
    });

    it('expands and closes via space', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.box').element.style.display).toBe('none');

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        expect(toggleSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).not.toBe('none');

        // closes content
        wrapper.find('.button').trigger('keydown.space');
        expect(toggleSpy).toHaveBeenCalledTimes(2);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).toBe('none');
    });

    it('closes via escape', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.box').element.style.display).toBe('none');

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        expect(closeSpy).toHaveBeenCalledTimes(0);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).not.toBe('none');

        // closes content
        wrapper.find('.button').trigger('keydown.esc');
        expect(closeSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).toBe('none');
    });

    it('closeUnlessHover closes only when not hovering', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: true } });
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.box').element.style.display).toBe('none');

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).not.toBe('none');

        wrapper.vm.closeUnlessHover();

        // closes content
        expect(wrapper.find('.box').element.style.display).not.toBe('none');

        wrapper.setProps({ hover: false });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        wrapper.vm.closeUnlessHover();

        // closes content
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.box').element.style.display).toBe('none');
    });
});
