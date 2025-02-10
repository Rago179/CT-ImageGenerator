import java.io.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

public class test extends Application {
	private short[][][] cthead;
	private float[][][] grey;
	private short min, max;
	private double skinOpacity = 0.12;

	@Override
	public void start(Stage stage) {
		stage.setTitle("CThead Viewer");
		try {
			ReadData();
		} catch (IOException e) {
			System.out.println("Error: Unable to load CThead data.");
			return;
		}

		WritableImage volumeRenderedZ = new WritableImage(256, 256);
		WritableImage volumeRenderedX = new WritableImage(256, 256);
		WritableImage volumeRenderedY = new WritableImage(256, 256);

		ImageView volumeRenderedViewZ = new ImageView(volumeRenderedZ);
		ImageView volumeRenderedViewX = new ImageView(volumeRenderedX);
		ImageView volumeRenderedViewY = new ImageView(volumeRenderedY);

		VolumeRenderZ(volumeRenderedZ);
		VolumeRenderX(volumeRenderedX);
		VolumeRenderY(volumeRenderedY);

		Slider skinOpacitySlider = new Slider(0, 100, skinOpacity * 100);
		skinOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
			skinOpacity = newVal.doubleValue() / 100.0;
			VolumeRenderZ(volumeRenderedZ);
			VolumeRenderX(volumeRenderedX);
			VolumeRenderY(volumeRenderedY);
		});

		GridPane grid = new GridPane();
		grid.add(volumeRenderedViewZ, 0, 0);
		grid.add(volumeRenderedViewX, 1, 0);
		grid.add(volumeRenderedViewY, 2, 0);
		grid.add(skinOpacitySlider, 1, 1);

		Scene scene = new Scene(grid, 800, 450);
		stage.setScene(scene);
		stage.show();
	}

	private void ReadData() throws IOException {
		File file = new File("CThead-256cubed.bin");
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		cthead = new short[256][256][256];
		grey = new float[256][256][256];
		min = Short.MAX_VALUE;
		max = Short.MIN_VALUE;

		for (int k = 0; k < 256; k++) {
			for (int j = 0; j < 256; j++) {
				for (int i = 0; i < 256; i++) {
					short read = (short) ((in.readByte() & 0xFF) | ((in.readByte() & 0xFF) << 8));
					min = (short) Math.min(min, read);
					max = (short) Math.max(max, read);
					cthead[k][j][i] = read;
				}
			}
		}
		in.close();

		for (int k = 0; k < 256; k++) {
			for (int j = 0; j < 256; j++) {
				for (int i = 0; i < 256; i++) {
					grey[k][j][i] = (float) (cthead[k][j][i] - min) / (max - min);
				}
			}
		}
	}

	private void VolumeRenderZ(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[z][y][x]);
	}

	private void VolumeRenderX(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[y][z][x]);
	}

	private void VolumeRenderY(WritableImage image) {
		renderVolume(image, 256, 256, (x, y, z) -> cthead[y][x][z]);
	}

	private void renderVolume(WritableImage image, int width, int height, VoxelFetcher fetcher) {
		PixelWriter writer = image.getPixelWriter();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				double opacity = 0;
				double[] color = {0, 0, 0};

				for (int z = 0; z < 256; z++) {
					short value = fetcher.fetch(x, y, z);
					double[] tf = getTransferFunction(value);

					double voxelOpacity = tf[3];
					for (int c = 0; c < 3; c++) {
						color[c] += voxelOpacity * tf[c] * (1 - opacity);
					}
					opacity += voxelOpacity * (1 - opacity);
					if (opacity > 0.99) break;
				}
				writer.setColor(x, y, Color.color(color[0], color[1], color[2]));
			}
		}
	}

	private double[] getTransferFunction(short value) {
		if (value < -300) return new double[]{0, 0, 0, 0};
		if (value >= -300 && value <= 49) return new double[]{0.82, 0.49, 0.18, skinOpacity};
		if (value >= 50 && value <= 299) return new double[]{0, 0, 0, 0};
		return new double[]{1.0, 1.0, 1.0, 0.8};
	}

	public static void main(String[] args) {
		launch(args);
	}

	@FunctionalInterface
	interface VoxelFetcher {
		short fetch(int x, int y, int z);
	}
}
