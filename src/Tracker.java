import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;

import ij.plugin.filter.RankFilters;

import ij.measure.Measurements;

import java.util.ArrayList;

import java.awt.geom.Point2D;


import mmcorej.CharVector;
import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;

class TrackingTask implements Runnable {
    TrackStimController controller;
    String trackerXYStagePort;

    TrackingTask(TrackStimController controller_, String port){
        controller = controller_;
        trackerXYStagePort = port;
    }

    // return an estimate of the worm position in a binarized image
    // uses center of mass to detect position
    public static double[] detectWormPosition(ImagePlus binarizedImage){
        ImageStatistics stats = binarizedImage.getStatistics(Measurements.CENTROID + Measurements.CENTER_OF_MASS);

        double[] position = { stats.xCenterOfMass, stats.yCenterOfMass };

        return position;
    }

    // apply binarization to an image
    public static ImagePlus binarizeImage(ImagePlus imp, double thresholdValue){
        ImagePlus binarizedImage = imp.duplicate();
        int width = binarizedImage.getWidth();
        int height = binarizedImage.getHeight();

        // invert the image
        ImageProcessor ip = binarizedImage.getProcessor();
        ip.invert();

        // apply rank filtering (sort of like gaussian blur)
        // the second arg put in the rank function is the radius
        // the value is sort of arbitrary and just chosen via empirical
        // observation
        // https://en.wikipedia.org/wiki/Median_filter
        RankFilters rf = new RankFilters();
        rf.rank(ip, 5.0, RankFilters.MEDIAN);

        // get the statistics and threshold according to the mean
        // https://en.wikipedia.org/wiki/Thresholding_(image_processing)
        ImageStatistics stats = binarizedImage.getStatistics();
        ip.threshold( (int) (stats.mean * thresholdValue) );

        binarizedImage.setProcessor(ip);

        return binarizedImage;
    }

    private String translateWormPosToStageCommandVelocity(ImagePlus binarizedImage, double wormPosX, double wormPosY){
        Double wPosX = new Double(wormPosX);
        Double wPosY = new Double(wormPosY);

        int width = binarizedImage.getWidth();
        int height = binarizedImage.getHeight();

        String stageVelocityCommand = null;

        // sometimes a worm position is not able to be detected (it will be NaN)
        if(!wPosX.isNaN() && !wPosY.isNaN()){
            double xDistFromCenter = (width / 2) - wPosX;
            double yDistFromCenter = (height / 2) - wPosY;

            double distScalar = Math.sqrt((xDistFromCenter * xDistFromCenter) + (yDistFromCenter * yDistFromCenter));

            // legacy calculation to caclulate a velocity for the stage
            // dont know what 0.0018 is
            // acceleration factor is injected from the controller, and modified via the UI
            int accelerationFactor = controller.trackerSpeedFactor;
            double xVelocity = Math.round(-xDistFromCenter * accelerationFactor * 0.0018 * 1000.0) / 1000.0;
            double yVelocity = Math.round(yDistFromCenter * accelerationFactor * 0.0018 * 1000.0) / 1000.0;

            stageVelocityCommand = "VECTOR X=" + String.valueOf(xVelocity) + " Y=" + String.valueOf(yVelocity);
        } else {
            TrackingTask.stopAutoTracking(controller.core, trackerXYStagePort);
        }

        return stageVelocityCommand;
    }

    // accelerate to a specific velocity
    // the stage will continue indefinitely unless it is stopped by another command
    private void setXYStageVelocity(String velocityCommand){
        try {
            controller.core.setSerialPortCommand(trackerXYStagePort, velocityCommand, "\r");
            Point2D pos = controller.core.getXYStagePosition();
        } catch (java.lang.Exception e) {
            IJ.log("[ERROR] could not send " + velocityCommand + " command to the stage port");
            IJ.log(e.getMessage());
        }
    }

    // because the stage will keep going after calls to setXYStageVelocity()
    // we need to call this when we want the stage to stop
    public static void stopAutoTracking(CMMCore mmc, String trackerPort){
        String stopVelocitycommand = "VECTOR X=0 Y=0";
        try {
            mmc.setSerialPortCommand(trackerPort, stopVelocitycommand, "\r");
        } catch (java.lang.Exception e) {
            IJ.log("[ERROR] could not send " + stopVelocitycommand + " command to the stage port");
            IJ.log(e.getMessage());
        }
    }

    public void run(){
        if (controller.app.isLiveModeOn()){
            // get the image from micro manager live mode window
            ImagePlus liveModeImage = controller.app.getSnapLiveWin().getImagePlus();

            // binarize the image
            ImagePlus binarized = binarizeImage(liveModeImage, controller.thresholdValue);

            // get an estimate of the worm position from the binarized image
            double[] wormPosition = detectWormPosition(binarized);


            String stageCommand = translateWormPosToStageCommandVelocity(binarized, wormPosition[0], wormPosition[1]);

            setXYStageVelocity(stageCommand);
        }
    }

}

class Tracker {
    TrackStimController controller;

    String trackerXYStagePort;
    boolean initialized = false;

    private ArrayList<ScheduledFuture> trackerTasks;
    private ScheduledExecutorService trackingScheduler;
    private static final int DEFAULT_TRACKING_TASKS_PER_SECOND = 10;

    Tracker(TrackStimController controller_){
        controller = controller_;
        trackerXYStagePort = "";

        trackerTasks = new ArrayList<ScheduledFuture>();
    }

    // find and connect to the motorized xy stage (asi ms-2000)
    public boolean initialize(){
        boolean portFound = false;

        String stageDeviceLabel = controller.core.getXYStageDevice();
        try {
            trackerXYStagePort = controller.core.getProperty(stageDeviceLabel, "Port");
            portFound = true;
            initialized = true;
        } catch(java.lang.Exception e) {
            IJ.log("[ERROR] could not get xy stage port, tracker will not work");
            IJ.log(e.getMessage());
        }

        return portFound;
    }

    public void cancelTasks(){
        for (int k = 0; k < trackerTasks.size(); k++){
            trackerTasks.get(k).cancel(true);
        }

        trackingScheduler.shutdownNow();

        TrackingTask.stopAutoTracking(controller.core, trackerXYStagePort);
    }

    public void scheduleTrackingTasks(int numFrames, int fps) throws java.lang.Exception {
        trackingScheduler = Executors.newSingleThreadScheduledExecutor();
        ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();

        if(!initialized){
            throw new Exception("could not run tracker.  the tracker is not initialized");
        }

        // compute the total number of seconds the imaging tasks will take
        long imagingTaskTimeNano = TimeUnit.SECONDS.toNanos(numFrames / fps);

        // convert num tracking tasks per second to milliseconds
        long trackingCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / DEFAULT_TRACKING_TASKS_PER_SECOND);

        int totalTrackingTasks = (int) (imagingTaskTimeNano / trackingCycleNano);

        // schedule the tracking tasks at time intervals previously computed
        for(int trackingTaskIndex = 0; trackingTaskIndex < totalTrackingTasks; trackingTaskIndex++){
            long timePtNano = trackingTaskIndex * trackingCycleNano; // time when the tracking task will run
            TrackingTask t = new TrackingTask(controller, trackerXYStagePort);
            ScheduledFuture trackingTask = trackingScheduler.schedule(t, timePtNano, TimeUnit.NANOSECONDS);
            futureTasks.add(trackingTask);
        }

        // after all tracking tasks, execute a final task to stop auto tracking
        ScheduledFuture lastTrackingTask = trackingScheduler.schedule(new Runnable() {
            @Override
            public void run(){
                TrackingTask.stopAutoTracking(controller.core, trackerXYStagePort);
            }
        }, (totalTrackingTasks - 1) * trackingCycleNano,  TimeUnit.NANOSECONDS);
        trackerTasks.add(lastTrackingTask);

        trackerTasks = futureTasks;
    }
}