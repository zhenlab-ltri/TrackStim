package TrackStim;

import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.ItemListener;

import mmcorej.CMMCore;

import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.LogManager;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class TrackStim implements SciJavaPlugin, MenuPlugin {
   private TrackStimGUI gui;
   private Studio studio_;
   private CMMCore mmc_;
   private LogManager log;

   @Override
   public void setContext(Studio studio) {
      studio_ = studio;
      mmc_ = studio.getCMMCore();
      log = studio.getLogManager();
   }

   /**
    * This method is called when the plugin's menu option is selected.
    */
   @Override
   public void onPluginSelected() {
      gui = new TrackStimGUI();
      this.start();
   }

   public void start() {
      int numImages = 100;
      double intervalMs = 100.0;

      try {
        mmc_.startSequenceAcquisition(numImages, intervalMs, false);
      } catch (java.lang.Exception e) {
        log.logMessage("error starting sequence acquisition");
        log.logMessage(e.getMessage());
      }

      while(mmc_.isSequenceRunning()) {
         log.logMessage("sequence running");
      }

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