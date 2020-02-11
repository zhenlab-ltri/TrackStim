import java.io.File;

import java.util.ArrayList;
import java.text.DecimalFormat;

import mmcorej.CMMCore;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;

import ij.measure.Measurements;

import java.io.PrintWriter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;


class TrackStimController {
    // take live mode images and process them to show the user
    private ScheduledExecutorService micromanagerLiveModeProcessor;
    private ImagePlus trackerViewImage;

    // main components that generate imaging, stimulation, and tracking tasks
    private TrackStimGUI gui;
    private Stimulator stimulator;
    private Tracker tracker;
    private Imager imager;

    // sycned to ui sliders, used for thresholding images and the tracker velocity
    public volatile double thresholdValue;
    public volatile int trackerSpeedFactor;

    public CMMCore core;
    public ScriptInterface app;

    TrackStimController(CMMCore core_, ScriptInterface app_){
        core = core_;
        app = app_;

        stimulator = new Stimulator(this);
        stimulator.initialize();

        tracker = new Tracker(this);
        tracker.initialize();

        imager = new Imager(this);

        thresholdValue = 1.0;
        trackerSpeedFactor = 3;

        // start processing live mode images to show the user
        micromanagerLiveModeProcessor = Executors.newSingleThreadScheduledExecutor();
        trackerViewImage = new ImagePlus("Tracker View");
        processLiveModeImages();

    }

    public void setGui(TrackStimGUI g){
        gui = g;
    }

    public void destroy(){
        stopImageAcquisition();
        micromanagerLiveModeProcessor.shutdownNow();
        trackerViewImage.changes = false;
        trackerViewImage.close();
    }

    public int getStimulatorStrength(){
        return stimulator.currStimulationStrength;
    }

    public void updateThresholdValue(int newThresholdVal){
        double val = (double) newThresholdVal / 100;
        thresholdValue = 1.0 + val;
    }

    public void updateTrackerSpeedValue(int newSpeedVal){
        trackerSpeedFactor = newSpeedVal;
    }

    // main function called when the user presses the go btn
    // receives imaging, stimulator, and tracking args
    // calls the imager, tracker, and stimulator to schedule tasks
    public void startImageAcquisition(
        int numFrames, int framesPerSecond, String rootDirectory, // imaging args
        boolean enableStimulator, int preStim, int stimStrength, int stimDuration, // stimulator args
        int stimCycleDuration, int numStimCycles, boolean enableRamp,
        int rampBase, int rampStart, int rampEnd,
        boolean enableTracking // tracking args
    ){
        String imageSaveDirectory = FileUtil.createImageSaveDirectory(rootDirectory);

        taskRunningDisableUI();
        // ensure micro manager live mode is on so we can capture images
        if( !app.isLiveModeOn() ){
            app.enableLiveMode(true);
        }

        if( tracker.initialized  && enableTracking ){
            try {
                tracker.scheduleTrackingTasks(numFrames, framesPerSecond);
            } catch (java.lang.Exception e){
                IJ.log("[ERROR] could not start tracking. tracker is not initialized.");
            }
        }

        if( stimulator.initialized && enableStimulator ){
            try {
                stimulator.scheduleStimulationTasks(
                    enableRamp, preStim, stimStrength,
                    stimDuration, stimCycleDuration, numStimCycles,
                    rampBase, rampStart, rampEnd
                );
            } catch (java.lang.Exception e){
                IJ.log("[ERROR] could not start stimulation.  stimulator not initialized.");
            }
        }

        imager.scheduleImagingTasks(numFrames, framesPerSecond, imageSaveDirectory);
        saveImagingJobArgs(imageSaveDirectory, numFrames, framesPerSecond, enableStimulator, preStim, stimStrength, stimDuration, stimCycleDuration, numStimCycles, enableRamp, rampBase, rampStart, rampEnd, enableTracking);
    }

    public void stopImageAcquisition(){
        imager.cancelTasks();
        tracker.cancelTasks();
        stimulator.cancelTasks();

        noTaskRunningEnableUI();
    }

    // called once the imaging tasks are finished
    public void onImageAcquisitionDone(double totalTaskTimeSeconds){
        // stop live mode again
        // show how long it took to finish the task
        // flush the live mode circular buffer
        // start live mode again
        // enable the gui again

        app.enableLiveMode(false);
        String formattedTaskTime = new DecimalFormat("##.##").format(totalTaskTimeSeconds);
        IJ.showMessage("Task finished in " + formattedTaskTime + " seconds");
        try {
            core.clearCircularBuffer();

        } catch(java.lang.Exception e){
            IJ.log("[ERROR] unable to clear circular buffer");
            IJ.log(e.getMessage());
        }
        app.enableLiveMode(true);

        noTaskRunningEnableUI();
    }

    // block ui interaction when a imaging task is running
    private void taskRunningDisableUI(){
        // the user shouldnt be allowed to alter these while task is running
        gui.numFramesText.setEnabled(false);
        gui.framesPerSecondSelector.setEnabled(false);
        gui.changeDirectoryBtn.setEnabled(false);
        gui.enableStimulator.setEnabled(false);
        gui.preStimulationTimeMsText.setEnabled(false);
        gui.stimulationStrengthText.setEnabled(false);
        gui.stimulationDurationMsText.setEnabled(false);
        gui.stimulationCycleDurationMsText.setEnabled(false);
        gui.numStimulationCyclesText.setEnabled(false);
        gui.enableRamp.setEnabled(false);
        gui.rampBase.setEnabled(false);
        gui.rampStart.setEnabled(false);
        gui.rampEnd.setEnabled(false);
        gui.enableTracking.setEnabled(false);
        gui.goBtn.setEnabled(false);

        // the user should be able to stop the task
        gui.stopBtn.setEnabled(true);
    }

    private void noTaskRunningEnableUI(){
        // the user should be allowed to alter these when no task is running
        gui.numFramesText.setEnabled(true);
        gui.framesPerSecondSelector.setEnabled(true);
        gui.changeDirectoryBtn.setEnabled(true);
        gui.enableStimulator.setEnabled(true);
        gui.preStimulationTimeMsText.setEnabled(true);
        gui.stimulationStrengthText.setEnabled(true);
        gui.stimulationDurationMsText.setEnabled(true);
        gui.stimulationCycleDurationMsText.setEnabled(true);
        gui.numStimulationCyclesText.setEnabled(true);
        gui.enableRamp.setEnabled(true);
        gui.rampBase.setEnabled(true);
        gui.rampStart.setEnabled(true);
        gui.rampEnd.setEnabled(true);
        gui.enableTracking.setEnabled(true);
        gui.goBtn.setEnabled(true);

        // the user should not be able to press stop when there is no task running
        gui.stopBtn.setEnabled(false);
        gui.goBtn.setEnabled(true);
    }

    // show processed binarized images and where the center of mass is
    // (ideally it will be wormPos, but not always)
    private void processLiveModeImages(){
        micromanagerLiveModeProcessor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run(){
                if (app.isLiveModeOn()){
                    // take the current live mode image, binarize it and show the result
                    ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();

                    ImagePlus binarized = TrackingTask.binarizeImage(liveModeImage, thresholdValue);
                    double[] wormPosition = TrackingTask.detectWormPosition(binarized);

                    Double wormPosX = new Double(wormPosition[0]);
                    Double wormPosY = new Double(wormPosition[1]);

                    if(!wormPosX.isNaN() && !wormPosY.isNaN()){
                        PointRoi centerOfMassRoi = new PointRoi(wormPosX, wormPosY);
                        trackerViewImage.setRoi(centerOfMassRoi);
                    }

                    trackerViewImage.setProcessor(binarized.getProcessor());
                    trackerViewImage.show("Tracker View");
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
 

	// save all job arguments for later reference
	private void saveImagingJobArgs(
        String directory, int frameArg, int fpsArg,
        boolean useStim, int preStim, int stimStr, 
        int stimDur, int stimCycleDur, int numCycle, 
        boolean useRamp, int rampBase, int rampStart, int rampEnd,
        boolean useTracking
    ){
        ArrayList<String> content = new ArrayList<String>();
        content.add("number of frames: " + String.valueOf(frameArg));
        content.add("frames per second: " + String.valueOf(fpsArg));
        content.add("stimulator enabled: " + String.valueOf(useStim));

        if(useStim){
            content.add("pre stimulation (ms): " + String.valueOf(preStim));
            content.add("stimulation strength: " + String.valueOf(stimStr));
            content.add("stimulation duration (ms): " + String.valueOf(stimDur)); 
            content.add("stimulation cycle duration (ms): " + String.valueOf(stimCycleDur)); 
            content.add("number of cycles: " + String.valueOf(numCycle)); 
            content.add("ramp enabled: " + String.valueOf(useRamp)); 

            if(useRamp){
                content.add("ramp base: " + String.valueOf(rampBase)); 
                content.add("ramp start: " + String.valueOf(rampStart)); 
                content.add("ramp end: " + String.valueOf(rampEnd));    
            }
        }
        content.add("auto-tracking enabled: " + String.valueOf(useTracking));

        FileUtil.saveContentToTextFile(directory, "job-parameters", (String[]) content.toArray());
    }
}
