# NucleusMovement
ImageJ plugin to automate the measurement of yeast cell nucleus movements over time

INSTALL

Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) installed. If not, download the latest version of ImageJ bundled with Java and install it. 

The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

Download the latest copy of Bio-Formats into the ImageJ plugin directory for proprietary format image files.

Place Nucleus_Movement_.jar into the plugins directory of your ImageJ installation.

If everything has worked Wiggler should be in the Plugins menu.

Wiggler_.java is the editable code for the plugin should improvements or changes be required.

USAGE

First, open your image  stack and run it through StackReg  in imageJ to make sure the cells are in a stable position with no drift. The plugin was written for 1 channel fluorescent images over time, the channel colour shouldn't matter.

Using the registered image  stack, run the Wiggler plugin. You will be asked to threshold the image to select the cells. Carefully adjust the threshold so that individual yeast cells are completely thresholded. Touching yeast cells will not be split so these will have to be ignored.

The plugin will add all the objects (yeast cells) that it managed to find to the imageJ ROI manager. Each cell in the image will have a number overlaid which corresponds to the same number in the ROI manager. You will now be asked if you want to select a cell; type y or n . 

To select a cell, click on its number in the ROI Manager; this will highlight your cell of choice in the image. Click OK and the region containing the cell will be duplicated and oriented such that the long cell axis is vertical.

The plugin will now ask for a threshold which only selects the nucleus on the duplicate image. Once you are happy with the nucleus selection, click OK. The plugin will now measure the centroid position of the nucleus at every time point in the image for later detrending in Excel. 

Results are saved to the parent directory as a text file.

Once the centroid measurements are saved, the plugin will ask if you would like to select another cell, and the process is repeated.
