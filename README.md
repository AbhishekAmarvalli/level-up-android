# Level Up

Level Up is a native Android app (Kotlin + Jetpack Compose) built from `levelup-demo.html`.
It has the four tabs from the prototype (Today, Roadmap, Deadlines, Speaking), the
Minimum/streak logic, real CameraX speaking-practice recordings, optional Google Drive
backup, reminders and four Jetpack Glance home-screen widgets.

The app is dark-only, portrait-only and works entirely offline except for the optional
Google Drive backup.

---

## Download the APK

[![Download Level Up 1.0 APK](https://img.shields.io/badge/Download_Level_Up_1.0-APK-E24B4B?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AbhishekAmarvalli/level-up-android/raw/main/LevelUp-1.0-debug.apk)

**Tap the button above to download [`LevelUp-1.0-debug.apk`](./LevelUp-1.0-debug.apk).**

- **Location in this project:** `LevelUp-1.0-debug.apk` (repository root)
- **Requirements:** Android 8.0 or newer (`minSdk 26`), about 16 MB
- **SHA-256:** `e1f6f0b25f605ad310a21328f5eb237d9f6be1de5f54ecf0522cc8bd2ad4f162`
- This is a debug build signed with the debug keystore. When opening the file, allow your
  browser or file manager to install unknown apps if Android asks.

---

## 1. Deliverables

| Item | Path |
| --- | --- |
| Complete Android Studio project | repository root |
| Debug APK (download) | `LevelUp-1.0-debug.apk` |
| Fresh Gradle build output | `app/build/outputs/apk/debug/app-debug.apk` |
| Source | `app/src/main/java/com/levelup/app` |
| Design resources | `app/src/main/res` |

The project builds with **zero Kotlin/Java compile errors**:

```bash
./gradlew assembleDebug
```

Toolchain used for the verified build:

- JDK 17 (`.toolchain/jdk/jdk-17.0.21+9` in this workspace)
- Gradle 8.9 (wrapper)
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.25
- compileSdk 34, targetSdk 34, minSdk 26
- Build tools 34.0.0, platform `android-34`
- Package / application ID: `com.levelup.app`

### Build and install

```bash
# point Gradle at a JDK 17 installation
export JAVA_HOME="$PWD/.toolchain/jdk/jdk-17.0.21+9"   # or your own JDK 17
./gradlew assembleDebug

# install on a connected phone (USB debugging enabled)
adb install -r LevelUp-1.0-debug.apk
# or, after rebuilding:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Alternatively copy the APK to the phone and tap it, allowing installs from unknown
sources for the file manager. The debug APK is signed with the machine's debug keystore.

Android SDK pieces, if they are missing:

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

---

## 2. Free Google Drive setup (optional backup)

Level Up uses Google Sign-In (Play Services) and the **Google Drive REST API v3** with the
**`drive.file`** scope only. No API key, paid service or backend is needed. The app can only
see the files and the "Level Up" folder it creates itself.

Do this once, on the same computer whose debug keystore signs the APK:

1. **Create a project** at <https://console.cloud.google.com/> (any name, for example
   `Level Up`). No billing is required for normal Drive API usage.
2. **Enable the API**: *APIs & Services → Library → Google Drive API → Enable*.
3. **Configure the OAuth consent screen**: *APIs & Services → OAuth consent screen*.
   - User type: **External**.
   - App name: `Level Up`; add your email as support and developer contact.
   - Scopes: add `https://www.googleapis.com/auth/drive.file`.
   - While the app is in **Testing**, add your Google account under **Test users**.
     (Publishing/verification is not needed for personal use; the app stays in testing.)
4. **Get the debug SHA-1**:

   ```bash
   # Windows (Git Bash)
   keytool -list -v \
     -keystore "$USERPROFILE/.android/debug.keystore" \
     -alias androiddebugkey -storepass android -keypass android

   # macOS / Linux
   keytool -list -v \
     -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android
   ```

   Copy the **SHA1** fingerprint (`AA:BB:...`). The APK built in this workspace is
   signed with debug SHA-1
   `23:53:A4:4C:8A:70:BB:85:CF:7B:19:A2:6B:D6:92:BE:DE:12:FE:DD`; register that value if
   you install the supplied APK unchanged, or register your own if you rebuild it here.
   You can register more than one SHA-1 on the same Android OAuth client.
5. **Create the OAuth client**: *APIs & Services → Credentials → Create credentials →
   OAuth client ID → Android*.
   - Package name: `com.levelup.app`
   - SHA-1: the fingerprint from step 4.
   - Name: `Level Up debug`.
6. **No client ID is needed in the code.** Google Sign-In matches the Android OAuth
   client by package name + signing certificate. Rebuild/install the APK and open
   **Speaking → STORAGE → Back up videos to Google Drive**. The app asks for Drive access
   and creates a folder named **Level Up**.
7. For a future signed release build, add that keystore's SHA-1 as a second Android OAuth
   client (or add it to the same client). The debug and release certificates are different.

**Troubleshooting**

- `ApiException: 10` (`DEVELOPER_ERROR`): package name or SHA-1 mismatch. Check that the
  installed APK is signed by the keystore whose SHA-1 you registered. Re-download
  `google-services`-free apps still match automatically.
- `ApiException: 12501`: the user cancelled the account picker.
- Storage usage unavailable: confirm the Drive API is enabled and the Google account is
  a test user on the consent screen.
- The app requests `GET_ACCOUNTS` only so Play Services can exchange the signed-in account
  for a Drive access token; it does not read contacts or other accounts.

---

## 3. Permissions

| Permission | Why |
| --- | --- |
| `CAMERA` | Live front-camera preview and video recording. Requested from the Speaking tab with an in-app explanation. |
| `RECORD_AUDIO` | Audio track on speaking clips, and the real microphone waveform. |
| `POST_NOTIFICATIONS` | Daily Minimum reminder, deadline reminders, and the "waiting for Wi-Fi" upload notification (Android 13+). |
| `INTERNET` | Google Drive REST calls only. The rest of the app is offline. |
| `ACCESS_NETWORK_STATE` | Choosing Wi-Fi vs mobile data, detecting roaming, and respecting Data Saver. |
| `GET_ACCOUNTS` | Google Sign-In / token exchange. |
| `WAKE_LOCK` | WorkManager completing a backup/reminder. |
| `RECEIVE_BOOT_COMPLETED` | Merged by WorkManager so reminders/uploads survive a reboot. |
| `FOREGROUND_SERVICE` | Declared by WorkManager for long-running work. Level Up does not start its own foreground service. |

No storage permission is required: clips live in the app's private `files/clips` folder.

---

## 4. How it works

- **Storage**: Room (tasks, per-day task logs, phases, lessons, deadlines, day records,
  clips) and Preferences DataStore (settings).
- **State**: one Activity, MVVM with `StateFlow`, manual dependency injection through
  `LevelUpApp.AppContainer`.
- **Daily rollover**: task logs are per ISO date; missing logs mean "not done". A
  `DayRecord` is created for today on app open/resume and by a WorkManager midnight job,
  and today's aggregate is updated on every task change.
- **Bad day mode** hides Normal and Bonus. **Exam mode** hides AI-tagged tasks above
  Minimum and renames "20 min AI learning" to "10 min AI learning" for display only. Exam
  mode is automatic while an undone Exam deadline is within the configured threshold and
  can be overridden from the switch; Settings offers "Reset exam mode to auto".
- **Minimum and streak**: the Minimum is complete when every visible Minimum task is done.
  Completion is sticky for that date, so unchecking does not remove the streak or replay
  the celebration. The streak counts consecutive days ending today (or yesterday when
  today is not complete yet), and the seven dots are the last seven days ending today.
- **Celebration**: a red radial inner glow, twenty falling pieces and the streak message
  run for about two seconds, exactly once per day when the Minimum first completes.
- **Activity**: the 7-day view uses real per-day percentages, labelled by the actual
  weekday and ending today. The 4-week view averages four consecutive 7-day windows.
  Both are drawn as a monotone curved line chart (2.5 dp red line, 28% gradient fill,
  0/50/100 grid, reveal animation and a press/drag tooltip), in the Today card and in
  the Activity widget via the same geometry rendered to a bitmap.
- **Speaking**: CameraX front camera, 720p, real audio; a 5:00 countdown auto-stops and
  saves. The 30-bar waveform is driven by `AudioStats.getAudioAmplitude()` from the real
  recording status events. Clips get a real first-frame thumbnail, in-app playback and
  local delete.
- **Uploads**: WorkManager enqueues one job per clip. Wi-Fi only uses an `UNMETERED`
  constraint; Wi-Fi + mobile uses `CONNECTED`; Ask uses `CONNECTED` and, when on mobile,
  parks the clip as `WAITING_FOR_WIFI` and posts a notification with "Upload now" and
  "Wait for Wi-Fi". Roaming and Data Saver block mobile uploads. The upload uses the Drive
  resumable protocol in 2 MB chunks with exponential backoff.
- **Storage policy**: the Drive "about" endpoint supplies real usage. Below 80% shows the
  normal copy, 80–89% the warning, and 90%+ pauses queued clips and shows the paused copy.
  Refreshing the quota below 90% resumes paused clips.
- **Mobile cap**: bytes uploaded over mobile are counted per calendar month and reset on
  the first use in a new month. When the optional cap is reached the worker behaves as
  Wi-Fi only and the STORAGE card shows the cap message.
- **Reminders**: daily at the chosen time (default 20:00) only if today's Minimum is still
  open; deadline alerts at 09:00 three days before, 09:00 one day before, and 08:00 on the
  deadline. Channels are `Reminders` and `Backups`.
- **Widgets**: four Glance widgets refresh after every repository change, after widget
  taps, and at midnight via WorkManager. Tapping a widget's non-interactive area opens the
  matching tab.

---

## 5. Assumptions

1. **Seed data**: all six tasks and all 23 lessons across seven phases are seeded exactly
   as in the HTML. Per Section 5 of the brief, every lesson starts **not done** (the HTML
   demo has "Python basics" done).
2. **Deadlines** are seeded at install +4 days (Exam), +9 (Assignment) and +21 (Project).
   Every date, month, countdown and "today" value comes from the device clock.
3. **Skip for today** needs persistence, so `TaskLog` has an extra `skipped` flag. A skipped
   task disappears for the rest of the day (with an Undo snackbar) and returns tomorrow.
   It is excluded from the day's total, so it cannot silently block the Minimum.
4. If no visible Minimum tasks exist (for example all were deleted), the Minimum is treated
   as **not** complete rather than vacuously complete.
5. **Sticky Minimum**: once a date's Minimum is completed, it stays counted even if tasks
   are unchecked later that day. This is what prevents a repeated celebration.
6. The **4-week activity** chart averages all 28 days, counting days without a record as 0%.
7. **Exam mode label** shows "Exam mode (auto, exam in N days)" whenever an exam is inside
   the threshold, including while the user has temporarily overridden it off; otherwise it
   reads "Exam mode".
8. **Prompts** rotate by device date (`epochDay % promptCount`). The list is editable in
   Settings and always keeps at least one prompt.
9. **Clip names** are `Clip <database id>`, so deleting clips can skip a number. Files are
   private MP4s named `clip_<timestamp>.mp4`; Drive names are `Clip <id>.mp4`.
10. **Drive quota** with no reported limit shows used space without a meaningful percentage
    bar; pause thresholds only apply when a limit exists.
11. **Mobile cap** is checked before each upload; there is no background quota poll beyond
    storage refresh and upload preparation.
12. **Widget rendering** is controlled by the launcher's grid and resizing. The widget
    controls match the HTML, but exact pixel dimensions vary by device/launcher.
13. **Accessibility**: primary rows and navigation targets stay comfortably tappable,
    while checkboxes, switches, chips and icon buttons deliberately keep the prototype's
    compact 16–34 dp visual sizes to preserve the 1:1 HTML geometry.
14. The HTML demo's App/Home switcher, fake phone frame, 9:41/5G status bar, wave
    randomness, fake clips, hard-coded streak/activity/date values and "simulate usage"
    control are removed, as required.

---

## 6. Feature checklist

| Requirement | Status |
| --- | --- |
| Four tabs styled from the HTML | Implemented, builds |
| Bad day filtering | Implemented |
| Exam auto mode, filtering, AI-task rename, auto label | Implemented |
| Minimum completion celebration and streak | Implemented |
| Next step, Mark done, Skip, roadmap expand/collapse | Implemented |
| Real calendar, list/timeline, real countdowns, Done section | Implemented |
| Add/edit/delete tasks, phases, lessons, deadlines | Implemented |
| Live front camera, real audio, 5:00 limit, real waveform | Implemented (needs a physical camera to verify) |
| Saved clips with real thumbnails, playback, delete | Implemented |
| Drive sign-in with `drive.file`, "Level Up" folder | Implemented (needs Google Cloud setup) |
| Wi-Fi only / Wi-Fi + mobile / Ask before mobile data | Implemented |
| Resumable uploads, backoff, waiting notification actions | Implemented |
| 80% / 90% storage warnings, pause and resume | Implemented |
| Mobile data usage line and monthly cap | Implemented |
| Daily and deadline reminders | Implemented |
| Four Glance widgets with taps and refresh | Implemented (needs a launcher to verify) |
| Debug APK builds | Verified: `app/build/outputs/apk/debug/app-debug.apk` |

The debug APK was built successfully in this workspace. Camera, real Drive calls, widget
rendering and notification delivery require a physical device / configured OAuth client,
which are not available in the build environment, so those were verified by code review
and compilation rather than by running on hardware. No emulator or connected device was
available for screenshot comparison; the visual pass was checked statically against the
HTML at 1 CSS px = 1 dp.

---

## 7. Final self-review against `levelup-demo.html`

| HTML screen / element | Status |
| --- | --- |
| **Today** header date + EXAM MODE pill | **Matches**; date is the real device date in mono 16; pill only in exam mode. Added the required "+" (new task) and gear (settings). |
| **Today** NEXT STEP card | **Matches**; mono 15 title, 12 muted phase, primary/secondary buttons, "All lessons done." fallback. |
| **Today** STREAK and NEXT DEADLINE cards | **Matches**; equal-width cards, red 30 numbers, 7 dots, next deadline title. |
| **Today** Bad day / Exam mode switches | **Matches**; custom 34×20 switch; auto label includes the real days remaining. |
| **Today** MINIMUM / NORMAL / BONUS groups | **Matches**; empty groups hidden, 20 dp checkbox, 11×12 row padding, 6 dp gap, animated strike-through, completion flash and haptics. Swipe gesture hint removed and real swipe/long-press gestures added. |
| **Today** Activity card | **Matches the HTML activity card**; real DayRecord data, 7-day/4-week chips, 28 sp red average, 110 dp curved line chart with grid, reveal animation and press/drag tooltip. |
| **Today** celebration | **Matches**; red inner glow, 20 Canvas confetti pieces with rotation and drift, “Minimum done.” plus “N day streak” on one line, ~2 s, once per day. |
| **Roadmap** title + Expand all | **Matches**; added a "+" to create phases (required by the brief). |
| **Roadmap** timeline, nodes, percentages, bars | **Matches**; 1 px line, 19 dp nodes, red complete/white current/grey otherwise, 4 px progress, "x of y lessons". |
| **Roadmap** lesson rows | **Matches**; red check/strikethrough, empty circle, NEXT tag and external-link icon. Long-press adds rename/reorder/delete and URL editing. |
| **Deadlines** calendar | **Matches** with real data; real month, real weekday alignment (the demo starts the 1st in the first cell), today outlined, red dots. Prev/next month added per brief. |
| **Deadlines** List / Timeline chips | **Matches**; active/inactive styling and both layouts. |
| **Deadlines** countdown | **Matches**; red mono 24 number and "days left". |
| **Deadlines** DONE section | **Matches**; driven by real completed deadlines, checked box, title and completion date ("6 Nov" style). A checkbox was added to active rows as required. |
| **Speaking** prompt card | **Matches**; label, prompt, red mono 26 countdown (`05:00`). Prompts rotate daily and are editable. |
| **Speaking** camera area | **Real CameraX front preview** replacing the fake box; 240 dp tall, rounded 14, with the REC dot/label only while recording. |
| **Speaking** record button | **Matches**; 76 dp circle, surface fill, 1.5 border, red video/stop icon. |
| **Speaking** waveform | **Real microphone amplitude** from CameraX, 30 bars 3×3, 6 dp idle muted, moving off-white bars while recording. |
| **Speaking** helper text | **Matches**; idle/recording copy exactly as the HTML. |
| **Speaking** SAVED CLIPS | **Matches**; "Clip N", duration, 40 dp real-frame thumbnail with play icon, cloud state (only when backup is on), trash and in-app playback. Empty state "No clips yet." |
| **Speaking** STORAGE | **Matches** layout and copy; real Drive quota. The single Wi-Fi switch becomes the required three upload-mode chips. Cloud icons show only when backup is on. 80%/90% copy is exact. |
| **Speaking** WEEKLY REVIEW CHECKLIST | **Matches**; hint text and six selectable chips, saved per ISO week. |
| **Bottom tabs** | **Matches**; custom bar with no pill/no elevation, 0.5 top border, red active, muted inactive, 22 dp icons and 11 sp labels; 200 ms crossfade + 8 dp tab slide. |
| **App / Home screen switcher, phone frame, fake status bar, 720×1604 label** | **Removed** as required; real status/navigation bars with insets. |
| **Simulate usage / fake 9.2 GB** | **Removed**; real Drive "about" usage. |
| **Coming Up widget** | **Matches**; 56 sp red days, "days", title/date, two next rows with 0.5 dividers. |
| **Today widget** | **Matches**; 16 dp checkboxes, task titles, done strikethrough, taps toggle without opening the app. |
| **Activity widget** | **Matches**; real 7-day/4-week monotone curved chart rendered to a bitmap (same curve, 28% gradient, points, grid and labels), red 28 sp average, "of tasks done, average", tappable range chips. |
| **Streak + Next Step widget** | **Matches**; two cards, red streak, 7 dots, three tier bars, next lesson/phase or "All done". Tier bars have a fixed compact width inside the resizable widget. |
| **Fake clips, random waveform, fake timer, hard-coded streak/activity/dates** | **Replaced** with real recording, real amplitude, real time and Room data. |

---

## 8. Project layout

```text
app/src/main/java/com/levelup/app/
  LevelUpApp.kt                 Application + manual DI container + startup scheduling
  MainActivity.kt               Single activity, edge-to-edge, tab entry points
  data/                         Room entities, DAOs, database, settings, repository, seeding
  domain/                       Date formatting, visibility, streak, activity, exam logic
  drive/DriveClient.kt          Drive v3 REST: folder, quota, resumable upload, delete
  recording/CameraRecorder.kt   CameraX front preview + 720p video + real amplitude
  ui/                           Theme, components, tab shell and the four screens + settings
  work/                         Upload worker, reminders, notifications, midnight rollover
  widget/Widgets.kt             Four Glance widgets, action callbacks and receivers
app/src/main/res/
  drawable/                     Tabler-style vector icons and adaptive launcher
  font/                         Bundled JetBrains Mono
  xml/                          Widget provider metadata
```
