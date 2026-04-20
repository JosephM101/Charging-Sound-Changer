# Charging Sound Changer
### (I didn't have a better name)

Charging Sound Changer is a very simple and fun Android app that "replaces" your device's built-in charging sound with one of your choosing.

_I made this because I was bored lol_ In all seriousness, I enjoy doing goofy projects like these, even if the work it takes to get there was tedious and made me put this off for a year _(which, in the case of this app, mostly consisted of UI garbage and boilerplate, permissions management across various versions of Android and making sure there was less of a chance of the OS terminating the sound service in the background)_
Either way, it's (nearly) finished now, and ready for anyone keen to test out!

Minimum OS: Android 6 (Marshmallow), SDK 23

This app supports (and has been tested with) WAV and MP3 files with a 5-second duration limit.

**Proudly written with love, and _without_ "AI" :)**

## How to use
This app effectively replaces Android's built-in sound that plays when a device is plugged in. Before enabling this app, you should disable your device's built-in functionality.

On most Android phones, the process looks like this:
- Open the Android Settings app
- Scroll down and tap on "Sound & vibration"
- Scroll to the bottom and disable "Charging sounds and vibration"

This process may look different on phones from other brands that heavily customize their devices like Samsung.

Once all of that is complete, choose a sound that's within the 5-second limit, turn it on and plug in your device! A couple of sample sounds will be offered soon.

## Installing
\[WIP\]

## Permissions
After installing the app, you'll need to accept every permission to ensure proper functionality. Here are the permissions currently required, and their rationale:

- **Notification permissions**: These permissions are required on Android 13 and later for apps to post notifications. This app needs those permissions for the service notification that allows it to continue running in the background with a smaller likelihood of being terminated by Android when resources are low.
- **Battery optimization permissions**: Required to ensure that the app can run in the background consistently, and to ensure that sounds aren't delayed too much.
- **File permissions**: To load sounds, this app needs permission to access your files. Don't worry! This permission will only be used when you ask to load sounds. After a sound is chosen, it is copied to the app's local data directory for later. If you are keen, you could disable this permission after choosing a sound, and the app would continue to function.


## A notice about the future of Android: Google's attack on open-source and freedom
Google announced in August of 2025 that in September of 2026 they would begin enforcing rules that require *every* Android developer, even those who don't submit apps to the Google Play Store, to register with Google, or else their apps won't install on certified Android devices. The rollout of these changes is expected to begin in September of 2026, and are expected to be in full effect in 2027.

This doesn't just affect Play store apps... this affects *every* app, including apps from the F-Droid store or even direct APK downloads. These apps will not be blocked from installation on users' devices if their developers do not register with Google.

This move, coupled with the changes Google have made to the Android Open Source Project, directly threaten the open principles and freedoms that Android was founded on. The idea that we will no longer have the freedom of choice to install software of our choosing on the devices *we* own and have *paid for* is deeply saddening and honestly terrifying as a tech hobbyist. It's the reason I moved away from Apple devices nearly 7 years ago now.

**I will not be registering with Google, and thus, this app's development will be stalled indefinitely beginning in September of 2026.**

I am profoundly sad about this as I enjoyed making fun apps and tools for myself and for my friends, and the freedom that I've been lucky to enjoy for nearly 7 years now will be ripped away from me, and every other Android user around the world. Not just developers.

For more information and to support the cause, please visit [keepandroidopen.org](https://keepandroidopen.org/).
