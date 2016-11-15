package no.uio.sequencing.reagent_scanning;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class ScanResultsWorkflow {

	public final KitType kit;
	public final List<String> barcodes;
	private String expiryDate;
	private final static ITesseract tess = new Tesseract();
	private final Pattern datePattern = Pattern.compile("[^/\\d](\\d{4}/\\d{2}/\\d{2})[^/\\d]");
	
	public ScanResultsWorkflow(List<String> barcodes) {
		this.barcodes = barcodes;
		tess.setPageSegMode(6);
		tess.setTessVariable("load_system_dawg", "0");
		tess.setTessVariable("load_freq_dawg", "0");
		//tess.setTessVariable("tessedit_char_whitelist", "01234567890/ ");
		if (barcodes.size() > 1) {
			kit = getKit(barcodes.get(0));
		}
		else {
			kit = null;
		}
	}

	private KitType getKit(String ref) {
		return new KitType();
	}
	
	public boolean isValid() {
		if (kit == null || barcodes == null) return false;
		if (kit.withUniqueId) return barcodes.size() == 3;
		else return barcodes.size() == 2;
	}
	
	public boolean scanExpiryDate(BufferedImage image) {
		try {
			String text = tess.doOCR(image);
			Matcher m = datePattern.matcher(text);
			if (m.find()) {
				this.expiryDate = m.group(1);
				return true;
			}
			else {
				return false;
			}
		} catch (TesseractException e) {
			return false;
		}
	}
	
	public String getExpiryDate() {
		return expiryDate;
	}
	
}
