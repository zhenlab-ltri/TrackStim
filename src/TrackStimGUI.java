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

    // load the previous values of the ui from the last session (before ther user quit)
    private void loadPreferences(){
        prefs = Preferences.userNodeForPackage(this.getClass());
        numFramesText.setText(prefs.get("numFrames", "3000"));
        framesPerSecondSelector.select(prefs.get("framesPerSecond", "26"));
        saveDirectoryText.setText(prefs.get("saveDirectory", ""));
        enableTracking.setState(prefs.getBoolean("enableTracking", false));
    }

    // save the current values of the ui to preferences
    private void savePreferences(){
        prefs.put("saveDirectory", saveDirectoryText.getText());
        prefs.put("numFrames", numFramesText.getText());
        prefs.put("framesPerSecond", String.valueOf(framesPerSecondSelector.getSelectedItem()));
        prefs.put("enableTracking", String.valueOf(enableTracking.getState()));
    }

    // when go is pressed, validate ui values and send them to the controller to start
    // getting images
    private void goBtnActionPerformed(ActionEvent e){
        if(uiValuesAreValid()){
            savePreferences();

            // calcium imaging does not use the stimulator, send dummy values that will be ignored to the
            // controller function
            boolean enableStimulator = false;
            int preStim = 0;
            int stimStr = 0;
            int stimDur = 0;
            int stimCycleDur = 0;
            int numStimCycles = 0;
            boolean enableRamp = false;
            int rampBase = 0;
            int rampStart = 0;
            int rampEnd = 0;

            controller.startImageAcquisition(
                Integer.parseInt(numFramesText.getText()),
                Integer.parseInt(framesPerSecondSelector.getSelectedItem()),
                saveDirectoryText.getText(),

                enableStimulator,
                preStim,
                stimStr,
                stimDur,
                stimCycleDur,
                numStimCycles,
                enableRamp,
                rampBase,
                rampStart,
                rampEnd,

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

    // when the user updates the threshold slider, update it in the controller
    private void thresholdSliderValueChanged(ChangeEvent e){
        JSlider s = (JSlider) e.getSource();
        controller.updateThresholdValue(s.getValue());
    }

    // when the user updates the tracker speed slider, update it in the controller
    private void trackerSpeedValueChanged(ChangeEvent e){
        JSlider s = (JSlider) e.getSource();
        controller.updateTrackerSpeedValue(s.getValue());
    }

    private boolean saveDirectoryIsValid(){
        File f = new File(saveDirectoryText.getText());
        boolean isValid = f.exists() && f.isDirectory();
        if( !isValid ){
            IJ.showMessage("Save directory does not exist or is a file");
        }

        return isValid;
    }

    // validate integer ui values
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

            if(frame <= 0 || frame > 32000){
                valid = false;
                IJ.showMessage("Number of frames must be in the range of [1, 32000]");
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

        enableTracking = new Checkbox("Enable auto-tracking", false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.insets = externalPadding;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbl.setConstraints(enableTracking, gbc);
        add(enableTracking);

        Label thresholdSliderLabel = new Label("Auto-tracking threshold");
        gbc.gridx = 0;
        gbc.gridy = 4;
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
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(thresholdSlider, gbc);
        add(thresholdSlider);

        Label trackerSpeedLabel = new Label("Auto-tracking speed");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(trackerSpeedLabel, gbc);
        add(trackerSpeedLabel);

        trackerSpeedSlider = new JSlider(5, 12, 7);
        trackerSpeedSlider.setMajorTickSpacing(1);
        trackerSpeedSlider.setPaintTicks(true);
        trackerSpeedSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> trackerSpeedSliderLabels = new Hashtable<Integer, JLabel>();

        trackerSpeedSliderLabels.put(5, new JLabel("Slow"));
        trackerSpeedSliderLabels.put(8, new JLabel("Normal"));
        trackerSpeedSliderLabels.put(12, new JLabel("Ludicrous"));
        trackerSpeedSlider.setLabelTable(trackerSpeedSliderLabels);
        trackerSpeedSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                trackerSpeedValueChanged(e);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.insets = leftLabelPadding;
        gbl.setConstraints(trackerSpeedSlider, gbc);
        add(trackerSpeedSlider);

        goBtn = new Button("Go");
        gbc.gridx = 0;
        gbc.gridy = 6;
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
        gbc.gridy = 6;
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