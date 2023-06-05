import { rankWith, isControl } from '@jsonforms/core';
import { priorityRanks, inputFormats } from '../constants';
import CredentialsInput from '../uiComponents/CredentialsInput.vue';

export const credentialsTester = (uischema) => isControl(uischema) && uischema.options?.format === inputFormats.credentials;


export const credentialsRenderer = {
    renderer: CredentialsInput,
    tester: rankWith(priorityRanks.default, credentialsTester)
};
