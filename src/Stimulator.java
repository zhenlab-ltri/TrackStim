import ij.IJ;

import mmcorej.CharVector;
import mmcorej.CMMCore;
import mmcorej.Configuration;
import mmcorej.PropertySetting;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// for stimulation using arduino DA converter
class SignalSender implements Runnable {
    CMMCore mmc;
    int channel;
    int strength;
    int sendingdata;
    String stimulatorPort;

    int delaytime;
    int dulation;
    int[] changetimepoints;
    int[] changevalues;

    SignalSender(CMMCore cmmcore, String stimulatorPort) {
        mmc = cmmcore;
    }

    void setChannel(int channel_) {
        channel = channel_;
    }

    // use this
    void setSignalStrength(int strength_) {
        strength = strength_;
    }

    public void run() {
        IJ.log("SignalSender: system time is " + String.valueOf(System.nanoTime() / 1000000));
        IJ.log("SignalSender: strength is " + String.valueOf(strength));

        // testing sending vale
        sendingdata = channel << 7 | strength;
        CharVector sendingchrvec = new CharVector();
        sendingchrvec.add((char) sendingdata);

        try {
            mmc.writeToSerialPort(stimulatorPort, sendingchrvec);
        } catch (java.lang.Exception e) {
            IJ.log("SignalSender: error trying to write data " + String.valueOf(sendingdata) + " to the serial port " + stimulatorPort);
            IJ.log(e.getMessage());
        }
    }
}

class Stimulator {
    CMMCore mmc;
    String stimulatorPort;
    boolean initialized = false;

    static final int STIMULATION_CHANNEL = 0; // the channel to send ths signals to

    Stimulator(CMMCore cmmcore){
        mmc = cmmcore;
        stimulatorPort = "";
    }

    // legacy logic to get the port that the stimulator is connected to
    // returns true if a port is found
    // false otherwise
    public boolean initialize(){
        Configuration conf = mmc.getSystemState();
        long index = conf.size();
        PropertySetting ps = new PropertySetting();
        boolean portFound = false;

        for (int i = 0; i < index; i++) {
            try {
                ps = conf.getSetting(i);
            } catch (java.lang.Exception e) {
                IJ.log("Stimulator.initialize: error getting property settings");
                IJ.log(e.getMessage());
            }


            String propertyName = ps.getPropertyName();
            String propertyValue = ps.getPropertyValue();

            // just testing enviroment having only one port /dev/tty.usbmodem1d11
            // searching for usbmodem is a hack and it should be replaced
            if (propertyName == "Port" && propertyName.indexOf("usbmodem") > 0) {
                IJ.log("Stimulator.initialize: found stimulator port - " + propertyValue);

                stimulatorPort = propertyValue;
                portFound = true;
            }
        }

        initialized = portFound;
        return portFound;
    }


    void runStimulation(boolean useRamp, int preStimulation, int strength, int stimDuration, int stimCycleLength, int stimCycle, int rampBase, int rampStart, int rampEnd) throws java.lang.Exception {
        if(!initialized){
            throw new Exception("could not run stimulation.  the stimulator is not initialized");
        }

        try {
            if (useRamp) {
                int absdiff = Math.abs(rampEnd - rampStart);
                int signoframp = Integer.signum(rampEnd - rampStart);

                for (int i = 0; i < stimCycle; i++) {
                    for (int j = 0; j < absdiff + 1; j++) {
                        setSender(STIMULATION_CHANNEL, preStimulation + i * stimCycleLength + j * (stimDuration / absdiff),
                                rampStart + j * signoframp);
                    }
                    setSender(STIMULATION_CHANNEL, preStimulation + stimDuration + i * stimCycleLength, rampBase);
                }
            } else {
                for (int i = 0; i < stimCycle; i++) {
                    setSender(STIMULATION_CHANNEL, preStimulation + i * stimCycleLength, strength);
                    setSender(STIMULATION_CHANNEL, preStimulation + stimDuration + i * stimCycleLength, rampBase);
                }
            }
        } catch (java.lang.Exception e) {
            IJ.log("Stimulator.prepSignals: error sending signals to channel " + String.valueOf(STIMULATION_CHANNEL));
            IJ.log(e.getMessage());
        }
    }

    // mili sec, and 0-63
    // helper function for prepSignals
    void setSender(int channel, int timepoint, int signalstrength) {
        SignalSender sd = new SignalSender(mmc, stimulatorPort);
        sd.setChannel(channel);
        sd.setSignalStrength(signalstrength);
        ScheduledExecutorService ses;
        ScheduledFuture future = null;
        ses = Executors.newSingleThreadScheduledExecutor();
        future = ses.schedule(sd, timepoint * 1000, TimeUnit.MICROSECONDS);
    }

    void updateStimulatorSignal(int newExposure, int newLength) throws java.lang.Exception {
        if(!initialized){
            throw new Exception("could not run stimulation.  the stimulator is not initialized");
        }

        int newSignalData = 1 << 6 | newLength << 3 | newExposure;

        CharVector chrVector = new CharVector();
        chrVector.add((char) newSignalData);

        try {
            mmc.writeToSerialPort(stimulatorPort, chrVector);
        } catch(java.lang.Exception e){
            IJ.log("Stimulator.updateStimulatorSignal: unable to write new signal data to serial port");
            IJ.log(e.getMessage());
        }
    }
}