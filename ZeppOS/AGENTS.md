# AGENTS.md — Zepp OS watch apps

These rules apply to `ZeppOS/`. They add to the root [AGENTS.md](../AGENTS.md).

## 1. Structure

| Path | Content |
|---|---|
| `app.json` | Main watch app "Punkty Squash" (appId `20001`). API 4.0 to 4.2. |
| `page/index.js` | The watch screen. 4 large buttons in a 2 × 2 grid. |
| `app-side/index.js` | App Side Service in the Zepp phone app. It sends a `POST` to `http://127.0.0.1:28282/zepp-command`. |
| `workout-extension/` | Workout data page (appId `20002`). The same 4 commands. |
| `dist/*.zab` | Built packages. Do not edit. |
| `node_modules/` | Dependencies (`@zeppos/zml`). Ignored by git. |

Tools: Node.js 14 or later, Zeus CLI (`npm i @zeppos/zeus-cli -g`). Build: `zeus build`. Preview: `zeus preview`.

## 2. Rules

1. Send only the 4 allowed commands: `POINT_1`, `POINT_2`, `UNDO`, `RESET`. The phone rejects all other values.
2. Do not retry, queue, or replay a command after a failure. A late point changes the score incorrectly.
3. Send commands only to `127.0.0.1:28282`. Do not use another host, HTTPS proxy, or cloud relay.
4. Do not add browser headers (`Origin`, `Referer`). The phone bridge rejects them with `403`.
5. Do not request new permissions in `app.json` without a request.
6. Keep each button large. The watch screen is small and the user plays sport.
7. Use high contrast between the button text and the button color.
8. Increase `version.code` by 1 and `version.name` by a patch step for each watch release.

## 3. Caveats

1. The watch sends no token. The phone token check is off. Any app on the phone can send a command to `127.0.0.1:28282` while the Match screen is open.
2. The bridge uses plain HTTP on loopback. `network_security_config.xml` permits cleartext only for `127.0.0.1`.
3. The README files are in Polish.
4. A real watch preview needs a Zepp Developer account. Agents cannot test on a watch. Report this to the user.

