# TrackStim
micro-manager plugin to image moving C.Elegans


## What is TrackStim
TrackStim (tracker and stimulater) is a plugin for open source microscopy software called [micro-manager](https://micro-manager.org/wiki/).

This plugin is specialized to help image moving C.Elegans.

This repository is a reupload of the work done by Taizo Kawano.

## Requirements

This plugin needs the following devices:
- [ASI MS-4000 motorized stage](http://www.asiimaging.com/products/stages/inverted-stages/ms-2000-xyz-automated-stage/)
- [Hamamatsu Orca R2 CCD camera](http://hamamatsucameras.com/orca-r2/)
- [Photometrics Dualview beam splitter camera attachment](http://www.biovis.com/photometrics_multichannel.htm)

This plugin requires the following software:
- [micro-manager 1.3](https://micro-manager.org/wiki/Micro-Manager_Version_Archive)

This plugin also requires a specific directory structure to work
- TODO find out how the old directory was structured

## Installation
1.  Put TrackStim_.jar in an appropriate directory. Usually the micromanager/plugin/ folder.
2.  Restart micor-manager
3.  Set up micromanager preset. Read the wiki to learn how. [micro-manager](https://micro-manager.org/wiki/).
4.  Turn on stage controller, camera controller and microscope.
5.  Open micro-manager.
6.  In micro-manager main window, choose the preset with these settings:
      - 128 gain; 4x binning,
      - 128 digital gain
      - 100 msec exposure (can be set differently depending on your needs)
7.  Click the 'image' button and open a preview window.
8.  Start TrackStim from the plugin menu of imageJ, not the micro-manager plugin menu. You will see the plugin window as below.

![](https://user-images.githubusercontent.com/2328291/58439881-aef82280-80a4-11e9-8553-e1ac57a23d84.png)

## User interface guide

Ready
: Start tracking object without saving images.

Go
: Start tracking and imaging.

Stop
: Stop tracking and imaging.

exposure
: If SyncDAC connected, exposure time of the camera could be set by this pull down menu. 0, 10, 50, 100, 200, 500, 1000 msec.

Cycle length
: If SyncDAC connected, set cycle length of imaging. **required to be 2x exposure**. e.g. if exposure is set as 50 msec, cycle length must longer than 100 msec.

Frame num
: Number of images taken by camera. "one of" option determine how many of them are saved.

1x etc.
: Stage accelerating modulus.

Yen
: The thresholding method to perform binary image conversion and object detection.

one of
: Determine how many images discard. eg. one of 1 means save all images. one of 2 means half of images are saved and rest are throw away. So, If you set 50 msec exposure at micromanager main pannel (20 frame/sec), half of images are saved and resultant data is 10 frame/sec.

Center of Mass
: Change tracking method. When unchecked it, the plugin track a bright object close to the center of field of view. If checked it, center of mass of whole left half of view is calculated and track it.

manual track
: Disable tracking features. You have to track object by joystick. The stage position data is still available.

Full field
: Use whole field of view instead of left half.Only works when using the **Bright field** option

Bright field
: Using inverted image to detect object. So you can track dark object, not bright one. When you want to track an animal in bright field, enable this and the **Full field** option, and change the Dual-view set up so that the camera take images not split into to wave length.

Save at
: Assign directly for saved images here. By default saved images are created in a directory named "Untitled#" in every session.

Change dir
: Sets the directory to **Save at**

### SyncDAC only options
Light
: You need check this if you using SyncDAC to apply optical stimulation.
Run
: Apply light without taking images

Pre-stim
: Length or the period before the initial signal.

Strength
: Light strength. maximum 63.

Duration
: Length of light signal.

Cycle length
: Length of one cycle.

Cycle num
: number of cycles

ramp
: You can change the strength of light as liner ramp.

base
: Strength applied interval
start
: Strength at the beginning of the duration.
end
: Strength at the end of duration.

![](https://user-images.githubusercontent.com/2328291/58440287-fc758f00-80a6-11e9-8759-5ce368f79bc5.png)


### Unimplemented commands
Just closest
: not implemented

Use right
: not implemented


## System diagram
![](https://user-images.githubusercontent.com/2328291/58439366-9afef180-80a1-11e9-8453-059f04b7a7dc.png)

TrackStim manages the Real-time processing part in the above figure by processing images captured by the CCD camera to detect bright object and control the motorized stage to track the object.

The coordinate of the stage is stored in header of tiff image file. After the imaging, other programs, such as DVtracer, must be used for further analysis (Post processing part).

## How it works (general algorithm)
![](https://user-images.githubusercontent.com/2328291/58439437-0f399500-80a2-11e9-9f46-fb7e86f277f7.png)

Let the initial target object be the object closest to the center of the field of view in the initial image.

Then the next target object for the next image is will be the object closest to the target object from the previous image.

Repeat this process for each pair of (previous, current) images.

if the target is separated from the center of view, the StimTrack commands the motorized stage to re-center the target.

### Warnings
If there are multiple bright objects and the motion is too fast, the program may make mistakes in target tracking.

In the case that the fluorescence signals are not distributed wider than field of view (eg. a few cells in head) enabling "Centor of mass" option may help to achieve stable tracking (see Detailed explanation of interface).


## Notes
This software is provided "as is" and there is no warranty of any kind and no support at all.

Copyright © 2013 Taizo Kawano
