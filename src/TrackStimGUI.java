import java.awt.TextField;
import java.awt.Label;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Checkbox;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JSlider;
import javax.swing.JLabel;

import java.io.File;

import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.Hashtable;

import ij.IJ;
import ij.plugin.frame.PlugInFrame;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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

    TextField numSkipFramesText;	
    java.awt.Choice cameraExposureDurationSelector;	
    java.awt.Choice cameraCycleDurationSelector;	

    // tracker options	
    java.awt.Checkbox useClosest;// target definition method.	
    java.awt.Checkbox trackRightSideScreen;// field for tacking source	
    java.awt.Checkbox saveXYPositionsAsTextFile;// if save xy pos data into txt file. not inclued z.	
    java.awt.Choice stageAccelerationSelector;	
    java.awt.Choice thresholdMethodSelector;	
    java.awt.Checkbox useCenterOfMassTracking;// center of mass method	
    java.awt.Checkbox useManualTracking;	
    java.awt.Checkbox useFullFieldImaging;// full size filed.	
    java.awt.Checkbox useBrightFieldImaging;// Bright field tracking only works with full size	

    // stimulator options	
    java.awt.Checkbox enableStimulator;	
    TextField preStimulationTimeMsText;	
    TextField stimulationStrengthText;	
    TextField stimulationDurationMsText;	
    TextField stimulationCycledurationMsText;	
    TextField numStimulationCyclesText;	
    java.awt.Checkbox enableRamp;	
    TextField rampBase;	
    TextField rampStart;	
    TextField rampEnd;	
    
    Preferences prefs;


    TrackStimController controller;

    public TrackStimGUI() {
        super("TrackStim");

        initComponents(); // create the GUI
        setSize(800, 400);

        prefs = Preferences.userNodeForPackage(this.getClass());
        numFramesText.setText(prefs.get("numFrames", "3000"));
        saveDirectoryText.setText(prefs.get("saveDirectory", ""));
    }

    public void destroy(){
        // do nothing for now
    }

    public void setController(TrackStimController c){
        controller = c;
        c.updateThresholdValue(100);
    }

    // when go is pressed, validate ui values and send them to the controller to start
    // getting images
    private void goBtnActionPerformed(ActionEvent e){
        if(saveDirectoryIsValid() && frameNumbersAreValid()){
            prefs.put("saveDirectory", saveDirectoryText.getText());
            prefs.put("numFrames", numFramesText.getText());

            int numFrames = Integer.parseInt(numFramesText.getText());
            int framesPerSecond = Integer.parseInt(framesPerSecondText.getText());
            String rootDirectory = saveDirectoryText.getText();

            controller.startImageAcquisition(numFrames, framesPerSecond, rootDirectory);
        } else {
            IJ.showMessage("directory or frame number is invalid");
        }
    }

    // pick new directory
    private void directoryBtnActionPerformed(ActionEvent e){
        saveDirectoryText.setText(IJ.getDirectory("user.home"));
    }

    // stop controller from getting images
    private void stopBtnActionPerformed(ActionEvent e){
        controller.stopImageAcquisition();
    }

    private void sliderValueChanged(ChangeEvent e){
        JSlider s = (JSlider) e.getSource();
        controller.updateThresholdValue(s.getValue());
    }

    private boolean saveDirectoryIsValid(){
        File f = new File(saveDirectoryText.getText());
        return f.exists() && f.isDirectory();
    }

    private boolean frameNumbersAreValid(){
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
    private void initComponents(){
        // Prepare GUI
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        Button b = new Button("Ready");
        b.setPreferredSize(new Dimension(100, 60));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b, gbc);
        b.setEnabled(false);
        add(b);

        Button b2 = new Button("Go");
        b2.setPreferredSize(new Dimension(100, 60));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b2, gbc);
        b2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBtnActionPerformed(evt);
            }
        });
        add(b2);

        Button b3 = new Button("Stop");
        b3.setPreferredSize(new Dimension(100, 60));
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b3, gbc);
        b3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });
        add(b3);

        Label labelexpduration = new Label("exposure");
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        labelexpduration.setEnabled(false);
        add(labelexpduration);

        cameraExposureDurationSelector = new Choice();
        cameraExposureDurationSelector.setPreferredSize(new Dimension(80, 20));
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        cameraExposureDurationSelector.add("0");
        cameraExposureDurationSelector.add("1");
        cameraExposureDurationSelector.add("10");
        cameraExposureDurationSelector.add("50");
        cameraExposureDurationSelector.add("100");
        cameraExposureDurationSelector.add("200");
        cameraExposureDurationSelector.add("500");
        cameraExposureDurationSelector.add("1000");
        gbl.setConstraints(cameraExposureDurationSelector, gbc);
        cameraExposureDurationSelector.setEnabled(false);
        add(cameraExposureDurationSelector);

        Label labelcameracyclelength = new Label("cycle len.");
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelcameracyclelength, gbc);
        labelcameracyclelength.setEnabled(false);
        add(labelcameracyclelength);

        cameraCycleDurationSelector = new Choice();
        cameraCycleDurationSelector.setPreferredSize(new Dimension(80, 20));
        gbc.gridx = 7;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        cameraCycleDurationSelector.add("0");
        cameraCycleDurationSelector.add("50");
        cameraCycleDurationSelector.add("100");
        cameraCycleDurationSelector.add("200");
        cameraCycleDurationSelector.add("500");
        cameraCycleDurationSelector.add("1000");
        cameraCycleDurationSelector.add("2000");
        gbl.setConstraints(cameraCycleDurationSelector, gbc);
        cameraCycleDurationSelector.setEnabled(false);
        add(cameraCycleDurationSelector);

        Label labelframe = new Label("Frame num");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelframe, gbc);
        add(labelframe);

        numFramesText = new TextField("300", 5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(numFramesText, gbc);
        add(numFramesText);

        useClosest = new Checkbox("Just closest", true);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(useClosest, gbc);
        useClosest.setEnabled(false);
        add(useClosest);

        stageAccelerationSelector = new Choice();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        stageAccelerationSelector.add("1x");
        stageAccelerationSelector.add("2x");
        stageAccelerationSelector.add("4x");
        stageAccelerationSelector.add("5x");
        stageAccelerationSelector.add("6x");
        gbl.setConstraints(stageAccelerationSelector, gbc);
        stageAccelerationSelector.setEnabled(false);
        add(stageAccelerationSelector);

        thresholdMethodSelector = new Choice();
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        thresholdMethodSelector.add("Yen");// good for normal
        thresholdMethodSelector.add("Triangle");// good for on coli
        thresholdMethodSelector.add("Otsu");// good for ventral cord?

        thresholdMethodSelector.add("Default");
        thresholdMethodSelector.add("Huang");
        thresholdMethodSelector.add("Intermodes");
        thresholdMethodSelector.add("IsoData");
        thresholdMethodSelector.add("Li");
        thresholdMethodSelector.add("MaxEntropy");
        thresholdMethodSelector.add("Mean");
        thresholdMethodSelector.add("MinError(I)");
        thresholdMethodSelector.add("Minimum");
        thresholdMethodSelector.add("Moments");
        thresholdMethodSelector.add("Percentile");
        thresholdMethodSelector.add("RenyiEntropy");
        thresholdMethodSelector.add("Shanbhag");
        thresholdMethodSelector.setPreferredSize(new Dimension(80, 20));
        gbl.setConstraints(thresholdMethodSelector, gbc);
        thresholdMethodSelector.setEnabled(false);
        add(thresholdMethodSelector);

        trackRightSideScreen = new Checkbox("Use right", false);
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(trackRightSideScreen, gbc);
        trackRightSideScreen.setEnabled(false);
        add(trackRightSideScreen);

        saveXYPositionsAsTextFile = new Checkbox("Save xypos file", false);
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbl.setConstraints(saveXYPositionsAsTextFile, gbc);
        saveXYPositionsAsTextFile.setEnabled(false);
        add(saveXYPositionsAsTextFile);

        Label labelskip = new Label("one of ");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelskip, gbc);
        labelskip.setEnabled(false);
        add(labelskip);

        numSkipFramesText = new TextField("1", 2);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(numSkipFramesText, gbc);
        numSkipFramesText.setEnabled(false);
        add(numSkipFramesText);

        useCenterOfMassTracking = new Checkbox("Center of Mass", false);
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(useCenterOfMassTracking, gbc);
        useCenterOfMassTracking.setEnabled(false);
        add(useCenterOfMassTracking);

        useManualTracking = new Checkbox("manual track", false);
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbl.setConstraints(useManualTracking, gbc);
        useManualTracking.setEnabled(false);
        add(useManualTracking);

        useFullFieldImaging = new Checkbox("Full field", false);
        gbc.gridx = 5;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(useFullFieldImaging, gbc);
        useFullFieldImaging.setEnabled(false);
        add(useFullFieldImaging);

        useBrightFieldImaging = new Checkbox("Bright field", false);
        gbc.gridx = 6;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(useBrightFieldImaging, gbc);
        useBrightFieldImaging.setEnabled(false);
        add(useBrightFieldImaging);

        Label labeldir = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbl.setConstraints(labeldir, gbc);
        add(labeldir);

        saveDirectoryText = new TextField("", 40);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(saveDirectoryText, gbc);
        add(saveDirectoryText);
        saveDirectoryText.setEnabled(false);
        gbc.fill = GridBagConstraints.NONE;// return to default

        Button b4 = new Button("Change dir");
        gbc.gridx = 6;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbl.setConstraints(b4, gbc);
        b4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryBtnActionPerformed(evt);
            }
        });
        add(b4);

        // gui for stimulation
        enableStimulator = new Checkbox("Light", false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(enableStimulator, gbc);
        add(enableStimulator);

        b = new Button("Run");
        b.setPreferredSize(new Dimension(40, 20));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(b, gbc);
        b.setEnabled(false);
        add(b);
        gbc.gridheight = 1;

        Label labelpre = new Label("Pre-stim");
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelpre, gbc);
        add(labelpre);

        preStimulationTimeMsText = new TextField(String.valueOf(10000), 6);
        preStimulationTimeMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(preStimulationTimeMsText, gbc);
        add(preStimulationTimeMsText);

        Label labelstrength = new Label("Strength <63");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelstrength, gbc);
        add(labelstrength);

        stimulationStrengthText = new TextField(String.valueOf(63), 6);
        stimulationStrengthText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationStrengthText, gbc);
        add(stimulationStrengthText);

        Label labelduration = new Label("Duration");
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelduration, gbc);
        add(labelduration);

        stimulationDurationMsText = new TextField(String.valueOf(5000), 6);
        stimulationDurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationDurationMsText, gbc);
        add(stimulationDurationMsText);

        Label labelcyclelength = new Label("Cycle length");
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclelength, gbc);
        add(labelcyclelength);

        stimulationCycledurationMsText = new TextField(String.valueOf(10000), 6);
        stimulationCycledurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 4;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationCycledurationMsText, gbc);
        add(stimulationCycledurationMsText);

        Label labelcyclenum = new Label("Cycle num");
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclenum, gbc);
        add(labelcyclenum);

        numStimulationCyclesText = new TextField(String.valueOf(3), 6);
        numStimulationCyclesText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 4;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(numStimulationCyclesText, gbc);
        add(numStimulationCyclesText);

        enableRamp = new Checkbox("ramp", false);
        gbc.gridx = 5;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        // gbc.gridheight=3;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(enableRamp, gbc);
        add(enableRamp);

        Label labelbase = new Label("base");
        gbc.gridx = 6;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelbase, gbc);
        add(labelbase);

        rampBase = new TextField(String.valueOf(0), 3);
        rampBase.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 7;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampBase, gbc);
        add(rampBase);

        Label labelrampstart = new Label("start");
        gbc.gridx = 6;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelrampstart, gbc);
        add(labelrampstart);

        rampStart = new TextField(String.valueOf(0), 3);
        rampStart.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 7;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampStart, gbc);
        add(rampStart);

        Label labelrampend = new Label("end");
        gbc.gridx = 6;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelrampend, gbc);
        add(labelrampend);

        rampEnd = new TextField(String.valueOf(63), 3);
        rampEnd.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 7;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampEnd, gbc);
        add(rampEnd);

        JSlider slider = new JSlider(0, 200, 100);
        slider.setMajorTickSpacing(50);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        
        Hashtable<Integer, JLabel> sliderTickLabels = new Hashtable<Integer, JLabel>();

        sliderTickLabels.put(0, new JLabel("Low"));
        sliderTickLabels.put(100, new JLabel("Average"));
        sliderTickLabels.put(200, new JLabel("High"));
        slider.setLabelTable(sliderTickLabels);
        slider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                sliderValueChanged(e);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 5;
        gbl.setConstraints(slider, gbc);
        add(slider);
    }

}