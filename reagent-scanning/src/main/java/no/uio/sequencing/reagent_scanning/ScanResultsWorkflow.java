package no.uio.sequencing.reagent_scanning;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class ScanResultsWorkflow {

	public final Kit kit;
	public final List<String> barcodes;
	private Date expiryDate;
	private boolean completed = false;
	private final WebTarget apiBase;
	
	
	private final static ITesseract tess = new Tesseract();
	// Here comes another millennium bug
	private final static Pattern datePattern = Pattern.compile("\\b(20[1-9]\\d/[01]\\d/[0123]\\d)\\b");
	private final static SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy/mm/dd");
	
	static {
		// Tesseract initialisation
		tess.setPageSegMode(6);
		tess.setTessVariable("load_system_dawg", "0");
		tess.setTessVariable("load_freq_dawg", "0");
		//tess.setTessVariable("tessedit_char_whitelist", "01234567890/ ");
	}
	
	public ScanResultsWorkflow(WebTarget apiBase, List<String> barcodes) throws IOException {
		this.barcodes = barcodes;
		this.apiBase = apiBase;
		if (barcodes.size() > 1) {
			kit = getKit(barcodes.get(0));
		}
		else {
			kit = null;
		}
	}

	private Kit getKit(String ref) {
		try {
			return apiBase.path("kits").path(ref).request(MediaType.APPLICATION_JSON_TYPE).get(Kit.class);
		} catch (NotFoundException e) {
			return null;
		}
	}
	
	public boolean isValidBarcodes() {
		if (kit == null || barcodes == null) return false;
		if (kit.requestLotName) return barcodes.size() == 3;
		else return barcodes.size() == 2;
	}
	
	public boolean scanExpiryDate(BufferedImage image) {
		try {
			String text = tess.doOCR(image);

			JOptionPane.showMessageDialog(null, text);
			Matcher m = datePattern.matcher(text);
			if (m.find()) {
				setExpiryDate(m.group(1));
				return true;
			}
			else {
				return false;
			}
		} catch (TesseractException | ParseException e) {
			return false;
		}
	}
	
	public void setExpiryDate(String dateString) throws ParseException {
		this.expiryDate = dt1.parse(dateString);
	}
	
	public boolean valiDate() {
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DATE, -1);
		Calendar twoYearsInFuture = Calendar.getInstance();
		twoYearsInFuture.add(Calendar.YEAR, 2);
		return (expiryDate.after(yesterday.getTime()) && expiryDate.before(twoYearsInFuture.getTime()));
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public String getExpiryDateString() {
		return dt1.format(expiryDate);
	}

	public boolean isCompleted() {
		return completed;
	}

	public void save() throws IOException {
		
	}
	
}
