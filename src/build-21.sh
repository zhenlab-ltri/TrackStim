javac -source 1.5 -target 1.5 -classpath /Applications/Micro-Manager1.4.21/ij.jar:/Applications/Micro-Manager1.4.21/plugins/Micro-Manager/MMCoreJ.jar:/Applications/Micro-Manager1.4.21/plugins/Micro-Manager/MMJ_.jar -Xlint:unchecked -sourcepath . TrackStimPlugin.java

jar cvf TrackStim_.jar TrackStimPlugin.class TrackStim_04.class TrackingThread11.class SignalSender01.class

cp -f ./TrackStim_.jar /Applications/Micro-Manager1.4.21/mmplugins/
