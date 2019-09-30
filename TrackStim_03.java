import java.util.*;
import java.util.prefs.Preferences;
import java.util.concurrent.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.PlugInFrame;

import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import mmcorej.StrVector;
import org.micromanager.*;
import mmcorej.Configuration;
import mmcorej.PropertySetting;

public class TrackStim_03 extends PlugInFrame implements ActionListener, ImageListener, MouseListener, ItemListener {
    // public class RealTimeTracker_09 extends PlugInFrame implements
    // ActionListener,ImageListener{

    // globals just in this class
    Preferences prefs;
    TextField framenumtext;
    TextField skiptext;
    // TextField intervaltext;
    // TrackingThread(RealTimeTracker_01 tpf);
    TrackingThread10 tt = null;

    // filed used in TrackingThread
    java.awt.Checkbox closest;// target definition method.
    java.awt.Checkbox right;// field for tacking source
    java.awt.Checkbox objective_ten;// change stage movement velocity
    java.awt.Checkbox objective_40;// change stage movement velocity
    java.awt.Checkbox textpos;// if save xy pos data into txt file. not inclued z.
    java.awt.Choice acceleration;
    java.awt.Choice thresholdmethod;
    java.awt.Checkbox CoM;// center of mass method
    java.awt.Checkbox manualtracking;
    java.awt.Checkbox FULL;// full size filed.
    java.awt.Checkbox BF;// Bright field tracking only works with full size
    TextField savedir;
    String dir;

    // camera trigger
    java.awt.Choice exposureduration;
    java.awt.Choice cyclelength;

    // Stimulation
    java.awt.Checkbox STIM;
    TextField prestimulation;
    TextField stimstrength;
    TextField stimduration;
    TextField stimcyclelength;
    TextField stimcyclenum;
    java.awt.Checkbox ramp;// ramp
    TextField rampbase;
    TextField rampstart;
    TextField rampend;

    String adportsname;// stimulation port for arduino

    // pass to TrackingThread
    CMMCore mmc_;
    ImagePlus imp;
    ImageCanvas ic;
    String dirforsave;
    int frame = 1200;// String defaultframestring;
    boolean ready;

    public TrackStim_03() {
        super("TrackerwithStimulater");
        // First, check if there is mmc.
        if (mmc_ == null) {
            // if core object is not set attempt to get its global handle
            mmc_ = MMStudioPlugin.getMMCoreInstance();
        }
        if (mmc_ == null) {
            IJ.error("Unable to get Micro-Manager Core API handle.\n"
                    + "If this module is used as ImageJ plugin, Micro-Manager Studio must be running first!");
            return;
        }
        IJ.log("TrackStim Constructor: MMCore initialized");

        MMStudioMainFrame mmcmf = MMStudioPlugin.frame_;

        prefs = Preferences.userNodeForPackage(this.getClass());// make instance?
        imp = WindowManager.getCurrentImage();
        ImageWindow iw = imp.getWindow();
        ic = iw.getCanvas();
        ic.addMouseListener(this);

        // try to get preferences directory and frame
        try {
            dir = prefs.get("DIR", "");
            IJ.log("pref dir " + dir);
            if (prefs.get("FRAME", "") == "") {
                frame = 3000;
            } else {
                frame = Integer.parseInt(prefs.get("FRAME", ""));
            }
            IJ.log("TrackStim Constructor: Frame value is " + String.valueOf(frame));
        } catch (java.lang.Exception e){
            IJ.log("TrackStim Constructor: Could not get frame value from preferences obj");
            IJ.log(e.getMessage());
        }

        int dircount = 0;
        if (dir == "") {
            // check directry in the current
            dir = IJ.getDirectory("current");
            if (dir == null) {
                dir = IJ.getDirectory("home");
            }
            IJ.log("TrackStim Constructor: initial dir is " + dir);
            File currentdir = new File(dir);
            File[] filelist = currentdir.listFiles();
            if (filelist != null) {
                for (int i = 0; i < filelist.length; i++) {
                    if (filelist[i].isDirectory()) {
                        dircount++;
                    }
                }
            }
        }
        IJ.log("TrackStim Constructor: initial dir is " + dir);
        IJ.log("TrackStim Constructor: number of directories is " + String.valueOf(dircount));

        ImagePlus.addImageListener(this);
        requestFocus(); // may need for keylistener

        // Prepare GUI
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        Button b = new Button("Ready");
        b.setPreferredSize(new Dimension(100, 60));
        b.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbl.setConstraints(b, gbc);
        add(b);

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

        Label labelexpduration = new Label("exposure");
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbl.setConstraints(labelexpduration, gbc);
        add(labelexpduration);

        exposureduration = new Choice();
        exposureduration.setPreferredSize(new Dimension(80, 20));
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        exposureduration.add("0");
        exposureduration.add("1");
        exposureduration.add("10");
        exposureduration.add("50");
        exposureduration.add("100");
        exposureduration.add("200");
        exposureduration.add("500");
        exposureduration.add("1000");
        exposureduration.addItemListener(this);
        gbl.setConstraints(exposureduration, gbc);
        add(exposureduration);

        Label labelcameracyclelength = new Label("cycle len.");
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelcameracyclelength, gbc);
        add(labelcameracyclelength);

        cyclelength = new Choice();
        cyclelength.setPreferredSize(new Dimension(80, 20));
        gbc.gridx = 7;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        cyclelength.add("0");
        cyclelength.add("50");
        cyclelength.add("100");
        cyclelength.add("200");
        cyclelength.add("500");
        cyclelength.add("1000");
        cyclelength.add("2000");
        cyclelength.addItemListener(this);
        gbl.setConstraints(cyclelength, gbc);
        add(cyclelength);

        Label labelframe = new Label("Frame num");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelframe, gbc);
        add(labelframe);

        framenumtext = new TextField(String.valueOf(frame), 5);
        framenumtext.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(framenumtext, gbc);
        add(framenumtext);

        closest = new Checkbox("Just closest", true);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(closest, gbc);
        add(closest);

        acceleration = new Choice();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        acceleration.add("1x");
        acceleration.add("2x");
        acceleration.add("4x");
        acceleration.add("5x");
        acceleration.add("6x");
        gbl.setConstraints(acceleration, gbc);
        add(acceleration);

        thresholdmethod = new Choice();
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        thresholdmethod.add("Yen");// good for normal
        thresholdmethod.add("Triangle");// good for on coli
        thresholdmethod.add("Otsu");// good for ventral cord?

        thresholdmethod.add("Default");
        thresholdmethod.add("Huang");
        thresholdmethod.add("Intermodes");
        thresholdmethod.add("IsoData");
        thresholdmethod.add("Li");
        thresholdmethod.add("MaxEntropy");
        thresholdmethod.add("Mean");
        thresholdmethod.add("MinError(I)");
        thresholdmethod.add("Minimum");
        thresholdmethod.add("Moments");
        thresholdmethod.add("Percentile");
        thresholdmethod.add("RenyiEntropy");
        thresholdmethod.add("Shanbhag");
        thresholdmethod.setPreferredSize(new Dimension(80, 20));
        gbl.setConstraints(thresholdmethod, gbc);
        add(thresholdmethod);

        right = new Checkbox("Use right", false);
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbl.setConstraints(right, gbc);
        add(right);

        textpos = new Checkbox("Save xypos file", false);
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbl.setConstraints(textpos, gbc);
        add(textpos);

        Label labelskip = new Label("one of ");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(labelskip, gbc);
        add(labelskip);

        skiptext = new TextField("1", 2);
        skiptext.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(skiptext, gbc);
        add(skiptext);

        CoM = new Checkbox("Center of Mass", false);
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(CoM, gbc);
        add(CoM);

        manualtracking = new Checkbox("manual track", false);
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbl.setConstraints(manualtracking, gbc);
        add(manualtracking);

        FULL = new Checkbox("Full field", false);
        gbc.gridx = 5;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(FULL, gbc);
        add(FULL);

        BF = new Checkbox("Bright field", false);
        gbc.gridx = 6;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbl.setConstraints(BF, gbc);
        add(BF);

        Label labeldir = new Label("Save at");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbl.setConstraints(labeldir, gbc);
        add(labeldir);

        savedir = new TextField(dir, 40);
        savedir.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(savedir, gbc);
        add(savedir);
        gbc.fill = GridBagConstraints.NONE;// return to default

        Button b4 = new Button("Change dir");
        b4.addActionListener(this);
        gbc.gridx = 6;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbl.setConstraints(b4, gbc);
        add(b4);

        // gui for stimulation
        STIM = new Checkbox("Light", false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(STIM, gbc);
        add(STIM);

        b = new Button("Run");
        b.setPreferredSize(new Dimension(40, 20));
        b.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(b, gbc);
        add(b);
        gbc.gridheight = 1;

        Label labelpre = new Label("Pre-stim");
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelpre, gbc);
        add(labelpre);

        prestimulation = new TextField(String.valueOf(10000), 6);
        prestimulation.setPreferredSize(new Dimension(50, 30));
        prestimulation.addActionListener(this);
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(prestimulation, gbc);
        add(prestimulation);

        Label labelstrength = new Label("Strength <63");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelstrength, gbc);
        add(labelstrength);

        stimstrength = new TextField(String.valueOf(63), 6);
        stimstrength.setPreferredSize(new Dimension(50, 30));
        stimstrength.addActionListener(this);
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimstrength, gbc);
        add(stimstrength);

        Label labelduration = new Label("Duration");
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelduration, gbc);
        add(labelduration);

        stimduration = new TextField(String.valueOf(5000), 6);
        stimduration.setPreferredSize(new Dimension(50, 30));
        stimduration.addActionListener(this);
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimduration, gbc);
        add(stimduration);

        Label labelcyclelength = new Label("Cycle length");
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclelength, gbc);
        add(labelcyclelength);

        stimcyclelength = new TextField(String.valueOf(10000), 6);
        stimcyclelength.setPreferredSize(new Dimension(50, 30));
        stimcyclelength.addActionListener(this);
        gbc.gridx = 4;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimcyclelength, gbc);
        add(stimcyclelength);

        Label labelcyclenum = new Label("Cycle num");
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelcyclenum, gbc);
        add(labelcyclenum);

        stimcyclenum = new TextField(String.valueOf(3), 6);
        stimcyclenum.setPreferredSize(new Dimension(50, 30));
        stimcyclenum.addActionListener(this);
        gbc.gridx = 4;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(stimcyclenum, gbc);
        add(stimcyclenum);

        ramp = new Checkbox("ramp", false);
        gbc.gridx = 5;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        // gbc.gridheight=3;
        gbc.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(ramp, gbc);
        add(ramp);

        Label labelbase = new Label("base");
        gbc.gridx = 6;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelbase, gbc);
        add(labelbase);

        rampbase = new TextField(String.valueOf(0), 3);
        rampbase.setPreferredSize(new Dimension(30, 30));
        rampbase.addActionListener(this);
        gbc.gridx = 7;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampbase, gbc);
        add(rampbase);

        Label labelrampstart = new Label("start");
        gbc.gridx = 6;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelrampstart, gbc);
        add(labelrampstart);

        rampstart = new TextField(String.valueOf(0), 3);
        rampstart.setPreferredSize(new Dimension(30, 30));
        rampstart.addActionListener(this);
        gbc.gridx = 7;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampstart, gbc);
        add(rampstart);

        Label labelrampend = new Label("end");
        gbc.gridx = 6;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(labelrampend, gbc);
        add(labelrampend);

        rampend = new TextField(String.valueOf(63), 3);
        rampend.setPreferredSize(new Dimension(30, 30));
        rampend.addActionListener(this);
        gbc.gridx = 7;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(rampend, gbc);
        add(rampend);
        setSize(700, 225);
        setVisible(true);


        // find the port that the xy stage is on
        ArrayList<String> portslist = getPortLabels();
        adportsname = "";
        for (int i = 0; i < portslist.size(); i++) {
            if (portslist.get(i).indexOf("usbmodem") > 0)// adhoc. need better way later.
            {
                adportsname = portslist.get(i);
            }
        }

        IJ.log("TrackStim Constructor: port list");
        IJ.log(Arrays.toString(portslist.toArray()));
        IJ.log("TrackStim Constructor: adportsname is" + adportsname);

        if (adportsname.equals("")) {
            IJ.log("TrackStim Constructor: adportsname is empty string.  stimulator is not assigned");
        } else {
            IJ.log("TrackStim Constructor: stimulator is assigned at adportsname: " + adportsname);
        }
    }

    void prepSignals(int channel) {
        IJ.log("prepSignals: parsing/validating UI values to send through channel " + Integer.toString(channel));

        int inputval1 = 0;
        int inputval2 = 0;
        int inputval3 = 0;
        int inputval4 = 0;
        int inputval5 = 0;
        int inputval6 = 0;
        int inputval7 = 0;
        int inputval8 = 0;
        try {
            inputval1 = Integer.parseInt(prestimulation.getText());// initial delay
            inputval2 = Integer.parseInt(stimstrength.getText());// strength
            inputval3 = Integer.parseInt(stimduration.getText());// duration
            inputval4 = Integer.parseInt(stimcyclelength.getText());// cycle time
            inputval5 = Integer.parseInt(stimcyclenum.getText());// repeat
            inputval6 = Integer.parseInt(rampbase.getText());// base
            inputval7 = Integer.parseInt(rampstart.getText());// start
            inputval8 = Integer.parseInt(rampend.getText());// end
            // ramp

            if (ramp.getState()) {
                int absdiff = Math.abs(inputval8 - inputval7);
                int signoframp = Integer.signum(inputval8 - inputval7);

                for (int i = 0; i < inputval5; i++) {
                    for (int j = 0; j < absdiff + 1; j++) {
                        setSender(channel, inputval1 + i * inputval4 + j * (inputval3 / absdiff),
                                inputval7 + j * signoframp);
                    }
                    setSender(channel, inputval1 + inputval3 + i * inputval4, inputval6);
                }
            } else {
                for (int i = 0; i < inputval5; i++) {
                    setSender(channel, inputval1 + i * inputval4, inputval2);
                    setSender(channel, inputval1 + inputval3 + i * inputval4, inputval6);
                }
            }
        } catch (java.lang.Exception e) {
            IJ.log("prepSignals: error sending signals to channel " + Integer.toString(channel));
            IJ.log(e.getMessage());
        }
    }

    // mili sec, and 0-63
    // helper function for prepSignals
    void setSender(int channel, int timepoint, int signalstrength) {
        Signalsender sd = new Signalsender(this);
        sd.setChannel(channel);
        sd.setSignalStrength(signalstrength);
        ScheduledExecutorService ses;
        ScheduledFuture future = null;
        ses = Executors.newSingleThreadScheduledExecutor();
        future = ses.schedule(sd, timepoint * 1000, TimeUnit.MICROSECONDS);
    }

    ArrayList<String> getPortLabels() {
        ArrayList<String> portlist = new ArrayList<String>();
        Configuration conf = mmc_.getSystemState();
        long index = conf.size();
        String[] tempports = new String[(int) index];
        PropertySetting ps = new PropertySetting();

        for (int i = 0; i < index; i++) {
            try {
                ps = conf.getSetting(i);
            } catch (java.lang.Exception e) {
                IJ.log("getPortLabels: error getting config value");
                IJ.log(e.getMessage());
            }

            IJ.log("getPortLabels: property setting ps is " + ps.Serialize());

            // just testing enviroment having only one port /dev/tty.usbmodem1d11
            if (ps.Serialize().indexOf("Port") > 0) {
                // tempports[i]=ps.Serialize().split(" ")[2];
                String parsedPort = ps.Serialize().split(" ")[2];

                IJ.log("getPortLabels: parsedPort is " + parsedPort);
                portlist.add(ps.Serialize().split(" ")[2]);
            }
        }

        return portlist;
    }

    public void imageOpened(ImagePlus imp) {
    }

    public void imageUpdated(ImagePlus imp) {
    }

    // Write logic to clear valables when image closed here
    public void imageClosed(ImagePlus impc) {
        IJ.log("imageClosed: cleaning up");
        if (imp == impc) {
            imp = null;
            IJ.log("imageClosed: imp set to null");
        }
    }

    // validate the frame field in the UI
    boolean checkFrameField(){
        int testint;

        try {
            testint = Integer.parseInt(framenumtext.getText());
            frame = testint;
            return true;
        } catch (java.lang.Exception e) {
            IJ.log("checkFrameField: the current value of the frame field is not an int");
            IJ.log(e.getMessage());
            framenumtext.setText(String.valueOf(frame));
            return false;
        }
    }

    // validate the directory field in the UI
    boolean checkDirField(){
        String dirName = savedir.getText();
        File checkdir = new File(dirName);

        if (checkdir.exists()) {
            IJ.log("checkDirField: directory " + dirName + " exists");
            dir = savedir.getText();
            return true;
        } else {
            IJ.log("checkDirField: directory " + dirName + " DOES NOT EXIST! Please create the directory first");
            savedir.setText(dir);
            return false;
        }
    }

    // method required by ItemListener
    public void itemStateChanged(ItemEvent e) {
        int selectedindex = 0;
        int exposurechoice = 0;
        int lengthchoice = 0;
        int sendingdata = 0;
        Choice cho = (Choice) e.getSource();
        selectedindex = cho.getSelectedIndex();

        IJ.log("itemStateChanged: the item at index " + String.valueOf(selectedindex) + " has been selected");

        if (e.getSource() == exposureduration || e.getSource() == cyclelength) {
            /*
             * arduino code int triggerlengtharray[]={ 0,1,10,50,100,200,500,1000};//3bit 8
             * values msec. int cyclelengtharray[]={ 0,50,100,200,500,1000,2000};//use 3bit
             */
            exposurechoice = exposureduration.getSelectedIndex();
            lengthchoice = cyclelength.getSelectedIndex();
            if ((exposurechoice == 1 || exposurechoice == 2) && lengthchoice == 0) {
                cyclelength.select(1);
                lengthchoice = cyclelength.getSelectedIndex();
            } else if (lengthchoice + 1 <= exposurechoice) {
                cyclelength.select(exposurechoice - 1);
                lengthchoice = cyclelength.getSelectedIndex();
            }
            sendingdata = 1 << 6 | lengthchoice << 3 | exposurechoice;

            // sending vale to arduino
            mmcorej.CharVector sendingchrvec = new mmcorej.CharVector();
            IJ.log("itemStateChanged: sending the following data to the arduino: " + String.valueOf(sendingdata));
            sendingchrvec.add((char) sendingdata);
            try {
                mmc_.writeToSerialPort(adportsname, sendingchrvec);
            } catch (java.lang.Exception ex) {
                IJ.log("itemStateChanged:  error trying to send data " + String.valueOf(sendingdata) + "to arduino");
                IJ.log(ex.getMessage());
            }
        }

    }

    // handle button presses
    public void actionPerformed(ActionEvent e) {
        ImagePlus currentimp = WindowManager.getCurrentImage();
        String lable = e.getActionCommand();

        IJ.log("actionPerformed: button clicked is " + lable);

        // frame num changed
        if (lable.equals(framenumtext.getText())){
            boolean checkframefield = checkFrameField();
            IJ.log("actionPerformed: frame is " + String.valueOf(frame));

            // savedir has changed
        } else if (lable.equals(savedir.getText())){
            checkDirField();
            IJ.log("actionPerformed: directory is " + savedir.getText());

            // any button pushed
        } else {

            // ready button pushed
            if (lable.equals("Ready")) {
                ready = true;
                tt = new TrackingThread10(this);
                tt.start();

                // go button pressed
            } else if (lable.equals("Go")) {

                // frame number is valid and directory exists
                if (checkFrameField() && checkDirField()) {
                    dir = savedir.getText();
                    prefs.put("DIR", dir); // save in the preference
                    prefs.put("FRAME", String.valueOf(frame));

                    // get count number of directories N so that we can create directory N+1
                    File currentdir = new File(dir);
                    File[] filelist = currentdir.listFiles();
                    int dircount = 0;
                    for (int i = 0; i < filelist.length; i++) {
                        if (filelist[i].isDirectory()) {
                            dircount++;
                        }
                    }

                    IJ.log("actionPerformed: number for directories in " + dir + " is: " + String.valueOf(dircount));
                    int i = 1;
                    File newdir = new File(dir + "temp" + String.valueOf(dircount + i));
                    while (newdir.exists()) {
                        i++;
                        newdir = new File(dir + "temp" + String.valueOf(dircount + i));
                    }

                    newdir.mkdir();
                    dirforsave = newdir.getPath();// this one doen't have "/" at the end
                    IJ.log("actionPerformed: created new directory " + dirforsave);

                    // check if the tracking thread is running
                    if (tt != null){
                        if (tt.isAlive()) {
                            IJ.log("actionPerformed: GO was pressed but the tracking thread is still running, stopping sequence acquisition");

                            // stop by stopping sequence acquisition. don't know better way but seems work.
                            try {
                                // probably this code cause lots of death of micromanager?
                                // cant solve now.
                                mmc_.stopSequenceAcquisition();
                            } catch (java.lang.Exception ex) {
                                IJ.log("actionPerformed: GO was pressed but could not stop previous thread sequence acquisition");
                                IJ.log(ex.getMessage());
                            }
                        } else {
                            IJ.log("actionPerformed: GO was pressed and the tracking thread is not active, nothing needs to be done");
                        }

                        IJ.log("actionPerformed: GO was pressed, setting tracking thread to null before creating new tracking thread");
                        tt = null;
                    } else {
                        IJ.log("actionPerformed: GO was pressed and tracking thread is null, nothing to do before creating new tracking thread");
                    }

                    ready = false;
                    if (STIM.getState()) {
                        prepSignals(0);
                    }

                    tt = new TrackingThread10(this);
                    tt.start();
                }
            } else if (lable.equals("Stop")) {

                if (tt != null) {
                    if (tt.isAlive()) {
                        IJ.log("actionPerformed: STOP was pressed, stopping sequence acquisition");
                        try {
                            mmc_.stopSequenceAcquisition();
                        } catch (java.lang.Exception ex) {
                            IJ.log("actionPerformed: STOP was pressed but could not stop previous thread sequence acquisition");
                            IJ.log(ex.getMessage());
                        }
                    } else {
                        IJ.log("actionPerformed: STOP was pressed, but the tracking thread is not alive, nothing to do");
                    }
                } else {
                    IJ.log("actionPerformed: STOP was pressed, but the tracking thread is null, nothing to do");
                }

            } else if (lable.equals("Change dir")) {
                DirectoryChooser dc = new DirectoryChooser("Directory for temp folder");
                String dcdir = dc.getDirectory();
                savedir.setText(dcdir);
            } else if (lable.equals("Run")){
                IJ.log("actionPerformed: RUN was pressed, calling prepSignals...");
                prepSignals(0);
            }
        }
    }

    /** Handle the key typed event from the text field. */
    public void keyTyped(KeyEvent e) {
        IJ.log("keyTyped: " + KeyEvent.getKeyText(e.getKeyCode()));
    }

    /** Handle the key-pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
        IJ.log("keyPressed: " + KeyEvent.getKeyText(e.getKeyCode()));
    }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) {
        IJ.log("keyReleased: " + KeyEvent.getKeyText(e.getKeyCode()));
    }

    // Handle mouse click
    public void mouseClicked(MouseEvent e) {
        IJ.log("mouseClicked: ");
        if (tt != null) {
            if (tt.isAlive()) {
                IJ.log("mouseClicked: tracking thread is active, changing target...");
                tt.changeTarget();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}// public class RealTimeTracker_02 extends PlugInFrame implements
 // ActionListener,ImageListener, KeyListener{ end

////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////
// To watch the process during data collection, use different thread.
class TrackingThread10 extends Thread {
    // vaiables recieve from RealTimeTracker
    TrackStim_03 tpf;
    CMMCore mmc_;
    ImagePlus imp;
    ImageCanvas ic;
    String dirforsave;
    int frame;// String defaultframestring;
    boolean ready;

    // inside of thread
    static int countslice = 0;
    static ImageStack binaryimgstack = null;
    static ArrayList<Roi> preroiarraylist = null;
    static double predistance = 0.0;
    static double pretheta = 10.0;// normal radian must beteween =-pi. so this value could use to check if there
                                  // is pretheta.
    static int[] threshbuffer = new int[30];
    static int threshsum = 0;
    static int threahaverage = 0;

    static ImageProcessor ip_;
    static ImageProcessor ip_resized;
    static ImageProcessor ip_resizedori;
    static ImagePlus impresizedori;
    static ImageProcessor iplbyte;

    double[][] measurespre;
    double[][] measures;
    // targethistory[slicenumber][roi index, x, y]
    double[][] targethistory;

    /*---------------------------------------  constant variables-------------------------------------------------*/
    // if the shift is larger than limit, move stage.
    double LIMIT = 5;
    // adhoc coefficencty for x10=6.3. x63=1.0
    // double coef=6.3;
    double COEF = 1.0;
    // allowance angle change
    static double minanglecos = Math.cos(60.0 / 180 * Math.PI);// 45/180=0 because int...... so need to change order or
                                                               // add .0
    // allowance distance change
    static double mindistancechange = 0.3;

    TrackingThread10(TrackStim_03 tpf) {
        IJ.log("constructor");
        this.tpf = tpf;
        mmc_ = tpf.mmc_;
        imp = tpf.imp;
        ic = tpf.ic;
        dirforsave = tpf.dirforsave;
        frame = tpf.frame;// String defaultframestring;
        ready = tpf.ready;
    }

    public void run() {
        IJ.log("start");
        this.startAcq("from thread");
    }

    public void changeTarget() {
        // get clicked xy order
        Point cursorpoint = ic.getCursorLoc();
        IJ.log("clicked at " + String.valueOf(cursorpoint.x) + " " + String.valueOf(cursorpoint.y));
        if (tpf.closest.getState())// center of mass method
        {
            targethistory[countslice - 1][1] = cursorpoint.x;
            targethistory[countslice - 1][2] = cursorpoint.y;
            // also change current slice's data...
            targethistory[countslice][1] = cursorpoint.x;
            targethistory[countslice][2] = cursorpoint.y;
        } else// normal thresholding method
        {
            // compare with measurespre[roinumber][area,mean,x,y]
            double distancescalar = 0;
            double minval = 0;
            int minindex = 0;
            double dx = 0;
            double dy = 0;
            double mindx = 0;
            double mindy = 0;
            for (int i = 0; i < measurespre.length; i++) {
                dx = cursorpoint.x - measurespre[i][2];
                dy = cursorpoint.y - measurespre[i][3];
                distancescalar = Math.sqrt(dx * dx + dy * dy);
                if (i != 0) {
                    if (minval > distancescalar) {
                        minval = distancescalar;
                        minindex = i;
                        mindx = dx;
                        mindy = dy;
                    }
                } else {
                    minval = distancescalar;
                    minindex = 0;
                }
            }

            double correctedx = measurespre[minindex][2];
            double correctedy = measurespre[minindex][3];
            // change the targethistory[slicenum][roiindex,x,y]

            targethistory[countslice - 1][0] = minindex;
            targethistory[countslice - 1][1] = correctedx;
            targethistory[countslice - 1][2] = correctedy;
            // also change current slice's data...
            targethistory[countslice][0] = minindex;
            targethistory[countslice][1] = correctedx;
            targethistory[countslice][2] = correctedy;
            IJ.log("changed the target to roi " + String.valueOf(minindex) + "; " + String.valueOf(correctedx) + " "
                    + String.valueOf(correctedy));
        }
    }
    /*
     * public void mouseClicked(MouseEvent e) { //get clicked xy order Point
     * cursorpoint=ic.getCursorLoc();
     * IJ.log("clicked at "+String.valueOf(cursorpoint.x)+ " "
     * +String.valueOf(cursorpoint.y)); //compare with
     * measurespre[roinumber][area,mean,x,y] double distancescalar=0; double
     * minval=0; int minindex=0; double dx=0; double dy=0; double mindx=0; double
     * mindy=0; for(int i=0; i<measurespre.length; i++) {
     * dx=cursorpoint.x-measurespre[i][2]; dy=cursorpoint.y-measurespre[i][3];
     * distancescalar=Math.sqrt(dx * dx + dy * dy); if(i!=0) {
     * if(minval>distancescalar) { minval=distancescalar; minindex=i; mindx=dx;
     * mindy=dy; } } else { minval=distancescalar; minindex=0; } }
     *
     * double correctedx=measurespre[minindex][2]; double
     * correctedy=measurespre[minindex][3]; //change the
     * targethistory[slicenum][roiindex,x,y]
     *
     * targethistory[countslice-1][0]=minindex;
     * targethistory[countslice-1][1]=correctedx;
     * targethistory[countslice-1][2]=correctedy; //also change current slice's
     * data... targethistory[countslice][0]=minindex;
     * targethistory[countslice][1]=correctedx;
     * targethistory[countslice][2]=correctedy; IJ.log("changed the target to roi "
     * +String.valueOf(minindex) + "; " + String.valueOf(correctedx) + " "
     * +String.valueOf(correctedy)); //targethistory[countslice-1][0]=(double)
     * closesttopoint;
     *
     * } public void mouseEntered(MouseEvent e) {} public void
     * mouseExited(MouseEvent e) {} public void mousePressed(MouseEvent e) {} public
     * void mouseReleased(MouseEvent e) {}
     */

    /*----------------------------------------------- functions --------------------------------------------------*/
    // look for stage serial port name to send command
    private String getStagePortLabel(String stagelabel) {
        String stagelabel_ = stagelabel;
        Configuration conf = mmc_.getSystemState();
        long index = conf.size();
        // print(index);
        int i;
        String PORT = "";
        PropertySetting ps = new PropertySetting();
        for (i = 0; i < index; i++) {
            try {
                ps = conf.getSetting(i);
            } catch (java.lang.Exception e) {
            }
            String str = ps.getDeviceLabel();
            IJ.log(str);
            IJ.log(ps.Serialize());// this should include port name
            if (ps.Serialize().indexOf(stagelabel_) == 0) {
                // print("^"+ps.Serialize());//this should include port name
                if (ps.Serialize().indexOf("Port") > 0) {
                    PORT = ps.Serialize().split(" ")[2];
                }
            }
        }
        return PORT;
    }

    // second new method to track
    // make bynary image, watershed, detect objects above particular size
    // measure objects positions (and area mean values)
    // conpare previous image and determine which object is a target using closest
    // position
    // calculate distance the target from centor of image
    // returnval=[roinumber][area,mean,xCentroid,yCentroid]
    // static is bit faster
    static double[][] getObjmeasures(ImagePlus imp, ImageProcessor ip, boolean savebinary, String method) {
        String thresholdmethodstring = method;
        // the imp ip must left half or something not full image
        // ImageProcessor ip_=ip.duplicate();
        ip_ = ip.duplicate();

        // ImageProcessor ip_resized=ip_.resize(ip_.getWidth()/2);
        // ImageProcessor ip_resizedori=ip_resized.duplicate();
        ip_resized = ip_.resize(ip_.getWidth() / 2);
        RankFilters rf = new RankFilters();
        // rf.setup("median",imp);
        rf.rank(ip_resized, 0.0, 4);// median 4 periodic black white noize cause miss thresholding, so eliminate
                                    // those noize

        ip_resizedori = ip_resized.duplicate();

        // test non resized one
        // seems not able to do more than 2min 50msec....
        // ip_resized=ip_.duplicate();
        // ip_resizedori=ip_.duplicate();

        // print(ip_resized.getWidth());
        // ImagePlus impresizedori=new ImagePlus("l",ip_resizedori);
        impresizedori = new ImagePlus("l", ip_resizedori);

        // initiall 31 slice calculate Autothresho at every time
        // also static value doesnt clear after the imaging by "ready" ,use it to reduce
        // calc.?
        // if(countslice<30&&threshsum==0)
        // changed to clear every time. since hard to clear manually
        if (countslice < 30) {
            // ip_resized.setAutoThreshold("Default",true,0);
            ip_resized.setAutoThreshold(thresholdmethodstring, true, 0);// seems good. and fast? 13msec/per->less than
                                                                        // 10msec. better than otsu at head
            // need check after median filter if this is better.
            // ip_resized.setAutoThreshold("Intermodes",true,0);//too small tend lost
            // ip_resized.setAutoThreshold("MaxEntropy",true,0);//looks good
            // ip_resized.setAutoThreshold("Minimum",true,0);//too small tend lost
            // ip_resized.setAutoThreshold("RenyiEntropy",true,0);//looks good
            // ip_resized.setAutoThreshold("Shanbhag",true,0);//seems unstabel lost.
            // ip_resized.setAutoThreshold("Triangle",true,0);//bit big
            // ip_resized.setAutoThreshold("Otsu",true,0);//good and fast 10msec/per. not
            // good for head
            threshbuffer[countslice] = (int) ip_resized.getMinThreshold();
            threahaverage = (int) ip_resized.getMinThreshold();
            if (countslice == 29) {
                IJ.log("threshsum" + String.valueOf(threshsum));
                // sum and average threshold
                int i;
                for (i = 0; i < 30; i++) {
                    threshsum = threshsum + threshbuffer[i];
                }
                threahaverage = threshsum / 30;
            }
        }
        // after 30 slice
        else {
            // every 50 slice calculate setAutoThreshold
            if (countslice % 50 == 0) {
                ip_resized.setAutoThreshold(thresholdmethodstring, true, 0);
                int slotindex = ((countslice - 30) / 50) % 30;
                int newdata = (int) ip_resized.getMinThreshold();
                int olddata = threshbuffer[slotindex];
                threshbuffer[slotindex] = newdata;
                threshsum = threshsum - olddata + newdata;
                threahaverage = threshsum / 30;
            }
        }

        // print("th val "+ip_resized.getAutoThreshold());
        ip_resized.threshold(threahaverage);
        ImageStatistics imstat;

        // ipleft.invert();
        // ImageProcessor iplbyte=ip_resized.convertToByte(false);
        iplbyte = ip_resized.convertToByte(false);
        // convertToByte(true);
        // ts= new Thresholder();
        // ts.run("");
        // print("is binary "+ipleft.isBinary());

        EDM edm = new EDM();

        edm.toWatershed(iplbyte);

        iplbyte.invert();

        ImagePlus impleftbyte = new ImagePlus("lbyte", iplbyte);
        countslice++;
        if (savebinary) {

            binaryimgstack.setPixels(iplbyte.getPixelsCopy(), countslice);
        }
        int widthleft = impleftbyte.getWidth();
        int heightleft = impleftbyte.getHeight();

        // impleftbyte.show();
        Wand wand = new Wand(iplbyte);
        // non resized=60 resized=12. x10 3 try
        int minimamarea = 6;
        int x;
        int y;
        // int counter=0;
        iplbyte.setValue(255);
        ArrayList<Roi> roiarraylist = new ArrayList<Roi>();
        preroiarraylist = roiarraylist;
        Roi roi;

        // this whole image scan loop is too heaby. take 200msec per resized half image
        // need faster method....
        // So, don't scan every row/column. every 3 line may enough?
        // now 0.5/10 slice. 50 msec, 2sec/40
        // non resized image with each 6. 2.8sec/40, 70msec.
        // this might because get()? using pixel arry is better?
        // ...All these methods should only be used if you intend to modify just a few
        // pixels. If you
        // want to modify large parts of the image it is faster to work with the pixel
        // array....yoru
        // Don't need get every time. just have array and pick up
        // every 2line tri in ver2
        byte[] pixels = (byte[]) iplbyte.getPixels();
        for (y = 0; y < heightleft; y = y + 3) {
            for (x = 0; x < widthleft; x = x + 3) {
                // if(iplbyte.get(x,y)==0)
                if (pixels[y * widthleft + x] == 0)
                // if(iplbyte.getPixel(x,y)==0)
                {

                    wand.autoOutline(x, y, 0.0, 1.0, 4);
                    // type polygonroi=2
                    roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, 2);
                    // print(x+" "+y);
                    impleftbyte.setRoi(roi);
                    imstat = impleftbyte.getStatistics(1);// area 1 mean 2
                    if (imstat.area > minimamarea) {
                        // roiarray=roiarray+
                        // roiarray[counter]=roi;
                        roiarraylist.add(roi);
                        /*
                         * if(savebinary) { ImageProcessor
                         * drawip=binaryimgstack.getProcessor(countslice); Rectangle r =
                         * roi.getBounds();
                         *
                         * drawip.moveTo(r.x,r.y); drawip.setValue(128);
                         * drawip.drawString(String.valueOf(roiarraylist.size())); //counter++; }
                         */
                    }
                    // delet already detected roi.
                    iplbyte.fill(roi);
                    // break;

                }
            }
        }

        // print(counter);
        // impleftbyte.show();

        int i;
        // 3 measurement factors, area, mean, centroid
        double[][] roimeasures = new double[roiarraylist.size()][4];
        Roi roi_;
        for (i = 0; i < roiarraylist.size(); i++) {
            roi_ = (Roi) roiarraylist.get(i);
            impresizedori.setRoi(roi_);
            imstat = impresizedori.getStatistics(1 + 2 + 32 + 4 + 64);// area 1 mean 2, sd 4, centerofmass 64
            // print("roi "+i+" "+imstat.area+" "+imstat.mean+" "+imstat.xCentroid+"
            // "+imstat.yCentroid);
            // IJ.log("roi "+String.valueOf(i)+" "+String.valueOf(imstat.area)+"
            // "+String.valueOf(imstat.mean)
            // +" "+String.valueOf(imstat.xCentroid)+" "+String.valueOf(imstat.yCentroid));
            roimeasures[i][0] = imstat.area;
            roimeasures[i][1] = imstat.mean;
            roimeasures[i][2] = imstat.xCentroid;
            roimeasures[i][3] = imstat.yCentroid;
        }
        return roimeasures;
    }

    // direction from arg1 to arg2
    // output is [meauresroinum][minindex,minimaldistancel,dx,dy]
    // measures=[roinum][area,mean,centroidx,centroidy]
    static double[][] getMinDist(double[][] measurespre_, double[][] measures_) {
        int i;
        int j;
        double distancescalar = 0;
        double minval = 0;
        int minindex = 0;
        double dx = 0;
        double dy = 0;
        double mindx = 0;
        double mindy = 0;
        IJ.log("measurespre_.length " + String.valueOf(measurespre_.length));
        double[][] returnval = new double[measurespre_.length][4];
        for (i = 0; i < measurespre_.length; i++) {
            for (j = 0; j < measures_.length; j++) {
                dx = measures_[j][2] - measurespre_[i][2];
                dy = measures_[j][3] - measurespre_[i][3];
                distancescalar = Math.sqrt(dx * dx + dy * dy);
                if (j != 0) {
                    if (minval > distancescalar) {
                        minval = distancescalar;
                        minindex = j;
                        mindx = dx;
                        mindy = dy;
                    }
                } else {
                    minval = distancescalar;
                    minindex = 0;
                }
            }
            returnval[i][0] = minindex;
            returnval[i][1] = minval;
            returnval[i][2] = mindx;
            returnval[i][3] = mindy;
        }
        return returnval;
    }

    // arg is measures, or roiorder is also ok
    // return [roi#][order by distance from target, distance, dx, dy]
    // roi# is orederd by getObjmeasures, which means top left most is fisrt roi
    static double[][] getRoiOrder(int targetroinum, double[][] measures_) {
        double[] targetcoordinate = new double[2];
        double dx = 0;
        double dy = 0;
        double[][] returnval = new double[measures_.length][4];
        targetcoordinate[0] = measures_[targetroinum][2];// x
        targetcoordinate[1] = measures_[targetroinum][3];// y
        IJ.log("target num " + String.valueOf(targetroinum) + " x " + String.valueOf(targetcoordinate[0]) + " y "
                + String.valueOf(targetcoordinate[1]));
        double[] distancescaler = new double[measures_.length];
        for (int i = 0; i < measures_.length; i++) {
            dx = targetcoordinate[0] - measures_[i][2];
            dy = targetcoordinate[1] - measures_[i][3];
            returnval[i][2] = dx;
            returnval[i][3] = dy;
            distancescaler[i] = Math.sqrt(dx * dx + dy * dy);
            returnval[i][1] = distancescaler[i];
            IJ.log("distance " + String.valueOf(distancescaler[i]) + " roi " + String.valueOf(i));
        }
        double[] copydistance = distancescaler.clone();
        Arrays.sort(copydistance);// is there any method to get sorted index?
        for (int i = 0; i < distancescaler.length; i++) {
            for (int j = 0; j < copydistance.length; j++) {
                if (distancescaler[i] == copydistance[j]) {
                    returnval[i][0] = (double) j;
                }
            }
        }

        return returnval;

    }

    // roiorder [roi#][order by distance from target, distance, dx, dy]
    // returen same format of roiorder
    // slice is first image is 1 not 0
    static double[][] checkDirDis(int slice, double[][] roiorder, double[][] measures_) {
        double[][] returnvalue;// new roi order, if could found, or same value as input, if cannot found better
                               // pattern.
        boolean trackstatus = false;
        int adjacentroi = 0;
        for (int j = 0; j < roiorder.length; j++) {
            if ((int) (roiorder[j][0]) == 1)// ==1 means the closest roi. 0 is target.
            {
                adjacentroi = j;
            }
        }
        double theta = Math.atan2(roiorder[adjacentroi][2], roiorder[adjacentroi][3]);// 2 x, 3y, ???? this should be
                                                                                      // mistake? atan2(Y,X) not (X,Y)
        IJ.log("theta " + String.valueOf(theta / Math.PI * 180));
        if (pretheta < 10)// pretheta<10 means there is slice having more than 2 rois and processed before
                          // than this slice
        {
            // minanglecos
            double deltaanglecos = Math.cos(pretheta - theta);
            double deltadistanceratio = Math.abs(predistance - roiorder[adjacentroi][1]) / predistance;
            IJ.log("pretheta degree " + String.valueOf(pretheta / Math.PI * 180));
            IJ.log("theta degree " + String.valueOf(theta / Math.PI * 180));
            IJ.log("deltaanglecos " + String.valueOf(deltaanglecos) + " deltadistanceratio "
                    + String.valueOf(deltadistanceratio));
            IJ.log("minanglecos " + String.valueOf(minanglecos));
            IJ.log("predistance " + String.valueOf(predistance));
            if (deltaanglecos < minanglecos || deltadistanceratio > mindistancechange)// 1st time
            {
                if (deltaanglecos < minanglecos) {
                    IJ.log("wrong direction");
                } else {
                    IJ.log("distance change is large");
                }
                // try getroiorder again using next roi as target
                // return [roi#][order by distance from target, distance, dx, dy]
                // static double[][] getRoiOrder(int targetroinum, double[][] measures_)
                double[][] roiorder2 = getRoiOrder(adjacentroi, measures_);
                int adjacentroi2 = 0;
                for (int j = 0; j < roiorder2.length; j++) {
                    if ((int) (roiorder2[j][0]) == 1) {
                        adjacentroi2 = j;
                    }
                }
                theta = Math.atan2(roiorder2[adjacentroi2][2], roiorder2[adjacentroi2][3]);
                deltaanglecos = Math.cos(pretheta - theta);
                deltadistanceratio = Math.abs(predistance - roiorder2[adjacentroi2][1]) / predistance;
                IJ.log("pretheta degree " + String.valueOf(pretheta / Math.PI * 180));
                IJ.log("theta degree " + String.valueOf(theta / Math.PI * 180));
                IJ.log("predistance " + String.valueOf(predistance));
                IJ.log("2nd trial angle cos " + String.valueOf(deltaanglecos) + " deltadistanceratio "
                        + String.valueOf(deltadistanceratio));

                if (deltaanglecos < minanglecos || deltadistanceratio > mindistancechange)// 2nd
                {
                    // try onemore time again. this time initail target and 2nd next roi as
                    // direction
                    int adjacentroi3 = 0;
                    for (int j = 0; j < roiorder.length; j++) {
                        if ((int) (roiorder[j][0]) == 2)// here is 2nd next closser roi.
                        {
                            adjacentroi3 = j;
                        }
                    }
                    theta = Math.atan2(roiorder[adjacentroi3][2], roiorder[adjacentroi3][3]);
                    deltaanglecos = Math.cos(pretheta - theta);
                    deltadistanceratio = Math.abs(predistance - roiorder[adjacentroi3][1]) / predistance;
                    IJ.log("theta degree " + String.valueOf(theta / Math.PI * 180));
                    IJ.log("3rd trial angle cos " + String.valueOf(deltaanglecos) + " deltadistanceratio "
                            + String.valueOf(deltadistanceratio));
                    if (deltaanglecos < minanglecos || deltadistanceratio > mindistancechange)// 3rd if still strange..
                                                                                              // give up this slice
                    {
                        IJ.log("give up. see next slice");
                        trackstatus = false;
                        roiorder = new double[][] { { -1 } };// return negative value because failed
                    } else// 3rd
                    {
                        IJ.log("ok at 3rd");
                        predistance = roiorder[adjacentroi][1];
                        pretheta = theta;
                        trackstatus = true;
                    } // if(deltaanglecos<minanglecos || deltadistanceratio>mindistancechange) 3rd
                      // time end
                      // targethistory[i-1][0]=;//don't need to change target in this case
                } else// 2nd
                {
                    IJ.log("ok at 2nd");
                    // targethistory[slice-1][0]=adjacentroi;//change the target// This should done
                    // at calling code.
                    predistance = roiorder2[adjacentroi2][1];
                    pretheta = theta;
                    roiorder = roiorder2;
                    trackstatus = true;
                } //// if(deltaanglecos<minanglecos || deltadistanceratio>mindistancechange) else
                  //// 2nd time end
            } else {
                predistance = roiorder[adjacentroi][1];
                pretheta = theta;
                trackstatus = true;
            } // if(deltaanglecos<minanglecos || deltadistanceratio>mindistancechange) else
              // 1st time end
        } else if (pretheta == 10.0)// if this is the first slice having 2 or more rois.
        {
            predistance = roiorder[adjacentroi][1];
            pretheta = theta;
        } // if(pretheta<10) else if (pretheta==10.0) end

        returnvalue = roiorder;
        return returnvalue;
    }

    void drawRoiOrder(int slice, double[][] roiorder, double[][] measures_, boolean trackstatus) {
        ImageProcessor drawip = binaryimgstack.getProcessor(slice);
        // Rectangle r = roi.getBounds();
        for (int j = 0; j < roiorder.length; j++) {
            String order = String.valueOf((int) roiorder[j][0]);
            IJ.log(order + " slice " + String.valueOf(slice));
            drawip.moveTo((int) measures_[j][2], (int) measures_[j][3]);
            if (trackstatus == true) {
                drawip.setValue(200);
            } else {
                drawip.setValue(100);
            }
            drawip.drawString(order);
        }
    }

    // Save x and y position to .txt file.
    void outputData(double[] xposarray, double[] yposarray) {
        String strforsave;
        String header = "x,y";
        String BR = System.getProperty("line.separator");
        strforsave = header + BR;
        // actual data
        String aslicestring = "";
        int frame_ = xposarray.length;
        for (int i = 0; i < frame_; i++) {
            aslicestring = String.valueOf(xposarray[i]) + "," + String.valueOf(yposarray[i]);
            strforsave = strforsave + aslicestring + BR;
        }

        // String defdir=IJ.getDirectory("image");
        // String defdir=IJ.getDirectory("");
        // String shorttitle=imp.getShortTitle();
        // IJ.saveString(strforsave,defdir+shorttitle+"pos.txt");
        // show dialog
        IJ.saveString(strforsave, "");
        IJ.log("Output is saved in; ");
    }

    // for non-thresholding method
    // this returns x y of centor of mass backgournd subtracted
    static double[] getCenterofMass(ImagePlus imp, ImageProcessor ip, Roi roi, int x, int y) {
        ImagePlus imp_ = imp;
        ImageProcessor ip_ = ip;
        Roi roi_ = roi;
        ImageStatistics imstat_ = imp_.getStatistics(2);
        int backgroundvalue = (int) imstat_.mean;
        ImageProcessor ip2 = ip_.duplicate();
        // ip2.add(-backgroundvalue);
        ip2.add(-backgroundvalue * 1.5);
        ImagePlus imp2 = new ImagePlus("subtracted", ip2);
        roi_.setLocation(x, y);
        imp2.setRoi(roi_);

        // median filter ver 7 test
        RankFilters rf = new RankFilters();
        // rf.setup("median",imp);
        rf.rank(ip2, 0.0, 4);// median 4 periodic black white noize cause miss thresholding, so eliminate
                             // those noize
        ImageStatistics imstat2 = imp2.getStatistics(64 + 32);
        // xcom=imstat2.xCenterOfMass;
        // ycom=imstat2.yCenterOfMass;
        // print("center of mass:"+imstat2.xCenterOfMass+","+imstat2.yCenterOfMass);
        // print("center of roi:"+imstat2.xCentroid+","+imstat2.yCentroid);
        // double xshift=imstat2.xCenterOfMass-imstat2.xCentroid;
        // double yshift=imstat2.yCenterOfMass-imstat2.yCentroid;
        // print("shift:"+xshift+","+yshift);
        // double[] shift={xshift,yshift};
        double[] returnval = { imstat2.xCenterOfMass, imstat2.yCenterOfMass };
        countslice++;
        return (returnval);
    }

    /*---------------------------------------  start process-------------------------------------------------*/

    //////////////////////////////////////////////////////////////////////////////////
    public void startAcq(String arg) {
        // static values are last even after process. need to clear onece have done.
        countslice = 0;
        binaryimgstack = null;
        preroiarraylist = null;
        predistance = 0.0;
        pretheta = 10.0;// normal radian must beteween =-pi. so this value could use to check if there
                        // is pretheta.
        threshsum = 0;
        threahaverage = 0;

        // CMMCore mmc_=mmcmf.getMMCore ();
        if (mmc_.isSequenceRunning()) {
            try {
                IJ.log("tried stopSequenceAcquisition");
                mmc_.stopSequenceAcquisition(); // need to be catched
            } catch (java.lang.Exception e) // the arg must be differ by method
            {
                IJ.log("failed stopSequenceAcquisition");
            }
        }
        long width2 = mmc_.getImageWidth();
        IJ.log(String.valueOf(width2));

        String stagelabel = mmc_.getXYStageDevice();
        String zstagelabel = mmc_.getFocusDevice();
        IJ.log(stagelabel);
        IJ.log(zstagelabel);
        double zpos = 0;
        try {
            zpos = mmc_.getPosition(zstagelabel);
        } catch (java.lang.Exception e) {

        }
        IJ.log("zpos " + String.valueOf(zpos));
        double xpos = 0;
        try {
            xpos = mmc_.getXPosition(stagelabel);
        } catch (java.lang.Exception e) {

        }
        IJ.log("xpos " + String.valueOf(xpos));
        String PORT = getStagePortLabel(stagelabel);
        // actually not null, "" will be returned.
        if (PORT != null) {
            IJ.log("PORT" + PORT);
        } else {
            IJ.log("stage is not connected");
            return;
        }

        // if(true)return;
        // ImagePlus imp;
        ImageProcessor ip;
        // get info. from the live window
        // why imp need to getCurrentImage? it should be given by RealTimeTracker?
        /*
         * imp=WindowManager.getCurrentImage(); ImageWindow iw = imp.getWindow();
         * IJ.log("check error"); ic= iw.getCanvas(); ic.addMouseListener(this);
         */
        // java.awt.Component.addMouseListener(ic);
        ip = imp.getProcessor();
        Roi roi = imp.getRoi();
        int width = imp.getWidth();
        int height = imp.getHeight();
        int roiwidth = width / 2;
        int roiheight = height;
        Roi leftroi;
        leftroi = new Roi(0, 0, width / 2, height);
        Roi rightroi;
        rightroi = new Roi(width / 2, 0, width / 2, height);
        if (roi != null && !tpf.right.getState()) {
            IJ.log(roi.getClass().getName());
            Rectangle r = roi.getBounds();
            roiwidth = r.width;
            roiheight = r.height;
        } else {
            if (!tpf.right.getState()) {
                // set roi at left half.
                IJ.log("no roi! set roi = left half");
                roi = (Roi) leftroi.clone();
            } else {
                // set roi at right half.
                IJ.log("no roi! set roi = right half");
                roi = (Roi) rightroi.clone();

            }
        }
        ImageStatistics imstat = imp.getStatistics(16);
        // imp.close();
        // imp.setRoi(roi);
        // ImageProcessor ipleft=ip.crop();
        // ImagePlus impleft=new ImagePlus("l",ipleft);
        // double[][] measures=getObjmeasures(impleft, ipleft);
        /*
         * ypos=mmc.getYPosition(stagelabel); print(ypos);
         */
        // If there is stack, process it without stage control.
        if (imp.getImageStackSize() > 1) {
            // get start time
            Date d1 = new java.util.Date();
            IJ.log("start at" + d1.getTime());
            ImageStack imgstack = imp.getStack();
            int slicenumber = imp.getNSlices();
            binaryimgstack = new ImageStack(width / 4, height / 2, slicenumber);// out put to check how look like
            // if width is not multiple of 4, cause error
            measurespre = new double[][] { { 0 }, { 0 } };
            // measures;
            double[][] mindist;
            // targethistory[slicenumber][roi index, x, y]
            targethistory = new double[slicenumber][3];
            double[] shift = new double[2];
            double roiorder[][] = null;
            for (int i = 0; i < slicenumber; i++) {
                imp.setSlice(i + 1);
                imp.setRoi(leftroi);
                ImageProcessor ipleft = ip.crop();
                ImagePlus impleft = new ImagePlus("l", ipleft);
                String thresholdmethod = tpf.thresholdmethod.getSelectedItem();
                measures = getObjmeasures(impleft, ipleft, true, thresholdmethod);
                if (measures.length == 0)// if lost any cells
                {
                    IJ.log("target lost");
                    // test to continue imaging
                    measures = measurespre;
                    /*
                     * ImagePlus imp3=new ImagePlus("binarystack",binaryimgstack); imp3.show();
                     * return;
                     */
                }
                if (i != 0) {
                    mindist = getMinDist(measurespre, measures);
                    int j;
                    int previoustarget = (int) targethistory[i - 1][0];
                    int newtarget = (int) mindist[previoustarget][0];
                    targethistory[i][0] = newtarget;
                    targethistory[i][1] = measures[newtarget][2];
                    targethistory[i][2] = measures[newtarget][3];
                    shift[0] = mindist[previoustarget][2];
                    shift[1] = mindist[previoustarget][3];
                    IJ.log(shift[0] + "," + shift[1]);
                    // here put stage control code.
                } else {
                    // mock meaures to detect most centorized roi for resized scan, divide 4
                    double[][] mock = { { 0, 0, ipleft.getWidth() / 4, ipleft.getHeight() / 4 } };
                    double[][] initialtarget = getMinDist(mock, measures);
                    int target = (int) initialtarget[0][0];
                    IJ.log("target #" + String.valueOf(target) + " roi at " + String.valueOf(measures[target][2]) + ","
                            + String.valueOf(measures[target][3]));
                    targethistory[0][0] = target;
                    targethistory[0][1] = measures[target][2];
                    targethistory[0][2] = measures[target][3];
                }

                // return [roi#][order by distance from target, distance, dx, dy]
                // static double[][] getRoiOrder(int targetroinum, double[][] measures)
                IJ.log("before getRoiOrder " + String.valueOf(targethistory[i][0]));
                roiorder = getRoiOrder((int) targethistory[i][0], measures);
                // check target is collect or not by direcion/distance towards next roi. if
                // there are more than 2 rois.
                if (measures.length >= 2) {
                    // roiorder [roi#][order by distance from target, distance, dx, dy]
                    // returen same format.
                    // static double[][] checkDirDis(int slice, double[][] roiorder,double[][]
                    // measures)
                    double[][] checkedroiorder = checkDirDis(i - 1, roiorder, measures);
                    boolean trackstatus = false;
                    double[][] finalroiorder = new double[][] { { 0 } };
                    if ((int) checkedroiorder[0][0] == -1)// negative means failed
                    {
                        trackstatus = false;
                        finalroiorder = roiorder;
                    } else {
                        trackstatus = true;
                        finalroiorder = checkedroiorder;
                        int newtarget = 0;
                        for (int j = 0; j < finalroiorder.length; j++) {
                            if ((int) (finalroiorder[j][0]) == 0)// here is target
                            {
                                newtarget = j;
                            }
                        }
                        // targethistory[slicenumber][roi index, x, y]
                        targethistory[i][0] = newtarget;
                        targethistory[i][1] = measures[newtarget][2];
                        targethistory[i][2] = measures[newtarget][3];

                    }
                    IJ.log("after getRoiOrder " + String.valueOf(targethistory[i][0]));
                    // void drawRoiOrder(int slice, double[][] roiorder, double[][] measures,
                    // boolean trackstatus)
                    drawRoiOrder(i - 1, finalroiorder, measures, trackstatus);
                } // if(measures.length>2) end
                  // use targethistory[i][0] to calculate distance from centor.
                measurespre = measures;
            } // for(int i=1;i<=slicenumber;i++) end
            Date d2 = new java.util.Date();
            IJ.log("end at" + d2.getTime());
            IJ.log(String.valueOf((d2.getTime() - d1.getTime()) / 1000.0) + " sec");
            ImagePlus imp3 = new ImagePlus("binarystack", binaryimgstack);
            imp3.show();
            // imp3.setDisplayRange(imstat.min,imstat.max);
            return;
        } // if(imp.getImageStackSize()>1) end
          // image acquisition
        else {
            File outfile = null;
            OutputStream outstream = null;
            FileInfo fi = imp.getFileInfo();
            TiffEncoder tiffencoder = new TiffEncoder(fi);
            // start acquisition
            // int frame=1200;
            double[] xposarray = new double[frame];
            double[] yposarray = new double[frame];

            // ZStage zs=new ZStage();
            // int zpos=zs.GetPositionUm();
            // 50msec wait doesn't work?
            try {
                mmc_.startSequenceAcquisition(frame, 0, false);
            } catch (java.lang.Exception e) {
            }
            // ip=new ShortProcessor(width,height);//Hamcam 16bit short
            // imp=new ImagePlus("stream",ip);
            // ImageStack imgstk=new ImageStack(width, height, frame);
            // binaryimgstack=new ImageStack(width/4, height/2, frame);
            imp.setDisplayRange(imstat.min, imstat.max);
            imp.show();
            Date d1 = new java.util.Date();
            IJ.log("start at" + d1.getTime());
            measurespre = new double[][] { { 0 }, { 0 } };
            // measures;
            double[][] mindist;
            // targethistory[slicenumber][roi index, x, y]
            targethistory = new double[frame][3];
            double[] shift = new double[2];
            double[] stagepos = new double[2];
            double roiorder[][] = null;
            double[] distancefromcenter = new double[2];
            java.lang.Object img = new short[width * height];
            double xv = 0;
            double yv = 0;
            String ans = "";
            int i = 0;
            int skip = Integer.parseInt(tpf.skiptext.getText());// if it 2, keep 1 out of 2 image
            // int interval= Integer.parseInt(tpf.intervaltext.getText());//msec. if it 0,
            // ignore.
            int skipcount = 0;
            double[] centorofmass = new double[2];
            // while imaging...
            long nanotimecurrent = System.nanoTime();
            long nanotimepre = 0;
            while (mmc_.isSequenceRunning()) {
                if (mmc_.getRemainingImageCount() > 0) {
                    try {
                        img = mmc_.popNextImage();// img is byte array,[B, or Short array [S
                    } catch (java.lang.Exception e) {
                    }
                    skipcount = (skipcount + 1) % skip;
                    // nanotimepre= nanotimecurrent;
                    nanotimecurrent = System.nanoTime();
                    // IJ.log("time diff "+String.valueOf((nanotimepre-nanotimecurrent)/1000000));
                    // if(interval==0 || (nanotimecurrent -nanotimepre)> interval*1000000)//else
                    // throw the data
                    if (skipcount == 0)// else throw the data
                    {
                        nanotimepre = nanotimecurrent;
                        ip.setPixels(img);
                        imp.setProcessor(imp.getTitle(), ip);
                        imp.updateImage();
                        // I don't get why there are two getXYpos methods in both mmc and gui.
                        try// it seems some case, getting xpos fails. try serial port. not resolved by
                           // serial port.
                        {

                            xposarray[i] = mmc_.getXPosition(stagelabel);
                            yposarray[i] = mmc_.getYPosition(stagelabel);

                            /*
                             * mmc_.setSerialPortCommand(PORT,"WHERE X Y","\r");
                             * ans=mmc_.getSerialPortAnswer(PORT, "\r\n"); String[]
                             * ansvalues=ans.split(" "); xposarray[i]=Double.parseDouble(ansvalues[1]);
                             * yposarray[i]=Double.parseDouble(ansvalues[2]); IJ.log("serialport x "+
                             * ansvalues[1]+" y "+ ansvalues[2]);
                             */
                            // IJ.log("mmc get x "+ String.valueOf(xposarray[i])+" y "+
                            // String.valueOf(yposarray[i]));
                            zpos = mmc_.getPosition(zstagelabel);
                            // zpos=mme_.
                        } catch (java.lang.Exception e) {
                        }
                        if (!ready)// if this is runnning as real imaging process, save images.
                        {
                            // out put each frame
                            outfile = new File(dirforsave + "/" + String.valueOf(i) + ".tif");
                            try {
                                outfile.createNewFile();
                            } catch (java.lang.Exception e) {
                            }
                            try {
                                outstream = new FileOutputStream(outfile);
                            } catch (java.lang.Exception e) {
                            }
                            fi = imp.getFileInfo();
                            // fi.info="xpos1.0913341ypos0.13325";//here put stage position data.
                            fi.info = "xpos=" + String.valueOf(xposarray[i]) + ",ypos=" + String.valueOf(yposarray[i])
                                    + ",zpos=" + String.valueOf(zpos);
                            tiffencoder = new TiffEncoder(fi);
                            try {
                                tiffencoder.write(outstream);
                            } catch (java.lang.Exception e) {
                            }
                            try {
                                outstream.close();
                            } catch (java.lang.Exception e) {
                            }
                        } // if(!ready) end

                        /*
                         * gui version. need to look for gui api? stagepos=gui.getXYStagePosition();
                         * xposarray[i]=stagepos.getX();//.x cause error? yposarray[i]=stagepos.getY();
                         * print("stagepos:"+stagepos.x+","+stagepos.y+", at "+i);
                         */

                        // imgstk.setPixels(img,i+1);
                        if (!tpf.manualtracking.getState()) {
                            if (!tpf.CoM.getState() && !tpf.FULL.getState())// for normal thresholding method.
                            {

                                imp.setRoi(roi);
                                ImageProcessor ip_current = imp.getProcessor();
                                ImageProcessor ipleft = ip_current.crop();
                                ImagePlus impleft = new ImagePlus("l", ipleft);
                                // measures=getObjmeasures(impleft, ipleft, false);//true saves binaly image
                                String thresholdmethod = tpf.thresholdmethod.getSelectedItem();
                                measures = getObjmeasures(impleft, ipleft, false, thresholdmethod);
                                if (measures.length == 0)// if lost any cells
                                {
                                    IJ.log("target lost");
                                    // test to continue imaging
                                    measures = measurespre;
                                    /*
                                     * try { mmc_.stopSequenceAcquisition(); } catch(java.lang.Exception e){}
                                     * if(PORT!="")//demo stage cant do this { try {
                                     * mmc_.setSerialPortCommand(PORT,"VECTOR X=0 Y=0","\r"); }
                                     * catch(java.lang.Exception e){} try { ans=mmc_.getSerialPortAnswer(PORT,
                                     * "\r\n"); } catch(java.lang.Exception e){} //print(ans); try {
                                     * mmc_.setSerialPortCommand(PORT,"VECTOR X? Y?","\r"); }
                                     * catch(java.lang.Exception e){} try { ans=mmc_.getSerialPortAnswer(PORT,
                                     * "\r\n"); } catch(java.lang.Exception e){} } //outputData(xposarray,
                                     * yposarray); return;
                                     */
                                }
                                if (i != 0)// after second image
                                {
                                    mindist = getMinDist(measurespre, measures);
                                    int j;
                                    int previoustarget = (int) targethistory[i - 1][0];
                                    int newtarget = (int) mindist[previoustarget][0];
                                    targethistory[i][0] = newtarget;
                                    targethistory[i][1] = measures[newtarget][2];
                                    targethistory[i][2] = measures[newtarget][3];
                                    shift[0] = mindist[previoustarget][2];
                                    shift[1] = mindist[previoustarget][3];
                                    IJ.log(shift[0] + "," + shift[1]);
                                    // multiply 2 because resized 1/2
                                    // print("target #"+newtarget+" roi at
                                    // "+measures[newtarget][2]+","+measures[newtarget][3]);
                                    distancefromcenter[0] = width / 4 - measures[newtarget][2] * 2;
                                    distancefromcenter[1] = height / 2 - measures[newtarget][3] * 2;
                                    // for non resised version
                                    // distancefromcenter[0]=width/4-measures[newtarget][2];
                                    // distancefromcenter[1]=height/2-measures[newtarget][3];

                                } // after second image end
                                else// first image
                                {
                                    // mock meaures to detect most centorized roi for resized scan, divide 4
                                    double[][] mock = { { 0, 0, ipleft.getWidth() / 4, ipleft.getHeight() / 4 } };
                                    double[][] initialtarget = getMinDist(mock, measures);
                                    // for non resised version
                                    // double[][] mock= {{0,0,ipleft.getWidth()/2,ipleft.getHeight()/2}};
                                    // double[][] initialtarget=getMinDist(mock, measures);
                                    int target = (int) initialtarget[0][0];
                                    IJ.log("target #" + String.valueOf(target) + " roi at "
                                            + String.valueOf(measures[target][2]) + ","
                                            + String.valueOf(measures[target][3]));
                                    targethistory[0][0] = target;
                                    targethistory[0][1] = measures[target][2];
                                    targethistory[0][2] = measures[target][3];
                                    // multiply 2 because resized 1/2
                                    distancefromcenter[0] = width / 4 - measures[target][2] * 2;
                                    distancefromcenter[1] = height / 2 - measures[target][3] * 2;
                                } // first image end

                                // return [roi#][order by distance from target, distance, dx, dy]
                                // static double[][] getRoiOrder(int targetroinum, double[][] measures)
                                if (!tpf.closest.getState()) {
                                    IJ.log("before getRoiOrder " + String.valueOf(targethistory[i][0]));
                                    roiorder = getRoiOrder((int) targethistory[i][0], measures);
                                    // check target is collect or not by direcion/distance towards next roi. if
                                    // there are more than 2 rois.
                                    if (measures.length >= 2) {
                                        // roiorder [roi#][order by distance from target, distance, dx, dy]
                                        // returen same format.
                                        // static double[][] checkDirDis(int slice, double[][] roiorder,double[][]
                                        // measures)
                                        double[][] checkedroiorder = checkDirDis(i + 1, roiorder, measures);
                                        boolean trackstatus = false;
                                        double[][] finalroiorder = new double[][] { { 0 } };
                                        if ((int) checkedroiorder[0][0] == -1)// negative means failed
                                        {
                                            trackstatus = false;
                                            finalroiorder = roiorder;
                                        } else {
                                            trackstatus = true;
                                            finalroiorder = checkedroiorder;
                                            int newtarget = 0;
                                            for (int j = 0; j < finalroiorder.length; j++) {
                                                if ((int) (finalroiorder[j][0]) == 0)// here is target
                                                {
                                                    newtarget = j;
                                                }
                                            }
                                            // targethistory[slicenumber][roi index, x, y]
                                            targethistory[i][0] = newtarget;
                                            targethistory[i][1] = measures[newtarget][2];
                                            targethistory[i][2] = measures[newtarget][3];

                                        }
                                        IJ.log("after getRoiOrder " + String.valueOf(targethistory[i][0]));
                                        // void drawRoiOrder(int slice, double[][] roiorder, double[][] measures,
                                        // boolean trackstatus)
                                        drawRoiOrder(i + 1, finalroiorder, measures, trackstatus);
                                    } // if(!closest.getState()) end

                                    // use targethistory[i][0] to calculate distance from centor.
                                    // multiply 2 because resized 1/2
                                    // print("target #"+newtarget+" roi at
                                    // "+measures[newtarget][2]+","+measures[newtarget][3]);
                                    distancefromcenter[0] = width / 4 - measures[(int) targethistory[i][0]][2] * 2;
                                    distancefromcenter[1] = height / 2 - measures[(int) targethistory[i][0]][3] * 2;
                                } // if(measures.length>2) end
                                  //
                                  // here put stage control code.
                            } else if (tpf.FULL.getState())// full field
                            {
                                // imp.setRoi(roi);
                                roi = new Roi(4, 4, width - 8, height - 8);// trim edge of image since it may have dark
                                                                           // reagion
                                imp.setRoi(roi);
                                ImageProcessor ip_current = imp.getProcessor();
                                if (tpf.BF.getState())// for brightfield
                                {
                                    ip_current.invert();
                                }
                                ImagePlus impinv = new ImagePlus("l", ip_current);
                                // get data and put it into double[] distancefromcenter =new double[2];
                                if (i != 0)// after second image
                                {
                                    // IJ.log("roiwidth "+String.valueOf(roiwidth)+" width/2
                                    // "+String.valueOf(width/2));
                                    // centorofmass=getCenterofMass(impinv, ip_current, roi,0,0);
                                    centorofmass = getCenterofMass(impinv, ip_current, roi, 4, 4);// trim 4 pix?
                                    distancefromcenter[0] = width / 2 - centorofmass[0];
                                    distancefromcenter[1] = height / 2 - centorofmass[1];
                                    // targethistory[slicenumber][roi index, x, y]
                                    targethistory[i][0] = -1;// for center of mass method, the roi index use -1,
                                    targethistory[i][1] = centorofmass[0];
                                    targethistory[i][2] = centorofmass[1];
                                } else// first image
                                {
                                    centorofmass = getCenterofMass(impinv, ip_current, roi, 0, 0);
                                    distancefromcenter[0] = width / 2 - centorofmass[0];
                                    distancefromcenter[1] = height / 2 - centorofmass[1];
                                    // targethistory[slicenumber][roi index, x, y]
                                    targethistory[i][0] = -1;// for center of mass method, the roi index use -1,
                                    targethistory[i][1] = centorofmass[0];
                                    targethistory[i][2] = centorofmass[1];
                                }
                                imp.setRoi(roi);// just for visible.
                                IJ.log("FULL " + String.valueOf(centorofmass[0]) + " "
                                        + String.valueOf(centorofmass[1]));
                                IJ.log("dis cent " + String.valueOf(distancefromcenter[0]) + " "
                                        + String.valueOf(distancefromcenter[1]));

                            }
                            // if(!tpf.CoM.getState()) center of mass end
                            else // for center of mass method
                            {

                                imp.setRoi(leftroi);
                                ImageProcessor ip_current = imp.getProcessor();
                                ImageProcessor ipleft = ip_current.crop();
                                ImagePlus impleft = new ImagePlus("l", ipleft);
                                // get data and put it into double[] distancefromcenter =new double[2];
                                if (i != 0)// after second image
                                {
                                    // IJ.log("roiwidth "+String.valueOf(roiwidth)+" width/2
                                    // "+String.valueOf(width/2));
                                    if (roiwidth == width / 2 && roiheight == height)// usr didn't drow a roi
                                    {
                                        centorofmass = getCenterofMass(impleft, ipleft, roi, 0, 0);// the roi should be
                                                                                                   // left roi
                                    } else {
                                        int roishiftx = (int) (targethistory[i - 1][1] - roiwidth / 2.0);
                                        int roishifty = (int) (targethistory[i - 1][2] - roiheight / 2.0);
                                        centorofmass = getCenterofMass(impleft, ipleft, roi, roishiftx, roishifty);// use
                                                                                                                   // the
                                                                                                                   // previous
                                                                                                                   // roi
                                                                                                                   // pos
                                    }
                                    distancefromcenter[0] = width / 4 - centorofmass[0];
                                    distancefromcenter[1] = height / 2 - centorofmass[1];
                                    // targethistory[slicenumber][roi index, x, y]
                                    targethistory[i][0] = -1;// for center of mass method, the roi index use -1,
                                    targethistory[i][1] = centorofmass[0];
                                    targethistory[i][2] = centorofmass[1];
                                } else// first image
                                {
                                    centorofmass = getCenterofMass(impleft, ipleft, leftroi, 0, 0);// this roi is left
                                                                                                   // roi or usr
                                                                                                   // defined?
                                    distancefromcenter[0] = width / 4 - centorofmass[0];
                                    distancefromcenter[1] = height / 2 - centorofmass[1];
                                    // targethistory[slicenumber][roi index, x, y]
                                    targethistory[i][0] = -1;// for center of mass method, the roi index use -1,
                                    targethistory[i][1] = centorofmass[0];
                                    // targethistory[i][1]=width/4;
                                    targethistory[i][2] = centorofmass[1];
                                    // targethistory[i][2]=height/2;
                                }
                                imp.setRoi(roi);// just for visible.
                                IJ.log("CoM " + String.valueOf(centorofmass[0]) + " "
                                        + String.valueOf(centorofmass[1]));
                                IJ.log("dis cent " + String.valueOf(distancefromcenter[0]) + " "
                                        + String.valueOf(distancefromcenter[1]));
                            }
                            double distancescalar = Math.sqrt((distancefromcenter[0]) * (distancefromcenter[0])
                                    + (distancefromcenter[1]) * (distancefromcenter[1]));
                            if (distancescalar > LIMIT) {
                                // print("distancefromcenter "+distancefromcenter[0]+","+distancefromcenter[1]+"
                                // distancescalar"+distancescalar);
                                // 100 msec 0.0018?
                                xv = Math.round(-distancefromcenter[0] * 0.0018 * 1000.0) / 1000.0;
                                yv = Math.round(distancefromcenter[1] * 0.0018 * 1000.0) / 1000.0;
                                // 10x obj, need to be increased...may be x4?
                                int accelint = 1;
                                if (tpf.acceleration.getSelectedItem() == "1x") {
                                    accelint = 1;
                                } else if (tpf.acceleration.getSelectedItem() == "2x") {
                                    accelint = 2;
                                } else if (tpf.acceleration.getSelectedItem() == "4x") {
                                    accelint = 4;
                                } else if (tpf.acceleration.getSelectedItem() == "5x") {
                                    accelint = 5;
                                } else if (tpf.acceleration.getSelectedItem() == "6x") {
                                    accelint = 6;
                                }
                                xv = xv * accelint;
                                yv = yv * accelint;
                                IJ.log("xv;" + String.valueOf(xv) + " yv;" + String.valueOf(yv));
                                if (PORT != "")// demo stage cant do this
                                {
                                    try {
                                        mmc_.setSerialPortCommand(PORT,
                                                "VECTOR X=" + String.valueOf(xv) + " Y=" + String.valueOf(yv), "\r");
                                    } catch (java.lang.Exception e) {
                                    }
                                    try {
                                        ans = mmc_.getSerialPortAnswer(PORT, "\r\n");
                                    } catch (java.lang.Exception e) {
                                    }
                                } // if(PORT!="")//demo stage cant do this end
                                IJ.log("ans " + ans + " moving");
                            } else {
                                if (PORT != "")// demo stage cant do this
                                {
                                    try {
                                        mmc_.setSerialPortCommand(PORT, "VECTOR X=0 Y=0", "\r");
                                    } catch (java.lang.Exception e) {
                                    }
                                    // follwoing may not need execpt for checking the control
                                    // NO NEED By unknown reason, comment out following cause failure of getting
                                    // stage position.

                                    try {
                                        ans = mmc_.getSerialPortAnswer(PORT, "\r\n");
                                    } catch (java.lang.Exception e) {
                                    }
                                    // print(ans);
                                    try {
                                        mmc_.setSerialPortCommand(PORT, "VECTOR X? Y?", "\r");
                                    } catch (java.lang.Exception e) {
                                    }
                                    try {
                                        ans = mmc_.getSerialPortAnswer(PORT, "\r\n");
                                    } catch (java.lang.Exception e) {
                                    }

                                } // if(PORT!="")//demo stage cant do this end
                                  // print(ans);
                            } // if(distancescalar>LIMIT) else end
                        } // if manual tracking
                        measurespre = measures;
                        IJ.log("x " + String.valueOf(xposarray[i]) + " y " + String.valueOf(yposarray[i]) + " at "
                                + String.valueOf(i));
                        i++;
                    } // if(skipcount==0)end
                } // if (mmc.getRemainingImageCount() > 0) end
            } // while (mmc.isSequenceRunning()) end

            try {
                mmc_.setSerialPortCommand(PORT, "VECTOR X=0 Y=0", "\r");
            } catch (java.lang.Exception e) {
            }
            try {
                ans = mmc_.getSerialPortAnswer(PORT, "\r\n");
            } catch (java.lang.Exception e) {
            }
            // print(ans);
            try {
                mmc_.setSerialPortCommand(PORT, "VECTOR X? Y?", "\r");
            } catch (java.lang.Exception e) {
            }
            try {
                ans = mmc_.getSerialPortAnswer(PORT, "\r\n");
            } catch (java.lang.Exception e) {
            }

            Date d2 = new java.util.Date();
            IJ.log("acquistion end at" + d2.getTime());
            IJ.log(String.valueOf((d2.getTime() - d1.getTime()) / 1000.0) + " sec");
            if (!ready) {
                if (tpf.textpos.getState()) {
                    outputData(xposarray, yposarray);
                }
            }
            // ImagePlus imp3=new ImagePlus("binarystack",binaryimgstack);
            // imp3.show();

        } // image acquisition case end
          // ic.removeMouseListener(this);
    }// public void void startAcq(String arg) { end
}// class TrackingThread9 extends Thread { end

/// for stimulation using arduino DA converter
class Signalsender implements Runnable {
    // ControlPannel cp;
    TrackStim_03 ts;
    int channel;
    int strength;
    int sendingdata;

    int delaytime;
    int dulation;
    int[] changetimepoints;
    int[] changevalues;
    // ScheduledExecutorService ses;
    // ScheduledFuture future = null;

    // Signalsender(ControlPannel cp_)
    Signalsender(TrackStim_03 ts_) {
        // cp=cp_;
        ts = ts_;
    }

    void setChannel(int channel_) {
        channel = channel_;
    }

    // use this
    void setSignalStrength(int strength_) {
        strength = strength_;
    }

    // not using now
    // mili sec and 0-255
    void setOrder(int delaytime_, int strength_, int dulation_) {
        delaytime = delaytime_;
        strength = strength_;
        dulation = dulation_;
        // ScheduledExecutorService
        changetimepoints = new int[] { delaytime, delaytime_ + dulation };
        changevalues = new int[] { strength, 0 };
        // ses = Executors.newSingleThreadScheduledExecutor();
        // future=ses.schedule(Runnable command, long delay, TimeUnit unit);
        // future=ses.scheduleWithFixedDelay(this,0, 1000/processrate,
        // TimeUnit.MICROSECONDS);
    }

    public void run() {
        // System.out.println(String.valueOf(System.nanoTime()/1000000) +" "+
        // String.valueOf(strength));
        IJ.log(String.valueOf(System.nanoTime() / 1000000) + " " + String.valueOf(strength));
        // cp.st.port.write(strength);
        // testing sending vale
        sendingdata = channel << 7 | strength;
        mmcorej.CharVector sendingchrvec = new mmcorej.CharVector();
        // sendingchrvec.add((char) i);
        sendingchrvec.add((char) sendingdata);
        try {
            // ans=mmc_.getSerialPortAnswer(adportsname, "\r\n");
            ts.mmc_.writeToSerialPort(ts.adportsname, sendingchrvec);
            // ans=mmc_.getSerialPortAnswer(adportsname, String.valueOf(0xff));
        } catch (java.lang.Exception e) {
        }
        /*
         * int count=0; long starttime = System.nanoTime();
         * while(count<changevalues.length) {
         * if((starttime-System.nanoTime())/1000000>changetimepoints[count]) {
         * System.out.println(String.valueOf(changetimepoints[count]));
         * cp.st.port.write(changevalues[count]); count++; }
         *
         * }
         */
    }

}