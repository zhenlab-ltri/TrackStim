import ij.ImagePlus;
import ij.IJ;

import ij.io.FileInfo;
import ij.io.TiffEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;

// Take an image and save it
class ImagingTask implements Runnable {
	CMMCore core;
	ScriptInterface app;
	long timePoint;
	String saveDirectory;
	int frameIndex;

	String[] stimStrengthFrameData;
	String[] stagePosFrameData;

	TrackStimController controller;

	ImagingTask(
		CMMCore core_, 
		ScriptInterface app_, 
		long timePoint_, 
		String saveDirectory_, 
		int frameIndex_, 
		String[] stimStrengthFrameData_, 
		String[] stagePosFrameData_,
		TrackStimController c
		){
		core = core_;
		timePoint = timePoint_;
		app = app_;
		saveDirectory = saveDirectory_;
		frameIndex = frameIndex_;

		stimStrengthFrameData = stimStrengthFrameData_;
		stagePosFrameData = stagePosFrameData_;

		controller = c;
	}

	public void run(){
		if( !app.isLiveModeOn() ){
			IJ.log("[ERROR] Could not acquire image.  Live mode must be on.  Please press STOP." );
			return;
		}

		ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();
		double[] stagePosInfo = getStagePositionInfo();
		int stimStrength = controller.getStimulatorStrength();

		// add the stim strength at this current frame to the data to save
		String frameAndStimStrengthData = String.valueOf(frameIndex) + ", " + String.valueOf(stimStrength) + System.getProperty("line.separator");
		stimStrengthFrameData[frameIndex] = frameAndStimStrengthData;

		// add the stage position at this current frame to the data to save
		String stagePosInfoCSV = String.valueOf(stagePosInfo[0]) + ", " + String.valueOf(stagePosInfo[1]) + ", " + String.valueOf(stagePosInfo[2]);
		String frameStagePosStr = String.valueOf(frameIndex) + ", " + stagePosInfoCSV + System.getProperty("line.separator");
		stagePosFrameData[frameIndex] = frameStagePosStr;

		saveSnapshotToTiff(liveModeImage, stagePosInfo);
	}

	private double[] getStagePositionInfo(){
		double currXPos = 0.0;
		double currYPos = 0.0;
		double currZPos = 0.0;

		try {
			currXPos = core.getXPosition();
			currYPos = core.getYPosition();
			currZPos = core.getPosition(); // getPosition gives z position for some reason
		} catch (java.lang.Exception e){
			IJ.log("[ERROR] unable to get stage position from micro manager core");
			IJ.log(e.getMessage());
		}

		return new double[]{ currXPos, currYPos, currZPos };
	}

	private void saveSnapshotToTiff(ImagePlus snapshot, double[] stagePosInfo){
		String filePath = saveDirectory + "/" + String.valueOf(frameIndex) + ".tiff";
		FileInfo fi = snapshot.getFileInfo();

		// legacy info that Yanning and Anson scripts depend on
		String stagePositionInfoString ="xpos=" + String.valueOf(stagePosInfo[0]) +
		",ypos=" + String.valueOf(stagePosInfo[1]) +
		",zpos=" + String.valueOf(stagePosInfo[2]);

		fi.info = stagePositionInfoString;

		try {
			File toSave = new File(filePath);
			toSave.createNewFile();
			FileOutputStream outputStream = new FileOutputStream(toSave);
			TiffEncoder te = new TiffEncoder(fi);

			te.write(outputStream);
			outputStream.close();
		} catch (java.lang.Exception e){
			IJ.log("[ERROR] unable to write tiff file with stage position info");
			IJ.log(e.getMessage());
		}
	}
}

// Handles the scheduling of imaging tasks 
class Imager {

	TrackStimController controller;

	private ArrayList<ScheduledFuture> imagingTasks;
	private ScheduledExecutorService imagingScheduler;
	private long imagingTaskStartTime;

	private String[] stimStrengthFrameData;  // each frame will append the current stimulator value here
	private String[] stagePosFrameData;      // each frame will append its stage position here

	Imager(TrackStimController c){

		controller = c;

		imagingTasks = new ArrayList<ScheduledFuture>();
		imagingTaskStartTime = 0;
	}

    // schedule a number of snapshots at fixed time interval to ensure that images are taken
    // at the given fps
    public void scheduleImagingTasks(int numFrames, int fps, String rootDirectory){
		final String imageSaveDirectory = createImageSaveDirectory(rootDirectory);
		stimStrengthFrameData = new String[numFrames];
		stagePosFrameData = new String[numFrames];

    	imagingScheduler = Executors.newSingleThreadScheduledExecutor();
   		ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();

		long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every cycle

		imagingTaskStartTime = System.nanoTime();

		// schedule when each frame should be taken
        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ImagingTask s = new ImagingTask(
				controller.core, 
				controller.app, 
				timePtNano, 
				imageSaveDirectory, 
				curFrameIndex, 
				stimStrengthFrameData,
				stagePosFrameData,
				controller
			);

            ScheduledFuture snapShot = imagingScheduler.schedule(s, timePtNano, TimeUnit.NANOSECONDS);
            futureTasks.add(snapShot);
		}

		// schedule an additional task to let the controller know when the last frame has been taken
		ScheduledFuture lastImagingTask = imagingScheduler.schedule(new Runnable() {
			@Override
			public void run(){
				saveStimStrengthDataToFile(imageSaveDirectory);
				saveStagePosDataToFile(imageSaveDirectory);
				controller.onImageAcquisitionDone(computeImageTaskTimeInSeconds());
			}
		}, (numFrames + 1) * frameCycleNano, TimeUnit.NANOSECONDS);
			imagingTasks.add(lastImagingTask);

      imagingTasks = futureTasks;
    }

	// cancel all imaging tasks
    public void cancelTasks(){
		for (int i = 0; i < imagingTasks.size(); i++ ){
			ScheduledFuture task = imagingTasks.get(i);
      		task.cancel(true);
		}

		imagingScheduler.shutdownNow();
	}

	// take all the stim strength data per frame and save it to a file
	private void saveStimStrengthDataToFile(String directory){
		PrintWriter p = null;
		try {
			p = new PrintWriter(directory + "/" + "stim-strength.txt");

			for( int i = 0; i < stimStrengthFrameData.length; i++ ){
				p.println(stimStrengthFrameData[i]);
			}
		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to write stim strength file");
		} finally {
			if( p != null ){
				p.close();
			}
		}
	}
	
	// take all the stage pos data per frame and save it to a file
	private void saveStagePosDataToFile(String directory){
		PrintWriter p = null;
		try {
			p = new PrintWriter(directory + "/" + "stage-pos.txt");

			for( int i = 0; i < stagePosFrameData.length; i++ ){
				p.println(stagePosFrameData[i]);
			}
		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to write stage pos file");
		} finally {
			if( p != null ){
				p.close();
			}
		}
	}

	private double computeImageTaskTimeInSeconds(){
		double imagingTaskDoneTime = System.nanoTime();
		return (imagingTaskDoneTime - imagingTaskStartTime) / 1000000000.0;
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
		return newdir.getPath();
	}
}
