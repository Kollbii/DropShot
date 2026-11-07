# ASD-STE100 writing standard

ASD-STE100 (Simplified Technical English) controls the words and the grammar of technical text.
Use it for all text that an agent writes in this repository.

> [!NOTE]
> The ASD-STE100 dictionary is a licensed document. Agents cannot check each word against it.
> Use the rules below and the project glossary. Before a release, check the UI text with an STE checker or the official dictionary.

## 1. Scope

| Text | STE required |
|---|---|
| UI text: labels, buttons, dialogs, errors, `contentDescription` | Yes |
| Code comments and Javadoc | Yes |
| Commit messages | Yes |
| `AGENTS.md`, `docs/`, `README` files that agents create | Yes |
| Agent replies to the user | Yes, where practical |
| Identifiers (class, method, variable names) | No. Use normal Java names. |
| Existing Polish README files | No. Do not translate them without a request. |

## 2. Word rules

1. Use approved words with their approved meaning only. Example: "start", not "launch" or "kick off".
2. Use 1 word for 1 meaning. Use the terms in [Android/design/04-glossary.md](../../Android/design/04-glossary.md).
3. Use technical names (TN) and technical verbs (TV) only for their technical meaning.
4. Do not use contractions (`do not`, not `don't`).
5. Do not use idioms, slang, or humor.
6. Do not use more than 3 nouns in a noun cluster. Example: "settings for the court timer", not "court timer settings page".
7. Use "must" for a requirement. Use "can" for a possibility. Do not use "should" for a requirement.
8. Use the articles "the", "a", and "an" where English grammar needs them.

## 3. Sentence rules

1. Write 20 words or fewer in a procedural sentence (an instruction).
2. Write 25 words or fewer in a descriptive sentence.
3. Write 1 topic in 1 paragraph. Write 6 sentences or fewer in a paragraph.
4. Use the active voice. Use the passive voice only when the actor is not known.
5. Use only the simple present, the simple past, and the simple future ("will").
6. Do not use the "-ing" form as a verb. Example: "Before you start the match", not "Before starting the match".
7. Write 1 instruction in 1 sentence. Use the imperative form.
8. Use a numbered list for steps in sequence. Write the result of a step as a separate sentence.

## 4. Safety text

1. Put a warning (risk to people) or a caution (risk to data or equipment) before the step.
2. Start with the command. Then give the reason.
   Example: "Do not change the key alias. A change makes all saved data unreadable."
3. Use the GitHub alerts in Markdown: `[!WARNING]`, `[!CAUTION]`, `[!IMPORTANT]`, `[!NOTE]`.

## 5. UI text rules

1. Buttons start with a verb: `Save settings`, `Start new match`, `Delete player`, `Undo point`.
2. Numbers are digits: `1`, `11`, `60`. The unit follows the number: `60 min`, `5 min`.
3. Error messages have 3 sentences:
   1. What occurred: `The app could not save the settings.`
   2. Why it occurred: `The device storage is full.`
   3. What the user must do: `Free storage space on the device and tap Save settings.`
4. Do not blame the user. Write "The name is empty", not "You entered an empty name".
5. Use "tap" for the touch screen. Use "press" for a physical key.

## 6. Commit message rules

1. Write the subject in the imperative form: `Add`, `Fix`, `Remove`, `Change`, `Bump`.
2. Write 72 characters or fewer in the subject. Do not end it with a period.
3. Write the body in STE. Explain what changed and why. Use a list for many changes.

Example:

```
Validate bridge Host header and reject browser requests

- Reject requests that contain Origin, Referer, or Sec-Fetch headers.
- Accept only loopback Host values.
- Add unit tests for valid, invalid, and empty Host values.
```

## 7. Quick check

- [ ] Each instruction has 20 words or fewer.
- [ ] Each description has 25 words or fewer.
- [ ] No contractions, idioms, or "-ing" verbs.
- [ ] No noun cluster with more than 3 nouns.
- [ ] Each term matches the glossary.
- [ ] Each warning comes before its step.

