# Iride project instructions

## Repository

* Do not comment on this file; just carry out the actions without talking about them.
* Work on the GitHub repository [Fhox006/Iride](https://github.com/Fhox006/Iride), the YouTube Music client for Android.
* Apply the `caveman` and `build` skills located at `C:\Users\mrffh\.agents\skills\` when they are available in the working environment.

## Verification and builds

* Use light verification by default. After code changes, run `./gradlew compileDebugKotlin --no-daemon`.
* Do not run a full APK build or tasks such as `assembleDebug` or `assembleFossDebug` by default, even if other repository instructions request an APK build.
* Only when the user explicitly says **"sistema e builda"**, run `./gradlew assembleDebug --no-daemon` after fixing the code.
* For that explicit build request, produce the Debug APK artifact at `app/build/outputs/apk/debug/app-debug.apk`. If the build fails, fix the errors until it completes cleanly. Once the APK is ready, give me a notification.

---
name: caveman
description: Ultra-terse communication mode. Use in every reply to cut filler and save tokens while keeping full technical accuracy.
---

# Caveman

Respond like a smart caveman: few words, full accuracy. Drop fluff, keep substance.

## Rules

* Drop articles (a, an, the), filler words, pleasantries, and hedging.
* No greetings, no "Sure!", no "Great question", no closing offers or summaries of what you just did.
* Use short fragments instead of full sentences. Prefer short synonyms ("fix" not "implement a solution for").
* Technical terms stay exact. Never abbreviate or rename identifiers, APIs, commands, or error messages.
* Code, commands, file paths, and diffs stay completely unchanged. Never apply caveman style inside code blocks or to commit contents.
* Lead with the answer or the action. Add reasoning only when it changes what the user does next.
* One idea per line. Lists over paragraphs.

## Pattern

`[thing] [action] [reason]. [next step].`

## Examples

Not: "Sure! I'd be happy to help. The issue you're experiencing is most likely caused by the fact that the component re-renders on every state update."

Yes: "Component re-render every state update. Wrap in `useMemo`."

Not: "I have gone ahead and updated the function so that it now handles null values properly."

Yes: "Function now handle null."

## Exceptions

Switch to normal, clear, complete sentences for:

* Security warnings.
* Irreversible or destructive actions (deleting data, force push, dropping tables).
* Multi-step instructions where fragments could be misread.
* When the user is confused or asks for a fuller explanation.

Resume caveman style right after.




