import java.io.File;

import java.util.ArrayList;

import mmcorej.CMMCore;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;

import ij.measure.Measurements;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;


class TrackStimController {
    // take live mode images and process them to show the user
    private ScheduledExecutorService micromanagerLiveModeProcessor;
    private ImagePlus binarizedLiveModeImage;

    // main components that generate imaging, stimulation, and tracking tasks
    private TrackStimGUI gui;
    private Stimulator stimulator;
    private Tracker tracker;
    private Imager imager;

    // sycned to threshold slider, used for thresholding images
    public volatile double thresholdValue;
    public volatile int trackerSpeedFactor;

    public CMMCore core;
    public ScriptInterface app;

    TrackStimController(CMMCore core_, ScriptInterface app_){
        core = core_;
        app = app_;

        stimulator = new Stimulator(core_);
        stimulator.initialize();

        tracker = new Tracker(this);
        tracker.initialize();

        imager = new Imager(this);

        thresholdValue = 1.0;
        trackerSpeedFactor = 3;

        micromanagerLiveModeProcessor = Executors.newSingleThreadScheduledExecutor();
        binarizedLiveModeImage = new ImagePlus("Binarized images");
        processLiveModeImages();

    }

    public void setGui(TrackStimGUI g){
        gui = g;
    }

    public void destroy(){
        stopImageAcquisition();
        micromanagerLiveModeProcessor.shutdownNow();
    }

    public void updateThresholdValue(int newThresholdVal){
        double val = (double) newThresholdVal / 100;
        thresholdValue = 1.0 + val;
    }

    public void updateTrackerSpeedValue(int newSpeedVal){
        trackerSpeedFactor = newSpeedVal;
    }

    public void startImageAcquisition(
        int numFrames, int framesPerSecond, String rootDirectory, // imaging args
        boolean enableStimulator, int preStim, int stimStrength, int stimDuration, // stimulator args
        int stimCycleDuration, int numStimCycles, boolean enableRamp,
        int rampBase, int rampStart, int rampEnd,
        boolean enableTracking // tracking args
    ){

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

        imager.scheduleImagingTasks(numFrames, framesPerSecond, rootDirectory);
    }

    public void stopImageAcquisition(){
        imager.cancelTasks();
        tracker.cancelTasks();
        stimulator.cancelTasks();
    }

    public void onImageAcquisitionDone(double totalTaskTimeSeconds){
        // stop live mode again
        // show how long it took to finish the task
        // start live mode again
        // enable the gui again

        app.enableLiveMode(false);
        IJ.showMessage("Task finished in " + String.valueOf(totalTaskTimeSeconds) + " seconds");
        app.enableLiveMode(true);

        noTaskRunningEnableUI();
    }

    // block ui interaction when a imaging task is running
    private void taskRunningDisableUI(){
        // the user shouldnt be allowed to alter these while task is running
        gui.numFramesText.setEnabled(false);
        gui.framesPerSecondText.setEnabled(false);
        gui.saveDirectoryText.setEnabled(false);
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
        gui.framesPerSecondText.setEnabled(true);
        gui.saveDirectoryText.setEnabled(true);
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
                        binarizedLiveModeImage.setRoi(centerOfMassRoi);
                    }

                    binarizedLiveModeImage.setProcessor(binarized.getProcessor());
                    binarizedLiveModeImage.show();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
}
