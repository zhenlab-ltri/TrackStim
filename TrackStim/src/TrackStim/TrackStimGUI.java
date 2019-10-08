package TrackStim;

import javax.swing.*;

// This class handles the GUI and all the values for the GUI
// The GUI is implemented using swing -- a java package for building user interfaces
public class TrackStimGUI {
  TrackStimGUI(){
    JFrame frame = new JFrame("Track Stim");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(700, 225);

    JButton button1 = new JButton("Press");
    frame.getContentPane().add(button1);
    frame.setVisible(true);
  }
}