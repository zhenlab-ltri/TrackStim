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
import java.awt.Insets;

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

    // variables in use
    TextField numFramesText;
    TextField saveDirectoryText;
    TextField framesPerSecondText;
    Checkbox enableTracking;
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
    JSlider slider;
    
    Preferences prefs;
    TrackStimController controller;

    // legacy variables that will eventually be deleted
    TextField numSkipFramesText;	
    java.awt.Choice cameraExposureDurationSelector;	
    java.awt.Choice cameraCycleDurationSelector;	
    java.awt.Checkbox useClosest;// target definition method.	
    java.awt.Checkbox trackRightSideScreen;// field for tacking source	
    java.awt.Checkbox saveXYPositionsAsTextFile;// if save xy pos data into txt file. not inclued z.	
    java.awt.Choice stageAccelerationSelector;	
    java.awt.Choice thresholdMethodSelector;	
    java.awt.Checkbox useCenterOfMassTracking;// center of mass method	
    java.awt.Checkbox useManualTracking;	
    java.awt.Checkbox useFullFieldImaging;// full size filed.	
    java.awt.Checkbox useBrightFieldImaging;// Bright field tracking only works with full size	


    public TrackStimGUI() {
        super("TrackStim");

        setSize(800, 400);
        initComponents(); // create the GUI


        // try to set values from preferences
        prefs = Preferences.userNodeForPackage(this.getClass());
        numFramesText.setText(prefs.get("numFrames", "3000"));
        saveDirectoryText.setText(prefs.get("saveDirectory", ""));
        slider.setValue(prefs.getInt("sliderValue", 100));
        enableTracking.setState(prefs.getBoolean("enableTracking", false));
        enableStimulator.setState(prefs.getBoolean("enableStimulator", false));
        enableRamp.setState(prefs.getBoolean("enableRamp", false));
        preStimulationTimeMsText.setText(prefs.get("preStimulationTimeMs", "10000"));
        stimulationStrengthText.setText(prefs.get("stimulationStrength", "63"));
        stimulationDurationMsText.setText(prefs.get("stimulationDuration", "5000"));
        stimulationCycledurationMsText.setText(prefs.get("stimulationCycleDuration", "10000"));
        numStimulationCyclesText.setText(prefs.get("numStimulationCycles", "3"));
        rampBase.setText(prefs.get("rampBase", "0"));
        rampStart.setText(prefs.get("rampStart", "0"));
        rampEnd.setText(prefs.get("rampEnd", "63"));
    }

    public void destroy(){
        // do nothing for now
    }

    public void setController(TrackStimController c){
        controller = c;
        c.updateThresholdValue(slider.getValue());
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

        Button b;

        Insets externalPadding = new Insets(10, 5, 10, 5);
        Insets noPadding = new Insets(0, 0, 0, 0);
        Insets topPadding = new Insets(10, 0, 0, 0);

        Label labelframe = new Label("Number of frames");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(labelframe, gbc);
        add(labelframe);

        numFramesText = new TextField("300", 5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(numFramesText, gbc);
        add(numFramesText);

        Label labeldir = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(labeldir, gbc);
        add(labeldir);

        saveDirectoryText = new TextField("", 40);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.insets = externalPadding;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(saveDirectoryText, gbc);
        add(saveDirectoryText);
        saveDirectoryText.setEnabled(false);
        gbc.fill = GridBagConstraints.NONE;// return to default

        Button b4 = new Button("Change dir");
        gbc.gridx = 6;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = externalPadding;
        gbl.setConstraints(b4, gbc);
        b4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryBtnActionPerformed(evt);
            }
        });
        add(b4);

        Button b2 = new Button("Go");
        gbc.gridx = 2;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(b2, gbc);
        b2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBtnActionPerformed(evt);
            }
        });
        add(b2);

        Button b3 = new Button("Stop");
        gbc.gridx = 5;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(b3, gbc);
        b3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });
        add(b3);

        // gui for stimulation
        enableStimulator = new Checkbox("Enable stimulator", false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(enableStimulator, gbc);
        add(enableStimulator);

        Label labelpre = new Label("Pre-stim");
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelpre, gbc);
        add(labelpre);

        preStimulationTimeMsText = new TextField(String.valueOf(10000), 6);
        preStimulationTimeMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(preStimulationTimeMsText, gbc);
        add(preStimulationTimeMsText);

        Label labelstrength = new Label("Strength <= 63");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelstrength, gbc);
        add(labelstrength);

        stimulationStrengthText = new TextField(String.valueOf(63), 6);
        stimulationStrengthText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationStrengthText, gbc);
        add(stimulationStrengthText);

        Label labelduration = new Label("Duration");
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelduration, gbc);
        add(labelduration);

        stimulationDurationMsText = new TextField(String.valueOf(5000), 6);
        stimulationDurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationDurationMsText, gbc);
        add(stimulationDurationMsText);

        Label labelcyclelength = new Label("Cycle length");
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclelength, gbc);
        add(labelcyclelength);

        stimulationCycledurationMsText = new TextField(String.valueOf(10000), 6);
        stimulationCycledurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 4;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimulationCycledurationMsText, gbc);
        add(stimulationCycledurationMsText);

        Label labelcyclenum = new Label("Cycle num");
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclenum, gbc);
        add(labelcyclenum);

        numStimulationCyclesText = new TextField(String.valueOf(3), 6);
        numStimulationCyclesText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 4;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(numStimulationCyclesText, gbc);
        add(numStimulationCyclesText);

        enableRamp = new Checkbox("ramp", false);
        gbc.gridx = 5;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        // gbc.gridheight=3;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(enableRamp, gbc);
        add(enableRamp);

        Label labelbase = new Label("base");
        gbc.gridx = 6;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelbase, gbc);
        add(labelbase);

        rampBase = new TextField(String.valueOf(0), 3);
        rampBase.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 7;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
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
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampStart, gbc);
        add(rampStart);

        Label labelrampend = new Label("end");
        gbc.gridx = 6;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelrampend, gbc);
        add(labelrampend);

        rampEnd = new TextField(String.valueOf(63), 3);
        rampEnd.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 7;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampEnd, gbc);
        add(rampEnd);

        enableTracking = new Checkbox("Enable auto-tracking", false);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.insets = externalPadding;
        gbl.setConstraints(enableTracking, gbc);
        add(enableTracking);
        
        Label thresholdSliderLabel = new Label("Auto-tracking threshold");
        gbc.gridx = 3;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.insets = externalPadding;
        gbl.setConstraints(thresholdSliderLabel, gbc);
        add(thresholdSliderLabel);

        slider = new JSlider(0, 200, 100);
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
        gbc.gridx = 5;
        gbc.gridy = 8;
        gbc.gridwidth = 5;
        gbc.insets = externalPadding;
        gbl.setConstraints(slider, gbc);
        add(slider);


                    // LEGACY VARIABLES

                    // test stimulator
                    b = new Button("Run");
                    b.setPreferredSize(new Dimension(40, 20));
                    gbc.gridx = 0;
                    gbc.gridy = 6;
                    gbc.gridwidth = 1;
                    gbc.gridheight = 2;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbl.setConstraints(b, gbc);
                    b.setVisible(false);
                    add(b);
                    gbc.gridheight = 1;


                    b = new Button("Ready");
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.gridwidth = 2;
                    gbl.setConstraints(b, gbc);
                    b.setVisible(false);
                    add(b);


                    Label labelexpduration = new Label("exposure");
                    gbc.gridx = 6;
                    gbc.gridy = 0;
                    gbc.gridwidth = 1;
                    gbc.gridheight = 1;
                    labelexpduration.setVisible(false);
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
                    cameraExposureDurationSelector.setVisible(false);
                    add(cameraExposureDurationSelector);

                    Label labelcameracyclelength = new Label("cycle len.");
                    gbc.gridx = 6;
                    gbc.gridy = 1;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(labelcameracyclelength, gbc);
                    labelcameracyclelength.setVisible(false);
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
                    cameraCycleDurationSelector.setVisible(false);
                    add(cameraCycleDurationSelector);

                    useClosest = new Checkbox("Just closest", true);
                    gbc.gridx = 2;
                    gbc.gridy = 2;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(useClosest, gbc);
                    useClosest.setVisible(false);
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
                    stageAccelerationSelector.setVisible(false);
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
                    thresholdMethodSelector.setVisible(false);
                    add(thresholdMethodSelector);

                    trackRightSideScreen = new Checkbox("Use right", false);
                    gbc.gridx = 5;
                    gbc.gridy = 2;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(trackRightSideScreen, gbc);
                    trackRightSideScreen.setVisible(false);
                    add(trackRightSideScreen);

                    saveXYPositionsAsTextFile = new Checkbox("Save xypos file", false);
                    gbc.gridx = 6;
                    gbc.gridy = 2;
                    gbc.gridwidth = 2;
                    gbl.setConstraints(saveXYPositionsAsTextFile, gbc);
                    saveXYPositionsAsTextFile.setVisible(false);
                    add(saveXYPositionsAsTextFile);

                    Label labelskip = new Label("one of ");
                    gbc.gridx = 0;
                    gbc.gridy = 3;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(labelskip, gbc);
                    labelskip.setVisible(false);
                    add(labelskip);

                    numSkipFramesText = new TextField("1", 2);
                    gbc.gridx = 1;
                    gbc.gridy = 3;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(numSkipFramesText, gbc);
                    numSkipFramesText.setVisible(false);
                    add(numSkipFramesText);

                    useCenterOfMassTracking = new Checkbox("Center of Mass", false);
                    gbc.gridx = 2;
                    gbc.gridy = 3;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(useCenterOfMassTracking, gbc);
                    useCenterOfMassTracking.setVisible(false);
                    add(useCenterOfMassTracking);

                    useManualTracking = new Checkbox("manual track", false);
                    gbc.gridx = 3;
                    gbc.gridy = 3;
                    gbc.gridwidth = 2;
                    gbl.setConstraints(useManualTracking, gbc);
                    useManualTracking.setVisible(false);
                    add(useManualTracking);

                    useFullFieldImaging = new Checkbox("Full field", false);
                    gbc.gridx = 5;
                    gbc.gridy = 3;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(useFullFieldImaging, gbc);
                    useFullFieldImaging.setVisible(false);
                    add(useFullFieldImaging);

                    useBrightFieldImaging = new Checkbox("Bright field", false);
                    gbc.gridx = 6;
                    gbc.gridy = 3;
                    gbc.gridwidth = 1;
                    gbl.setConstraints(useBrightFieldImaging, gbc);
                    useBrightFieldImaging.setVisible(false);
                    add(useBrightFieldImaging);

        pack();
    }

}