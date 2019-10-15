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
   public TrackStimGUI gui;
   public Studio studio;
   public CMMCore mmc;
   public LogManager log;
   public DataManager dm;
   public DisplayManager dism;

   @Override
   public void setContext(Studio studio) {
      studio = studio;
      mmc = studio.getCMMCore();
      log = studio.getLogManager();
      dm = studio.data();
      dism = studio.displays();

      gui = new TrackStimGUI(studio);
   }

   /**
    * This method is called when the plugin's menu option is selected.
    */
   @Override
   public void onPluginSelected() {
      gui.setVisible(true);
   }

   public void start() {     
      int nrFrames = 100;
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