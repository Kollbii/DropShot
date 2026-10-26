import { BasePage } from '@zeppos/zml/base-page'
import { createWidget, widget, align } from '@zos/ui'
import { getDeviceInfo } from '@zos/device'
import { setWakeUpRelaunch } from '@zos/display'

// The font installed on Active 2 reliably supports these ASCII symbols.
// Blue = player 1, red = player 2, << = undo, R = reset.
const BUTTONS = [
  { symbol: '+1', command: 'POINT_1', normal: 0x1479d1, pressed: 0x0c559c, rim: 0x2897ee, size: 60 },
  { symbol: '+2', command: 'POINT_2', normal: 0xd8424f, pressed: 0x9f2531, rim: 0xf1636e, size: 60 },
  { symbol: '<<', command: 'UNDO', normal: 0x3c4b59, pressed: 0x26323d, rim: 0x627487, size: 66 },
  { symbol: 'R', command: 'RESET', normal: 0x157c68, pressed: 0x0d594b, rim: 0x28a58c, size: 82 }
]

function addButton({ x, y, w, h, radius, symbol, command, normal, pressed, rim, size }, onClick) {
  // Three layers create a restrained, high-contrast raised-button effect.
  createWidget(widget.FILL_RECT, {
    x: x + 3, y: y + 7, w, h, radius,
    color: 0x000000, alpha: 210
  })
  createWidget(widget.FILL_RECT, {
    x, y, w, h, radius,
    color: rim
  })
  createWidget(widget.BUTTON, {
    x: x + 2, y: y + 2, w: w - 4, h: h - 6, radius: radius - 3,
    text: symbol,
    color: 0xffffff,
    text_size: size,
    align_h: align.CENTER_H,
    align_v: align.CENTER_V,
    normal_color: normal,
    press_color: pressed,
    click_func: () => onClick(command)
  })
}

Page(BasePage({
  build() {
    const { width, height } = getDeviceInfo()

    createWidget(widget.FILL_RECT, {
      x: 0, y: 0, w: width, h: height,
      color: 0x080b10
    })

    // Four zones extend from the centre to the physical edge of the display.
    // The hardware clips their outer corners to the Active 2's round screen,
    // producing four large, easy-to-hit quarters of a disc.
    const gap = Math.max(6, Math.round(width * 0.017))
    const buttonWidth = Math.floor((width - gap) / 2)
    const buttonHeight = Math.floor((height - gap) / 2)
    const radius = 0
    const positions = [
      { x: 0, y: 0 },
      { x: buttonWidth + gap, y: 0 },
      { x: 0, y: buttonHeight + gap },
      { x: buttonWidth + gap, y: buttonHeight + gap }
    ]

    BUTTONS.forEach((button, index) => {
      addButton({ ...button, ...positions[index], w: buttonWidth, h: buttonHeight, radius },
        command => this.sendCommand(command))
    })

    // Zepp OS owns the real timeout for battery safety.  After waking the
    // display it returns straight to this remote instead of the watch face.
    try {
      setWakeUpRelaunch({ relaunch: true })
    } catch (_) {}
  },

  sendCommand(command) {
    // No retry: replaying a delayed point after reconnecting would be unsafe.
    this.request({ type: 'SQUASH_COMMAND', params: { command } }).catch(() => {})
  }
}))
