package no.uio.sequencing.scanning_experiments;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.oned.Code128Reader;

/**
 * Hello world!
 *
 */
public class Test 
{
    public static void main( String[] args ) 
    {
    	try {

    		final Code128Reader codeReader = new Code128Reader();
    		final HashMap<DecodeHintType, Object> hints = new HashMap<>();
    		hints.put(DecodeHintType.TRY_HARDER, true);
    		GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(codeReader);
    		
	    	File[] imageFiles = (new File("resources/")).listFiles();
	    	for (File file: imageFiles) {
	    		if (!file.getName().endsWith(".png"))
	    			continue;
				BufferedImage image = ImageIO.read(file);
			
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				HybridBinarizer biner = new HybridBinarizer(source);
				BinaryBitmap bitmap = new BinaryBitmap(biner);
	
				BufferedImage result = binaryBitmapToBufferedImage(bitmap, false);
				ImageIO.write(result, "PNG", new File("results/array-" + file.getName()));

				BufferedImage matrix = binaryBitmapToBufferedImage(bitmap, true);
				ImageIO.write(matrix, "PNG", new File("results/matrix-" + file.getName()));
				
				BinaryBitmap histBmp = new BinaryBitmap(new GlobalHistogramBinarizer(source));
				BufferedImage hist = binaryBitmapToBufferedImage(histBmp, true);
				ImageIO.write(hist, "PNG", new File("results/hist-" + file.getName()));
				
				try {
					BinaryBitmap matrixBmp = new BinaryBitmap(new LocalThresholdBinarizer(source));
					Result[] results = reader.decodeMultiple(matrixBmp, hints);
					System.out.println("** " + file.getName() + " **");
					for (Result r: results) {
						System.out.println(r.getText());
					}
				} catch (NotFoundException e) {
					System.out.println("-- Nothing found for " + file.getName() + " --");
				}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static BufferedImage binaryBitmapToBufferedImage(BinaryBitmap bb, boolean useMatrixMode) throws NotFoundException {
    	BufferedImage img = new BufferedImage(bb.getWidth(), bb.getHeight(), BufferedImage.TYPE_INT_RGB);
    	BitMatrix matrix = null;
    	if (useMatrixMode) {
    		matrix = bb.getBlackMatrix();
    	}
    	BitArray row = new BitArray(bb.getWidth());
		for (int y=0; y<bb.getHeight(); ++y) {
			if (!useMatrixMode) {
    			try {
    				row = bb.getBlackRow(y, null);
				} catch(NotFoundException e) {
		        	for (int x=0; x<bb.getWidth(); ++x) {
		        		img.setRGB(x, y, 0xFF);
		        	}
		        	continue;
				}
			}
        	for (int x=0; x<bb.getWidth(); ++x) {
        		if (useMatrixMode) {
	    			if (matrix.get(x, y)) 
	    				img.setRGB(x, y, 0);
	    			else
	    				img.setRGB(x, y, 0xFFFFFF);
        		}
        		else {
        			if (row.get(x))
	    				img.setRGB(x, y, 0);
        			else
	    				img.setRGB(x, y, 0xFFFFFF);
        		}
    		}
    	}
    	return img;
    }
}
