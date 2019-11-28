import java.awt.TextField;
import java.awt.Label;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Button;
import java.awt.GridBagLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.File;

import java.util.prefs.Preferences;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.IJ;
import ij.plugin.frame.PlugInFrame;


import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.micromanager.api.ScriptInterface;

// provides the ui for track stim

// implements a ImageJ plugin interface 
// **NOTE**: There is a big difference between a micro manager plugin and an imagej plugin
// this program was initially designed as an imageJ plugin, but is now wrapped inside a micromanager plugin
// to migrate it to new versions of micromanager
class TrackStimGUI extends PlugInFrame {

    // UI options
    TextField numFramesText;
    TextField saveDirectoryText;
    TextField framesPerSecondText;
    
    Preferences prefs;

    ArrayList<ScheduledFuture> snapshotTasks;

    CMMCore mmc;
    ScriptInterface app;

    public TrackStimGUI(CMMCore cmmcore, ScriptInterface app_) {
        super("TrackStim");

        // set up micro manager variables
        mmc = cmmcore;
        app = app_;

        initComponents(); // create the GUI
        setSize(700, 200);

        prefs = Preferences.userNodeForPackage(this.getClass());
        numFramesText.setText(prefs.get("numFrames", "3000"));
        saveDirectoryText.setText(prefs.get("saveDirectory", ""));

        snapshotTasks = new ArrayList<ScheduledFuture>();
    }

    String createImageSaveDirectory(String root){
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

    ArrayList<ScheduledFuture> scheduleSnapShots(int numFrames, int fps, String saveDirectory){
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ArrayList<ScheduledFuture> snapShots = new ArrayList<ScheduledFuture>();
        fps = 10; // fix fps at 10 for now

        long frameCycleNano = TimeUnit.MILLISECONDS.toNanos(1000 / fps); // take a pic every 100ms

        for(int curFrameIndex = 0; curFrameIndex < numFrames; curFrameIndex++){
            long timePtNano = curFrameIndex * frameCycleNano; // e.g. 0 ms, 100ms, 200ms, etc..
            ScheduledSnapShot s = new ScheduledSnapShot(mmc, app, timePtNano, saveDirectory, curFrameIndex);

            ScheduledFuture snapShot = ses.schedule(s, timePtNano, TimeUnit.NANOSECONDS);
            snapShots.add(snapShot);
        }

        return snapShots;
    }

    void goBtnActionPerformed(ActionEvent e){
        if(saveDirectoryIsValid() && frameNumbersAreValid()){
            prefs.put("saveDirectory", saveDirectoryText.getText());
            prefs.put("numFrames", numFramesText.getText());

            int numFrames = Integer.parseInt(numFramesText.getText());
            int framesPerSecond = Integer.parseInt(framesPerSecondText.getText());
            String rootDirectory = saveDirectoryText.getText();

            String imageSaveDirectory = createImageSaveDirectory(rootDirectory);

            if( !app.isLiveModeOn() ){
                app.enableLiveMode(true);
            }

            snapshotTasks = scheduleSnapShots(numFrames, framesPerSecond, imageSaveDirectory);
        } else {
            IJ.showMessage("directory or frame number is invalid");
        }
    }

    void directoryBtnActionPerformed(ActionEvent e){

        saveDirectoryText.setText(IJ.getDirectory("user.home"));
    }

    void stopBtnActionPerformed(ActionEvent e){
        for (int i = 0; i < snapshotTasks.size(); i++ ){
            snapshotTasks.get(i).cancel(true);
        }
    }


    boolean saveDirectoryIsValid(){
        File f = new File(saveDirectoryText.getText());
        return f.exists() && f.isDirectory();
    }

    boolean frameNumbersAreValid(){
        boolean valid = true;

        try {
            Integer.parseInt(numFramesText.getText());
            Integer.parseInt(framesPerSecondText.getText());

        } catch (java.lang.Exception e){
            valid = false;
        }

         return valid;
    }

    // create the GUI
    void initComponents(){
        // Prepare GUI
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        Button goBtn = new Button("Go");
        goBtn.setPreferredSize(new Dimension(100, 60));
        goBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBtnActionPerformed(evt);
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(goBtn, gbc);
        add(goBtn);

        Button stopBtn = new Button("Stop");
        stopBtn.setPreferredSize(new Dimension(100, 60));
        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(stopBtn, gbc);
        add(stopBtn);

        Label numFramesLabel = new Label("Number of frames");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(numFramesLabel, gbc);
        add(numFramesLabel);

        numFramesText = new TextField(String.valueOf("3000"), 5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(numFramesText, gbc);
        add(numFramesText);

        Label framesPerSecondLabel = new Label("Frames per second");
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(framesPerSecondLabel, gbc);
        add(framesPerSecondLabel);

        framesPerSecondText = new TextField("10", 5);
        framesPerSecondText.setEnabled(false);
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(framesPerSecondText, gbc);
        add(framesPerSecondText);

        Label directoryLabel = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbl.setConstraints(directoryLabel, gbc);
        add(directoryLabel);

        saveDirectoryText = new TextField("", 40);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(saveDirectoryText, gbc);
        add(saveDirectoryText);
        gbc.fill = GridBagConstraints.NONE;// return to default

        Button directoryBtn = new Button("Directory");
        directoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryBtnActionPerformed(evt);
            }
        });
        gbc.gridx = 6;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbl.setConstraints(directoryBtn, gbc);
        add(directoryBtn);
    }

}