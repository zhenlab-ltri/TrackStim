# TrackStim
micro-manager plugin to image moving C.Elegans


# Making changes to this plugin
micro-manager plugin development finicky.  As a result, being able to make changes and build this plugin will be finicky.  Here is a step-by-step guide on how to modify, build, and install this plugin for micromanager.

## Required Software (Assuming MacOSX)

- [NetBeans 7.3.1](https://netbeans.org/downloads/7.3.1/index.html)
- [Micro-Manager 1.4](https://micro-manager.org/wiki/Download%20Micro-Manager_Latest%20Release)
- [Java 6 (required by micromanager)](https://support.apple.com/kb/dl1572?locale=en_GB)


## Setup

1. Download this repository via git or github to your computer
2. Open NetBeans 7.3.1
3. Open the downloaded repository in NetBeans 7.3.1
4. In the ```projects``` panel, there should be a ```libraries``` icon
      a. right click ```libraries``` and click ```Add JAR/Folder```
            - navigate to the micromanager plugins 1.4 folder (e.g. /Applications/MicroManager 1.4/Plugins/Micro-Manager/ for Mac)
            - select all of the JAR files (CMD + A on Mac)
            - click choose
      b. right click ```libraries``` and click ```Add JAR/Folder```
            - navigate to the micromanager 1.4 folder (e.g. /Applications/MicroManager 1.4/ for Mac)
            - find the imageJ JAR file (ij.jar)
            - click choose
5.  In the ```projects``` panel, there should be a ```libraries``` icon
      a. expand the libraries icon and scroll to the bottom of the list
      b. confirm that the JDK version is 1.6
      c. if the JDK version is not 1.6, right click the JDK icon, and click edit
      d. choose JDK version 1.6 (see Required Software if JDK version 1.6 is not available)


## Building the plugin

1.  In netbeans, find the ```run``` icon/label, and in the submenu, find the ```Clean and Build Project``` submenu.
2.  Click ```Clean and Build Project```
3.  Open the project folder and find the ```dist``` folder
4.  The ```TrackStim.jar``` file should be found in the ```dist``` folder


## Installing the plugin to Micro-Manager

1.  navigate to the micromanager 1.4 mmplugins folder (e.g. /Applications/MicroManager 1.4/mmplugins/ for Mac)
2.  Copy the ```TrackStim.jar``` file to the micromanager mmplugins folder


## Testing the plugin

1.  Open micromanager 1.4
2.  The plugin should be registered in the plugins menu
