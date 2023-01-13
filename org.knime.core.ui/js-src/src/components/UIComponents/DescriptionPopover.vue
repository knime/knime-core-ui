<script>
import FunctionButton from '~/webapps-common/ui/components/FunctionButton.vue';
import Description from '~/webapps-common/ui/components/Description.vue';
import DescriptionIcon from '~/webapps-common/ui/assets/img/icons/circle-help.svg?inline';
import { mixin as clickaway } from 'vue-clickaway2';
import { Portal } from 'portal-vue';
import { createPopper } from '@popperjs/core';

const popperSkidding = 10;
const popperDistance = 10;

export default {
    components: {
        FunctionButton,
        Description,
        DescriptionIcon,
        Portal
    },
    mixins: [clickaway],
    props: {
        html: {
            default: null,
            type: String
        },
        hover: {
            default: false,
            type: Boolean
        },
        teleportDescription: {
            default: false,
            type: Boolean
        }
    },
    data() {
        return {
            expanded: false,
            popperInstance: null
        };
    },
    mounted() {
        this.$nextTick(() => this.activatePopper());
    },
    beforeDestroy() {
        this.destroyPopper();
    },
    methods: {
        activatePopper() {
            const tooltipEl = this.$refs.box;
            const referenceEl = this.$refs['popover-button'].$el;

            this.popperInstance = createPopper(referenceEl, tooltipEl, {
                placement: 'top-end',
                modifiers: [
                    {
                        name: 'preventOverflow',
                        options: {
                            mainAxis: false
                        }
                    },
                    {
                        name: 'offset',
                        options: {
                            offset: [popperSkidding, popperDistance]
                        }
                    }
                ]
            });
        },
        destroyPopper() {
            if (this.popperInstance) {
                this.popperInstance.destroy();
            }
        },
        toggle() {
            this.expanded = !this.expanded;
            this.$nextTick(() => this.popperInstance.update());
        },
        close() {
            this.expanded = false;
            // emit event to notify parent to set the hover prop to false
            this.$emit('close');
        },
        closeUnlessHover() {
            if (!this.hover) {
                this.close();
            }
        },
        mouseHoverDescription(hoverDescription) {
            this.$emit('mouseHoverDescription', hoverDescription);
        }
    }
};
</script>

<template>
  <div
    v-show="expanded || hover"
    v-on-clickaway="closeUnlessHover"
    class="popover"
  >
    <!-- use mouseup instead of click as the click event fires twice on key input in Firefox-->
    <FunctionButton
      ref="popover-button"
      title="Click for more information"
      class="button"
      :active="expanded"
      @mouseup.native.stop="toggle"
      @keydown.native.space.stop="toggle"
      @keydown.native.esc.stop="close"
    >
      <DescriptionIcon />
    </FunctionButton>
    <Portal
      to="inputDescriptionPopover"
      :disabled="!teleportDescription"
    >
      <div
        v-show="expanded"
        ref="box"
        :class="['box', { 'teleport': teleportDescription }]"
        @mouseover="mouseHoverDescription(true)"
        @mouseleave="mouseHoverDescription(false)"
      >
        <div
          data-popper-arrow
          class="arrow"
        />
        <Description
          :text="html"
          render-as-html
          class="content"
        />
      </div>
    </Portal>
  </div>
</template>

<style lang="postcss" scoped>
.popover {
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
  width: 100%;
  position: absolute;
  top: 0;

  & .button {
    pointer-events: auto;
    width: var(--description-button-size);
    height: var(--description-button-size);
    padding: 0;

    & svg {
      width: var(--description-button-size);
      height: var(--description-button-size);
    }
  }
}

.box {
  --popover-oversize: 10px; /* oversize to the left and right of the content box */

  z-index: 3; /* stack expanded popover on top of dialog */
  width: calc(100% + 2 * var(--popover-oversize));
  position: absolute;
  background: var(--knime-white);
  box-shadow: 0 2px 10px 0 var(--knime-gray-dark-semi);

  &.teleport {
    /* -40px, because .box is teleported to .dialog.form that has 20px padding on both sides */
    width: calc(100% - 40px);
  }

  &[data-popper-placement^='top'] .arrow{
    bottom: -5px;
  }

  &[data-popper-placement^='bottom'] .arrow{
    top: -5px;
  }

  & .arrow, & .arrow::before {
    position: absolute;
    width: 15px;
    height: 15px;
    background: inherit;
  }

  & .arrow {
    visibility: hidden;
  
    &::before {
      transform: rotate(45deg);
      content: '';
      visibility: visible;
    }
  }

  & .content {
    max-height: 300px;
    overflow: auto;
    pointer-events: auto;
    padding: 15px;
    font-size: 13px;
    line-height: 18.78px; /* Description component line-height-to-font-size-ratio of 26/18 times font size of 13 */
    color: var(--knime-masala);
  }
}
</style>
