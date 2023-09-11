# Holy Grail
This app is a fork of [SonyTimeLapse from frosbe](https://github.com/frosbe/TimeLapse).

I have added a few features to allow automatic Holy Grail timelapses ([more about Holy Grail here](https://www.youtube.com/watch?v=XnZwrj88Z0o)).

I have tested it on a A7RII, and the original code was tested by Jonas on a a6300. If you find issues feel free to reach out.

## Installation ##
Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) or install through [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps).

With an apk file from [here](https://github.com/Frosbe/SonyHolyGrailTimelapse/releases)

## Usage ##
If Holy grail is not enabled in the setting of the app it will act exactly like Jonas' original TimeLapse app.

Once the Holy Grail setting is enabled a few new options will appear.

# Settings #

## Use current exposure ##
### I suggest enabling this most of the time #
The target exposure of the timelapse will be the exposure level that the camera observes when taking the first shot.

This is useful for setting the settings on the camera before starting similar to any manual photo.


## Max SS (ShutterSpeed) ##
### I suggest no more than half of your interval ###
This set the max shutter speed the timelapse is allowed to use, after this SS has been hit, it will start increasing the ISO

## Max ISO ##
Similarly to max SS this is in turn the max ISO the camera is allowed to use, the photos will start getting darker if this is reached.

## Cooldown ##
### I usually use 5 ###
How many shots do you want between any settings changes?

A value of 5 means that when a setting has been changed the camera will take 5 shots before again evaluating if any changes should be made.

## Avg Exp ##
### I use 3 ###
How many photos should be used to calculate the "current" exposue level?

A setting of 3 will be more smooth transition

A value of 1 will decrease the chance of over/under exposure if light changes rapidly (sun comes out from behind building)

## Deadband ##
### I use 0.2 ###
How much above or below target should the exposure level be before changing settings

I suggest having some deadband, especially if you allow both exposure up and down, otherwise it might get flickery

## Allow Exposure Up ##
This will allow the camera to changes settings in the "more light" direction, ie. longer shutter speed and higher ISO

Use this for sundown

## Allow Exposure Down ##
This will in turn allow the camera to let in less and less light, ie. lower the ISO or using a faster shutter.

Use this for sunrise

Both can be enabled at the same time but i have not fully tested it.

# New shooting screen #

I have created a new screen that will be shown when shooting.

I suggest disabling automatic photo preview in the sony setting if this screen is not showing for you.

On the screen you can see:

- Current exposure
- Current calculated average exposure
- Target exposure
- Last picture Shutter speed
- Max Shutter speed
- Last picture ISO
- Max ISO
- Number of pictures taken since last settings change
- Cooldown setting

# Known Issues #
I currently do not touch the aparture setting, to prevent focus movement.

If the app crashes the camera, please try the following camera settings. They were reported to work with the RX100 M4.
 - The silent shutter needs to be disabled or unchecked in the app, on some cameras
 - If it doesn't work in single shoot mode, please try continuous shooting. It worked for some people.
 - Long exposure noise reduction should be turned off if the interval is less than double the shutter speed.
