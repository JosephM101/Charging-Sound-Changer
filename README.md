# Charging Sound Changer
### (I didn't have a better name)

Charging Sound Changer is a very simple and fun Android app that "replaces" your device's built-in charging sound with one of your choosing.

_I was bored lol_

This app supports a minimum of Android 7 (Nougat).

This app supports WAV, MP3 and OGG files, with a length limit of 5 seconds.

# How to use

## Installing
\[WIP\]

## Permissions
After installing the app, you'll need to accept every permission to ensure proper functionality. Here are the permissions currently required, and their rationale:

- **Notification permissions**: These permissions are required on Android 13 and later for apps to post notifications. This app needs those permissions for the service notification that allows it to continue running in the background with a smaller likelihood of being terminated by Android when resources are low.
- **Battery optimization permissions**: Required to ensure that the app can run in the background consistently, and to ensure that sounds aren't delayed too much.
- **File permissions**: To load sounds, this app needs permission to access your files. Don't worry! This permission will only be used when you ask to load sounds. After a sound is chosen, it is copied to the app's local data directory for later. If you are keen, you could disable this permission after choosing a sound, and the app would continue to function.

This app effectively replaces Android's built-in sound that plays when a device is plugged in. Before enabling this app, you should disable your device's built-in functionality.

On most Android phones, the process looks like this:
- Open the Android Settings app
- Scroll down and tap on "Sound & vibration"
- Scroll to the bottom and disable "Charging sounds and vibration"

This process may look different on phones from other brands that heavily customize their devices like Samsung.

Once all of that is complete, choose a sound that's within the 5-second limit, turn it on and plug in your device!
