import ij.ImagePlus;
import ij.IJ;

import ij.io.FileInfo;
import ij.io.TiffEncoder;

import java.io.File;
import java.io.FileOutputStream;
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

	int[] frameStimStrengthMap;
	String[]framePosMap;

	TrackStimController controller;

	ImagingTask(
		CMMCore core_, 
		ScriptInterface app_, 
		long timePoint_, 
		String saveDirectory_, 
		int frameIndex_, 
		int[] frameStimStrengthMap_, 
		String[] framePosMap_,
		TrackStimController c
		){
		core = core_;
		timePoint = timePoint_;
		app = app_;
		saveDirectory = saveDirectory_;
		frameIndex = frameIndex_;

		frameStimStrengthMap = frameStimStrengthMap_;
		framePosMap = framePosMap_;

		controller = c;
	}

	public void run(){
		if( !app.isLiveModeOn() ){
			IJ.log("[ERROR] Could not acquire image.  Live mode must be on.  Please press STOP." );
			return;
		}

		ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();
		String stagePosInfo = getStagePositionInfo();
		int stimStrength = controller.getStimulatorStrength();

		frameStimStrengthMap[frameIndex] = stimStrength;
		framePosMap[frameIndex] = stagePosInfo;

		saveSnapshotToTiff(liveModeImage, stagePosInfo);
	}

	// encode XYZ stage position as a string
	private String getStagePositionInfo(){
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

		String stagePositionInfo ="xpos=" + String.valueOf(currXPos) +
			",ypos=" + String.valueOf(currYPos) +
			",zpos=" + String.valueOf(currZPos);

		return stagePositionInfo;
	}

	private void saveSnapshotToTiff(ImagePlus snapshot, String stagePosInfo){
		String filePath = saveDirectory + "/" + String.valueOf(frameIndex) + ".tiff";
		FileInfo fi = snapshot.getFileInfo();

		fi.info = stagePosInfo;

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

	private int[] frameStimulatorStrengthMap;  // map frame index to current stimulator strength
	private String[] frameStagePositionMap;    // map frame to stage position

	Imager(TrackStimController c){

		controller = c;

		imagingTasks = new ArrayList<ScheduledFuture>();
		imagingTaskStartTime = 0;
	}

    // schedule a number of snapshots at fixed time interval to ensure that images are taken
    // at the given fps
    public void scheduleImagingTasks(int numFrames, int fps, String rootDirectory){

		String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

    	imagingScheduler = Executors.newSingleThreadScheduledExecutor();
   		ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();

		long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every cycle

		imagingTaskStartTime = System.nanoTime();

		frameStimulatorStrengthMap = new int[numFrames];
		frameStagePositionMap = new String[numFrames];

		// schedule when each frame should be taken
        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ImagingTask s = new ImagingTask(
				controller.core, 
				controller.app, 
				timePtNano, 
				imageSaveDirectory, 
				curFrameIndex, 
				frameStimulatorStrengthMap,
				frameStagePositionMap,
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
				saveFrameStimulationStrengthToFile();
				saveFrameStagePositionToFile();
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

	private void saveFrameStimulationStrengthToFile(){
		// TODO
		// save stimulator frame file comma seperated by new lines
		// to the same directory as the images
	}

	private void saveFrameStagePositionToFile(){
		// TODO
		// save stage pos to file comma seperated by new lines
		// to the same directory as the images
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
