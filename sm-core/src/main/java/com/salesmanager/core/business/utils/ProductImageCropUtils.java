package com.salesmanager.core.business.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductImageCropUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageCropUtils.class);
	
	private boolean cropeable = true;

    private int cropeBaseline = 0;

	private int getCropeBaseline() {
		return cropeBaseline;
	}



	private double cropAreaWidth = 0;
	private double cropAreaHeight = 0;

    private BufferedImage originalFile = null;



	public ProductImageCropUtils(BufferedImage file, int largeImageWidth, int largeImageHeight) {
		
		
	
			try {


				this.originalFile = file;

				int width = originalFile.getWidth();
				int height = originalFile.getHeight();
				determineCropeable(width, largeImageWidth, height, largeImageHeight);

				determineCropArea(width, largeImageWidth, height, largeImageHeight);

			} catch (Exception e) {
				LOGGER.error("Image Utils error in constructor", e);
			}
		


		
		
		
	}
	
	
	private void determineCropeable(int width, int specificationsWidth,
			int height, int specificationsHeight) {
		int y = height - specificationsHeight;
		int x = width - specificationsWidth;

		if (x < 0 || y < 0) {
			setCropeable(false);
		}

		if (x == 0 && y == 0) {
			setCropeable(false);
		}


		if ((height % specificationsHeight) == 0 && (width % specificationsWidth) == 0) {
			setCropeable(false);
		}


	}


	private void determineCropArea(int width, int specificationsWidth,
			int height, int specificationsHeight) {

		cropAreaWidth = specificationsWidth;
		cropAreaHeight = specificationsHeight;


		double factorWidth = new Integer(width).doubleValue() / new Integer(specificationsWidth).doubleValue();
		double factorHeight = new Integer(height).doubleValue() / new Integer(specificationsHeight).doubleValue();

		double factor = factorWidth;

		if (factorWidth > factorHeight) {
			factor = factorHeight;
		}

		double w = factor * specificationsWidth;
		double h = factor * specificationsHeight;

		if (w == h) {
			setCropeable(false);
		}


		cropAreaWidth = w;

		if (cropAreaWidth > width)
			cropAreaWidth = width;

		cropAreaHeight = h;

		if (cropAreaHeight > height)
			cropAreaHeight = height;

	}
	
	
	public File getCroppedImage(File originalFile, int x1, int y1, int width,
			int height) throws Exception {
		
		if(!this.cropeable) {
			return originalFile;
		}

		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String contentType = fileNameMap.getContentTypeFor(originalFile.getName());
		
		String extension = contentType.substring(contentType.indexOf("/"),contentType.length());
		
		BufferedImage image = ImageIO.read(originalFile);
		BufferedImage out = image.getSubimage(x1, y1, width, height);
		File tempFile = File.createTempFile("temp", "." + extension );
		tempFile.deleteOnExit();
		ImageIO.write(out, extension, tempFile);
		return tempFile;
	}
	
	public BufferedImage getCroppedImage() throws IOException {


		Rectangle goal = new Rectangle((int) this.getCropAreaWidth(), (int) this.getCropAreaHeight());

		Rectangle clip = goal.intersection(new Rectangle(originalFile.getWidth(), originalFile.getHeight()));

		BufferedImage clippedImg = originalFile.getSubimage(clip.x, clip.y, clip.width, clip.height);


		return clippedImg;


	}
	


	
	public double getCropAreaWidth() {
		return cropAreaWidth;
	}

	public void setCropAreaWidth(int cropAreaWidth) {
		this.cropAreaWidth = cropAreaWidth;
	}

	public double getCropAreaHeight() {
		return cropAreaHeight;
	}

	public void setCropAreaHeight(int cropAreaHeight) {
		this.cropAreaHeight = cropAreaHeight;
	}

	public void setCropeable(boolean cropeable) {
		this.cropeable = cropeable;
	}

	public boolean isCropeable() {
		return cropeable;
	}



}
