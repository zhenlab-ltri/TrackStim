


if (!tpf.CoM.getState()){
    imp.setRoi(roi);
    ImageProcessor ip_current = imp.getProcessor();
    ImageProcessor ipleft = ip_current.crop();
    ImagePlus impleft = new ImagePlus("l", ipleft);
    String thresholdmethod = tpf.thresholdmethod.getSelectedItem();
    measures = getObjmeasures(impleft, ipleft, false, thresholdmethod);

    if (i != 0){
        mindist = getMinDist(measurespre, measures);
        int j;
        int previoustarget = (int) targethistory[i - 1][0];
        int newtarget = (int) mindist[previoustarget][0];
        targethistory[i][0] = newtarget;
        targethistory[i][1] = measures[newtarget][2];
        targethistory[i][2] = measures[newtarget][3];
        shift[0] = mindist[previoustarget][2];
        shift[1] = mindist[previoustarget][3];
        distancefromcenter[0] = width / 4 - measures[newtarget][2] * 2;
        distancefromcenter[1] = height / 2 - measures[newtarget][3] * 2;
    } else {
        // mock meaures to detect most centorized roi for resized scan, divide 4
        double[][] mock = {
            {
                0,
                0,
                ipleft.getWidth() / 4,
                ipleft.getHeight() / 4
            }
        };
        double[][] initialtarget = getMinDist(mock, measures);
        // for non resised version
        int target = (int) initialtarget[0][0];
        targethistory[0][0] = target;
        targethistory[0][1] = measures[target][2];
        targethistory[0][2] = measures[target][3];
        // multiply 2 because resized 1/2
        distancefromcenter[0] = width / 4 - measures[target][2] * 2;
        distancefromcenter[1] = height / 2 - measures[target][3] * 2;
    }

    // return [roi#][order by distance from target, distance, dx, dy]
    // static double[][] getRoiOrder(int targetroinum, double[][] measures)
    if (!tpf.closest.getState()) {
        roiorder = getRoiOrder((int) targethistory[i][0], measures);
        // check target is collect or not by direcion/distance towards next roi. if
        // there are more than 2 rois.
        if (measures.length >= 2) {
            double[][] checkedroiorder = checkDirDis(i + 1, roiorder, measures);
            boolean trackstatus = false;
            double[][] finalroiorder = new double[][] {
                {
                    0
                }
            };
            if ((int) checkedroiorder[0][0] == -1) // negative means failed
            {
                trackstatus = false;
                finalroiorder = roiorder;
            } else {
                trackstatus = true;
                finalroiorder = checkedroiorder;
                int newtarget = 0;
                for (int j = 0; j < finalroiorder.length; j++) {
                    if ((int)(finalroiorder[j][0]) == 0) // here is target
                    {
                        newtarget = j;
                    }
                }
                // targethistory[slicenumber][roi index, x, y]
                targethistory[i][0] = newtarget;
                targethistory[i][1] = measures[newtarget][2];
                targethistory[i][2] = measures[newtarget][3];

            }
            drawRoiOrder(i + 1, finalroiorder, measures, trackstatus);
        }
        distancefromcenter[0] = width / 4 - measures[(int) targethistory[i][0]][2] * 2;
        distancefromcenter[1] = height / 2 - measures[(int) targethistory[i][0]][3] * 2;
    }
} else {
    imp.setRoi(leftroi);
    ImageProcessor ip_current = imp.getProcessor();
    ImageProcessor ipleft = ip_current.crop();
    ImagePlus impleft = new ImagePlus("l", ipleft);
    // get data and put it into double[] distancefromcenter =new double[2];
    if (i != 0) {
        if (roiwidth == width / 2 && roiheight == height){
            // usr didn't drow a roi
            centorofmass = getCenterofMass(impleft, ipleft, roi, 0, 0); // the roi should be
            // left roi
        } else {
            int roishiftx = (int)(targethistory[i - 1][1] - roiwidth / 2.0);
            int roishifty = (int)(targethistory[i - 1][2] - roiheight / 2.0);
            centorofmass = getCenterofMass(impleft, ipleft, roi, roishiftx, roishifty); // use
            // the
            // previous
            // roi
            // pos
        }
        distancefromcenter[0] = width / 4 - centorofmass[0];
        distancefromcenter[1] = height / 2 - centorofmass[1];
        // targethistory[slicenumber][roi index, x, y]
        targethistory[i][0] = -1; // for center of mass method, the roi index use -1,
        targethistory[i][1] = centorofmass[0];
        targethistory[i][2] = centorofmass[1];
    } else {
        centorofmass = getCenterofMass(impleft, ipleft, leftroi, 0, 0); // this roi is left
        // roi or usr
        // defined?
        distancefromcenter[0] = width / 4 - centorofmass[0];
        distancefromcenter[1] = height / 2 - centorofmass[1];
        // targethistory[slicenumber][roi index, x, y]
        targethistory[i][0] = -1; // for center of mass method, the roi index use -1,
        targethistory[i][1] = centorofmass[0];
        targethistory[i][2] = centorofmass[1];
    }
    imp.setRoi(roi); // just for visible.
}