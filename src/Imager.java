import ij.ImagePlus;
import ij.IJ;

import ij.io.FileInfo;
import ij.io.TiffEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

	FileWriter stimStrengthFrameData;
	FileWriter stagePosFrameData;

	TrackStimController controller;

	ImagingTask(
		CMMCore core_, 
		ScriptInterface app_, 
		long timePoint_, 
		String saveDirectory_, 
		int frameIndex_, 
		FileWriter stimStrengthFrameData_, 
		FileWriter stagePosFrameData_,
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

		saveStagePosToFile(stagePosInfo);
		saveStimStrengthToFile(stimStrength);
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

	private void saveStimStrengthToFile(int currStimStrength){
		if( stimStrengthFrameData != null ){
			try {
				String frameAndStimStrengthData = String.valueOf(frameIndex) + ", " + String.valueOf(currStimStrength) + System.getProperty("line.separator");
				stimStrengthFrameData.write(frameAndStimStrengthData);
			} catch( java.io.IOException e){
				IJ.log("[ERROR] unable to save stim strength to file");
			}
		}
	}

	private void saveStagePosToFile(double[] stagePositionInfo){
		if( stagePosFrameData != null ){
			try {
				// encode stage position in a comma seperated value format
				String stagePosInfoCSV = String.valueOf(stagePositionInfo[0]) + ", " + String.valueOf(stagePositionInfo[1]) + ", " + String.valueOf(stagePositionInfo[2]);
				String frameStagePosStr = String.valueOf(frameIndex) + ", " + stagePosInfoCSV + System.getProperty("line.separator");
				stagePosFrameData.write(frameStagePosStr);
			} catch( java.io.IOException e){
				IJ.log("[ERROR] unable to save stage pos to file");
			}
		}
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

	FileWriter stimStrengthFrameData;  // get the current stimulation strength per frame
	FileWriter stagePosFrameData;      // get the current stage pos per frame

	Imager(TrackStimController c){

		controller = c;

		imagingTasks = new ArrayList<ScheduledFuture>();
		imagingTaskStartTime = 0;
	}

    // schedule a number of snapshots at fixed time interval to ensure that images are taken
    // at the given fps
    public void scheduleImagingTasks(int numFrames, int fps, String rootDirectory){
		String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

		try {
			stimStrengthFrameData = new FileWriter(imageSaveDirectory + "/" + "stim-strength.txt", true);
			stagePosFrameData = new FileWriter(imageSaveDirectory + "/" + "stage-pos.txt", true);	
		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to create stim-strength.txt and stage-pos.txt");
			stimStrengthFrameData = null;
			stagePosFrameData = null;
		}

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
				controller.onImageAcquisitionDone(computeImageTaskTimeInSeconds());
			}
		}, (numFrames - 1) * frameCycleNano, TimeUnit.NANOSECONDS);
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
