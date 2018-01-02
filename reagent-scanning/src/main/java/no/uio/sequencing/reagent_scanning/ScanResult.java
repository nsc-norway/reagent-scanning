package no.uio.sequencing.reagent_scanning;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Scan result type. This class describes what was scanned. Create properties here
 * to control how the ScanResultsWorkflow should process the list of results. 
 */
public class ScanResult {

	// data contains a list of bar codes, in a specified order -- REF, LOT, RGT.
	// (this is somewhat tied to a certain vendor's kits, and could be changed in the future)
	public List<String> data;
	public Date expiryDate;
	public boolean useKitNameAsLotName = false; // Some people want this. For now it's tied to 
												// the type of bar code. Should it instead be set
	 											// on the backend in kits.yaml?
	
	public ScanResult() {
		this.data = new ArrayList<String>();
	}
	
	public ScanResult(List<String> data) {
		this.data = data;
	}
	
}
