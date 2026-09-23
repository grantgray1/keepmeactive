# Keep Me Active on iPhone

## Why there is no iPhone app

iOS gives apps no way to send an SMS in the background. The only messaging API available to a
third-party app, `MFMessageComposeViewController`, opens the Messages compose sheet with the
text pre-filled and waits for a human to press send. There is no entitlement, no developer
programme tier, and no sideloading trick that changes this. An App Store app, an enterprise
app and a self-signed sideloaded app are all equally blocked.

So on iPhone the answer is not an app. It is a **Shortcuts personal automation**, which is a
first-party feature with the privilege to do it.

## Build it: about two minutes

1. Open **Shortcuts** > **Automation** tab > **+** (top right).
2. Choose **Time of Day**.
3. Pick a time when the phone is reliably on and has signal — mid-morning is better than
   3 am, because a failed send at 3 am goes unnoticed.
4. Set repeat to **Monthly**, and pick a day of the month.
5. Turn **Ask Before Running** OFF. Confirm **Run Immediately** when prompted.
   Leave **Notify When Run** ON — you want to see that it fired.
6. Tap **Next**, then **New Blank Automation**.
7. Add the action **Send Message**:
   - **Message**: something short, e.g. `Keeping this number active.`
   - **Recipients**: another phone you own.
8. Tap the **Send Message** action and make sure it is set to send via **Messages**, not
   WhatsApp. If the recipient is also an iPhone this matters a great deal — see below.
9. Tap **Done**.

## The iMessage trap

This is the thing that silently defeats most people.

If you text another iPhone, Messages sends it as an **iMessage** — over data, in blue. An
iMessage is not a chargeable SMS and **will not keep the SIM alive**. It looks like it worked;
it did not.

Force a real green-bubble SMS by doing one of these:

- **Text an Android phone.** Simplest and most reliable.
- **Text a landline.** It will fail to deliver usefully, but it goes out as an SMS.
- Turn off iMessage entirely (Settings > Messages > iMessage off) if the phone is a spare
  whose only job is keeping the number alive.

Before you trust any of this, send one manually and check the bubble is **green**. Green is
SMS and counts. Blue is iMessage and does not.

## Verify it actually runs

Do not wait a month to find out. Set the automation to a time three minutes from now, put the
phone down, and watch. Once you have seen it fire and land as a green message on the other
phone, edit the automation back to monthly.

Repeat this check after any major iOS update. Apple has changed the rules around automations
running while the phone is locked more than once, and an automation that worked on one version
can start requiring a tap on the next. That is the main weakness of the iPhone route.

## Backstop

Add a repeating **Reminder** a couple of weeks before your carrier's actual cut-off that says
"check the keep-alive text went out". Open Messages, look at the last green message to that
number, and if it is stale, send one by hand. Thirty seconds, twice a year.

## The honest recommendation

If this number genuinely matters, the iPhone route is the weaker of the two. A cheap Android
handset left on charge with the SIM in it and the Keep Me Active app installed is far more
dependable, because Android lets the app confirm the send actually happened and shout at you
when it did not. iOS gives you no such confirmation.
