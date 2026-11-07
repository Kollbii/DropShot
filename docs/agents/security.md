# Security standard

This standard applies to all code in this repository.
It uses these references:

| Reference | Version | Scope |
|---|---|---|
| OWASP Top 10 | 2021 | General web and service risks (applies to the loopback bridge and data handling) |
| OWASP Mobile Top 10 | 2024 | Mobile app risks |
| OWASP MASVS | v2.1 | Mobile verification requirements (control groups below) |
| OWASP MASTG | current | Test procedures for MASVS |
| OWASP ASVS | 4.0.3 | Validation, access control, cryptography, logging |
| CWE Top 25 | current | Weakness names in review notes |

Tag a security-relevant comment with the control ID, as the current code does: `[MASVS-STORAGE]`, `[ASVS V5.1]`.

---

## 1. Threat model (short)

| Asset | Threat | Current control |
|---|---|---|
| Match score during play | Late or forged command changes the score | No retry on the watch; command allowlist; loopback bind; `Host` check; browser header block |
| Match history and profiles | Read by another app or by backup | AES-256-GCM via AndroidKeyStore; `allowBackup=false`; data extraction rules |
| Reward unlocks | Unlock without a roll | Signed single-use tickets; profile binding; encrypted inventory |
| App integrity | Tapjacking, intent injection | `filterTouchesWhenObscured`; `exported=false`; Intent extra validation |
| Logs | Leak of tokens or personal data | `SafeLog` scrub; R8 strips `Log.v/d/i` in release |

Out of scope: a rooted device, a modified APK, and physical access to an unlocked device.
State this limit in each security report.

## 2. MASVS v2 controls

### MASVS-STORAGE

1. Store profiles, history, and unlocks only through `SecureStorageEngine.putEncryptedString`.
2. Do not write personal data to external storage, `cache/`, logs, or the clipboard.
3. Keep `android:allowBackup="false"`, `fullBackupContent="false"`, and `data_extraction_rules.xml`.
4. When you add a new store, use a unique `(prefFile, key)` pair. The pair is the AAD.

### MASVS-CRYPTO

1. Use AES-256-GCM with a random 96-bit IV and a 128-bit tag. Do not use ECB, CBC without MAC, or a fixed IV.
2. Keep keys in AndroidKeyStore. Do not hardcode a key. Do not derive a key from a constant.
3. Use `SecureRandom` for IDs, tokens, nonces, and game rolls.
4. Use `MessageDigest.isEqual` for secret compares (constant time).
5. Use HMAC-SHA256 for signatures. Do not use plain SHA for authentication.
6. Zero `byte[]` buffers that hold plaintext after use.

### MASVS-AUTH

1. The app has no user accounts. Do not add authentication without a request.
2. If you enable the bridge token, generate it with `SecureRandom` (128 bits or more) at each server start.
3. Do not add client-side "admin", "developer", or "unlock all" roles (OWASP A01, Broken Access Control).

### MASVS-NETWORK

1. Cleartext is blocked. Only `127.0.0.1` is permitted, for the watch bridge.
2. Bind servers to `127.0.0.1` only. Do not bind to `0.0.0.0` or a Wi-Fi interface.
3. If you add a remote host, use TLS 1.2 or later with system trust anchors. Consider certificate pinning.
4. Set socket timeouts and size limits for each request (headers, body, line length).

### MASVS-PLATFORM

1. Set `android:exported="false"` on each new activity, service, receiver, and provider. Only `MainActivity` is exported (launcher).
2. Validate all Intent extras. Profile IDs must match `^[a-zA-Z0-9_-]{1,64}$`.
3. Parse JSON extras in `try`/`catch`. Clamp all numbers. Reject unknown values.
4. Set `setFilterTouchesWhenObscured(true)` on buttons that change or delete data.
5. Do not use `WebView`. If a task needs it, disable JavaScript and file access by default.
6. Do not use implicit Intents for internal navigation.

### MASVS-CODE

1. Validate input at each trust boundary with `InputValidator`:
   names (strip control and BiDi characters, max. 32 characters), IDs, rules, scores, durations.
2. Cap collection sizes from untrusted data (`MAX_RECORDED_POINTS = 5000`, max. 100 matches).
3. Prevent division by zero in charts and statistics (`pointsToWinSet >= 1`).
4. Keep `minSdk 26` or higher. Keep `targetSdk` at the current Play requirement.
5. Do not add a dependency without a request. Check each new dependency for known CVEs.

### MASVS-RESILIENCE

1. Keep R8 minify and resource shrink on for release.
2. Keep the `Log` strip rules in `proguard-rules.pro`.
3. Do not ship debug flags, test keys, or test data in release code. Mark temporary code with `TEMPORARY:`.

### MASVS-PRIVACY

1. Collect only the data that the scoreboard needs: player names, colors, scores, times.
2. Do not add analytics, crash reporting, advertising, or location without a request.
3. When the user deletes a profile, also delete its reward unlocks (`RewardInventory.forgetProfile`).

## 3. OWASP Top 10 (2021) mapping

| ID | Risk | Rule in this repository |
|---|---|---|
| A01 | Broken Access Control | Profile-bound tickets (anti-IDOR/BOLA); no client admin roles; `exported=false` |
| A02 | Cryptographic Failures | AES-256-GCM + AndroidKeyStore; `SecureRandom`; HMAC-SHA256 |
| A03 | Injection | Allowlist bridge commands; JSON parse with `org.json`; no SQL; no shell; strip control characters |
| A04 | Insecure Design | Score-safety rules (root `AGENTS.md` 5.1); fail closed on invalid input |
| A05 | Security Misconfiguration | Network security config; backup off; release R8; no debuggable release |
| A06 | Vulnerable Components | 0 runtime dependencies; check AGP and ZML versions on update |
| A07 | Identification and Auth Failures | Constant-time token compare; random tokens only |
| A08 | Software and Data Integrity | GCM tag check on load; signed tickets; reject tampered data |
| A09 | Logging and Monitoring Failures | `SafeLog` with scrub; no secrets in logs |
| A10 | SSRF | No outbound requests from the Android app; watch posts only to loopback |

## 4. OWASP Mobile Top 10 (2024) mapping

| ID | Risk | Rule |
|---|---|---|
| M1 | Improper Credential Usage | No hardcoded credentials or keys |
| M2 | Inadequate Supply Chain Security | No new dependencies without review |
| M3 | Insecure Authentication/Authorization | No client-side privilege; tickets bound to profile |
| M4 | Insufficient Input/Output Validation | `InputValidator` at each boundary |
| M5 | Insecure Communication | Loopback only; TLS for any remote host |
| M6 | Inadequate Privacy Controls | Minimal data; no backup; delete on profile removal |
| M7 | Insufficient Binary Protections | R8 obfuscation and shrink |
| M8 | Security Misconfiguration | `exported=false`; backup off; cleartext off |
| M9 | Insecure Data Storage | Encrypted stores |
| M10 | Insufficient Cryptography | GCM, 256-bit keys, random IVs |

## 5. Watch bridge (`WatchCommandServer`) rules

1. Bind to `127.0.0.1:28282`. Verify that the peer address is loopback.
2. Reject requests with `Origin`, `Referer`, or `Sec-Fetch-*` headers (`403`).
3. Accept only loopback `Host` values.
4. Accept only `POST /zepp-command` with a small JSON body.
5. Accept only `POINT_1`, `POINT_2`, `UNDO`, `RESET`.
6. Run commands on the main thread through the `Handler`. Do not change UI state from the socket thread.
7. Start the server in `onResume`. Stop it in `onPause`.

> [!CAUTION]
> The token check is off now. `isLoopbackHost` also accepts an empty `Host`.
> Any local app can send a score command while the Match screen is open.
> To close this gap, change the watch App Side Service and the phone together. Do not change only 1 side.

## 6. Security review checklist

- [ ] All new input is validated and bounded.
- [ ] No new plaintext storage of personal or game data.
- [ ] No new secret, key, token, or back door in code.
- [ ] No new exported component, permission, or network host.
- [ ] `SecureRandom` for all random security values.
- [ ] Secret compares are constant time.
- [ ] Logs contain no personal data or secrets.
- [ ] A unit test covers each new security check (valid, invalid, and tampered input).

