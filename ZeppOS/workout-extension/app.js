// BaseApp initializes ZML's shared BLE messaging transport.  Without it a
// DataWidget can draw and receive clicks, but BasePage.request() has no
// companion channel to use.
import { BaseApp } from '@zeppos/zml/base-app'

App(BaseApp({
  globalData: {},
  onCreate() {},
  onDestroy() {}
}))
