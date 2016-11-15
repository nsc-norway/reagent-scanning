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

	public final String ref;
	public Kit kit;
	public List<String> barcodes;
	private Date expiryDate;
	private boolean completed = false;
	long negCache = 0;
	private final WebTarget apiBase;
	
	
	private final static ITesseract tess = new Tesseract();
	// Here comes another millennium bug
	private final static Pattern datePattern = Pattern.compile("\\b(20[1-9]\\d/[01]\\d/[0123]\\d)\\b");
	private final static SimpleDateFormat dt1 = new SimpleDateFormat("yyyyy/mm/dd");
	private static final long CACHE_TIME = 5000;
	
	static {
		// Tesseract initialisation
		tess.setPageSegMode(6);
		tess.setTessVariable("load_system_dawg", "0");
		tess.setTessVariable("load_freq_dawg", "0");
		//tess.setTessVariable("tessedit_char_whitelist", "01234567890/ ");
	}
	
	public ScanResultsWorkflow(WebTarget apiBase, String ref) {
		this.apiBase = apiBase;
		this.ref = ref;
	}

	public void loadKit() throws KitNotFoundException {
		this.completed = false;
		if (System.currentTimeMillis() - negCache < CACHE_TIME)
			throw new KitNotFoundException("Kit " + ref + " not found.");

		try {
			kit = apiBase.path("kits").path(ref).request(MediaType.APPLICATION_JSON_TYPE).get(Kit.class);
		}
		catch (NotFoundException e) {
			negCache = System.currentTimeMillis();
			throw new KitNotFoundException("Kit " + ref + " not found.");
		}
	}
	
	public void setBarcodes(List<String> barcodes) throws InvalidBarcodeSetException, IOException {
		this.barcodes = barcodes;
		this.completed = false;
		
		if (kit.requestLotName && barcodes.size() != 3) {
			throw new InvalidBarcodeSetException("Need three barcodes for kit " + kit.ref + ".");
		}
		else if (!kit.requestLotName && barcodes.size() != 3) {
			throw new InvalidBarcodeSetException("Need two barcodes for kit " + kit.ref + ".");
		}

	}
	
	public void scanExpiryDate(BufferedImage image) throws DateParsingException {
		try {
			String text = tess.doOCR(image);
			Matcher m = datePattern.matcher(text);
			if (m.find()) {
				setExpiryDate(m.group(1));
			}
			else {
				throw new DateParsingException();
			}
		} catch (TesseractException | ParseException e) {
			throw new DateParsingException();
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
