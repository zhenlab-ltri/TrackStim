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

    // current job id
    public int currentJobId;

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
        trackerSpeedFactor = 7;
        currentJobId = 0;

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
        String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

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

                    ImagePlus binarized = TrackingTask.processCalciumImage(liveModeImage);
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

    // create a directory of the form temp<i> where i is the first available
	// i such that temp<i> can be created
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
        currentJobId = numSubDirectories + i;
		return newdir.getPath();
    }

	// save all job arguments for later reference
	private void saveImagingJobArgs(
        String directory,
        int frameArg,
        int fpsArg,
        boolean useStim,
        int preStim,
        int stimStr,
        int stimDur,
        int stimCycleDur,
        int numCycle,
        boolean useRamp,
        int rampBase,
        int rampStart,
        int rampEnd,
        boolean useTracking
    ){
		PrintWriter p = null;
		try {
			p = new PrintWriter(directory + "/" + "temp" + String.valueOf(currentJobId) + "_" + "job-args.txt");

            p.println("number of frames: " + String.valueOf(frameArg));
            p.println("frames per second: " + String.valueOf(fpsArg));
            p.println("stimulator enabled: " + String.valueOf(useStim));

            if(useStim){
                p.println("pre stimulation (ms): " + String.valueOf(preStim));
                p.println("stimulation strength: " + String.valueOf(stimStr));
                p.println("stimulation duration (ms): " + String.valueOf(stimDur));
                p.println("stimulation cycle duration (ms): " + String.valueOf(stimCycleDur));
                p.println("number of cycles: " + String.valueOf(numCycle));
                p.println("ramp enabled: " + String.valueOf(useRamp));

                if(useRamp){
                    p.println("ramp base: " + String.valueOf(rampBase));
                    p.println("ramp start: " + String.valueOf(rampStart));
                    p.println("ramp end: " + String.valueOf(rampEnd));
                }
            }
            p.println("auto-tracking enabled: " + String.valueOf(useTracking));

		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to write job args to file");
		} finally {
			if( p != null ){
				p.close();
			}
        }
    }
}
