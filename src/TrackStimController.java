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

    private TrackStimGUI gui;
    private Stimulator stimulator;
    private Tracker tracker;
    private Imager imager;

    // sycned to threshold slider, used for thresholding images
    public volatile double thresholdValue;

    public CMMCore core;
    public ScriptInterface app;

    TrackStimController(CMMCore core_, ScriptInterface app_){
        core = core_;
        app = app_;

        stimulator = new Stimulator(core_);
        stimulator.initialize();

        tracker = new Tracker(this);
        tracker.initialize();

        imager = new Imager(core_, app_);
        
        thresholdValue = 1.0;
        
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

    public void startImageAcquisition(int numFrames, int framesPerSecond, String rootDirectory){

        // ensure micro manager live mode is on so we can capture images
        if( !app.isLiveModeOn() ){
            app.enableLiveMode(true);
        }

        if( stimulator.initialized ){
            try {
                // stimulatorTasks = stimulator.scheduleStimulationTasks(
                // false, 30000, 63, 
                // 15000, 30000, 2, 
                // 0, 0, 63);
            } catch (java.lang.Exception e){
                IJ.log("[ERROR] could not start stimulation.  stimulator not initialized.");
            }
        }

        if( tracker.initialized ){
            try {
                tracker.scheduleTrackingTasks(numFrames, framesPerSecond);
            } catch (java.lang.Exception e){
                IJ.log("[ERROR] could not start tracking. tracker is not initialized.");
            }
        }

        imager.scheduleImagingTasks(numFrames, framesPerSecond, rootDirectory);
    }

    public void stopImageAcquisition(){
        imager.cancelTasks();
        tracker.cancelTasks();
        stimulator.cancelTasks();
    }

    // show processed binarized images and show where the center of mass is 
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
