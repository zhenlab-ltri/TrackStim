package TrackStim;

import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.ItemListener;

import mmcorej.CMMCore;

import ij.plugin.frame.PlugInFrame;
import ij.ImageListener;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;


public class TrackStim implements MMPlugin {
   public static final String menuName = "TrackStim";
   public static final String tooltipDescription = "Real-time imaging of C.Elegans";


   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   private ScriptInterface app_;
   // Provides access to the Micro-Manager Core API (for direct hardware
   // control)
   private CMMCore core_;

   private TrackStimGUI gui;

   @Override
   public void setApp(ScriptInterface app) {
      app_ = app;
      core_ = app.getMMCore();
   }

   @Override
   public void dispose() {
      // We do nothing here as the only object we create, our dialog, should
      // be dismissed by the user.
   }

   @Override
   public void show() {
      // JOptionPane.showMessageDialog(null, "Hello, world!", "Hello world!",
      //       JOptionPane.PLAIN_MESSAGE);
      gui = new TrackStimGUI();
   }

   @Override
   public String getInfo () {
      return "Displays a simple greeting.";
   }

   @Override
   public String getDescription() {
      return tooltipDescription;
   }

   @Override
   public String getVersion() {
      return "1.0";
   }

   @Override
   public String getCopyright() {
      return "University of California, 2015";
   }
}
