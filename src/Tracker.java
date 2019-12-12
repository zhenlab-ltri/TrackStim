import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ByteProcessor;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;

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
    // 
    public static double[] detectWormPosition(ImagePlus binarizedImage){
        ImageStatistics stats = binarizedImage.getStatistics(Measurements.CENTROID + Measurements.CENTER_OF_MASS);

        double[] position = { stats.xCenterOfMass, stats.yCenterOfMass };    

        return position;
    }

    public static ImagePlus binarizeImage(ImagePlus imp, double thresholdValue){
        ImagePlus binarizedImage = imp.duplicate();
        int width = binarizedImage.getWidth();
        int height = binarizedImage.getHeight();
        ImageProcessor ip = binarizedImage.getProcessor();
        ip.setRoi(0, 0, width, height);
        ip = ip.crop();
        ip.invert();

        ImageStatistics stats = binarizedImage.getStatistics();
        ip.threshold( (int) (stats.mean * thresholdValue) );

        binarizedImage.setProcessor(ip);

        return binarizedImage;
    }

    private String translateWormPosToStageCommandVelocity(ImagePlus binarizedImage, double[] wormPosition){
        Double wormPosX = new Double(wormPosition[0]);
        Double wormPosY = new Double(wormPosition[1]);

        int width = binarizedImage.getWidth();
        int height = binarizedImage.getHeight();

        String stageVelocityCommand = null;

        // sometimes a worm position is not able to be detected
        if(!wormPosX.isNaN() && !wormPosY.isNaN()){
            double xDistFromCenter = (width / 2) - wormPosX;
            double yDistFromCenter = (height / 2) - wormPosY;

            double distScalar = Math.sqrt((xDistFromCenter * xDistFromCenter) + (yDistFromCenter * yDistFromCenter));


            try {
                // controller.core.setSerialPortCommand(trackerXYStagePort, velocityCommand, "\r");
                Point2D pos = controller.core.getXYStagePosition();
                double nextX = pos.getX() - xDistFromCenter;
                double nextY = pos.getY() - yDistFromCenter;

                controller.core.setXYPosition(nextX, nextY);
                

            } catch (java.lang.Exception e) {
                IJ.log(e.getMessage());
            }

            // legacy calculation to caclulate a velocity for the stage to move to
            double xVelocity = Math.round(-xDistFromCenter * 0.0018 * 1000.0) / 1000.0;
            double yVelocity = Math.round(yDistFromCenter * 0.0018 * 1000.0) / 1000.0;

            stageVelocityCommand = "VECTOR X=" + String.valueOf(xVelocity) + " Y=" + String.valueOf(yVelocity);
        }   

        return stageVelocityCommand;
    }

    private void sendXYStageCommand(String velocityCommand){
        try {
            // controller.core.setSerialPortCommand(trackerXYStagePort, velocityCommand, "\r");
            Point2D pos = controller.core.getXYStagePosition();


        } catch (java.lang.Exception e) {
            IJ.log("startAcq: error setting serial port command " + velocityCommand);
            IJ.log(e.getMessage());
        }
    }

    public void run(){
        if (controller.app.isLiveModeOn()){
            ImagePlus liveModeImage = controller.app.getSnapLiveWin().getImagePlus();

            ImagePlus binarized = binarizeImage(liveModeImage, controller.thresholdValue);
            double[] wormPosition = detectWormPosition(binarized);
            String stageCommand = translateWormPosToStageCommandVelocity(binarized, wormPosition);
            // sendXYStageCommand(stageCommand);
        }
    }

}

class Tracker {
    TrackStimController controller;

    String trackerXYStagePort;
    boolean initialized = false;

    private ArrayList<ScheduledFuture> trackerTasks;
    private static final int NUM_TRACKING_TASKS_PER_SECOND = 1;

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

        IJ.log("[INFO] tracker xy stage port is " + trackerXYStagePort);

        return portFound;
    }

    public void cancelTasks(){
        for (int k = 0; k < trackerTasks.size(); k++){
            trackerTasks.get(k).cancel(true);
        }
    }

    public void scheduleTrackingTasks(int numFrames, int fps){
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();

        long imagingTaskTimeNano = TimeUnit.SECONDS.toNanos(numFrames / fps);
        
        // perform tracking every 250ms
        long trackingCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / NUM_TRACKING_TASKS_PER_SECOND);

        int totalTrackingTasks = (int) (imagingTaskTimeNano / trackingCycleNano);

        IJ.log("[INFO] total tracking tasks " + String.valueOf(totalTrackingTasks));


        for(int trackingTaskIndex = 0; trackingTaskIndex < 5; trackingTaskIndex++){
            long timePtNano = trackingTaskIndex * trackingCycleNano; // e.g. 0 ms, 250ms, 500ms, etc..
            TrackingTask t = new TrackingTask(controller, trackerXYStagePort);
            ScheduledFuture trackingTask = ses.schedule(t, timePtNano, TimeUnit.NANOSECONDS);
            futureTasks.add(trackingTask);
        }

        trackerTasks = futureTasks;
    }
}