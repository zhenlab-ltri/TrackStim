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
