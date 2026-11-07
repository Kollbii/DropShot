# DropShot Workspace Agents — Token Optimization Guide (`.agents/agents/`)

This directory defines module-scoped AI agents mapped 1-to-1 to the DropShot repository structure.
Using these agents cuts token usage across **Google Antigravity (Gemini)**, **OpenAI (Codex)**, and **Anthropic (Claude Code)** by restricting each agent's file-reading scope and routing routine tasks to fast, cost-efficient models.

---

## 1. Agent Routing & Cross-Platform Model Matrix

| Agent File | Repository Scope | Gemini (Antigravity) | OpenAI (Codex) | Claude (Claude Code) | When to Use |
|---|---|---|---|---|---|
| [`android-app.md`](android-app.md) | `Android/app/` (Scoreboard, `MatchState`, stats, stores, charts, UI) | `flash` *(or `pro` for multi-screen refactors)* | `gpt-5-mini` / `o4-mini` (`reasoning_effort: medium`) | `claude-sonnet-4-5` (`effort: medium`) | Scoreboard logic, match history, statistics, settings, and phone UI screens. |
| [`lootbox-cosmetics.md`](lootbox-cosmetics.md) | `Android/lootbox/`, `Android/app/src/main/assets/fonts/`, `PlayerCustomizationActivity` | `flash` *(low/medium effort)* | `gpt-5-mini` (`reasoning_effort: low`) | `claude-haiku-4-5` (`effort: low`) | Adding/editing colors, bundled fonts, 120 Hz tile rendering, spin tickets, and AES-GCM storage. |
| [`supabase-cloud.md`](supabase-cloud.md) | `supabase/`, `SupabaseSyncClient.java`, `CloudConfigStore.java`, `docs/agents/cloud-sync.md` | `flash` *(medium effort)* | `gpt-5-mini` (`reasoning_effort: medium`) | `claude-haiku-4-5` / `sonnet` (`effort: medium`) | PostgreSQL schema migrations, JSONB expansion (`equipped_cosmetics`, `stats_ext`), RLS policies, and PostgREST sync. |
| [`watch-companion.md`](watch-companion.md) | `AndroidWatch/`, `ZeppOS/`, `BluetoothWatchServer.java`, `WatchCommandServer.java` | `flash` *(low/medium effort)* | `gpt-5-mini` (`reasoning_effort: low`) | `claude-haiku-4-5` (`effort: low`) | Wear OS 75/25 watch remote, Amazfit Zepp OS apps, and RFCOMM/loopback watch bridges. |
| [`docs-and-ste-auditor.md`](docs-and-ste-auditor.md) | `README.md`, `AGENTS.md`, module `AGENTS.md`, `docs/agents/*.md` | `flash` / `flash_lite` *(low effort)* | `gpt-5-mini` (`reasoning_effort: low`) | `claude-haiku-4-5` (`effort: low`) | Synchronizing Markdown documentation and auditing ASD-STE100 text, WCAG 2.2 AA, and security rules from `git diff`. |
| [`release-manager.md`](release-manager.md) | `Android/app/build.gradle`, `AndroidWatch/build.gradle`, Git branches/tags, `gh release` | `flash` *(low effort)* | `gpt-5-mini` (`reasoning_effort: low`) | `claude-haiku-4-5` (`effort: low`) | End-to-end version bump, unit test & APK build verification, merge to `master`, annotated tag, push, GitHub Release, and branch deletion. |

---

## 2. How to Invoke Across Platforms

### Google Antigravity (IDE / App / `agy` CLI)
- **As a Primary Agent**: Select the agent (`android-app`, `lootbox-cosmetics`, `supabase-cloud`, `watch-companion`, `docs-and-ste-auditor`, or `release-manager`) from the agent selector or run `agy --agent <name>`.
- **As a Subagent (from a Pro Coordinator)**: Call `invoke_subagent` with `TypeName: "<name>"` and `Model: "flash"` so the sub-task executes in an isolated context window on Gemini Flash.

### OpenAI Codex CLI / Projects
- Pass the domain agent file as the scoped instructions for the task:
  ```bash
  codex --model gpt-5-mini -c model_reasoning_effort="low" "Follow .agents/agents/lootbox-cosmetics.md: <task>"
  ```

### Anthropic Claude Code
- Reference or symlink `.agents/agents/*.md` (compatible with `.claude/agents/*.md` frontmatter) or invoke directly in prompt context:
  ```bash
  claude --model claude-haiku-4-5 "Follow .agents/agents/watch-companion.md: <task>"
  ```

---

## 3. Five Golden Rules for Token Optimization

1. **Strict Directory Isolation**: Never load `AndroidWatch/`, `ZeppOS/`, `supabase/`, and `Android/lootbox/` into the same context window unless a change crosses their protocol boundary.
2. **Line-Range Reads on Large Files**: Never read `MainActivity.java` (1,330+ lines) or `PlayersPageView.java` (680+ lines) from line 1 to EOF. Locate symbols with `grep -n` first and read 80–120 lines around the target.
3. **Module-Scoped Test Runs**: During development, run `:lootbox:testDebugUnitTest`, `:app:testDebugUnitTest`, or `-p AndroidWatch testDebugUnitTest` individually instead of building all APKs on every edit.
4. **Diff-Driven Doc Updates**: Use `git diff` inside `docs-and-ste-auditor` so the documentation agent reads only changed lines rather than scanning the entire codebase.
5. **Flash / Mini / Haiku for Mechanical Pipelines**: Always run `release-manager` and `docs-and-ste-auditor` on `flash` / `gpt-5-mini` / `claude-haiku-4-5` with low/medium reasoning effort.
