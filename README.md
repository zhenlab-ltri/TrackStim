# TrackStim

This is a work in progrss version of TrackStim.

This program is a micro manager plugin that provides:
- automated tracking of moving c.elegans in real time
- xy stage velocity data for analysis

## Requirements
- Micro-Manager 1.3
- [usb to uart vcp driver for ASI MS-2000 XY stage](https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers)
- [hamamatsu dcam api](https://dcam-api.com/)


### MacOSX specific requirements
- [Java SE 6 Legacy](https://support.apple.com/kb/dl1572?locale=en_GB)
- [hamamatsu dcam api mac](https://ftp.hostedftp.com/~hamamatsu/DCAM-API/2013/13.11/DCAM-API%20for%20Mac%20OS%20X%20%2813.11.4451%29.zip)

## Building the plugin

In this repository is a file called TrackStim.java

You must use this command to take TrackStim.java and other packages that TrackStim.java depends on to build the plugin (macOSX specific):

```sh
javac -source 1.5 -target 1.5 -classpath /Applications/Micro-Manager1.3/ij.jar:/Applications/Micro-Manager1.3/plugins/Micro-Manager/MMCoreJ.jar:/Applications/Micro-Manager1.3/plugins/Micro-Manager/MMJ_.jar -Xlint:unchecked TrackStim_05.java

```

Then, run the following command:

```sh
jar cvf TrackStim_.jar TrackStim_05.class TrackingThread12.class SignalSender02.class
```


Once the command finishes, you should see a file called ```TrackStim.jar```.  You need to take this jar file and copy it to ```/Applications/Micro-Manager1.3/mmplugins/```.

Once you start micromanager, you should be able to select TrackStim from the plugins menu.
