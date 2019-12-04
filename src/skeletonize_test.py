from ij import IJ, ImagePlus  

from ij.process import ImageProcessor,ByteProcessor, ImageStatistics, ImageConverter

from ij.plugin.filter import RankFilters, EDM

from ij.gui import Roi, PolygonRoi, Wand, ImageCanvas, PointRoi

from ij.measure.Measurements import MEAN, CENTER_OF_MASS, CENTROID

import math

from ij.process.AutoThresholder import Method

from sc.fiji.analyzeSkeleton import AnalyzeSkeleton_


#1 
#original = IJ.getImage()
#width = original.getWidth()
#height = original.getHeight()
#original.setRoi(Roi(20, 20, width - 40, height - 40))
#inverted = original.duplicate()
#
##inverted_processor = inverted.getProcessor().crop()
#inverted_processor = inverted.getProcessor()
#
#inverted_processor.invert()
#mean = int(inverted_processor.getStats().mean * 1.1)
#
#inverted_processor.threshold(mean)
#
#inverted.setProcessor(inverted_processor)
#inverted.show()


#2
original = IJ.getImage()
width = original.getWidth()
height = original.getHeight()
original.setRoi(Roi(20, 20, width - 40, height - 40))
inverted = original.duplicate()

#inverted_processor = inverted.getProcessor().crop()
inverted_processor = inverted.getProcessor()

inverted_processor.invert()
mean = int(inverted_processor.getStats().mean * 1.1)

inverted_processor.threshold(mean)
skeletonizer = ByteProcessor(inverted_processor, False)
skeletonizer.skeletonize()

inverted.setProcessor(skeletonizer)
inverted.show()