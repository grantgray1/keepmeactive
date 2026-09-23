# Keep Me Active

Sends a short SMS from the phone's own SIM on a schedule, so the carrier never sees the
number as dormant and disconnects it.

## Read this first: what actually counts as activity

Carriers cut off pay-as-you-go numbers after a period with no **chargeable activity on that
SIM**. Two consequences shape the whole design:

- **WhatsApp does not work for this.** It is data traffic to Meta's servers, not a billable
  network event on your account. A WhatsApp message will not reset the inactivity clock. An
  outbound **SMS** or an outbound **call** will.
- **A cloud service cannot do it for you.** Twilio, or any server sending a text *to* the
  phone, produces no outbound activity from your SIM. The message has to leave the handset.
  That is why this is a phone app and not a script.

The inactivity window is usually somewhere between 90 and 180 days, but it varies by carrier
and changes over time. Check yours before relying on a setting here — search for your
carrier's name plus "pay as you go inactivity disconnection". Some carriers *also* require a
top-up every so often regardless of activity; texting will not save you from that, so check
for both rules.

The default interval in the app is 21 days, comfortably inside any of these windows, at a cost
of roughly 17 texts a year.

## What the app does

- One SMS to a number you choose, every N days.
- Survives reboots and app updates (re-arms its alarm on boot).
- Retries in 6 hours if the phone had no signal, was in flight mode, or the send failed.
- Notifies you on every send, success or failure, so a silent failure cannot go unnoticed.
- A manual "send one now" button for testing, behind a confirmation because it costs real money.
- SIM picker, so on a dual-SIM phone it sends from the SIM you are actually keeping alive.
- Only resets the countdown when the radio confirms the send, not when it is queued.

Nothing leaves the phone except the text itself. No account, no server, no analytics.

## Getting the APK

No Play Store involved. Two routes.

### Route A: build it in GitHub Actions (no tools to install)

1. Create a new **private** repository on GitHub.
2. Push this folder to it (or drag the files into the web uploader).
3. The `Build APK` workflow runs automatically. When it finishes, open the repo's
   **Releases** page — there is a `latest` release with `keep-me-active.apk` attached.
4. Open that release page in the phone's browser and tap the APK to download it.

Re-running the workflow (Actions tab > Build APK > Run workflow) rebuilds and replaces it.

### Route B: build it locally

Install Android Studio, open this folder, let it sync, then `Build > Build Bundle(s) / APK(s)
> Build APK(s)`. The file lands in `app/build/outputs/apk/debug/`.

Either way the APK is signed with the standard debug key. That is fine for sideloading onto
your own phone; it just means it can never be uploaded to the Play Store as-is.

## Installing

1. Tap the downloaded APK.
2. Android will block it the first time: allow **Install unknown apps** for whichever app you
   downloaded it with (Chrome, Files), then tap the APK again.
3. Play Protect may warn that the app is unrecognised. It says that about every sideloaded
   app. Choose "Install anyway".

## Setting it up

1. Open the app and grant **SMS**, **notifications**, and **phone state** when asked.
   Phone state is only used to list the SIMs in the picker.
2. Enter the number to text. **Use another phone you own** — your own second handset is
   ideal. Avoid shortcodes, premium numbers, and free service numbers; a text to those may
   not register as normal chargeable activity.
3. Set the interval in days. 21 is a sensible default.
4. Pick the SIM if the phone has more than one.
5. Turn the switch on and press **Save schedule**.
6. Press **Send one now (test)** and confirm the text actually arrives on the other phone.
   Do not skip this — it is the only thing that proves the whole chain works.
7. Press **Stop Android killing this app** and set the app's battery usage to
   **Unrestricted**. On Samsung, Xiaomi, Oppo, OnePlus and Huawei also add it to the
   protected/auto-start list in the battery settings, or the system will eventually stop it
   waking up.

## Things that will still break it

The app cannot send if the phone is off, has no signal, is in flight mode, or the SIM is out
of credit. It notifies you on failure and retries every 6 hours, but a phone left in a drawer
with a flat battery is beyond its reach.

Two habits make that safe:

- Leave the phone on charge, not in a drawer.
- Set a calendar reminder a couple of weeks before your carrier's actual cut-off, as a
  backstop. The app tells you the last successful send date on its main screen; glance at it
  when the reminder fires.

If you are keeping a spare SIM alive, consider putting it in a cheap always-plugged-in Android
handset. That is by far the most reliable arrangement.

## iPhone

iOS does not allow any app to send an SMS without you tapping send, so there is no iPhone
build of this. The workable equivalent is a Shortcuts automation — see
[IOS-SHORTCUT.md](IOS-SHORTCUT.md).

## Project layout

| File | What it does |
| --- | --- |
| `MainActivity.kt` | The single settings screen |
| `Prefs.kt` | Settings and state, plus the due-date arithmetic |
| `Scheduler.kt` | Arms the repeating alarm |
| `SmsSender.kt` | Picks the SIM and hands the text to the radio |
| `AlarmReceiver.kt` | Fires on schedule, sends if genuinely due |
| `SmsResultReceiver.kt` | The radio's verdict; resets the clock only on success |
| `BootReceiver.kt` | Re-arms the alarm after a reboot or update |
