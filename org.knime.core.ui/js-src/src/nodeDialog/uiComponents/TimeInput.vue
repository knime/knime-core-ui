<script lang="ts">

export type OnlyLocalTime = {
  hours: number;
  minutes: number;
  seconds: number;
  milliseconds: number;
};

export const onlyLocalTimeToString = (time: OnlyLocalTime) =>
  `${time.hours}:${time.minutes}:${time.seconds}.${time.milliseconds}`;

export const onlyLocalTimeFromString = (time: string): OnlyLocalTime => {
  console.log("time ",time)
  const regex = /^(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/;

  const matches = regex.exec(time);

  if (matches && matches.length >= 2) {
    const hours = parseInt(matches[1], 10);
    const minutes = parseInt(matches[2], 10);
    const seconds = matches[3] ? parseInt(matches[3], 10) : 0;
    const nanoSeconds = matches[4]
      ? parseInt(matches[4].padEnd(9, "0"), 10)
      : 0;

    return {
      hours,
      minutes,
      seconds,
      milliseconds: Math.floor(nanoSeconds / 1e6),
    };
  } else {
    throw new Error(`Invalid time format: ${time}`);
  }
};
</script>

<script setup lang="ts">

import { TimePartInput }
  from "@knime/components";
import {computed} from "vue";


const localTimeModel = defineModel<OnlyLocalTime>( {
  required: true,
});

const localTime = computed(
  {
    get: () => localTimeModel.value,
    set: (value: OnlyLocalTime) => {
      console.log("set ",value)
      localTimeModel.value = value;

    },
  }
);

type TimeProps = {
  id?: string | null;
  isValid?: boolean;
  required?: boolean;
  disabled?: boolean;
  compact?: boolean;
}

withDefaults(defineProps<TimeProps>(), {
  id: null,
  isValid: true,
  required: false,
  disabled: false,
  compact: false,
});

</script>

<template>
  <div class="date-time-input">
    <div :class="['time', { compact }]">
      <TimePartInput
        ref="hours"
        class="time-part"
        type="integer"
        :compact="compact"
        :min="0"
        :max="23"
        :min-digits="2"
        v-model="localTime.hours"
        :disabled="disabled"
      />
      <span class="time-colon">:</span>
      <TimePartInput
        ref="minutes"
        class="time-part"
        type="integer"
        :compact="compact"
        :min="0"
        :max="59"
        :min-digits="2"
        v-model="localTime.minutes"
        :disabled="disabled"
      />
      <span class="time-colon">:</span>
      <TimePartInput
        ref="seconds"
        class="time-part"
        type="integer"
        :compact="compact"
        :min="0"
        :max="59"
        :min-digits="2"
        v-model="localTime.seconds"
        :disabled="disabled"
      />
      <span class="time-colon">.</span>
      <TimePartInput
        ref="milliseconds"
        class="time-part"
        type="integer"
        :compact="compact"
        :min="0"
        :max="999"
        :min-digits="3"
        v-model="localTime.milliseconds"
        :disabled="disabled"
      />
    </div>
  </div>
</template>

<style lang="postcss">
@import url("v-calendar/dist/style.css");
</style>

<style lang="postcss" scoped>
.date-time-input {
  display: flex;
  width: auto;
  flex-wrap: wrap;
  gap: 10px 20px;

  /* time */
  & .time {
    display: flex;
    width: auto;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 0;

    & .time-part {
      width: 5rem;
    }

    &.two-lines {
      width: 100%;
    }

    & .time-colon {
      padding: 5px;
    }

    & span {
      display: flex;
      width: auto;
      flex-wrap: nowrap;
    }

    & .timezone {
      flex: 0 1 260px;
    }
  }

  & .date-picker {
    &.disabled {
      opacity: 0.5;
    }

    &.compact {
      height: var(--single-line-form-height-compact);

      & input {
        height: calc(
          var(--single-line-form-height-compact) - 2 * var(--form-border-width)
        );
      }

      & .button {
        height: calc(
          var(--single-line-form-height-compact) - 2 * var(--form-border-width)
        );
      }
    }

    /* v-calendar theme
       new 1.1+ theme with css-vars see https://github.com/nathanreyes/v-calendar/blob/master/src/styles/base.css */

    /* remove caret (triangle) */
    & :deep(.vc-popover-caret) {
      display: none;
    }

    /* no space between input and popover */
    & :deep(.vc-popover-content-wrapper) {
      --popover-vertical-content-offset: 0;
      --popover-horizontal-content-offset: 0;

      /* default animation is too slow */
      --popover-transition-time: 0.1s ease-in-out;
    }

    & :deep(.vc-day-content.is-disabled) {
      opacity: 0.5;
    }

    & :deep(.vc-popover-content) {
      /* popover box shadow */
      box-shadow: var(--shadow-elevation-1);
    }

    & :deep(.vc-arrow):not(:hover),
    & :deep(.vc-title) {
      background: var(--vc-white);
    }

    & :deep(.vc-container) {
      /* remove roundness */
      --vc-rounded: 0;
      --vc-rounded-lg: 0;

      /* color prop value (in our case 'masala' see above) and vc-COLOR-PROP-NAME need to be defined */
      --masala-100: var(--theme-date-input-accent-100);
      --masala-200: var(--theme-date-input-accent-200);
      --masala-300: var(--theme-date-input-accent-300);
      --masala-400: var(--theme-date-input-accent-400);
      --masala-500: var(--theme-date-input-accent-500);
      --masala-600: var(--theme-date-input-accent-600);
      --masala-700: var(--theme-date-input-accent-700);
      --masala-800: var(--theme-date-input-accent-800);
      --masala-900: var(--theme-date-input-accent-900);

      & .vc-masala {
        --vc-accent-100: var(--masala-100);
        --vc-accent-200: var(--masala-200);
        --vc-accent-300: var(--masala-300);
        --vc-accent-400: var(--masala-400);
        --vc-accent-500: var(--masala-500);
        --vc-accent-600: var(--masala-600);
        --vc-accent-700: var(--masala-700);
        --vc-accent-800: var(--masala-800);
        --vc-accent-900: var(--masala-900);
      }

      /* not themed items */
      & .vc-day-content:hover {
        background: var(--theme-date-input-day-content-background);
      }

      /* non "color" prop colors which are used regardless of color prop value */
      --vc-white: var(--theme-date-input-white);
      --vc-black: var(--theme-date-input-black);

      --vc-gray-100: var(--theme-date-input-gray-100);
      --vc-gray-200: var(--theme-date-input-gray-200);
      --vc-gray-300: var(--theme-date-input-gray-300);
      --vc-gray-400: var(--theme-date-input-gray-400);
      --vc-gray-500: var(--theme-date-input-gray-500);
      --vc-gray-600: var(--theme-date-input-gray-600);
      --vc-gray-700: var(--theme-date-input-gray-700);
      --vc-gray-800: var(--theme-date-input-gray-800);
      --vc-gray-900: var(--theme-date-input-gray-900);

      border: 1px solid var(--vc-gray-400);
    }

    /* -- end v-calendar 'theme' */

    /* input wrapper style */
    max-width: 9rem;
    min-width: 7.5rem;
    position: relative;
    border: var(--form-border-width) solid var(--theme-date-input-border-color);

    &:focus-within {
      border-color: var(--theme-date-input-border-focus-color);
    }

    /* stylelint-disable-next-line no-descending-specificity */
    & input {
      font-size: 13px;
      font-weight: 300;
      letter-spacing: inherit;
      height: calc(
        var(--single-line-form-height) - 2 * var(--form-border-width)
      );
      line-height: normal;
      border: 0;
      margin: 0;
      padding: 0 10px;
      border-radius: 0;
      width: calc(100% - 32px);
      outline: none;
      background-color: var(--theme-date-input-input-background);

      /* css3 invalid state */
      &:invalid {
        box-shadow: none; /* override default browser styling */
      }

      &:disabled {
        opacity: 0.5;
      }

      &:hover:not(:focus, :disabled) {
        background-color: var(--theme-date-input-input-hover-background);
      }
    }

    & .invalid-marker {
      position: absolute;
      display: block;
      width: 3px;
      left: -1px;
      top: 0;
      bottom: 0;
      z-index: 1;
      background-color: var(--theme-color-error);
    }

    /* stylelint-disable-next-line no-descending-specificity */
    & .button {
      position: absolute;
      z-index: 1;
      width: 32px;
      height: calc(
        var(--single-line-form-height) - 2 * var(--form-border-width)
      );
      padding-left: 10px;
      padding-right: 9px;

      &:hover:not(.disabled) {
        cursor: pointer;
        background-color: var(--theme-date-input-input-hover-background);
      }

      & svg {
        width: 100%;
        height: 100%;
        stroke-width: 1.5px;
      }

      &.disabled {
        opacity: 0.5;
      }
    }

    & .button:active:not(.disabled),
    & .button.active:not(.disabled) {
      color: var(--theme-date-input-white);
      background-color: var(--theme-date-input-button-active-color);

      & svg {
        stroke: var(--theme-date-input-white);
      }
    }
  }
}
</style>
