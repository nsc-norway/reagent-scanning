package no.uio.sequencing.reagent_scanning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.IOUtils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamPanel;
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

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;


public class WebcamScanner extends JFrame implements Runnable, WebcamImageTransformer {

	private static final long serialVersionUID = 6441489127408381878L;

	private static final long SCAN_PAUSE_TIME = 1000;

	private static final long ERROR_DISPLAY_TIME = 5000;

	private Webcam webcam = null;
	private JTextField scanRef;
	private JTextField scanLot;
	private JTextField scanRgt;
	private JTextField scanDate;
	private JLabel statusLabel;
	
	long prevScanTime, lastErrorTime;
	
	ScanResultsWorkflow workflow = null;
	
	private Set<String> scanPauseSet;
	
	private WebTarget apiBaseTarget;

	private JTextArea errorTextArea;

	private JLabel kitNameValue;

	private JPanel topRowPanel;

	private JCheckBox scanEnableCheckbox;

	private JPanel errorPanel;
	
	enum Beep {
		SUCCESSS,
		INFO,
		FAIL
	}
	

	public WebcamScanner(String apiUrl) {
		super();
		
		scanPauseSet = new HashSet<>();
		
		setTitle("Scanner application");
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
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		//JPanel webcamPanel = new WebcamPanel(webcam);
		JPanel webcamPanel = new JPanel();
		webcamPanel.setPreferredSize(res);
		panel.add(webcamPanel, BorderLayout.CENTER);
		
		JPanel textPanel = new JPanel();
		panel.add(textPanel, BorderLayout.EAST);
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		
		JPanel scanningPanel = new JPanel();
		scanningPanel.setAlignmentY(0.0f);
		textPanel.add(scanningPanel);
		scanningPanel.setLayout(new BoxLayout(scanningPanel, BoxLayout.Y_AXIS));
		
		topRowPanel = new JPanel();
		topRowPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
		topRowPanel.setBackground(new Color(230, 230, 250));
		scanningPanel.add(topRowPanel);
		topRowPanel.setLayout(new GridLayout(0, 2, 0, 0));
		
		statusLabel = new JLabel("Initialising...");
		topRowPanel.add(statusLabel);
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		
		scanEnableCheckbox = new JCheckBox("scan");
		scanEnableCheckbox.setHorizontalAlignment(SwingConstants.TRAILING);
		scanEnableCheckbox.setSelected(true);
		topRowPanel.add(scanEnableCheckbox);
		
		JPanel scanBox = new JPanel();
		scanBox.setBorder(new EmptyBorder(4, 4, 4, 4));
		scanningPanel.add(scanBox);
		scanBox.setBackground(UIManager.getColor("Panel.background"));
		scanBox.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblKitName = new JLabel("KIT");
		scanBox.add(lblKitName);
		
		kitNameValue = new JLabel("");
		kitNameValue.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		scanBox.add(kitNameValue);
		
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
		
		JLabel lblNewLabel = new JLabel(" ");
		scanningPanel.add(lblNewLabel);
		
		JPanel padding = new JPanel();
		padding.setBorder(new EmptyBorder(10, 10, 10, 10));
		scanningPanel.add(padding);
		padding.setLayout(new BorderLayout(0, 0));
		
		errorPanel = new JPanel();
		errorPanel.setBackground(Color.PINK);
		errorPanel.setVisible(false);
		errorPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
		padding.add(errorPanel, BorderLayout.NORTH);
		errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.X_AXIS));
		
		errorTextArea = new JTextArea();
		errorPanel.add(errorTextArea);
		errorTextArea.setLineWrap(true);
		errorTextArea.setWrapStyleWord(true);
		errorTextArea.setEditable(false);
		errorTextArea.setText("");
		errorTextArea.setBackground(Color.PINK);
		if (webcam != null) {
			webcamPanel = new WebcamPanel(webcam);
		}

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		// Full screen?
		//setExtendedState(JFrame.MAXIMIZED_BOTH); 
		setVisible(true);

		Client client = ClientBuilder.newClient();		
		apiBaseTarget = client.target(apiUrl);
		
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

		statusLabel.setText("Scanning...");
		topRowPanel.setBackground(new Color(230, 230, 250));
		while (scanEnableCheckbox.isSelected()) {
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

			long now = System.currentTimeMillis();
			// Create a list "data" sorted by y-coord
			Map<Float,String> resultMap = new TreeMap<>();
			for (Result r : results) {
				resultMap.put(r.getResultPoints()[0].getY(), r.getText());
			}
			List<String> data = new ArrayList<String>(resultMap.values());
			if (!scanPauseSet.isEmpty() && scanPauseSet.containsAll(data) 
					&& (now - prevScanTime < SCAN_PAUSE_TIME || data.isEmpty())) {
				continue;
			}
			scanPauseSet.clear();
			
			final JTextField[] destination = {scanRef, scanLot, scanRgt};

			int pointer = 0;
			for (String text : resultMap.values()) {
				if (pointer < destination.length) {
					destination[pointer++].setText(text);
				}
			}
			for (; pointer < destination.length; ++pointer) {
				destination[pointer].setText("");
			}
			if (data.isEmpty()) {
				kitNameValue.setText("");
			}
			scanDate.setText("");
			scanDate.setBackground(UIManager.getColor("TextArea.background"));

			if (now - lastErrorTime > ERROR_DISPLAY_TIME && results.length >= 1) {
				errorPanel.setVisible(false);
				//errorTextArea.setText("");
				//errorTextArea.setBackground(UIManager.getColor("Panel.background"));
			}
			
			if (data.size() >= 2) {
				processResult(image, data);
			}
			
		} 
		topRowPanel.setBackground(new Color(230, 230, 250));
		statusLabel.setText("Ready");
	}

	private void processResult(BufferedImage image, List<String> data) {
		try { // Globally catch IO exceptions (communication error)
			if (workflow == null || !workflow.ref.equals(data.get(0))) {
				statusLabel.setText("Kit lookup...");
				workflow = new ScanResultsWorkflow(apiBaseTarget, data.get(0));
			}
			workflow.loadKit();
			kitNameValue.setText(workflow.kit.name);
			workflow.setBarcodes(data);
			
			topRowPanel.setBackground(new Color(250, 250, 230));
			beep(Beep.INFO);
			statusLabel.setText("Reading date...");
			workflow.scanExpiryDate(image);
			scanDate.setText(workflow.getExpiryDateString());
			if (workflow.valiDate()) {
				statusLabel.setText("Saving...");
				try {
					workflow.save();
					scanRgt.setText(workflow.lotUniqueId);
					topRowPanel.setBackground(new Color(230, 250, 230));
					statusLabel.setText("✓ Lot saved");
					beep(Beep.SUCCESSS);
				} catch (BadRequestException e) {
					String message = "UNKNOWN ERROR!";
					if (e.getResponse().getEntity() instanceof InputStream) {
						StringWriter writer = new StringWriter();
						IOUtils.copy((InputStream)e.getResponse().getEntity(), writer, "UTF-8");
						message = writer.toString();
					}
					showError(message);
				}
				// This enables a pause between scan attempts after completion
				prevScanTime = System.currentTimeMillis();
				scanPauseSet = new HashSet<String>(data);
			}
			else {
				scanDate.setBackground(Color.ORANGE);
				showError("Unlikely date: in the past or too far in the future.");
			}
		}
		catch (IOException | ProcessingException | ClientErrorException e) {
			// Catches communication errors and unexpected HTTP response codes 
			beep(Beep.FAIL);
			JOptionPane.showMessageDialog(null, "Input/Output error while communicating with the backend:\n\n" + e.toString());
		} catch (InvalidBarcodeSetException e) {
		} catch (KitNotFoundException e) {
			kitNameValue.setText("");
			showError(e.getMessage());
			// This enables a pause between scan attempts after kit not found, for these exact data
			prevScanTime = System.currentTimeMillis();
			scanPauseSet = new HashSet<String>(data);
		} catch (DateParsingException e) {
			String message = "Unable to find the expiry date.";
			showError(message);
		}
		if (!workflow.isCompleted()) {
			statusLabel.setText("Scanning...");
			topRowPanel.setBackground(new Color(230, 230, 250));
		}
	}

	private void showError(String message) {
		beep(Beep.FAIL);
		errorPanel.setVisible(true);
		errorTextArea.setText(message);
		//errorTextArea.setBackground(Color.PINK);
		lastErrorTime = System.currentTimeMillis();
	}
	
	public static void main(String[] args) {
		if (args.length >= 1) {
			new WebcamScanner(args[0]);
		}
		else {
			new WebcamScanner("http://localhost:5001/");
			//JOptionPane.showMessageDialog(null, "Error: WebcamScanner must be called with the base API URL as a command line argument.");
		}
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
	
	void beep(Beep beep) {
		InputStream in;
		try {
			if (beep == Beep.SUCCESSS) {
				in = WebcamScanner.class.getResourceAsStream("resources/beep3.wav");
			}
			else if (beep == Beep.INFO) {
				in = WebcamScanner.class.getResourceAsStream("resources/beep2.wav");
			}
			else if (beep == Beep.FAIL) {
				in = WebcamScanner.class.getResourceAsStream("resources/beep1.wav");
			}
			else {
				return;
			}
			AudioStream as = new AudioStream(in);
			AudioPlayer.player.start(as);
		} catch (Exception e) { // Swallow all exceptions, beep is not important
			e.printStackTrace();
		}
	}
}
