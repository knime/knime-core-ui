import { rankWith, isStringControl } from '@jsonforms/core';
import { priorityRanks, inputFormats } from '../constants';
import MarkdownInput from '../uiComponents/MarkdownInput.vue';

export const markdownTester = (uischema, schema) => isStringControl && uischema.options?.format === inputFormats.markdown;

export const markdownRenderer = {
    renderer: MarkdownInput,
    tester: rankWith(100000, markdownTester)
};
