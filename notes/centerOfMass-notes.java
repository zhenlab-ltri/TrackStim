// for non-thresholding method
// this returns x y of centor of mass backgournd subtracted
static double[] getCenterofMass(ImagePlus imp, ImageProcessor ip, Roi roi, int x, int y) {
    ImagePlus imp_ = imp;
    ImageProcessor ip_ = ip;
    Roi roi_ = roi;
    ImageStatistics imstat_ = imp_.getStatistics(2);
    int backgroundvalue = (int) imstat_.mean;
    ImageProcessor ip2 = ip_.duplicate();
    ip2.add(-backgroundvalue * 1.5);
    ImagePlus imp2 = new ImagePlus("subtracted", ip2);
    roi_.setLocation(x, y);
    imp2.setRoi(roi_);

    // median filter ver 7 test
    RankFilters rf = new RankFilters();
    rf.rank(ip2, 0.0, 4);// median 4 periodic black white noize cause miss thresholding, so eliminate
                          // those noize
    ImageStatistics imstat2 = imp2.getStatistics(64 + 32);
    double[] returnval = { imstat2.xCenterOfMass, imstat2.yCenterOfMass };
    countslice++;
    return (returnval);
}