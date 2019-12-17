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
    Choice framesPerSecondSelector;
    Checkbox enableTracking;
    java.awt.Checkbox enableStimulator;
    java.awt.Checkbox enableRamp;
    TextField preStimulationTimeMsText;
    TextField stimulationStrengthText;
    TextField stimulationDurationMsText;
    TextField stimulationCycleDurationMsText;
    TextField numStimulationCyclesText;
    TextField rampBase;
    TextField rampStart;
    TextField rampEnd;
    JSlider thresholdSlider;
    JSlider trackerSpeedSlider;

    Button changeDirectoryBtn;
    Button stopBtn;
    Button goBtn;

    Preferences prefs;
    TrackStimController controller;

    public TrackStimGUI() {
        super("TrackStim");

        setSize(800, 400);
        initComponents(); // create the GUI
        loadPreferences(); // populate GUI with saved preferences
    }

    public void destroy(){
        // do nothing for now
    }

    public void setController(TrackStimController c){
        controller = c;
        c.updateThresholdValue(thresholdSlider.getValue());
        c.updateTrackerSpeedValue(trackerSpeedSlider.getValue());
    }

    private void loadPreferences(){
        prefs = Preferences.userNodeForPackage(this.getClass());
        numFramesText.setText(prefs.get("numFrames", "3000"));
        framesPerSecondSelector.select(prefs.get("framesPerSecond", "26"));
        saveDirectoryText.setText(prefs.get("saveDirectory", ""));
        enableTracking.setState(prefs.getBoolean("enableTracking", false));
        enableStimulator.setState(prefs.getBoolean("enableStimulator", false));
        enableRamp.setState(prefs.getBoolean("enableRamp", false));
        preStimulationTimeMsText.setText(prefs.get("preStimulationTimeMs", "10000"));
        stimulationStrengthText.setText(prefs.get("stimulationStrength", "63"));
        stimulationDurationMsText.setText(prefs.get("stimulationDuration", "5000"));
        stimulationCycleDurationMsText.setText(prefs.get("stimulationCycleDuration", "10000"));
        numStimulationCyclesText.setText(prefs.get("numStimulationCycles", "3"));
        rampBase.setText(prefs.get("rampBase", "0"));
        rampStart.setText(prefs.get("rampStart", "0"));
        rampEnd.setText(prefs.get("rampEnd", "63"));
    }

    private void savePreferences(){
        prefs.put("saveDirectory", saveDirectoryText.getText());
        prefs.put("numFrames", numFramesText.getText());
        prefs.put("framesPerSecond", String.valueOf(framesPerSecondSelector.getSelectedItem()));
        prefs.put("enableTracking", String.valueOf(enableTracking.getState()));
        prefs.put("enableStimulator", String.valueOf(enableStimulator.getState()));
        prefs.put("enableRamp", String.valueOf(enableRamp.getState()));
        prefs.put("preStimulationTimeMs", preStimulationTimeMsText.getText());
        prefs.put("stimulationStrength", stimulationStrengthText.getText());
        prefs.put("stimulationDuration", stimulationDurationMsText.getText());
        prefs.put("stimulationCycleDuration", stimulationCycleDurationMsText.getText());
        prefs.put("numStimulationCycles", numStimulationCyclesText.getText());
        prefs.put("rampBase", rampBase.getText());
        prefs.put("rampStart", rampStart.getText());
        prefs.put("rampEnd", rampEnd.getText());
    }

    // when go is pressed, validate ui values and send them to the controller to start
    // getting images
    private void goBtnActionPerformed(ActionEvent e){
        if(uiValuesAreValid()){
            savePreferences();

            controller.startImageAcquisition(
                Integer.parseInt(numFramesText.getText()),
                Integer.parseInt(framesPerSecondSelector.getSelectedItem()),
                saveDirectoryText.getText(),

                enableStimulator.getState(),
                Integer.parseInt(preStimulationTimeMsText.getText()),
                Integer.parseInt(stimulationStrengthText.getText()),
                Integer.parseInt(stimulationDurationMsText.getText()),
                Integer.parseInt(stimulationCycleDurationMsText.getText()),
                Integer.parseInt(numStimulationCyclesText.getText()),
                enableRamp.getState(),
                Integer.parseInt(rampBase.getText()),
                Integer.parseInt(rampStart.getText()),
                Integer.parseInt(rampEnd.getText()),

                enableTracking.getState()
            );
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

    private void thresholdSliderValueChanged(ChangeEvent e){
        JSlider s = (JSlider) e.getSource();
        controller.updateThresholdValue(s.getValue());
    }

    private void trackerSpeedValueChanged(ChangeEvent e){
        JSlider s = (JSlider) e.getSource();
        controller.updateTrackerSpeedValue(s.getValue());
    }

    private boolean saveDirectoryIsValid(){
        File f = new File(saveDirectoryText.getText());
        return f.exists() && f.isDirectory();
    }

    private boolean textNumbersAreValid(){
        boolean valid = true;
        int frame;
        int preStim;
        int stimStrength;
        int stimDuration;
        int stimCycleDuration;
        int numStimCycles;
        int rBase;
        int rEnd;
        int rStart;
        try {
            frame = Integer.parseInt(numFramesText.getText());
            preStim = Integer.parseInt(preStimulationTimeMsText.getText());
            stimStrength = Integer.parseInt(stimulationStrengthText.getText());
            stimDuration = Integer.parseInt(stimulationDurationMsText.getText());
            stimCycleDuration = Integer.parseInt(stimulationCycleDurationMsText.getText());
            numStimCycles = Integer.parseInt(numStimulationCyclesText.getText());
            rBase = Integer.parseInt(rampBase.getText());
            rStart = Integer.parseInt(rampStart.getText());
            rEnd = Integer.parseInt(rampEnd.getText());

            if(frame <= 0 || frame > 15600){
                valid = false;
                IJ.showMessage("Number of frames must be in the range of [1, 15600]");
            }

            if(preStim <= 0){
                valid = false;
                IJ.showMessage("Pre stimulation must be greater than 0");
            }

            if(stimStrength > 63 || stimStrength < 0){
                valid = false;
                IJ.showMessage("Stimulation strength must be in the range of [1, 63]");
            }

            if(stimCycleDuration <= 0){
                valid = false;
                IJ.showMessage("Stim cycle duration must be greater than 0");
            }

            if(numStimCycles <= 0){
                valid = false;
                IJ.showMessage("Num stim cycles must be greater than 0");
            }

            if(rBase < 0){
                valid = false;
                IJ.showMessage("Ramp base must be non-negative");
            }

            if(rStart < 0){
                valid = false;
                IJ.showMessage("Ramp start must be non-negative");
            }

            if(rEnd < 0 || rEnd > 63){
                valid = false;
                IJ.showMessage("Ramp end must be in the range of [0, 63]");
            }

        } catch (java.lang.Exception e){
            valid = false;
        }

         return valid;
    }

    private boolean uiValuesAreValid(){
        return saveDirectoryIsValid() && textNumbersAreValid();
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
        Insets leftLabelPadding = new Insets(5, 25, 5, 5);

        Label imagingHeader = new Label("Imaging");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(imagingHeader, gbc);
        add(imagingHeader);

        Label labelframe = new Label("Number of frames");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelframe, gbc);
        add(labelframe);

        numFramesText = new TextField("300", 5);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(numFramesText, gbc);
        add(numFramesText);

        Label fpsLabel = new Label("Frames per second");
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(fpsLabel, gbc);
        add(fpsLabel);

        framesPerSecondSelector = new Choice();
        framesPerSecondSelector.add("1");
        framesPerSecondSelector.add("10");
        framesPerSecondSelector.add("20");
        framesPerSecondSelector.add("26");
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(framesPerSecondSelector, gbc);
        add(framesPerSecondSelector);


        Label labeldir = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labeldir, gbc);
        add(labeldir);

        saveDirectoryText = new TextField("", 30);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(saveDirectoryText, gbc);
        add(saveDirectoryText);
        saveDirectoryText.setEnabled(false);

        changeDirectoryBtn = new Button("Change dir");
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbl.setConstraints(changeDirectoryBtn, gbc);
        changeDirectoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryBtnActionPerformed(evt);
            }
        });
        add(changeDirectoryBtn);

        // gui for stimulation
        enableStimulator = new Checkbox("Enable stimulator", false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(enableStimulator, gbc);
        add(enableStimulator);

        Label labelpre = new Label("Pre-stim");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelpre, gbc);
        add(labelpre);

        preStimulationTimeMsText = new TextField(String.valueOf(10000), 6);
        preStimulationTimeMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(preStimulationTimeMsText, gbc);
        add(preStimulationTimeMsText);

        Label labelstrength = new Label("Strength <= 63");
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelstrength, gbc);
        add(labelstrength);

        stimulationStrengthText = new TextField(String.valueOf(63), 6);
        stimulationStrengthText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(stimulationStrengthText, gbc);
        add(stimulationStrengthText);

        Label labelduration = new Label("Duration");
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelduration, gbc);
        add(labelduration);

        stimulationDurationMsText = new TextField(String.valueOf(5000), 6);
        stimulationDurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(stimulationDurationMsText, gbc);
        add(stimulationDurationMsText);

        Label labelcyclelength = new Label("Cycle length");
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelcyclelength, gbc);
        add(labelcyclelength);

        stimulationCycleDurationMsText = new TextField(String.valueOf(10000), 6);
        stimulationCycleDurationMsText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(stimulationCycleDurationMsText, gbc);
        add(stimulationCycleDurationMsText);

        Label labelcyclenum = new Label("Cycle num");
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelcyclenum, gbc);
        add(labelcyclenum);

        numStimulationCyclesText = new TextField(String.valueOf(3), 6);
        numStimulationCyclesText.setPreferredSize(new Dimension(50, 30));
        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        gbc.insets = noPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(numStimulationCyclesText, gbc);
        add(numStimulationCyclesText);

        enableRamp = new Checkbox("ramp", false);
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = topPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(enableRamp, gbc);
        add(enableRamp);

        Label labelbase = new Label("base");
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelbase, gbc);
        add(labelbase);

        rampBase = new TextField(String.valueOf(0), 3);
        rampBase.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(rampBase, gbc);
        add(rampBase);

        Label labelrampstart = new Label("start");
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(labelrampstart, gbc);
        add(labelrampstart);

        rampStart = new TextField(String.valueOf(0), 3);
        rampStart.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 3;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(rampStart, gbc);
        add(rampStart);

        Label labelrampend = new Label("end");
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(labelrampend, gbc);
        add(labelrampend);

        rampEnd = new TextField(String.valueOf(63), 3);
        rampEnd.setPreferredSize(new Dimension(30, 30));
        gbc.gridx = 3;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(rampEnd, gbc);
        add(rampEnd);

        enableTracking = new Checkbox("Enable auto-tracking", false);
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(enableTracking, gbc);
        add(enableTracking);

        Label thresholdSliderLabel = new Label("Auto-tracking threshold");
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(thresholdSliderLabel, gbc);
        add(thresholdSliderLabel);

        thresholdSlider = new JSlider(0, 200, 100);
        thresholdSlider.setMajorTickSpacing(50);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> thresholdSliderTickLabels = new Hashtable<Integer, JLabel>();

        thresholdSliderTickLabels.put(0, new JLabel("Low"));
        thresholdSliderTickLabels.put(100, new JLabel("Average"));
        thresholdSliderTickLabels.put(200, new JLabel("High"));
        thresholdSlider.setLabelTable(thresholdSliderTickLabels);
        thresholdSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                thresholdSliderValueChanged(e);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 12;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(thresholdSlider, gbc);
        add(thresholdSlider);

        Label trackerSpeedLabel = new Label("Auto-tracking speed");
        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(trackerSpeedLabel, gbc);
        add(trackerSpeedLabel);

        trackerSpeedSlider = new JSlider(1, 7, 4);
        trackerSpeedSlider.setMajorTickSpacing(1);
        trackerSpeedSlider.setPaintTicks(true);
        trackerSpeedSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> trackerSpeedSliderLabels = new Hashtable<Integer, JLabel>();

        trackerSpeedSliderLabels.put(1, new JLabel("Slow"));
        trackerSpeedSliderLabels.put(4, new JLabel("Normal"));
        trackerSpeedSliderLabels.put(7, new JLabel("Ludicrous"));
        trackerSpeedSlider.setLabelTable(trackerSpeedSliderLabels);
        trackerSpeedSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                trackerSpeedValueChanged(e);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 13;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(trackerSpeedSlider, gbc);
        add(trackerSpeedSlider);

        goBtn = new Button("Go");
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 1;
        gbc.ipadx = 10;
        gbc.ipady = 10;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbl.setConstraints(goBtn, gbc);
        goBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBtnActionPerformed(evt);
            }
        });
        add(goBtn);

        stopBtn = new Button("Stop");
        gbc.gridx = 1;
        gbc.gridy = 14;
        gbc.gridwidth = 1;
        gbc.ipadx = 10;
        gbc.ipady = 10;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(stopBtn, gbc);
        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });
        add(stopBtn);

        pack();
        setResizable(false);
    }

}