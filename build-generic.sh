rm ./dist/*
cd ./src/
javac -source 1.6 -target 1.6 -classpath /Applications/Micro-Manager1.4/ij.jar:/Applications/Micro-Manager1.4/plugins/Micro-Manager/MMCoreJ.jar:/Applications/Micro-Manager1.4/plugins/Micro-Manager/MMJ_.jar -Xlint:unchecked -sourcepath . -d ../dist TrackStimPlugin.java

cd ../dist/
jar cvf TrackStim_.jar *.class

cp -f ./TrackStim_.jar /Applications/Micro-Manager1.4/mmplugins/
