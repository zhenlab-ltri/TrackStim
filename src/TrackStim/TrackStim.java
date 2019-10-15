package TrackStim;

import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.ItemListener;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.LogManager;

import org.micromanager.data.Datastore;
import org.micromanager.data.DataManager;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.Image;

import org.micromanager.display.DisplayManager;
import org.micromanager.display.DisplayWindow;


import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class TrackStim implements SciJavaPlugin, MenuPlugin {
   private TrackStimGUI gui;
   private Studio studio;
   private CMMCore mmc;
   private LogManager log;
   private DataManager dm;
   private DisplayManager dism;

   @Override
   public void setContext(Studio studio) {
      studio = studio;
      mmc = studio.getCMMCore();
      log = studio.getLogManager();
      dm = studio.data();
      dism = studio.displays();
   }

   /**
    * This method is called when the plugin's menu option is selected.
    */
   @Override
   public void onPluginSelected() {
      gui = new TrackStimGUI();
      this.startImageProcessingLoopAlt();
   }


   // acquire 100 images, snapping an image every 100ms to acquire 10 images every second
   public void startImageProcessingLoop() {
      int numImages = 100;
      double intervalMs = 100.0;

      try {
        mmc.startSequenceAcquisition(numImages, intervalMs, false);
      } catch (java.lang.Exception e) {
        log.logMessage("error starting sequence acquisition");
        log.logMessage(e.getMessage());
      }

      int numImagesPopped = 0;
      while(mmc.isSequenceRunning()) {
         int imagesRemaining = mmc.getRemainingImageCount();
         log.logMessage(String.valueOf(imagesRemaining) + " images in queue");

         if( imagesRemaining > 0 ){

            try {
               mmc.popNextImage();
               numImagesPopped++;
            } catch (java.lang.Exception e ){
               log.logMessage(e.getMessage());
            }
         }
         log.logMessage("sequence running");
      }

      log.logMessage(String.valueOf(numImagesPopped));
   }


   // alternative way to process images
   public void startImageProcessingLoopAlt() {
      int nrFrames = 10;
      double exposureMs = 100.00;
      String savePath = "C:\\Users\\Mei Zhen\\Desktop\\temp4\\a\\";

      // Create a Datastore for the images to be stored in, in RAM.
      Datastore store = dm.createRAMDatastore();
      // Create a display to show images as they are acquired.
      DisplayWindow ds = dism.createDisplay(store);

      Coords.CoordsBuilder builder = dm.getCoordsBuilder().time(0);

      // Start collecting images.
      // Arguments are the number of images to collect, the amount of time to wait
      // between images, and whether or not to halt the acquisition if the
      // sequence buffer overflows.

      try {
         mmc.startSequenceAcquisition(nrFrames, 0, true);
         // Set up a Coords.CoordsBuilder for applying coordinates to each image.
         int curFrame = 0;
         while (mmc.getRemainingImageCount() > 0 || mmc.isSequenceRunning(mmc.getCameraDevice())) {
            if (mmc.getRemainingImageCount() > 0) {
               TaggedImage tagged = mmc.popNextTaggedImage();
               // Convert to an Image at the desired timepoint.
               Image image = dm.convertTaggedImage(tagged,
                  builder.time(curFrame).build(), null);
               store.putImage(image);
               curFrame++;
            }
            else {
               // Wait for another image to arrive.
               mmc.sleep(Math.min(.5 * exposureMs, 20));
            }
         }

         mmc.stopSequenceAcquisition();
      } catch ( java.lang.Exception e ){
         log.logMessage("error");
         log.logMessage(e.getMessage());
      }
      // Have Micro-Manager handle logic for ensuring data is saved to disk.
      try {
         store.save(Datastore.SaveMode.SINGLEPLANE_TIFF_SERIES, savePath);
      } catch (java.io.IOException e){ 
         log.logMessage("error saving");
         log.logMessage(e.getMessage());
      }
      dism.manage(store);

   }

   /**
    * This method determines which sub-menu of the Plugins menu we are placed
    * into.
    */
   @Override
   public String getSubMenu() {
      return "";
      // Indicates that we should show up in the root Plugins menu.
//      return "";
   }

   @Override
   public String getName() {
      return "TrackStim";
   }

   @Override
   public String getHelpText() {
      return "Imaging system";
   }

   @Override
   public String getVersion() {
      return "0.0.1";
   }

   @Override
   public String getCopyright() {
      return "Zhen Lab, 2019";
   }
}