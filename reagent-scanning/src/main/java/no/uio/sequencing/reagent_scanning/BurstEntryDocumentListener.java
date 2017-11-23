package no.uio.sequencing.reagent_scanning;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class BurstEntryDocumentListener implements DocumentListener {

	public final static long DELAY = 50;
	
	volatile long lastEntryTime;
	volatile boolean running = false, waiting = false;
	volatile int lengthScanned = 0;
	final int lengthThreshold;
	
	public BurstEntryDocumentListener(int lengthThreshold) {
		this.lengthThreshold = lengthThreshold;
	}
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		update(e.getLength());
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		update(0);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		update(0);
	}
	
	private synchronized void update(int incr) {
		lastEntryTime = System.currentTimeMillis();
		if (incr >= 0)
			lengthScanned += incr;
		if (!running && !waiting) {
			new Thread() {
				@Override
				public void run() {
					backgroundWaitingThread();
				}
			}.start();
		}
		waiting = true;
	}
	
	void backgroundWaitingThread() {
		boolean loopflag = true;
		while (loopflag) {
			long waitedTime = 0;
			synchronized (this) {
				do {
					try {
						wait(DELAY - waitedTime);
					} catch (InterruptedException e) {
						return;
					}
					waitedTime = System.currentTimeMillis() - lastEntryTime;
				} while (waitedTime < DELAY);
				waiting = false;
				if (lengthScanned < lengthThreshold) {
					lengthScanned = 0;
					return;
				}
				running = true;
				lengthScanned = 0;
			}
			try {
				burstEntryDetected();
			}
			finally {
				running = false;
			}
			synchronized (this) {
				running = false;
				loopflag = waiting;
			}
		}
	}
	
	public abstract void burstEntryDetected();
	
}
