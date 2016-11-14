package no.uio.sequencing.reagent_scanning.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;


public class ImageBarcodeTest {

	public static void main(String[] args) throws IOException {
		final MultiFormatReader mfReader = new MultiFormatReader();
		final List<BarcodeFormat> allowedFormats = new ArrayList<>();
		allowedFormats.add(BarcodeFormat.CODE_128);
		allowedFormats.add(BarcodeFormat.DATA_MATRIX);
		final HashMap<DecodeHintType, Object> hints = new HashMap<>();
		hints.put(DecodeHintType.POSSIBLE_FORMATS, allowedFormats);
		hints.put(DecodeHintType.TRY_HARDER, true);
		mfReader.setHints(hints);
		GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(mfReader);
		
		Result [] results = {};
		for (String path : args) {
			System.out.println(path);
			long startTime = System.currentTimeMillis();
			File myFile = new File(path);
			BufferedImage image = ImageIO.read(myFile);
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			try {
				results = reader.decodeMultiple(bitmap);
				System.out.println("There are " + results.length + " results");
				for (Result r: results) {
					System.out.println("Read barcode: " + r.getText());
				}
			} catch (NotFoundException e) {
				System.out.println("Barcode not found");
			}
			long dt = System.currentTimeMillis() - startTime;
			System.out.println("In " + dt + " milliseconds");;
		}
	}
}
