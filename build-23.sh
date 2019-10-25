cd ./src/
javac -source 1.5 -target 1.5 -classpath /Applications/Micro-Manager1.4.23/ij.jar:/Applications/Micro-Manager1.4.23/plugins/Micro-Manager/MMCoreJ.jar:/Applications/Micro-Manager1.4.23/plugins/Micro-Manager/MMJ_.jar -Xlint:unchecked -sourcepath . -d ../dist TrackStimPlugin.java

cd ../dist/
jar cvf TrackStim_.jar TrackStimPlugin.class TrackStim.class TrackingThread.class SignalSender.class

cp -f ./TrackStim_.jar /Applications/Micro-Manager1.4.23/mmplugins/
