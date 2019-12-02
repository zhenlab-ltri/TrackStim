import ij.ImagePlus;
import ij.IJ;

import ij.io.FileInfo;
import ij.io.TiffEncoder;

import java.io.File;
import java.io.FileOutputStream;

import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;


class ScheduledSnapshot implements Runnable {
	CMMCore core;
	ScriptInterface app;
	long timePoint;
	String saveDirectory;
	int frameIndex;

	ScheduledSnapshot(CMMCore core_, ScriptInterface app_, long timePoint_, String saveDirectory_, int frameIndex_){
		core = core_;
		timePoint = timePoint_;
		app = app_;
		saveDirectory = saveDirectory_;
		frameIndex = frameIndex_;
	}

	public void run(){
		IJ.log("[INFO] Acquring image at time " + String.valueOf(timePoint));

		if( !app.isLiveModeOn() ){
			IJ.log("[ERROR] Could not acquire image.  Live mode must be on.  Please press STOP." );
			return;
		}
        ImagePlus liveModeImage = app.getSnapLiveWin().getImagePlus();


		saveSnapshotToTiff(liveModeImage);
	}

	private void saveSnapshotToTiff(ImagePlus snapshot){
		String filePath = saveDirectory + "/" + String.valueOf(frameIndex) + ".tiff";
		FileInfo fi = snapshot.getFileInfo();

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

		String stagePositionInfo = "xpos=" + String.valueOf(currXPos) + 
			",ypos=" + String.valueOf(currYPos) + 
			",zpos=" + String.valueOf(currZPos);

		fi.info = stagePositionInfo;

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
