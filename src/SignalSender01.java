import ij.IJ;

import mmcorej.CharVector;

// for stimulation using arduino DA converter
class SignalSender01 implements Runnable {
    TrackStim ts;
    int channel;
    int strength;
    int sendingdata;

    int delaytime;
    int dulation;
    int[] changetimepoints;
    int[] changevalues;

    SignalSender01(TrackStim ts_) {
        ts = ts_;
    }

    void setChannel(int channel_) {
        channel = channel_;
    }

    // use this
    void setSignalStrength(int strength_) {
        strength = strength_;
    }

    public void run() {
        IJ.log("SignalSender01: system time is " + String.valueOf(System.nanoTime() / 1000000));
        IJ.log("SignalSender01: strength is " + String.valueOf(strength));

        // testing sending vale
        sendingdata = channel << 7 | strength;
        CharVector sendingchrvec = new CharVector();
        sendingchrvec.add((char) sendingdata);

        try {
            ts.mmc_.writeToSerialPort(ts.adportsname, sendingchrvec);
        } catch (java.lang.Exception e) {
            IJ.log("SignalSender01: error trying to write data " + String.valueOf(sendingdata) + " to the serial port " + ts.adportsname);
            IJ.log(e.getMessage());
        }
    }
}