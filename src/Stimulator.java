import ij.IJ;

import java.util.ArrayList;

import mmcorej.CharVector;
import mmcorej.CMMCore;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// send signals to the stimulator to turn on/off the LED light
class StimulationTask implements Runnable {
    CMMCore mmc;
    int channel;
    int signal;
    String stimulatorPort;

    StimulationTask(CMMCore cmmcore_, String stimulatorPort_, int channel_, int signal_) {
        mmc = cmmcore_;
        stimulatorPort = stimulatorPort_;
        channel = channel_;
        signal = signal_;
    }

    // send signal data to the stimulator through the serial port
    public void run() {
        int signalData = channel << 7 | signal;
        CharVector signalDataVec = new CharVector();
        signalDataVec.add((char) signalData);

        try {
            mmc.writeToSerialPort(stimulatorPort, signalDataVec);
        } catch (java.lang.Exception e) {
            IJ.log("StimulationTask: error trying to write data " + String.valueOf(signalDataVec) + " to the serial port " + stimulatorPort);
            IJ.log(e.getMessage());
        }
    }
}

class Stimulator {
    CMMCore mmc;
    String stimulatorPort;
    boolean initialized = false;

    static final int STIMULATION_CHANNEL = 0; // the channel to send ths signals to
    static final String STIMULATOR_DEVICE_LABEL = "FreeSerialPort"; // hardcoded device label found in config trackstim-mm1.4.23mac.cfg

    Stimulator(CMMCore cmmcore){
        mmc = cmmcore;
        stimulatorPort = "";
    }

    // find and connect to the LED light stimulator
    // initialize the stimulator by sending an initial signal
    public boolean initialize(){
        boolean portFound = false;

        // see ./documentation/arduino.c line 16-21 for the signal format
        // binary 192 -> 11000000
           // 11 -> set trigger setting
           // 000 -> set lower three bits for tigger cycle
           // 000 -> set lower three bits for trigger length 
        // if we dont set trigger cycle and trigger length to 0,
        // we wont be able to turn the light on and off at the right times
        int initialSignal = (STIMULATION_CHANNEL << 8) | 192;
        CharVector initialSignalData = new CharVector();
        initialSignalData.add((char) initialSignal);


        try {
            stimulatorPort = mmc.getProperty(STIMULATOR_DEVICE_LABEL, "Port");

            // send initial signal to stimulator port
            mmc.writeToSerialPort(stimulatorPort, initialSignalData);
            portFound = true;

            IJ.log("[INFO] stimulator port found");
            IJ.log("[INFO] stimulator is connected at " + stimulatorPort);

        } catch (Exception e){
            IJ.log("error getting stimulator port");
            IJ.log(e.getMessage());
        }

        initialized = portFound;
        return portFound;
    }

    // schedules signals that will be run in the future at specific time points and intervals based on
    // the arguments:
    //    useRamp: whether to ramp up gradually to the full signal strength i.e. go from a weaker signal to a stronger signal
    //    preStimTimeMs: time in ms before any signals are sent
    //    signal: signal to send to the light -- usually 63 and it is rare if it is changed
    //    stimDurationMs: duration that the light is on in ms
    //    stimCycleDurationMs: duration that the light is off + duration that the light is on
    //    numStimCycles: number of cycles 
    //    rampBase: strength applied
    //    rampStart: signal at the start of the interval
    //    rampEnd: signal at the end of the interval
    public ArrayList<ScheduledFuture> scheduleStimulationTasks(
        boolean useRamp, int preStimTimeMs, int signal, 
        int stimDurationMs, int stimCycleDurationMs, int numStimCycles, 
        int rampBase, int rampStart, int rampEnd) throws java.lang.Exception {

        ArrayList<ScheduledFuture> stimulatorTasks = new ArrayList<ScheduledFuture>(); 
        if(!initialized){
            throw new Exception("could not run stimulation.  the stimulator is not initialized");
        }

        try {
            if (useRamp) {
                // incrementally increase light strength
                int rampSignalDelta = Math.abs(rampEnd - rampStart);
                int rampSign = Integer.signum(rampEnd - rampStart);


                for (int i = 0; i < numStimCycles; i++) {

                    // schedule signals with incrementally increasing light strength
                    for (int j = 0; j < rampSignalDelta + 1; j++) {
                        stimulatorTasks.add(scheduleSignal(preStimTimeMs + i * stimCycleDurationMs + j * (stimDurationMs / rampSignalDelta),
                                rampStart + j * rampSign));
                    }

                    // schedule signal to turn off light at end of cycle
                    stimulatorTasks.add(scheduleSignal(preStimTimeMs + stimDurationMs + i * stimCycleDurationMs, rampBase));
                }
            } else {
                // send full light strength right away
                for (int i = 0; i < numStimCycles; i++) {
                    int signalTimePtBegin = preStimTimeMs + i * stimCycleDurationMs;
                    int signalTimePtEnd = signalTimePtBegin + stimDurationMs;

                    // schedule signal to turn on light at beginning cycle
                    stimulatorTasks.add(scheduleSignal(signalTimePtBegin, signal));

                    // schedule signal to turn off light at end of cycle
                    stimulatorTasks.add(scheduleSignal(signalTimePtEnd, rampBase));
                }
            }
        } catch (java.lang.Exception e) {
            IJ.log("Stimulator.prepSignals: error sending signals");
            IJ.log(e.getMessage());
        }

        return stimulatorTasks;
    }

    // schedule a task to send a (on/off w/ certain intensity) signal to the LED light
    private ScheduledFuture scheduleSignal(int timePointMs, final int signal) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        StimulationTask stimulationTask = new StimulationTask(mmc, stimulatorPort, STIMULATION_CHANNEL, signal);

        // convert the timepoint to microseconds
        // legacy decision, not sure why we need to do it like this
        return ses.schedule(stimulationTask, timePointMs * 1000, TimeUnit.MICROSECONDS);
    }
}