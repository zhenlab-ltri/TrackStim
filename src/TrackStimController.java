import java.io.File;

import java.util.ArrayList;

import mmcorej.CMMCore;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;


class TrackStimController {
    private ArrayList<ScheduledFuture> snapshotTasks;
    private ArrayList<ScheduledFuture> stimulatorTasks;
    private String stimulatorPort;

    // take live mode images and process them to show the user
    private ScheduledExecutorService micromanagerLiveModeProcessor;
    private ImagePlus processedImageWindow;

    // sycned to threshold slider, used for thresholding images
    public volatile double thresholdValue;

    CMMCore core;
    ScriptInterface app;

    TrackStimGUI gui;
    

    TrackStimController(CMMCore core_, ScriptInterface app_){
        core = core_;
        app = app_;
        snapshotTasks = new ArrayList<ScheduledFuture>(); 
        stimulatorTasks = new ArrayList<ScheduledFuture>();
        
        thresholdValue = 1.0;
        
        micromanagerLiveModeProcessor = Executors.newSingleThreadScheduledExecutor();
        processedImageWindow = new ImagePlus("Binarized images");
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

        // create a new directory in rootDirectory to save images to
        String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

        // ensure micro manager live mode is on so we can capture images
        if( !app.isLiveModeOn() ){
            app.enableLiveMode(true);
        }

        snapshotTasks = scheduleSnapshots(numFrames, framesPerSecond, imageSaveDirectory);
    }

    public void stopImageAcquisition(){
        // cancel getting images
        for (int i = 0; i < snapshotTasks.size(); i++ ){
            snapshotTasks.get(i).cancel(true);
        }

        // cancel turning on the stimulator
        for (int j = 0; j < stimulatorTasks.size(); j++ ){
            snapshotTasks.get(j).cancel(true);
        }
    }

    // periodically takes images from live mode and applies thresholding to them
    private void processLiveModeImages(){
        micromanagerLiveModeProcessor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run(){
                if (app.isLiveModeOn()){
                    IJ.log("[INFO] Processing live mode image");
                    // take the current live mode image, binarize it and show the result
                    ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();
                    ImagePlus inverted = liveModeImage.duplicate();
                    int width = inverted.getWidth();
                    int height = inverted.getHeight();
                    ImageProcessor ip = inverted.getProcessor();
                    ip.setRoi(0, 0, width, height);
                    ip.invert();

                    ImageStatistics stats = inverted.getStatistics();
                    ip.threshold( (int) (stats.mean * thresholdValue) );
                    processedImageWindow.setProcessor(ip);

                    stats = inverted.getStatistics();
                    double centerOfMassX = stats.xCenterOfMass;
                    double centerOfMassY = stats.yCenterOfMass;
                    
                    IJ.log("[INFO] Center of mass is x: " + String.valueOf(centerOfMassX) + ", y: " + String.valueOf(centerOfMassY) );

                    PointRoi centerOfMassRoi = new PointRoi(centerOfMassX, centerOfMassY);
                    processedImageWindow.setRoi(centerOfMassRoi);

                    processedImageWindow.setProcessor(ip);
                    processedImageWindow.show();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    // schedule a number of snapshot at fixed time interval to ensure that images are taken
    // at the given fps
    private ArrayList<ScheduledFuture> scheduleSnapshots(int numFrames, int fps, String saveDirectory){
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();
        fps = 10; // fix fps at 10 for now

        long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every 100ms

        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ScheduledSnapshot s = new ScheduledSnapshot(core, app, timePtNano, saveDirectory, curFrameIndex);

            ScheduledFuture snapShot = ses.schedule(s, timePtNano, TimeUnit.NANOSECONDS);
            futureTasks.add(snapShot);
        }

        return futureTasks;
    }
    
    private String createImageSaveDirectory(String root){
        // get count number of directories N so that we can create directory N+1
        File saveDirectoryFile = new File(root);
        File[] fileList = saveDirectoryFile.listFiles();
        int numSubDirectories = 0;
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isDirectory()) {
                numSubDirectories++;
            }
        }

        // choose first temp<i> which does not exist yet and create directory with name tempi
        int i = 1;
        File newdir = new File(root + "temp" + String.valueOf(numSubDirectories + i));
        while (newdir.exists()) {
            i++;
            newdir = new File(root + "temp" + String.valueOf(numSubDirectories + i));
        }

        newdir.mkdir();
        return newdir.getPath();
    }

}
