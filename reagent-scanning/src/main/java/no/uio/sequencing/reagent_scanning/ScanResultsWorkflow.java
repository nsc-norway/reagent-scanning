package no.uio.sequencing.reagent_scanning;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class ScanResultsWorkflow {

	public final String ref, group;
	public String lotNumber, uniqueId;
	public Kit kit;
	public Lot lot;
	private Date expiryDate;
	private boolean completed = false;
	long negCache = 0;
	private final WebTarget apiBase;
	
	private final static SimpleDateFormat dt1 = new SimpleDateFormat("yyyy/MM/dd");
	private static final long CACHE_TIME = 10000;
		
	public ScanResultsWorkflow(WebTarget apiBase, String ref, String group) {
		this.apiBase = apiBase;
		this.ref = ref;
		this.group = group;
	}

	public void loadKit() throws KitNotFoundException {
		this.completed = false;
		if (System.currentTimeMillis() - negCache < CACHE_TIME)
			throw new KitNotFoundException("Kit " + ref + " not found.");

		if (kit == null) {
			try {
				kit = apiBase.path("kits").path(ref).request(MediaType.APPLICATION_JSON_TYPE).get(Kit.class);
			}
			catch (NotFoundException e) {
				negCache = System.currentTimeMillis();
				throw new KitNotFoundException("Kit " + ref + " not found.");
			}
		}
	}
	
	public void setBarcodes(List<String> barcodes) throws InvalidBarcodeSetException, IOException {
		this.completed = false;
		
		if (kit.hasUniqueId) {
			if (barcodes.size() == 3) {
				lotNumber = barcodes.get(1);
				uniqueId = barcodes.get(2);
			}
			else {
				throw new InvalidBarcodeSetException("Need three barcodes for kit " + kit.ref + ".");
			}
		}
		else {
			if (barcodes.size() == 2) {
				lotNumber = barcodes.get(1);
			}
			else {
				throw new InvalidBarcodeSetException("Need two barcodes for kit " + kit.ref + ".");
			}
		}
	}
	
	public void setExpiryDate(String dateString) throws ParseException {
		this.expiryDate = dt1.parse(dateString);
	}
	
	public boolean valiDate() {
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DATE, -1);
		Calendar futureMaxValid = Calendar.getInstance();
		futureMaxValid.add(Calendar.YEAR, 10);
		return (expiryDate != null && expiryDate.after(yesterday.getTime()) && expiryDate.before(futureMaxValid.getTime()));
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
	
	public boolean tryGetLotDate() {
		try {
			lot = apiBase.path("lots").path(ref).path(lotNumber).path(group)
					.request(MediaType.APPLICATION_JSON_TYPE)
					.get(Lot.class);
			if (lot.expiryDate == null) {
				return false;
			}
			setExpiryDate(lot.expiryDate.replace('-', '/'));
			return true;
		} catch (NotFoundException | ParseException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void save() throws IOException {
		if (kit.hasUniqueId) {
			lot = new Lot();
			lot.lotnumber = lotNumber;
			lot.uniqueId = uniqueId;
		}
		else if (lot != null)  {
			lot = new Lot();
			lot.lotnumber = lotNumber;
		}
		lot.expiryDate = getExpiryDateString().replace('/','-');
		lot.known = true;
		lot.ref = ref;
		lot = apiBase.path("lots").path(ref).path(lot.lotnumber).path(group)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(lot), Lot.class);
		completed = true;
	}

	public void editLot() {
		lot = apiBase.path("editlot").path(lot.limsId)
			.request(MediaType.APPLICATION_JSON_TYPE)
			.put(Entity.json(lot), Lot.class);
	}

	public void resetLot() {
		lotNumber = null;
		uniqueId = null;
		lot = null;
		expiryDate = null;
	}
	
}
