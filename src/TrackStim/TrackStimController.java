/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrackStim;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.micromanager.Studio;
import org.micromanager.LogManager;

import org.micromanager.data.Datastore;
import org.micromanager.data.DataManager;
import org.micromanager.data.Coords;
import org.micromanager.data.Image;

import org.micromanager.display.DisplayManager;
import org.micromanager.display.DisplayWindow;


// Performs image acqusition, analysis, and tracking
// - receives commands to start/stop image acquisitions
// - once image acquisition tasks are finished, notifies the UI
public class TrackStimController implements Runnable {
    public TrackStimGUI gui;

    public Studio studio;    
    public CMMCore mmc;
    public LogManager lm;
    public DataManager dm;
    public DisplayManager dism;

    private int numFrames;
    private double exposureMs;
    private String savePath;

    private final int skipCount = 10;

    public TrackStimController(Studio studio_, TrackStimGUI gui_, int numFrames_, double exposureMs_, String directoryPath_){
        gui = gui_;
                
        // get micro manager scripting utilities and the micro manager core
        studio = studio_;
        mmc = studio.getCMMCore();
        lm = studio.logs();
        dm = studio.data();
        dism = studio.displays();

        numFrames = numFrames_ * skipCount; // we need num frames to be 10x more because we are taking 1 image every 10 frames
        exposureMs = exposureMs_;
        Path directoryPath = Paths.get(directoryPath_);
        savePath = Paths.get(directoryPath_, "temp-" + String.valueOf(new Date().getTime())).toString();

        // hard code binning for now
        this.setCameraProperties(exposureMs, "4x4");
    }

    // set camera properties
    // exposure should be in ms
    // binning should be one of "1x1", "2x2", or "4x4"
    public void setCameraProperties(double exposureMs, String binning){
        String cameraName = mmc.getCameraDevice();
        try {
            mmc.setProperty(cameraName, "Exposure", String.valueOf(exposureMs));
            mmc.setProperty(cameraName, "Binning", binning);
        } catch (Exception ex) {
            lm.logMessage("unable to set camera properties");
            lm.logMessage(ex.getMessage());
        }
    }

    @Override
    public void run(){
        studio.live().setLiveMode(false);
        // Create a Datastore for the images to be stored in, in RAM.
        Datastore store = dm.createRAMDatastore();
        // Create a display to show images as they are acquired.
        DisplayWindow ds = dism.createDisplay(store);

        Coords.CoordsBuilder builder = dm.getCoordsBuilder().time(0);

        // Start collecting images.
        // Arguments are the number of images to collect, the amount of time to wait
        // between images, and whether or not to halt the acquisition if the
        // sequence buffer overflows.
        long imageAcqusitionStartTime = System.nanoTime();

        try {
            mmc.startSequenceAcquisition(numFrames, 0, true);

            int curFrame = 0;
            int curImage = 0;
            // check that there are still images to get or check if TrackStimGUI has cancelled this imaging task
            // TrackStimGUI cancels this task by calling thread.interrupt().  We can check if the thread was interrupted by using isInterrupted()
            while ((mmc.getRemainingImageCount() > 0 || mmc.isSequenceRunning(mmc.getCameraDevice())) && !Thread.currentThread().isInterrupted()) {
                if (mmc.getRemainingImageCount() > 0) {
                TaggedImage tagged = mmc.popNextTaggedImage();
                // Convert to an Image at the desired timepoint.

                if( curImage % skipCount == 0 ) {
                    Image image = dm.convertTaggedImage(tagged,
                    builder.time(curFrame).build(), null);
                    store.putImage(image);
                    curFrame++;
                }
                
                curImage++;
                }
            }

            mmc.stopSequenceAcquisition();
        } catch ( java.lang.Exception e ){
            lm.logMessage("error");
            lm.logMessage(e.getMessage());
        }

        Long imageAcqusitionEndTime = System.nanoTime();

        lm.logMessage("image acqusiition finished in " + String.valueOf( (imageAcqusitionEndTime - imageAcqusitionStartTime) / 1000000 ) + "ms");

        // Have Micro-Manager handle logic for ensuring data is saved to disk.
        try {
            store.save(Datastore.SaveMode.SINGLEPLANE_TIFF_SERIES, savePath);
        } catch (java.io.IOException e){ 
            lm.logMessage("error saving");
            lm.logMessage(e.getMessage());
        }
        dism.manage(store);
        ds.requestToClose();

        gui.taskDone();

        lm.showMessage("Finished imaging task");
        studio.live().setLiveMode(true);
    }
}
