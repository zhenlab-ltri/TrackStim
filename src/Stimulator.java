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
    Stimulator stimulator;

    public static final int ON_SIGNAL = 63;
    public static final int OFF_SIGNAL = 0;

    StimulationTask(CMMCore cmmcore_, String stimulatorPort_, int channel_, int signal_, Stimulator s) {
        mmc = cmmcore_;
        stimulatorPort = stimulatorPort_;
        channel = channel_;
        signal = signal_;
        
        stimulator = s;
    }

    // use micromanager to send a signal to the USB connection of the stimulator
    public static void sendSignal(CMMCore core_, String port_, int channel_, int signal_){
        int signalData = channel_ << 7 | signal_;
        CharVector signalDataVec = new CharVector();
        signalDataVec.add((char) signalData);

        try {
            core_.writeToSerialPort(port_, signalDataVec);
        } catch (java.lang.Exception e) {
            IJ.log("[ERROR] could not write data " + String.valueOf(signalDataVec) + " to the serial port " + port_);
            IJ.log(e.getMessage());
        }
    }

    // send signal data to the stimulator through the serial port
    public void run() {
        StimulationTask.sendSignal(mmc, stimulatorPort, channel, signal);
        // update the current stimulation signal so the stimulator can check if the LED light is on
        stimulator.currStimulationStrength = signal;
    }
}

class Stimulator {
    TrackStimController controller;
    String stimulatorPort;
    boolean initialized = false;

    private ArrayList<ScheduledFuture> stimulatorTasks;

    static final int STIMULATION_CHANNEL = 0; // the channel to send ths signals to
    static final String STIMULATOR_DEVICE_LABEL = "FreeSerialPort"; // hardcoded device label found in config

    public volatile int currStimulationStrength;

    Stimulator(TrackStimController c){
        controller = c;
        stimulatorPort = "";

        stimulatorTasks = new ArrayList<ScheduledFuture>();
        currStimulationStrength = StimulationTask.OFF_SIGNAL;
    }

    public void turnOnLEDLight(CMMCore core, String port){
        StimulationTask.sendSignal(controller.core, stimulatorPort, 0, StimulationTask.ON_SIGNAL);
        currStimulationStrength = StimulationTask.ON_SIGNAL;
    }

    public void turnOffLEDLight(CMMCore core, String port){
        StimulationTask.sendSignal(controller.core, stimulatorPort, 0, StimulationTask.OFF_SIGNAL);
        currStimulationStrength = StimulationTask.OFF_SIGNAL;
    }

    public boolean isLEDLightOn(){
        return currStimulationStrength > 0;
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
            stimulatorPort = controller.core.getProperty(STIMULATOR_DEVICE_LABEL, "Port");

            // send initial signal to stimulator port
            controller.core.writeToSerialPort(stimulatorPort, initialSignalData);
            portFound = true;

        } catch (Exception e){
            IJ.log("[ERROR] could not find stimulator port");
            IJ.log(e.getMessage());
        }

        initialized = portFound;
        return portFound;
    }

    public void cancelTasks(){
        for (int k = 0; k < stimulatorTasks.size(); k++){
            stimulatorTasks.get(k).cancel(true);
        }
        // turn the light off if it is currently in the middle of a stimulation cycle
        turnOffLEDLight(controller.core, stimulatorPort);
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
    public void scheduleStimulationTasks(
        boolean useRamp, int preStimTimeMs, int signal,
        int stimDurationMs, int stimCycleDurationMs, int numStimCycles,
        int rampBase, int rampStart, int rampEnd) throws java.lang.Exception {

        ArrayList<ScheduledFuture> futureTasks = new ArrayList<ScheduledFuture>();
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
                        futureTasks.add(scheduleSignal(preStimTimeMs + i * stimCycleDurationMs + j * (stimDurationMs / rampSignalDelta),
                                rampStart + j * rampSign));
                    }

                    // schedule signal to turn off light at end of cycle
                    futureTasks.add(scheduleSignal(preStimTimeMs + stimDurationMs + i * stimCycleDurationMs, rampBase));
                }
            } else {
                // send full light strength right away
                for (int i = 0; i < numStimCycles; i++) {
                    int signalTimePtBegin = preStimTimeMs + i * stimCycleDurationMs;
                    int signalTimePtEnd = signalTimePtBegin + stimDurationMs;

                    // schedule signal to turn on light at beginning cycle
                    futureTasks.add(scheduleSignal(signalTimePtBegin, signal));

                    // schedule signal to turn off light at end of cycle
                    futureTasks.add(scheduleSignal(signalTimePtEnd, rampBase));
                }
            }
        } catch (java.lang.Exception e) {
            IJ.log("[ERROR] could not send signals to the stimulator");
            IJ.log(e.getMessage());
        }

        stimulatorTasks = futureTasks;
    }

    // schedule a task to send a (on/off w/ certain intensity) signal to the LED light
    private ScheduledFuture scheduleSignal(int timePointMs, final int signal) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        StimulationTask stimulationTask = new StimulationTask(controller.core, stimulatorPort, STIMULATION_CHANNEL, signal, this);

        // convert the timepoint to microseconds
        // legacy decision, not sure why we need to do it like this
        return ses.schedule(stimulationTask, timePointMs * 1000, TimeUnit.MICROSECONDS);
    }
}