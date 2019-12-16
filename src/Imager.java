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


class ImagingTask implements Runnable {
	CMMCore core;
	ScriptInterface app;
	long timePoint;
	String saveDirectory;
	int frameIndex;

	ImagingTask(CMMCore core_, ScriptInterface app_, long timePoint_, String saveDirectory_, int frameIndex_){
		core = core_;
		timePoint = timePoint_;
		app = app_;
		saveDirectory = saveDirectory_;
		frameIndex = frameIndex_;
	}

	public void run(){
		if( !app.isLiveModeOn() ){
			IJ.log("[ERROR] Could not acquire image.  Live mode must be on.  Please press STOP." );
			return;
		}

		ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();


		saveSnapshotToTiff(liveModeImage);
	}

	// encode stage position as a string
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

	private void saveSnapshotToTiff(ImagePlus snapshot){
		String filePath = saveDirectory + "/" + String.valueOf(frameIndex) + ".tiff";
		FileInfo fi = snapshot.getFileInfo();

		fi.info = getStagePositionInfo();

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


class Imager {

	TrackStimController controller;

	private ArrayList<ScheduledFuture> imagingTasks;
	private ScheduledExecutorService imagingScheduler;
	private long imagingTaskStartTime;


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

		long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every 100ms

		imagingTaskStartTime = System.nanoTime();

        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ImagingTask s = new ImagingTask(controller.core, controller.app, timePtNano, imageSaveDirectory, curFrameIndex);

            ScheduledFuture snapShot = imagingScheduler.schedule(s, timePtNano, TimeUnit.NANOSECONDS);
            futureTasks.add(snapShot);
		}

		ScheduledFuture lastImagingTask = imagingScheduler.schedule(new Runnable() {
			@Override
			public void run(){
				controller.onImageAcquisitionDone(computeImageTaskTimeInSeconds());
			}
		}, (numFrames - 1) * frameCycleNano, TimeUnit.NANOSECONDS);
			imagingTasks.add(lastImagingTask);

      imagingTasks = futureTasks;
    }

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
