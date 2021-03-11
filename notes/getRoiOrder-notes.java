
// given a target region of interest index, sort the measures based on their distance to the target region

static double[][] getRoiOrder(int targetroinum, double[][] measures_) {
    double[] targetcoordinate = new double[2];
    targetcoordinate[0] = measures_[targetroinum][2];// x
    targetcoordinate[1] = measures_[targetroinum][3];// y

    double dx = 0;
    double dy = 0;
    double[][] returnval = new double[measures_.length][4];

    double[] distancescaler = new double[measures_.length];
    for (int i = 0; i < measures_.length; i++) {
        dx = targetcoordinate[0] - measures_[i][2];
        dy = targetcoordinate[1] - measures_[i][3];
        returnval[i][2] = dx;
        returnval[i][3] = dy;
        distancescaler[i] = Math.sqrt(dx * dx + dy * dy);
        returnval[i][1] = distancescaler[i];
    }

    double[] copydistance = distancescaler.clone();
    Arrays.sort(copydistance);// is there any method to get sorted index?
    for (int i = 0; i < distancescaler.length; i++) {
        for (int j = 0; j < copydistance.length; j++) {
            if (distancescaler[i] == copydistance[j]) {
                returnval[i][0] = (double) j;
            }
        }
    }

    return returnval;
}