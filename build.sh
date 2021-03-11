rm ./dist/*
cd ./src/

javac -source 1.5 -target 1.5 -classpath .:/Users/dylanfong/src/work/TrackStim/dependencies/ij.jar:/Users/dylanfong/src/work/TrackStim/dependencies/MMCoreJ.jar:/Users/dylanfong/src/work/TrackStim/dependencies/MMJ_.jar -Xlint:unchecked -sourcepath . -d ../dist TrackStimPlugin.java

cd ../dist/
jar cvf TrackStim_.jar *.class
