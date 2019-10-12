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

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class TrackStim implements SciJavaPlugin, MenuPlugin {
   private TrackStimGUI gui;
   private Studio studio;
   private CMMCore mmc;
   private LogManager log;

   @Override
   public void setContext(Studio studio) {
      studio = studio;
      mmc = studio.getCMMCore();
      log = studio.getLogManager();
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
      int numImages = 100;
      int curImage = 0;
      double intervalMs = 100.0;
      DataManager dm = studio.data();
      Datastore store = dm.createRAMDatastore();

      studio.displays().createDisplay();

      try {
         mmc.startSequenceAcquisition(numImages, intervalMs, false);
      } catch (java.lang.Exception e) {
         log.logMessage("error starting sequence acquisition");
         log.logMessage(e.getMessage());
      }

      Coords.Builder builder = Coordinates.builder().z(0).channel(0).stagePosition(0);

      while(mmc.isSequenceRunning()) {
         int imagesRemaining = mmc.getRemainingImageCount();
         log.logMessage(String.valueOf(imagesRemaining) + " images in queue");

         if( imagesRemaining > 0 ){

            try {
               TaggedImage tImg = mmc.popNextTaggedImage();
               Image img = studio.data().convertTaggedImage(
                  tImg,
                  builder.time(curImage).build(),
                  null
               );
               store.putImage(img);
               curImage++;
            } catch (java.lang.Exception e ){
               log.logMessage(e.getMessage());
            }
         }
         log.logMessage("sequence running");
      }

      try {
         mmc.stopSequenceAcquisition();
      } catch (java.lang.Exception e ){
         log.logMessage(e.getMessage());
      }
      studio.displays().manage(store);
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