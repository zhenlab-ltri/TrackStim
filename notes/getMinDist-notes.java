
// given N region of interest statistics,
// get the:
//  minimum distance between the N regions
//  region index with that contains the min distance
//
static double[][] getMinDist(double[][] measurespre_, double[][] measures_) {
    int i;
    int j;
    double distancescalar = 0;
    double minval = 0;
    int minindex = 0;
    double dx = 0;
    double dy = 0;
    double mindx = 0;
    double mindy = 0;
    double[][] returnval = new double[measurespre_.length][4];

    for (i = 0; i < measurespre_.length; i++) {
        for (j = 0; j < measures_.length; j++) {
            dx = measures_[j][2] - measurespre_[i][2];
            dy = measures_[j][3] - measurespre_[i][3];
            distancescalar = Math.sqrt(dx * dx + dy * dy);
            if (j != 0) {
                if (minval > distancescalar) {
                    minval = distancescalar;
                    minindex = j;
                    mindx = dx;
                    mindy = dy;
                }
            } else {
                minval = distancescalar;
                minindex = 0;
            }
        }
        returnval[i][0] = minindex;
        returnval[i][1] = minval;
        returnval[i][2] = mindx;
        returnval[i][3] = mindy;
    }
    return returnval;
}