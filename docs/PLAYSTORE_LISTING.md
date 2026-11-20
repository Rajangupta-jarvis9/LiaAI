# Lia AI — Play Store Listing

## Artifact to upload

```
./gradlew bundlePlayRelease
```
File: `app/build/outputs/bundle/playRelease/app-play-release.aab`

Signing requires `keystore.properties` (git-ignored) with your key details.

---

## Short description (80 chars)

> Talk, type and do — your personal AI companion powered by Gemini.

## Full description (4000 chars)

Lia AI is a full-duplex voice and text assistant that lives on your phone.

**Talk to it in Hindi, Hinglish or English** — barge in any time, switch
language mid-sentence, and pick from 10 distinct personalities: Normal,
Friendly, Professional, Funny, Companion, GF Mode, Teacher, Developer, Hungry
and Nautanki.

**It does things on your phone**
- Open any installed app by name ("YouTube kholo")
- Call contacts directly ("Mummy ko call karo")
- Prepare WhatsApp or SMS messages ("Rahul ko message karo")

**Build animated websites live**
Say "ek cafe ki animated website banao" and watch the 3D Forge open in real
time. Sections stack, animations appear, and a finished site is revealed — all
from a single sentence.

**A real 3D presence**
Lia's orb is a per-pixel lit sphere rendered on the GPU. It tilts with your
phone and ripples with your voice. Choose from 7 styles: Nova, Aurora, Plasma,
Glass, Energy, Minimal and Archer.

**Your data stays on your device**
Your Gemini API key is stored in private SharedPreferences and sent only to
Google's Gemini API. No conversation data is stored on Lia's servers.

**What you need**
- A free or paid Gemini API key from Google AI Studio (aistudio.google.com)
- Android 8.0 or higher

---

## Category

Productivity

## Content rating

Everyone

## Tags / keywords

AI assistant, voice assistant, Gemini, Hindi AI, Hinglish, chatbot, personal assistant,
voice chat, website builder, 3D orb

---

## Data Safety answers

### Data collected

| Data type          | Collected? | Shared? | Processed ephemerally? | Required? |
|--------------------|------------|---------|------------------------|-----------|
| Voice audio        | Yes        | No*     | Yes                    | Yes       |
| Contacts           | No         | No      | Yes (in RAM only)      | No        |
| Device ID          | No         | No      | No                     | No        |

*Voice audio is streamed directly to Google's Gemini Live API as part of the
user's own Gemini API key usage. Lia AI does not retain, store, or forward
audio to any Lia server.

### Data not collected

- Location
- Financial information
- Health and fitness
- Messages content
- Photos and videos
- Files and documents
- Calendar events
- App activity beyond what Gemini processes in-session

### Security practices

- Data is encrypted in transit (TLS 1.3).
- Users can delete all app data via Android Settings → Apps → Lia AI → Clear Data.

---

## Permission justifications

| Permission | Justification |
|---|---|
| `RECORD_AUDIO` | Required for real-time voice conversation via Gemini Live. The app cannot function as a voice assistant without it. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Keeps the voice session alive when the user leaves the Voice screen, so conversations continue in the background. |
| `READ_CONTACTS` | Lets Lia resolve a contact name ("Mummy", "Rahul") to a phone number when the user asks to call or message someone. Used on-demand only; contacts are never stored. |
| `CALL_PHONE` | Allows Lia to dial a number directly when the user says "Mummy ko call karo". Without this permission the dialler is opened instead. |
| `POST_NOTIFICATIONS` | Shows the persistent voice-session notification chip and delivers a "Your website is ready" notification when a Forge build finishes while the screen is off. |
| `INTERNET` | Required for all Gemini API calls (voice, text, website generation). |

---

## Submission checklist

- [ ] `bundlePlayRelease` builds without errors and without any `AccessibilityService` declaration.
- [ ] `assemblePlayRelease` APK < 50 MB (no native libs, assets gzipped).
- [ ] `testPlayDebugUnitTest` green.
- [ ] No hardcoded API keys, URLs or secrets in the repository.
- [ ] `keystore.properties` is in `.gitignore` and NOT in the AAB.
- [ ] Data-safety form completed in Play Console matching the table above.
- [ ] Short and full descriptions reviewed for 80-char / 4000-char limits.
- [ ] Screenshots (phone + 7" tablet) uploaded: Home, Voice Mode, Chat, Forge Building, Forge Result.
- [ ] Feature graphic (1024×500) uploaded.
- [ ] App icon 512×512 PNG (no alpha) uploaded.
- [ ] Content rating questionnaire completed (Everyone).
- [ ] App category set to Productivity.
- [ ] Target API level = 36 (Android 16).
- [ ] `minSdkVersion` = 26 (Android 8.0).
- [ ] Release notes written for v1.0.0.
- [ ] Internal test track → Closed test track → Open test → Production.
