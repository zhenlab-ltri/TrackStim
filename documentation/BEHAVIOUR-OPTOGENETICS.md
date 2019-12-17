## Using TrackStim for Behaviour and Optogenetics Analysis

### Turn on the system
1.  Turn the mac on
2.  Turn on the microscope
3.  Turn on the automated stage
4.  Turn on the camera controller

### Opening the program
1.  Open the TrackStim-Behaviour program
2.  Choose the "TrackStim-Behaviour.cfg" file
3.  Click on the "Default" preset in the micromanager window

### Configuring the microscope
1.  Ensure that the LED stimulator is in the socket under the microscope stage
    - Note: this stage is optional but should be performed to minimize variability.  Your results may vary if you leave this stage out
2.  Switch the microscope filter to "5" for dark field
3.  Click the splitter out half way and move the splitter switch to the left

### Configuring the live image
1.  Open the micromanager live mode
2.  Open micromanager plugins list and select 'Trackstim'
3.  Adjust the LED position via the two screws underneath it's base so that it is aligned in the center of the image
4.  Adjust the live image using the microscope light and 'auto' button on the micromanager window

### Configuring the binarized image window in TrackStim
To help you understand how TrackStim tracks the worm, a processed version of the live window is created when the program starts

To track the worm successfully, you need the processed live window to look like:

***INSERT IMAGE HERE***

To do this, you must also adjust the 'Auto-threshold slider' value in the TrackStim UI.  Adjust it so that the image looks like the above


### Configuring tracking speed
Depending on the age and the type of worm, you may want the tracker to adjust the tracker speed.  You can do this using the tracking speed slider.
