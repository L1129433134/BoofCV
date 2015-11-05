/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.demonstrations.shapes;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.Configuration;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// todo minimum contour size
// todo video support
// todo webcam support

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectBlackPolygonApp<T extends ImageSingleBand> extends DemonstrationBase
		implements ThresholdControlPanel.Listener
{

	Class<T> imageType;

	DetectPolygonControlPanel controls = new DetectPolygonControlPanel(this);

	VisualizePanel guiImage;

	InputToBinary<T> inputToBinary;
	BinaryPolygonDetector<T> detector;

	BufferedImage original;
	BufferedImage work;
	T input;
	ImageUInt8 binary = new ImageUInt8(1,1);

	volatile boolean processRequested = false;
	volatile boolean threadRunning = false;
	final Object handShakeLock = new Object();

	public DetectBlackPolygonApp(List<String> examples , Class<T> imageType) {
		super(examples);

		this.imageType = imageType;

		guiImage = new VisualizePanel();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		createDetector();
	}

	private synchronized void createDetector() {

		Configuration configRefine = null;

		if( controls.refineType == PolygonRefineType.LINE ) {
			configRefine = controls.getConfigLine();
		} else if( controls.refineType == PolygonRefineType.CORNER ) {
			configRefine = controls.getConfigCorner();
		}
		controls.getConfigPolygon().refine = configRefine;

		detector = FactoryShapeDetector.polygon(controls.getConfigPolygon(),imageType);
		if( input == null )
			input = GeneralizedImageOps.createSingleBand(detector.getInputType(),1,1);
		imageThresholdUpdated();
	}

	/**
	 * Creates a thread to process the image.  if a thread is already running it's told to run again.
	 */
	private synchronized void processImage() {
		boolean spawnThread = false;

		synchronized (handShakeLock) {
			if( threadRunning ) {
				processRequested = true;
			} else {
				spawnThread = true;
				processRequested = true;
				threadRunning = true;
			}
		}

		if( spawnThread ) {
			synchronized (handShakeLock) {
				threadRunning = true;
			}
			new Thread() {
				@Override
				public void run() {
					// run until there are no more requestes to run
					while( true ) {
						boolean doStuff;
						synchronized (handShakeLock) {
							doStuff = processRequested;
							processRequested = false;
						}

						if( doStuff ) {
							inputToBinary.process(input, binary);
							detector.process(input, binary);
							viewUpdated();
						} else {
							break;
						}
					}
					synchronized (handShakeLock) {
						threadRunning = false;
					}
				}
			}.start();
		}
	}

	public synchronized void setInput( BufferedImage image ) {
		this.original = image;
		work = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_BGR);

		input.reshape(work.getWidth(), work.getHeight());
		binary.reshape(work.getWidth(), work.getHeight());

		ConvertBufferedImage.convertFrom(original, input, true);

		guiImage.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		processImage();
	}


	/**
	 * Called when how the data is visualized has changed
	 */
	public void viewUpdated() {
		BufferedImage active = null;
		if( controls.selectedView == 0 ) {
			active = original;
		} else if( controls.selectedView == 1 ) {
			VisualizeBinaryData.renderBinary(binary,false,work);
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0));
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,work.getWidth(),work.getHeight());
			active = work;
		}

		guiImage.setScale(controls.zoom);

		guiImage.setBufferedImage(active);
		guiImage.repaint();
	}

	public void configUpdate() {
		createDetector();
		// does process and render too
	}

	@Override
	public synchronized void imageThresholdUpdated() {

		ConfigThreshold config = controls.getThreshold().config;

		inputToBinary = FactoryThresholdBinary.threshold(config,imageType);
		processImage();
	}

	@Override
	public void openFile(File file) {
		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample(file.getPath()));
		if( buffered == null ) {
			// TODO see if it's a video instead
			System.err.println("Couldn't read "+file.getPath());
		} else {
			setInput(buffered);
		}
	}

	class WebcamThread extends Thread {

		boolean requestStop = false;
		boolean running = true;

//		Webcam

		@Override
		public void run() {
			while( !requestStop ) {

			}

		}
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			synchronized ( DetectBlackPolygonApp.this ) {
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				if (controls.bShowContour) {
					List<Contour> contours = detector.getAllContours();
					g2.setStroke(new BasicStroke(1));
					g2.setColor(Color.RED);
					VisualizeBinaryData.renderExternal(contours, scale, g2);
				}

				if (controls.bShowLines) {
					List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));
					for (Polygon2D_F64 p : polygons) {
						int red = 255 * ((p.size() - 3) % 4) / 3;
						int green = 255 * ((p.size()) % 5) / 4;
						int blue = 255 * ((p.size() + 2) % 6) / 5;

						g2.setColor(new Color(red, green, blue));

						VisualizeShapes.drawPolygon(p, true, scale, g2);
					}
				}

				if (controls.bShowCorners) {
					List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

					g2.setColor(Color.BLUE);
					g2.setStroke(new BasicStroke(1));
					for (Polygon2D_F64 p : polygons) {
						for (int i = 0; i < p.size(); i++) {
							Point2D_F64 c = p.get(i);
							VisualizeFeatures.drawCircle(g2, scale * c.x, scale * c.y, 5);
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<String>();
		examples.add("shapes/polygons01.jpg");
		examples.add("shapes/shapes01.png");
		examples.add("shapes/shapes02.png");
		examples.add("shapes/concave01.jpg");
		examples.add("fiducial/binary/image0000.jpg");

		DetectBlackPolygonApp app = new DetectBlackPolygonApp(examples,ImageFloat32.class);

		app.openFile(new File(examples.get(0)));

		ShowImages.showWindow(app,"Detect Black Polygons",true);
	}



}
