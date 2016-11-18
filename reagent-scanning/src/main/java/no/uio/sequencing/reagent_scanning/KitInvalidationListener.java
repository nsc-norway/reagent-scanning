package no.uio.sequencing.reagent_scanning;

public interface KitInvalidationListener {

	void kitServerStatusChanged(String ref);
	
}
