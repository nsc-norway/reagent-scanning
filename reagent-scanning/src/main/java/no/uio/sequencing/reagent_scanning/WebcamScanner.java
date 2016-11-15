package no.uio.sequencing.reagent_scanning;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
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

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSplitPane;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;

import javax.swing.JTextField;
import java.awt.Component;
import javax.swing.UIManager;


public class WebcamScanner extends JFrame implements Runnable, WebcamImageTransformer {

	private static final long serialVersionUID = 6441489127408381878L;

	private static final long EQUAL_SCAN_DEBOUNCE = 500;

	private Webcam webcam = null;
	private JTextField scanRef;
	private JTextField scanLot;
	private JTextField scanRgt;
	private JTextField scanDate;
	
	private String prevRef, prevLot, prevRgt;
	long prevScanTime;
	ScanResultsWorkflow workflow = null;
	

	public WebcamScanner() {
		super();
		setTitle("Scanner (moose) application");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Dimension res = new Dimension(1280, 1024);
		webcam = Webcam.getDefault();
		if (webcam == null) {
			JOptionPane.showMessageDialog(null, "Error: No webcam detected");
			System.exit(1);
		}
		else {
			webcam.setCustomViewSizes(new Dimension[] {res});
			webcam.setViewSize(res);
			webcam.setImageTransformer(this);
		}
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.5);
		
		JPanel textPanel = new JPanel();
		splitPane.setRightComponent(textPanel);
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		
		JPanel scanningPanel = new JPanel();
		scanningPanel.setAlignmentY(0.0f);
		scanningPanel.setBackground(new Color(230, 230, 250));
		textPanel.add(scanningPanel);
		scanningPanel.setLayout(new BoxLayout(scanningPanel, BoxLayout.Y_AXIS));
		
		JLabel statusLabel = new JLabel("[status here]");
		statusLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		scanningPanel.add(statusLabel);
		
		JPanel scanBox = new JPanel();
		scanningPanel.add(scanBox);
		scanBox.setBackground(UIManager.getColor("Panel.background"));
		scanBox.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblRefBox = new JLabel("REF");
		scanBox.add(lblRefBox);
		
		scanRef = new JTextField();
		scanBox.add(scanRef);
		scanRef.setColumns(10);
		
		JLabel lblLot = new JLabel("LOT");
		scanBox.add(lblLot);
		
		scanLot = new JTextField();
		scanBox.add(scanLot);
		scanLot.setColumns(10);
		
		JLabel lblRgt = new JLabel("RGT");
		scanBox.add(lblRgt);
		
		scanRgt = new JTextField();
		scanBox.add(scanRgt);
		scanRgt.setColumns(10);
		
		JLabel lblDate = new JLabel("Date");
		scanBox.add(lblDate);
		
		scanDate = new JTextField();
		scanBox.add(scanDate);
		scanDate.setColumns(10);
		
		JPanel resultPanel = new JPanel();
		textPanel.add(resultPanel);
		getContentPane().add(splitPane);
		
		JPanel webcamPanel = new JPanel();
		if (webcam != null) {
			webcamPanel = new WebcamPanel(webcam);
		}
		webcamPanel.setPreferredSize(WebcamResolution.VGA.getSize());
		splitPane.setLeftComponent(webcamPanel);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	public void run() {

		final MultiFormatReader mfReader = new MultiFormatReader();
		final List<BarcodeFormat> allowedFormats = new ArrayList<>();
		allowedFormats.add(BarcodeFormat.CODE_128);
		allowedFormats.add(BarcodeFormat.DATA_MATRIX);
		final HashMap<DecodeHintType, Object> hints = new HashMap<>();
		hints.put(DecodeHintType.POSSIBLE_FORMATS, allowedFormats);
		hints.put(DecodeHintType.TRY_HARDER, true);
		mfReader.setHints(hints);
		GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(mfReader);
		
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Result [] results = {};
			BufferedImage image = null, mirrorImage = null;

			if (webcam != null && webcam.isOpen()) {

				if ((mirrorImage = webcam.getImage()) == null) {
					continue;
				}
				image = transform(mirrorImage);
				/*
				 * Debug: Read from local file instead
				 * try {
					image = ImageIO.read(new File("resources/test_image.png"));
				} catch (IOException e1) {
				}*/
				
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					results = reader.decodeMultiple(bitmap);
				} catch (NotFoundException e) {
					// fall thru, it means there is no QR code in image
				}
			}

			final JTextField[] destination = {scanRef, scanLot, scanRgt};
			Map<Float,String> resultMap = new TreeMap<>();
			for (Result r : results) {
				resultMap.put(r.getResultPoints()[0].getY(), r.getText());
			}
			int pointer = 0;
			for (String text : resultMap.values()) {
				if (pointer < destination.length) {
					destination[pointer++].setText(text);
				}
			}
			for (; pointer < destination.length; ++pointer) {
				destination[pointer].setText("");
			}
			scanDate.setText("");
			List<String> data = new ArrayList<String>(resultMap.values());
			workflow = new ScanResultsWorkflow(data);
			if (workflow.isValid()) {
				scanDate.setText("Processing...");
				if (workflow.scanExpiryDate(image)) {
					scanDate.setText(workflow.getExpiryDate());
					
				}
				else {
					scanDate.setText("");
				}
			}
		} while (true);
	}
	
	

	public static void main(String[] args) {
		new WebcamScanner();
	}

	@Override
	public BufferedImage transform(BufferedImage image) {
		// Mirror image
		AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(-1, 1));
        at.concatenate(AffineTransform.getTranslateInstance(-image.getWidth(), 0));
        BufferedImage newImage = new BufferedImage(
                image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.transform(at);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
	}
}
