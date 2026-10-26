import { BasePage } from '@zeppos/zml/base-page'
import { createWidget, widget, align, event, prop } from '@zos/ui'
import { px } from '@zos/utils'
import { Vibrator, VIBRATOR_SCENE_SHORT_LIGHT } from '@zos/sensor'

const DESIGN = 466
const GAP = 8
const HALF = Math.floor((DESIGN - GAP) / 2)
const vibrator = new Vibrator()

const BUTTONS = [
  { symbol: '+1', command: 'POINT_1', normal: 0x1479d1, pressed: 0x0c559c, rim: 0x2897ee, size: 60, x: 0, y: 0 },
  { symbol: '+2', command: 'POINT_2', normal: 0xd8424f, pressed: 0x9f2531, rim: 0xf1636e, size: 60, x: HALF + GAP, y: 0 },
  { symbol: '<<', command: 'UNDO', normal: 0x3c4b59, pressed: 0x26323d, rim: 0x627487, size: 66, x: 0, y: HALF + GAP },
  { symbol: 'R', command: 'RESET', normal: 0x157c68, pressed: 0x0d594b, rim: 0x28a58c, size: 82, x: HALF + GAP, y: HALF + GAP }
]

function addButton(button, onClick) {
  const x = px(button.x)
  const y = px(button.y)
  const w = px(HALF)
  const h = px(HALF)

  // Layers preserve the raised appearance without sacrificing touch area.
  createWidget(widget.FILL_RECT, {
    x: x + px(3), y: y + px(6), w, h, radius: 0,
    color: 0x000000, alpha: 210
  })
  createWidget(widget.FILL_RECT, {
    x, y, w, h, radius: 0,
    color: button.rim
  })
  // Workout Extensions do not invoke BUTTON.click_func.  Their supported
  // interaction is a normal widget CLICK event, so this colored surface is
  // intentionally a FILL_RECT rather than a BUTTON.
  const panelSurface = createWidget(widget.FILL_RECT, {
    x: x + px(2), y: y + px(2), w: w - px(4), h: h - px(6), radius: 0,
    color: button.normal
  })
  const symbol = createWidget(widget.TEXT, {
    x: x + px(2), y: y + px(2), w: w - px(4), h: h - px(6),
    text: button.symbol,
    color: 0xffffff,
    text_size: px(button.size),
    align_h: align.CENTER_H,
    align_v: align.CENTER_V
  })
  // Keep a practically transparent FILL_RECT as the highest layer.  On Active
  // 2 Workout Extensions this widget reliably receives the supported CLICK
  // event, whereas BUTTON callbacks are intentionally disabled by Zepp OS.
  const touchLayer = createWidget(widget.FILL_RECT, {
    x: x + px(2), y: y + px(2), w: w - px(4), h: h - px(6), radius: 0,
    color: 0x000000, alpha: 1
  })
  touchLayer.addEventListener(event.CLICK_DOWN, () => {
    const panel = { x: x + px(2), y: y + px(2), w: w - px(4), h: h - px(6), radius: 0 }
    const flash = color => {
      panelSurface.setProperty(prop.MORE, { ...panel, color })
      setTimeout(() => panelSurface.setProperty(prop.MORE, { ...panel, color: button.normal }), 1000)
    }
    panelSurface.setProperty(prop.MORE, { ...panel, color: button.pressed })
    try { vibrator.start({ mode: VIBRATOR_SCENE_SHORT_LIGHT }) } catch (_) {}
    setTimeout(() => panelSurface.setProperty(prop.MORE, { ...panel, color: button.normal }), 140)
    const request = onClick(button.command)
    // Workout Extensions cannot reliably display a system toast.  Flashing
    // the button itself gives an unmissable, temporary diagnostics signal:
    // green=Android accepted, orange=Zepp companion could not reach Android,
    // purple=watch could not reach the companion, red=Android rejected.
    if (request && typeof request.then === 'function') {
      request
        .then(result => {
          if (result?.accepted) flash(0x19b96b)
          else if (result?.stage === 'HTTP') flash(0xf08b32)
          else flash(0xd8455d)
        })
        .catch(() => flash(0x9b59b6))
    }
  })
}

DataWidget(BasePage({
  build() {
    createWidget(widget.FILL_RECT, {
      x: 0, y: 0, w: px(DESIGN), h: px(DESIGN),
      color: 0x080b10
    })
    BUTTONS.forEach(button => addButton(button, command => this.sendCommand(command)))
  },

  sendCommand(command) {
    // The Android app retains sole ownership of match state and history.
    return this.request({
      method: 'SQUASH_COMMAND',
      params: { command }
    }, { timeout: 8000 })
  }
}))
