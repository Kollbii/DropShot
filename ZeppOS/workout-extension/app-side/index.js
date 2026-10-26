import { BaseSideService } from '@zeppos/zml/base-side'

// This service runs in the Zepp phone app.  The watch never talks directly to
// the Android scoreboard; it only submits validated intent commands here.
const PHONE_BRIDGE_URL = 'http://127.0.0.1:28282/zepp-command'

AppSideService(BaseSideService({
  async onRequest(request, reply) {
    if (request.method !== 'SQUASH_COMMAND' || !request.params?.command) {
      reply(null, { accepted: false, stage: 'INVALID' })
      return
    }

    try {
      // BaseSideService exposes fetch specifically for requests executed in
      // the Zepp companion on the phone.  Keeping this on the companion is
      // essential: the Workout Extension itself has no direct access to the
      // Android app's loopback address.
      const response = await this.fetch({
        url: PHONE_BRIDGE_URL,
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: request.params.command }),
        timeout: 5000
      })
      const data = typeof response.body === 'string'
        ? JSON.parse(response.body)
        : response.body
      reply(null, { ...(data || { accepted: false }), stage: 'PHONE' })
    } catch (error) {
      // A disconnected watch or phone is expected. Do not retry scoring:
      // replaying a point later could change the result incorrectly.
      this.error('Phone bridge failed: %s', error?.message || 'unknown error')
      reply(null, { accepted: false, stage: 'HTTP' })
    }
  }
}))
