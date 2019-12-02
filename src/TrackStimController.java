import java.io.File;

import java.util.ArrayList;

import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;


class TrackStimController {
    private ArrayList<ScheduledFuture> snapshotTasks;
    private ArrayList<ScheduledFuture> stimulatorTasks;
    private String stimulatorPort;

    CMMCore core;
    ScriptInterface app;
    

    TrackStimController(CMMCore core_, ScriptInterface app_){
        core = core_;
        app = app_;
        snapshotTasks = new ArrayList<ScheduledFuture>(); 
        stimulatorTasks = new ArrayList<ScheduledFuture>();
    }

    public void startImageAcquisition(int numFrames, int framesPerSecond, String rootDirectory){

        // create a new directory in rootDirectory to save images to
        String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

        // ensure micro manager live mode is on so we can capture images
        if( !app.isLiveModeOn() ){
            app.enableLiveMode(true);
        }

        snapshotTasks = scheduleSnapShots(numFrames, framesPerSecond, imageSaveDirectory);
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

    // schedule a number of snapshot at fixed time interval to ensure that images are taken
    // at the given fps
    private ArrayList<ScheduledFuture> scheduleSnapShots(int numFrames, int fps, String saveDirectory){
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();
        fps = 10; // fix fps at 10 for now

        long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every 100ms

        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ScheduledSnapShot s = new ScheduledSnapShot(core, app, timePtNano, saveDirectory, curFrameIndex);

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
