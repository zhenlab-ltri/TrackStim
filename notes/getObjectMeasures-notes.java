

// reduce image noise
// compute threshold from previous thresholds
// watershed the image
// extract the regions of interest
// compute image statistics from regions of interest
// returns a list of region statistics
// each region will have a mean, area, center x, center y
static double[][] getObjectMeasures(ImagePlus i, ImageProcessor ip, String method) {
  duplicateImage();
  reduceNoise();
  getInitialThresholdAverage();
  threshold();
  watershed();
  extractRegionsOfInterest();
}



duplicateImage(){
    // the imp ip must left half or something not full image
  ip_ = ip.duplicate();
  ip_resized = ip_.resize(ip_.getWidth() / 2);
}

reduceNoise(){
  RankFilters rf = new RankFilters();
  rf.rank(ip_resized, 0.0, ij.plugin.filter.RankFilters.MEDIAN);// 4 means median
                            // periodic black white noize cause miss thresholding, so eliminate
                            // those noize
}

getInitialThresholdAverage(int nInitialImages){
  // take first 30 images
  // compute minimum threshold value for each slice and return the average
  int thresholdSum = 0;

  // for each image
  ip_resized.setAutoThreshold(thresholdmethodstring, true, 0);// seems good. and fast? 13msec/per->less than
  thresholdSum += (int) ip_resized.getMinThreshold();

  return thresholdSum / numInitialImages;
}

updateThresholdAfterEveryNImages(int nImages, int nInitialImages){
  // every 50 slice calculate setAutoThreshold
  // 30 is from the 30 initial images in getInitialThresholdAverage
  if (countslice % nImages == 0) {
      ip_resized.setAutoThreshold(thresholdmethodstring, true, 0);
      int slotindex = ((countslice - nInitialImages) / nImages) % nInitialImages;
      int newdata = (int) ip_resized.getMinThreshold();
      int olddata = threshbuffer[slotindex];
      threshbuffer[slotindex] = newdata;
      threshsum = threshsum - olddata + newdata;
      threahaverage = threshsum / nInitialImages;
  }

  return treahaverage;
}

extractRegionsOfInterest(){
  byte[] pixels = (byte[]) iplbyte.getPixels();
  for (y = 0; y < heightleft; y = y + 3) {
      for (x = 0; x < widthleft; x = x + 3) {

          if (pixels[y * widthleft + x] == 0){

              wand.autoOutline(x, y, 0.0, 1.0, 4);
              roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, 2);
              impleftbyte.setRoi(roi);
              imstat = impleftbyte.getStatistics(1);// area 1 mean 2
              if (imstat.area > minimamarea) {
                  roiarraylist.add(roi);
              }
              // delet already detected roi.
              iplbyte.fill(roi);
          }
      }
  }
}

extractRegionOfInterestStatistics(){
  // 3 measurement factors, area, mean, centroid
  double[][] roimeasures = new double[roiarraylist.size()][4];
  Roi roi_;
  for (int i = 0; i < roiarraylist.size(); i++) {
      roi_ = (Roi) roiarraylist.get(i);
      impresizedori.setRoi(roi_);
      imstat = impresizedori.getStatistics(1 + 2 + 32 + 4 + 64);// area 1 mean 2, sd 4, centerofmass 64
      roimeasures[i][0] = imstat.area;
      roimeasures[i][1] = imstat.mean;
      roimeasures[i][2] = imstat.xCentroid;
      roimeasures[i][3] = imstat.yCentroid;
  }
  return roimeasures;
}