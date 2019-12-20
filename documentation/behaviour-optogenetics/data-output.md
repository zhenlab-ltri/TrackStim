### Data Output
After running imaging jobs, the following files are produced:

- ```stage-pos.csv```
- ```stim-strength.csv```
- ```job-args.txt```
- ```*.tif```

These files can be found in the ```temp<i>``` directory that is created when running an imaging job.

#### stage-pos.csv

stage-pos.csv contains the stage position data at the time each frame was taken.

The stage position data is in micrometers.

#### stim-strength.csv

stim-strength.csv contains the stimulator strength at the time each frame was taken.  

The stimulator strength will range from 0 to 63.  

#### job-args.txt

job-args.txt records the ui options used for the imaging job. 

#### *.tif

Each frame saved as a .tif file