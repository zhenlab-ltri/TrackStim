import ij.io.FileInfo;
import ij.io.TiffEncoder;
import ij.io.FileSaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import ij.IJ;
import ij.ImagePlus;


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
		
		FileSaver f = new FileSaver(imp);
		f.saveAsPng();
	}

	static void saveJpgFile(String directory, String fname, ImagePlus imp) {
		String filePath = directory + "/" + fname + ".jpg";
		
		FileSaver f = new FileSaver(imp);
		f.setDefaultJpegQuality(100);
		f.saveAsPng();
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