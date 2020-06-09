import java.text.DecimalFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.String;

import javax.swing.JOptionPane; 

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.plugin.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.io.FileInfo;

/*
 * Wiggler Plugin to measure the movement of the 
 * yeast nucleus within the yeast cell. Yeast images
 * should contain yeast cells that do not move so 
 * it may be necessary to run a movement correction 
 * plugin/program first, for example Stack Reg from
 * ImageJ plugin downloads.
 * 
 * Author David Kelly
 * 
 * Date May 2014
 * 
 */

public class Wiggler_ implements PlugInFilter{
	ImagePlus imp;
	double threshMinLevel;
	double threshMaxLevel;
	String theparentDirectory;
	String filename;
	
	public int setup (String arg, ImagePlus imp) {
		this.imp =imp;
		return DOES_ALL;
	}
	
	public void run(ImageProcessor ip) {
		
		/*
		 * Section to assign the active image to 
		 * a variable and retrieve its filename and
		 * directory location to save the results to.
		 */
		ImagePlus TheOriginal = WindowManager.getCurrentImage();
		imp.unlock();	//Unlock the image for processing	
		filename = imp.getTitle(); 	//Get file name
		filename = filename.substring(0, filename.indexOf('.')); //Remove .tif from end
		FileInfo filedata = TheOriginal.getOriginalFileInfo();
		theparentDirectory = filedata.directory; //Get File Path
		
		
		int numslices = TheOriginal.getNFrames(); //Get number of slices in image
		if (numslices == 1){
			numslices = TheOriginal.getNSlices();
		}
		//Set Measurements
		IJ.run("Set Measurements...", "area centroid center bounding feret's redirect=None decimal=3");
		
		/*
		 * Set Threshold levels and threshold original image using a size limit to exclude
		 * doublets and cell fragments
		 */
		IJ.run("Threshold...","method='Default'");
		new WaitForUserDialog("Threshold", "Threshold Image to select whole yeast cell, then click OK.").show();
		threshMinLevel = TheOriginal.getProcessor().getMinThreshold(); 
		threshMaxLevel = TheOriginal.getProcessor().getMaxThreshold(); 
		IJ.run(TheOriginal, "Analyze Particles...", "size=1500-4500 pixel circularity=0.00-1.00 show=Nothing clear include add");
		
		
		/*
		 * Loop for user to Select Cells to Measure. There
		 * is a maximum limit of 50 cells per image.		
		 */
		String DoAnother = JOptionPane.showInputDialog("Do you want to select a cell y/n: ");
		int b = 1;
		int[] dupCellID = new int[50]; 
		while(DoAnother.equals("y")){
			new WaitForUserDialog("Selection", "Select Cell.").show();
			ImagePlus dupCell = new Duplicator().run(TheOriginal, 1, numslices);
			dupCell.show();
			dupCellID[b]=dupCell.getID();
			DoAnother = JOptionPane.showInputDialog("Do you want to select a cell y/n: ");
			b = b + 1;
		}
		
		CalculateAngles(dupCellID, b);  //Method to orientate cells along long axis
		
		MeasureMovement(dupCellID, numslices, b);  //Measure the selected cells
		
		new WaitForUserDialog("Finished", "Plugin Has Finished.").show();
		TheOriginal.changes = false;	
		TheOriginal.close();
	}
	
	public void CalculateAngles(int[] dupCellID, int b){
		
		/*
		 * Method to duplicate the chosen cells and then rotate 
		 * the yeast cells into a North-South orientation down
		 *  the long axis of the cells 
		 */
		for (int x = 1; x < b; x = x + 1){ 
			IJ.selectWindow(dupCellID[x]);
			ImagePlus TheDuplicate = WindowManager.getCurrentImage();
			DecimalFormat df = new DecimalFormat("#.0");
			ResultsTable rm = new ResultsTable();
			RoiManager r = RoiManager.getInstance();
			IJ.setThreshold(TheDuplicate, threshMinLevel, threshMaxLevel);
			TheDuplicate.setSlice(1);
			IJ.run(TheDuplicate, "Analyze Particles...", "size=1000-4500 pixel circularity=0.00-1.00 show=Nothing display clear include add slice");
			rm = Analyzer.getResultsTable();
			double fAngle = rm.getValueAsDouble(31, 0);
			double rAngle = fAngle - 90;
			rAngle = Double.valueOf(df.format(rAngle));
			r.runCommand("Delete");
			String Rotation = "angle=" + rAngle + " grid=1 interpolation=Bilinear stack";
			IJ.run(TheDuplicate, "Rotate... ", Rotation);
		}
	}
	
	public void MeasureMovement(int[] dupCellID, int numslices, int b){
		
		/*
		 * Loop Through Each Slice of extracted cell and measure the 
		 * centroid coordinates of the cell nucleus.
		 */
		for (int x = 1; x < b; x = x + 1){ 
			IJ.selectWindow(dupCellID[x]);
			ImagePlus TheDuplicate = WindowManager.getCurrentImage();
			IJ.run("Threshold...","method='Default'");
			new WaitForUserDialog("Threshold", "Threshold yeast cell to select the nucleus, then click OK.").show();
			ResultsTable res = new ResultsTable();
			double[] SliceNumY = new double[numslices];
			double[] SliceNumX = new double[numslices];
				for(int z = 1; z < numslices; z = z+1) {
					TheDuplicate.setSlice(z);
					IJ.run(TheDuplicate, "Analyze Particles...", "size=100-550 pixel circularity=0.00-1.00 show=Nothing display clear include add slice");
					res = Analyzer.getResultsTable();
					
					int MaxY = TheDuplicate.getHeight();
					int Midpoint = MaxY/2; 
					int MaxCount = res.getCounter();
					double yVal = 0;
					double xVal = 0;
					if (MaxCount > 1){
						for (int c = 0; c < (MaxCount - 1); c = c + 1){
							yVal = res.getValueAsDouble(7, c);
							xVal = res.getValueAsDouble(6, c);
							if (Math.abs((yVal/0.13) - Midpoint) <= 10 ){
								yVal = res.getValueAsDouble(7, c);
								xVal = res.getValueAsDouble(6, c);
							}
						}		
					}
					/*
					 * if statement to only record the centre X Y 
					 * coordinate of the nucleus when 1 object has 
					 * been found. If more than 1 object is identified
					 * then the XY coordinates default to zero to prevent 
					 * other fluorescent cell components from skewing the data
					 */
					if (MaxCount > 0 && MaxCount <2) {
						yVal = res.getValueAsDouble(7, 0);
						xVal = res.getValueAsDouble(6, 0);
						SliceNumY[z] = yVal;
						SliceNumX[z] = xVal;      
					}
					else{
						SliceNumY[z] = 0;
						SliceNumX[z] = 0;
					}
				}
			outputinfo(SliceNumY, SliceNumX, numslices); //Write the results to text file
			TheDuplicate.changes = false;	
			TheDuplicate.close();
		}
	}
	
	public void outputinfo(double[] SliceNumY, double[] SliceNumX, int numslices){
		String CreateName = theparentDirectory + filename + ".txt"; //Creates a text file of results in parent directory
		String FILE_NAME = CreateName;
	
		//Each result is written line by line into the text file for later offline analysis
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
			bufferedWriter.write("Start of Cell");
			bufferedWriter.newLine();
			for (int d=1; d < numslices; d = d + 1){
				bufferedWriter.write("X Position = " + SliceNumX[d] + " Y Position = " + SliceNumY[d]);
				bufferedWriter.newLine();
			}
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	}
	
}
