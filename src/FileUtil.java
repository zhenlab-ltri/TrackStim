import ij.io.FileInfo;
import ij.io.TiffEncoder;
import ij.io.FileSaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;


class FileUtil {
	// create a directory of the form temp<i> where i is the first available
	// i such that temp<i> can be created
	static String createImageSaveDirectory(String root){
		// get count number of directories N so that we can create directory N+1
		File saveDirectoryFile = new File(root);
		File[] fileList = saveDirectoryFile.listFiles();
		int numSubDirectories = 0;
		for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory()) {
						numSubDirectories++;
				}
		}

		// choose first temp<i> which does not exist yet and create directory with name tempi
		int i = 1;
		File newdir = new File(root + "temp" + String.valueOf(numSubDirectories + i));
		while (newdir.exists()) {
				i++;
				newdir = new File(root + "temp" + String.valueOf(numSubDirectories + i));
		}

		newdir.mkdir();
		return newdir.getPath();
	}

	static void savePngFile(String directory, String fname, ImagePlus imp) {
		String filePath = directory + "/" + fname + ".png";
		ImagePlus copy = imp.duplicate();
		ImageProcessor ip = copy.getProcessor();
		ip.resetRoi();
		ByteProcessor bp = ip.convertToByteProcessor(true); // save as 8-bit
		copy.setProcessor(bp);            
		FileSaver f = new FileSaver(copy);
		f.saveAsPng(filePath);
	}

	static void saveJpegFile(String directory, String fname, ImagePlus imp) {
		String filePath = directory + "/" + fname + ".jpg";
		ImagePlus copy = imp.duplicate();
		ImageProcessor ip = copy.getProcessor();
		ip.resetRoi();
		ByteProcessor bp = ip.convertToByteProcessor(true); // save as 8-bit
		copy.setProcessor(bp);            
		FileSaver f = new FileSaver(copy);
		f.setJpegQuality(100);
		f.saveAsJpeg(filePath);
	}
	
	static void saveTiffFile(String directory, String fname, FileInfo fi) {
		String filePath = directory + "/" + fname + ".tiff";

		try {
			File toSave = new File(filePath);
			toSave.createNewFile();
			FileOutputStream outputStream = new FileOutputStream(toSave);
			TiffEncoder te = new TiffEncoder(fi);

			te.write(outputStream);
			outputStream.close();
		} catch (java.lang.Exception e){
			IJ.log("[ERROR] unable to write tiff file");
			IJ.log(e.getMessage());
		}
	}

	static void saveTiffFile(String directory, String fname, ImagePlus imp) {
		String filePath = directory + "/" + fname + ".tiff";
		ImagePlus copy = imp.duplicate();
		ImageProcessor ip = copy.getProcessor();
		ip.resetRoi();
		ByteProcessor bp = ip.convertToByteProcessor(true); // save as 8-bit
		copy.setProcessor(bp);            
		FileSaver f = new FileSaver(copy);
		f.saveAsTiff(filePath);
	}

    static void saveCsvFile(String directory, String fname, String csvHeader, String[] csvData){
        PrintWriter p = null;
        try {
			p = new PrintWriter(directory + "/" + fname + ".csv");
            
            p.println(csvHeader);

			for( int i = 0; i < csvData.length; i++ ){
				if( csvData[i] != null ){
					p.println(csvData[i]);
				}
			}
		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to write " + fname +  " file");
		} finally {
			if( p != null ){
				p.close();
			}
		}
	}
	
	static void saveContentToTextFile(String directory, String fname, String[] content){
        PrintWriter p = null;
        try {
			p = new PrintWriter(directory + "/" + fname + ".txt");
            
			for( int i = 0; i < content.length; i++ ){
				if( content[i] != null ){
					p.println(content[i]);
				}
			}
		} catch (java.io.IOException e){
			IJ.log("[ERROR] unable to write " + fname +  " file");
		} finally {
			if( p != null ){
				p.close();
			}
		}
    }


}