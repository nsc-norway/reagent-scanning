package no.uio.sequencing.scanning_experiments;

import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

public class LocalThresholdBinarizer extends HybridBinarizer2 {

	public LocalThresholdBinarizer(LuminanceSource source) {
		super(source);
	}
	
	public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
	    LuminanceSource source = getLuminanceSource();
	    int width = source.getWidth();
	    if (row == null || row.getSize() < width) {
	      row = new BitArray(width);
	    } else {
	      row.clear();
	    }
	    for (int x=0; x<width; ++x) {
	    	if (getBlackMatrix().get(x, y)) {
	    		row.set(x);
	    	}
	    }
	    return row;
	}
}
