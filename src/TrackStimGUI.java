import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.prefs.Preferences;

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
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

import ij.ImageListener;
import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;
import ij.ImageStack;

import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.io.TiffEncoder;

import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import ij.gui.Roi;
import ij.gui.Wand;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;

import ij.plugin.filter.EDM;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.PlugInFrame;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import mmcorej.StrVector;
import mmcorej.Configuration;
import mmcorej.PropertySetting;

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
class TrackStimGUI extends PlugInFrame implements ActionListener {
    // globals just in this class
    Preferences prefs;

    // camera options
    TextField numFramesText;

    // directory to save to
    TextField saveDirectoryText;
    String saveDirectory; // trackstim will create subdirectories in this folder


    // pass to Tracker
    CMMCore mmc;
    ScriptInterface app;
    String imageSaveDirectory;  // a subfolder within saveDirectory
    int numFrames;

    public TrackStimGUI(CMMCore cmmcore, ScriptInterface app_) {
        super("TrackStim");

        // set up micro manager variables
        mmc = cmmcore;
        app = app_;
        // initialize GUI
        requestFocus(); // may need for keylistener
        initComponents(); // create the GUI
        setSize(700, 225);
        setVisible(true);
    }


    // method required by ItemListener
    public void itemStateChanged(ItemEvent e) {
    }

    void testFramesPerSecond(){

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        int numFrames = 100; //
        int cycleLengthMs = 100; // take a pic every 100ms
        for(int i = 0; i < numFrames; i++ ){
            int timePointMs = i * cycleLengthMs; // e.g. 0 ms, 100ms, 200ms, etc..
            ScheduledSnapShot s = new ScheduledSnapShot(mmc, app, timePointMs);

            ses.schedule(s, timePointMs, TimeUnit.MILLISECONDS);
        }
    }

    // handle button presses
    public void actionPerformed(ActionEvent e) {
        String clickedBtn = e.getActionCommand();


        // button to start TrackStim image acquisition
        if (clickedBtn.equals("Go")){
            IJ.log("Go");

            if( !app.isLiveModeOn() ){
                app.enableLiveMode(true);

            }

            testFramesPerSecond();
        }

        // stop image acquisition process
        if (clickedBtn.equals("Stop")){
        }
    }

    // create the GUI
    void initComponents(){
        // Prepare GUI
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        Button b2 = new Button("Go");
        b2.setPreferredSize(new Dimension(100, 60));
        b2.addActionListener(this);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b2, gbc);
        add(b2);

        Button b3 = new Button("Stop");
        b3.setPreferredSize(new Dimension(100, 60));
        b3.addActionListener(this);
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b3, gbc);
        add(b3);

        Label labelframe = new Label("Frame num");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelframe, gbc);
        add(labelframe);

        numFramesText = new TextField(String.valueOf(numFrames), 5);
        numFramesText.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(numFramesText, gbc);
        add(numFramesText);

        Label labeldir = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbl.setConstraints(labeldir, gbc);
        add(labeldir);

        saveDirectoryText = new TextField(saveDirectory, 40);
        saveDirectoryText.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(saveDirectoryText, gbc);
        add(saveDirectoryText);
        gbc.fill = GridBagConstraints.NONE;// return to default

        Button b4 = new Button("Change dir");
        b4.addActionListener(this);
        gbc.gridx = 6;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbl.setConstraints(b4, gbc);
        add(b4);
    }

}