package no.uio.sequencing.reagent_scanning.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import no.uio.sequencing.reagent_scanning.ScanResultsSaver;

public class DummyTest {

	public static void main(String[] args) throws IOException, TesseractException {
		ArrayList<String> arx = new ArrayList<>();
		arx.add("1202012");
		arx.add("1202012");
		arx.add("1202012");
		System.out.println(new File( "." ).getCanonicalPath());
		ScanResultsSaver srss = new ScanResultsSaver(arx);
		
		File fooFile = new File("resources/test_image.png");
		BufferedImage foo = ImageIO.read(fooFile);
		ITesseract tess = new Tesseract();
		System.out.println(tess.doOCR(foo));
	}
	
}
