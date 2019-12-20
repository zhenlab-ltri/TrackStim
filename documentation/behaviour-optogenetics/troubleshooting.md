### Known problems

#### General strategy
Here is what to do when you notice strange behaviour while running an imaging job and you don't know what to do:
1. Stop the imaging job
2. Restart micromanager
3. Start TrackStim again

#### Tracking fails when the worm goes to the edge of the plate
Unfortunately this is an unsolved and difficult problem.  
The only thing that can be done right now is to stop the job after tracking fails.

#### Live mode stops
Sometimes during an image acquisition task, micromanager live mode stops and the same image will be shown continuously.  

To fix this issue:
1. close live mode
2. close TrackStim
3. stop live mode  
4. open live mode
5. open TrackStim

#### The ASI-MS 2000 may sometimes be unresponsive
Sometimes the tracker may fail and you will get an error saying that the command could not be sent to the serial port.

To fix this issue:
1. Stop the current TrackStim job
2. restart micromanager
3. open live mode
4. open TrackStim

#### I can't control the ASI-MS 2000 stage after stopping a job
Sometimes the tracker may not be able to stop the tracker tasks and they will continue to execute.

To fix this issue:
1. wait until the tracking tasks have finished execution
