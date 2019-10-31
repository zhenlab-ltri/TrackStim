### Understanding Hamamatsu Orca R2 Camera Options in Micro-Manager
Binning: fold adjacent pixels to improve performance at the cost of resolution (1, 2, 4, 8)
Trigger: what triggers the camera to fire
- level: a signal that triggers the camera
- software: software triggers the camera
Scan mode (1, 2)
- 1 is the default scan mode (8.5 fps)
- 2 is the enhanced scan mode that doubles fps and readout speed
High dynamic range mode (1, 2) (I think 1 maps to false, 2 maps to true)
- only works when scan mode is set to default scan mode (8.5 fps)