/*
CS-256 Getting started code for the assignment
I do not give you permission to post this code online
Do not copy code
Do not use libraries to do the Slicing, MIP or Volume Rendering. That code must be written by yourself
You may use libraries / IDE to achieve a better GUI
*/
import java.io.*;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class test extends Application {
	short cthead[][][]; //store the 3D volume data set
	float grey[][][]; //store the 3D volume data set converted to 0-1 ready to copy to the image
	short min, max; //min/max value in the 3D volume data set
	private double skinOpacity = 0.12;

	int currZSlice=128;
	int currXSlice=128;
	int currYSlice=128;


    @Override
    public void start(Stage stage) throws FileNotFoundException {
		stage.setTitle("CThead Viewer");
		
		try {
			ReadData();
		} catch (IOException e) {
			System.out.println("Error: The CThead file is not in the working directory");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			return;
		}

		//We need 3 things to see an image
		//1. We need to create the image
		WritableImage sliceZImage = new WritableImage(256, 256); //allocate memory for the image
		WritableImage sliceXImage = new WritableImage(256, 256); //allocate memory for the image
		WritableImage sliceYImage = new WritableImage(256, 256); //allocate memory for the image


		WritableImage volumeRenderedZ = new WritableImage(256, 256);
		WritableImage volumeRenderedX = new WritableImage(256, 256);
		WritableImage volumeRenderedY = new WritableImage(256, 256);


		GetZSlice(currZSlice, sliceZImage); //make the image - in this case go get the slice and copy it into the image
		GetXSlice(currXSlice, sliceXImage);
		GetYSlice(currYSlice, sliceYImage);

		//2. We link a view in the GUI to that image
		ImageView sliceZView = new ImageView(sliceZImage); //and then see 3. below
		ImageView sliceXView = new ImageView(sliceXImage);
		ImageView sliceYView = new ImageView(sliceYImage);


		ImageView volumeRenderedViewZ = new ImageView(volumeRenderedZ);
		ImageView volumeRenderedViewX = new ImageView(volumeRenderedX);
		ImageView volumeRenderedViewY = new ImageView(volumeRenderedY);

		VolumeRenderZ(volumeRenderedZ);
		VolumeRenderX(volumeRenderedX);
		VolumeRenderY(volumeRenderedY);


		// Do the same for MIP
		WritableImage MIPZImage = new WritableImage(256, 256);
		GetZMIP(MIPZImage);
		ImageView MIPZView = new ImageView(MIPZImage);

		WritableImage MIPXImage = new WritableImage(256, 256);
		GetXMIP(MIPXImage);
		ImageView MIPXView = new ImageView(MIPXImage);

		WritableImage MIPYImage = new WritableImage(256, 256);
		GetYMIP(MIPYImage);
		ImageView MIPYView = new ImageView(MIPYImage);

		//Create the simple GUI
		Slider sliceZSlider = new Slider(0, 255, currZSlice);
		Slider sliceXSlider = new Slider(0, 255, currXSlice);
		Slider sliceYSlider = new Slider(0, 255, currYSlice);

		Slider skinOpacitySlider = new Slider(0, 100, skinOpacity * 100);



		sliceZSlider.valueProperty().addListener(new ChangeListener<Number>() { 
			public void changed(ObservableValue <? extends Number >  
					observable, Number oldValue, Number newValue) { 

				currZSlice = newValue.intValue();
				//We update our Image
		        GetZSlice(currZSlice, sliceZImage); //go get the slice image
				//Because sliceYView (an ImageView) is linked to it, this will automatically update the displayed image in the GUI
            } 
        });

		sliceXSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue <? extends Number >
										observable, Number oldValue, Number newValue) {

				currXSlice = newValue.intValue();
				//We update our Image
				GetXSlice(currXSlice, sliceXImage);
				//Because sliceYView (an ImageView) is linked to it, this will automatically update the displayed image in the GUI
			}
		});

		sliceYSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue <? extends Number >
										observable, Number oldValue, Number newValue) {

				currYSlice = newValue.intValue();
				//We update our Image
				GetYSlice(currYSlice, sliceYImage);
				//Because sliceXView (an ImageView) is linked to it, this will automatically update the displayed image in the GUI
			}
		});

		skinOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
			skinOpacity = newVal.doubleValue() / 100.0;
			VolumeRenderZ(volumeRenderedZ);
			VolumeRenderX(volumeRenderedX);
			VolumeRenderY(volumeRenderedY);
		});


		//Add all the GUI elements
		//I'll start a grid for you
		GridPane grid = new GridPane();
		grid.add(sliceXSlider,0,0);
		grid.add(sliceYSlider,2,0);
		grid.add(sliceZSlider, 4, 0); // Slider at column 0, row 0

		grid.add(skinOpacitySlider, 2, 5);

		grid.add(volumeRenderedViewX, 0, 4);
		grid.add(volumeRenderedViewY, 2, 4);
		grid.add(volumeRenderedViewZ, 4, 4);


		grid.setHgap(10);
        grid.setVgap(10);

        //3. (referring to the 3 things we need to display an image)
      	//we need to add it to the grid
		grid.add(sliceXView,0,1);
		grid.add(sliceYView,2,1);
		grid.add(sliceZView, 4, 1); // Slider at column 0, row 1

		grid.add(MIPXView,0,2);
		grid.add(MIPYView,2,2);
		grid.add(MIPZView, 4, 2);


		// Create a scene and set the stage
        Scene scene = new Scene(grid, 800, 840);
        stage.setTitle("CT Data Viewer");
        stage.setScene(scene);
        stage.show();
    }
    

	//Function to read in the cthead data set
	public void ReadData() throws IOException {
		//If you've put the test.java in a directory called "src" and put the dataset in the parent directory, then this will be the correct path
		File file = new File("CThead-256cubed.bin");
		//Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find the equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
		int i, j, k; //loop through the 3D data set
		
		min=Short.MAX_VALUE; max=Short.MIN_VALUE; //set to extreme values
		short read; //value read in
		int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around
		
		cthead = new short[256][256][256]; //allocate the memory - note this is fixed for this data set
		grey= new float[256][256][256];
		//loop through the data reading it in
		for (k=0; k<256; k++) {
			for (j=0; j<256; j++) {
				for (i=0; i<256; i++) {
					//because the Endianess is wrong, it needs to be read byte at a time and swapped
					b1=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					b2=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					read=(short)((b2<<8) | b1); //and swizzle the bytes around
					if (read<min) min=read; //update the minimum
					if (read>max) max=read; //update the maximum
					cthead[k][j][i]=read; //put the short into memory (in C++ you can replace all this code with one fread)
				}
			}
		}
		System.out.println(min+" "+max); //diagnostic - for CThead-256cubed.bin this should be -1897, 3029
		//(i.e. there are 4927 levels of grey, and now we will normalise them to 0-1 for display purposes
		//I know the min and max already, so I could have put the normalisation in the above loop, but I put it separate here
		for (k=0; k<256; k++) {
			for (j=0; j<256; j++) {
				for (i=0; i<256; i++) {
					grey[k][j][i]=((float) cthead[k][j][i]-(float) min)/((float) max-(float) min);
				}
			}
		}
		//At this point, cthead is the original dataset
		//and grey is 0-1 float data that can be displayed by Java
	}


	private void VolumeRenderZ(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[z][y][x]);
	}

	private void VolumeRenderX(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[y][x][z]);
	}

	private void VolumeRenderY(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[y][z][x]);
	}



	private void renderVolume(WritableImage image, int width, int height, VoxelFetcher fetcher) {
		PixelWriter writer = image.getPixelWriter();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				double accumulatedOpacity = 0;
				double[] color = {0, 0, 0};

				//catches depth and color details using transfer function and color contribution and accumulated opacity.
				for (int z = 0; z < 256; z++) {
					short value = fetcher.fetch(x, y, z);
					double[] tf = getTransferFunction(value);

					double voxelOpacity = tf[3];
					for (int c = 0; c < 3; c++) {
						color[c] += voxelOpacity * tf[c] * (1 - accumulatedOpacity);
					}
					accumulatedOpacity += voxelOpacity * (1 - accumulatedOpacity);
					if (accumulatedOpacity > 0.99) break; //completely opaque
				}
				writer.setColor(x, y, Color.color(color[0], color[1], color[2]));
			}
		}
	}

	private double[] getTransferFunction(short value) {
		//RGB + Opacity
		if (value < -300) return new double[]{0, 0, 0, 0};
		if (value >= -300 && value <= 49) return new double[]{0.82, 0.49, 0.18, skinOpacity};
		if (value >= 50 && value <= 299) return new double[]{0, 0, 0, 0};
		//if CT values exceed 300
		return new double[]{1.0, 1.0, 1.0, 0.8};
	}

	public void GetZMIP(WritableImage image) {
		GetMIP(image, (int) image.getWidth(), (int) image.getHeight(), (x, y, z) -> grey[z][y][x]);
	}

	public void GetXMIP(WritableImage image) {
		GetMIP(image, (int) image.getWidth(), (int) image.getHeight(), (x, y, z) -> grey[y][x][z]);
	}

	public void GetYMIP(WritableImage image) {
		GetMIP(image, (int) image.getWidth(), (int) image.getHeight(), (x, y, z) -> grey[y][z][x]);
	}

	private void GetMIP(WritableImage image, int width, int height, IntensityFetcher fetcher) {
		// Find the width and height of the image to be processed

		// Get an interface to write to the image memory
		PixelWriter imageWriter = image.getPixelWriter();
		// Iterate over all pixels in the 2D plane (x, y)
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Initialize the maximum intensity value for this (x, y) position
				float maxIntensity = 0.0f;

				// Iterate through all slices along the Z-axis
				for (int z = 0; z < grey.length; z++) {
					// Get the intensity value at (z, y, x)
					float intensity = fetcher.fetch(x, y, z);

					// Update the maximum intensity if the current intensity is greater
					if (intensity > maxIntensity) {
						maxIntensity = intensity;
					}
				}

				// Create a grayscale color using the maximum intensity
				Color color = Color.color(maxIntensity, maxIntensity, maxIntensity);
				// Apply the new color to the image
				imageWriter.setColor(x, y, color);
			}
		}
	}

	public void GetXSlice(int slice, WritableImage image) {
		GetSlice(image, (int) image.getWidth(), (int) image.getHeight(), slice, (x, y, z) -> grey[y][x][z]);
	}

	public void GetYSlice(int slice, WritableImage image) {
		GetSlice(image, (int) image.getWidth(), (int) image.getHeight(), slice, (x, y, z) -> grey[y][z][x]);
	}

	public void GetZSlice(int slice, WritableImage image) {
		GetSlice(image, (int) image.getWidth(), (int) image.getHeight(), slice, (x, y, z) -> grey[z][y][x]);
	}


	// Method to extract a slice along the X-axis
	private void GetSlice(WritableImage image, int width, int height, int slice, IntensityFetcher fetcher) {
		// Find the width and height of the image to be processed
		PixelWriter imageWriter = image.getPixelWriter();
		// Get an interface to write to that image memory
		// Iterate over all pixels
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Extract the value from the grey array at the specified X-slice
				float val = fetcher.fetch(x, y, slice);
				// Create a grayscale color
				Color color = Color.color(val, val, val);
				// Apply the new color
				imageWriter.setColor(x, y, color);
			}
		}
	}

    public static void main(String[] args) {
        launch();
    }
	@FunctionalInterface
	interface VoxelFetcher {
		short fetch(int x, int y, int z);
	}

	@FunctionalInterface
	private interface IntensityFetcher {
		float fetch(int x, int y, int z);
	}
}

