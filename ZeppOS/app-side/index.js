import { BaseSideService } from '@zeppos/zml/base-side'

const PHONE_BRIDGE_URL = 'http://127.0.0.1:28282/zepp-command'

AppSideService(BaseSideService({
  async onRequest(request, reply) {
    if (request.type !== 'SQUASH_COMMAND') {
      reply(null, { accepted: false })
      return
    }
    try {
      const response = await fetch({
        url: PHONE_BRIDGE_URL,
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: request.params.command })
      })
      const data = typeof response.body === 'string' ? JSON.parse(response.body) : response.body
      reply(null, data || { accepted: false })
    } catch (error) {
      // A temporarily disconnected watch/phone is normal.  Never retry a score command.
      reply(null, { accepted: false })
    }
  }
}))
