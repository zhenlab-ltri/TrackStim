import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class TrackStimPlugin implements MMPlugin {
   public static final String menuName = "TrackStim";
   public static final String tooltipDescription =
      "TrackStim";

   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   private ScriptInterface app_;
   // Provides access to the Micro-Manager Core API (for direct hardware
   // control)
   private CMMCore core_;

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
        TrackStimGUI ts = new TrackStimGUI(core_, app_);
   }

   @Override
   public String getInfo () {
      return "C. elegans imaging.";
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
      return "Zhen lab";
   }
}
