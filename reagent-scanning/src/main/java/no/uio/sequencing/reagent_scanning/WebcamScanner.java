package no.uio.sequencing.reagent_scanning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.oned.Code128Reader;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;


public class WebcamScanner extends JFrame implements Runnable, KitInvalidationListener {

	private static final long serialVersionUID = 6441489127408381878L;

	private static final long SCAN_PAUSE_TIME = 5000;

	private static final long ERROR_DISPLAY_TIME = 5000;

	private Webcam webcam = null;
	private WebcamPanel webcamPanelRef;
	private JTextField scanRef;
	private JTextField scanLot;
	private JTextField scanRgt;
	private JTextField scanDate;
	private JLabel statusLabel;
	
	long prevScanTime, lastErrorTime;
	private boolean returnToScanning = false;
	
	ScanResultsWorkflow workflow = null;
	
	private Set<String> scanPauseSet;
	private WebTarget apiBaseTarget;
	private JTextArea errorTextArea;
	private JLabel kitNameValue;
	private JLabel lblGroupValue;
	private JPanel topRowPanel;
	private JCheckBox scanEnableCheckbox;
	private JPanel errorPanel;

	private JButton btnAdd;

	private JButton btnEdit;

	private NewKitDialog newKitDialog;
	
	enum Beep {
		SUCCESSS,
		INFO,
		FAIL
	}
	
	private int dots = 0;

	public WebcamScanner(String apiUrl, int x, int y, int icam) {
		super();
		
		scanPauseSet = new HashSet<>();
		
		setTitle("NSC Item Scanning");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Dimension res = new Dimension(x, y);
		if (icam == -1) {
			webcam = Webcam.getDefault();
		}
		else {
			webcam = Webcam.getWebcams().get(icam);
		}
		if (webcam == null) {
			JOptionPane.showMessageDialog(null, "Error: No webcam detected");
			System.exit(1);
		}
		else {
			webcam.setCustomViewSizes(new Dimension[] {res});
			webcam.setViewSize(res);
		}
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel webcamPanel = new JPanel();
		if (webcam != null) {
			webcamPanelRef = new WebcamPanel(webcam);
			webcamPanel = webcamPanelRef;
			webcamPanelRef.setFitArea(true);
		}
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		webcamPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		webcamPanel.setPreferredSize(res);
		panel.add(webcamPanel, BorderLayout.CENTER);
		if (res.getHeight() > screenSize.getHeight() * 0.9) {
			webcamPanel.setPreferredSize(
					new Dimension(
							(int)(screenSize.getWidth()*0.8f),
							(int)(screenSize.getHeight()*0.8f))
					);
		}
		
		JPanel textPanel = new JPanel();
		panel.add(textPanel, BorderLayout.EAST);
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		
		JPanel scanningPanel = new JPanel();
		scanningPanel.setAlignmentY(0.0f);
		textPanel.add(scanningPanel);
		scanningPanel.setLayout(new BoxLayout(scanningPanel, BoxLayout.Y_AXIS));
		
		topRowPanel = new JPanel();
		topRowPanel.setMaximumSize(new Dimension(32767, 20));
		topRowPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
		topRowPanel.setBackground(new Color(230, 230, 250));
		scanningPanel.add(topRowPanel);
		topRowPanel.setLayout(new BorderLayout(0, 0));
		
		statusLabel = new JLabel("Starting up...");
		topRowPanel.add(statusLabel, BorderLayout.CENTER);
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		
		scanEnableCheckbox = new JCheckBox("scan");
		scanEnableCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					Thread thread = new Thread(WebcamScanner.this);
					thread.setDaemon(true);
					thread.start();
				}
				else if (!returnToScanning) {
					resetManualInput();
				}
			}

		});
		scanEnableCheckbox.setHorizontalAlignment(SwingConstants.TRAILING);
		topRowPanel.add(scanEnableCheckbox, BorderLayout.EAST);
		
		JPanel scanBox = new JPanel();
		scanBox.setBorder(new EmptyBorder(4, 4, 4, 4));
		scanningPanel.add(scanBox);
		scanBox.setBackground(UIManager.getColor("Panel.background"));

		JLabel lblGroup = new JLabel("GROUP");
		lblGroupValue = new JLabel("");

		JLabel lblKitName = new JLabel("KIT");
		
		kitNameValue = new JLabel("");
		kitNameValue.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		
		JLabel lblRefBox = new JLabel("REF");
		
		scanRef = new JTextField();
		scanRef.setEditable(false);
		scanRef.setColumns(10);
		scanRef.getDocument().addDocumentListener(new BurstEntryDocumentListener(4) {
			@Override
			public void burstEntryDetected() {
				manualRefScanned();
			}
		});
		
		JLabel lblLot = new JLabel("LOT");
		
		scanLot = new JTextField();
		scanLot.setColumns(10);
		scanLot.getDocument().addDocumentListener(new BurstEntryDocumentListener(4) {
			@Override
			public void burstEntryDetected() {
				manualLotScanned();
			}
		});
		
		
		JLabel lblRgt = new JLabel("RGT");
		
		scanRgt = new JTextField();
		scanRgt.setColumns(10);
		scanRgt.getDocument().addDocumentListener(new BurstEntryDocumentListener(4) {
			@Override
			public void burstEntryDetected() {
				manualRgtScanned();
			}
		});
		
		JLabel lblDate = new JLabel("Date");
		
		scanDate = new JTextField();
		scanDate.setColumns(10);
		GroupLayout gl_scanBox = new GroupLayout(scanBox);
		gl_scanBox.setHorizontalGroup(
			gl_scanBox.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_scanBox.createSequentialGroup()
					.addGap(6)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_scanBox.createParallelGroup(Alignment.TRAILING, false)
							.addComponent(lblGroup, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblDate, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblRgt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblLot, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblRefBox, GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE))
						.addComponent(lblKitName, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE))
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addComponent(lblGroupValue, GroupLayout.PREFERRED_SIZE, 263, GroupLayout.PREFERRED_SIZE)
						.addComponent(kitNameValue, GroupLayout.PREFERRED_SIZE, 263, GroupLayout.PREFERRED_SIZE)
						.addComponent(scanRgt, Alignment.TRAILING)
						.addComponent(scanDate, Alignment.TRAILING)
						.addComponent(scanLot, Alignment.TRAILING)
						.addComponent(scanRef, GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE))
					.addContainerGap())
		);
		gl_scanBox.setVerticalGroup(
			gl_scanBox.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_scanBox.createSequentialGroup()
					.addGap(3)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
							.addComponent(lblGroup, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
							.addComponent(lblGroupValue, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
							.addComponent(lblKitName, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
							.addComponent(kitNameValue, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addComponent(lblRefBox, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
						.addComponent(scanRef, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addComponent(lblLot, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
						.addComponent(scanLot, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addComponent(lblRgt, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
						.addComponent(scanRgt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_scanBox.createParallelGroup(Alignment.LEADING)
						.addComponent(lblDate, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
						.addComponent(scanDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		scanBox.setLayout(gl_scanBox);
				
		JPanel buttonPanel = new JPanel();
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		scanningPanel.add(buttonPanel);
		
		btnAdd = new JButton("Add lot");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manualAddLot();
			}
		});
		btnAdd.setEnabled(false);
		buttonPanel.add(btnAdd);
		
		btnEdit = new JButton("Edit");
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editLot();
			}
		});
		btnEdit.setEnabled(false);
		buttonPanel.add(btnEdit);
		
		Component horizontalStrut = Box.createHorizontalStrut(300);
		scanningPanel.add(horizontalStrut);
		
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
		
		JPanel optionsPanel = new JPanel();
		padding.add(optionsPanel, BorderLayout.SOUTH);
		optionsPanel.setLayout(new BorderLayout(0, 0));
		
		JButton btnNewKitType = new JButton("New kit type...");
		btnNewKitType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showNewKitDialog();
			}
		});
		padding.add(btnNewKitType, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		// Full screen?
		//setExtendedState(JFrame.MAXIMIZED_BOTH); 
		setVisible(true);

		Client client = ClientBuilder.newBuilder()
				.register(MoxyJsonFeature.class)
				.build();
		apiBaseTarget = client.target(apiUrl);
		
		new StartupGroupSelectDialog(this);
	}


	public void groupSelected(String groupIdentifier) {
		lblGroupValue.setText(groupIdentifier);
		// Note: This triggers the event, to start scanning! (Intentionally)
		scanEnableCheckbox.setSelected(true);
	}
	
	public void run() {

		final Code128Reader codeReader = new Code128Reader();
		final HashMap<DecodeHintType, Object> hints = new HashMap<>();
		hints.put(DecodeHintType.TRY_HARDER, true);
		GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(codeReader);

		if (!returnToScanning) { // If in return to.. mode, we want to show "Saved" / what ever
			statusLabel.setText("Scanning...");
			topRowPanel.setBackground(new Color(230, 230, 250));
		}
		scanRef.setEditable(false);
		btnEdit.setEnabled(false);
		btnAdd.setEnabled(false);
		returnToScanning = false;
		while (scanEnableCheckbox.isSelected()) {
			webcamPanelRef.resume();
			Result [] results = {};
			BufferedImage image = null;

			if (webcam != null && webcam.isOpen()) {

				if ((image = webcam.getImage()) == null) {
					continue;
				}
				/*
				 * Debug: Read from local file instead
				 * try {
					image = ImageIO.read(new File("resources/test_image.png"));
				} catch (IOException e1) {
				}*/
				
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					results = reader.decodeMultiple(bitmap, hints);
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
			
			boolean noNewBarcodes = !scanPauseSet.isEmpty() && scanPauseSet.containsAll(data);
			boolean timeout = now - prevScanTime >= SCAN_PAUSE_TIME;

			if (timeout || !noNewBarcodes) {
				String dotsStr = "";
				for (int i=0; i < dots; ++i)
					dotsStr += ".";
				if (dots++ == 4) {
					dots = 0;
				}
				statusLabel.setText("Scanning" + dotsStr);
				
				topRowPanel.setBackground(new Color(230, 230, 250));
			}
			if (noNewBarcodes && (!timeout || data.isEmpty())) {
				continue;
			}
			
			btnEdit.setEnabled(false);
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

			if (now - lastErrorTime > ERROR_DISPLAY_TIME && results.length > 1) {
				errorPanel.setVisible(false);
				//errorTextArea.setText("");
				//errorTextArea.setBackground(UIManager.getColor("Panel.background"));
			}
			
			if (data.size() >= 2) {
				webcamPanelRef.pause();
				final JTextField[] fields = {scanLot, scanRgt, scanDate};
				for (JTextField field: fields)
					field.setEditable(false);
				processResult(image, data);
				for (JTextField field: fields)
					field.setEditable(true);
			}
		}
		if (!returnToScanning) {
			topRowPanel.setBackground(new Color(230, 230, 250));
			statusLabel.setText("Ready");
		}
		webcamPanelRef.pause();
		// End of scan loop
		btnAdd.setEnabled(true);
		btnEdit.setEnabled(false);
		scanRef.setEditable(true);
	}

	private synchronized void processResult(BufferedImage image, List<String> data) {
		try { // Globally catch IO exceptions (communication error)
			if (workflow == null || !workflow.ref.equals(data.get(0))) {
				statusLabel.setText("Kit lookup...");
				workflow = new ScanResultsWorkflow(apiBaseTarget, data.get(0), lblGroupValue.getText());
			}
			workflow.loadKit();
			kitNameValue.setText(workflow.kit.name);
			scanRgt.setEnabled(workflow.kit.hasUniqueId);
			workflow.setBarcodes(data);
			
			topRowPanel.setBackground(new Color(250, 250, 230));
			statusLabel.setText("Saving...");
			beep(Beep.INFO);
			if (workflow.tryGetLotDate()) {
				scanDate.setText(workflow.getExpiryDateString());
			}
			
			if (workflow.valiDate()) {
				try {
					workflow.save();
					scanRgt.setText(workflow.lot.uniqueId);
					topRowPanel.setBackground(new Color(230, 250, 230));
					statusLabel.setText("✓ Lot saved");
					errorPanel.setVisible(false);
					beep(Beep.SUCCESSS);
					btnEdit.setEnabled(true);
				} catch (BadRequestException e) {
					String message = readHttpErrorMessage(e);
					showError(message);
				}
				// This enables a pause between scan attempts after completion
				prevScanTime = System.currentTimeMillis();
				scanPauseSet = new HashSet<String>(data);
			}
			else {
				throw new DateParsingException();
			}
		}
		catch (InternalServerErrorException e) {
			showError("Internal server error");
			String error = readHttpErrorMessage(e);
			JOptionPane.showMessageDialog(null, "500 Internal Server Error:\n" + error.substring(0, Math.min(100, error.length())));
		} catch (IOException | ProcessingException | ClientErrorException e) {
			// Catches communication errors and unexpected HTTP response codes 
			showError("Communication error");
			JOptionPane.showMessageDialog(null, "Input/Output error while communicating with the backend:\n\n" + e.toString());
		} catch (InvalidBarcodeSetException e) { // Ignored, should try again
		} catch (KitNotFoundException e) {
			kitNameValue.setText("");
			showError(e.getMessage());
			// This enables a pause between scan attempts after kit not found, for these exact data
			prevScanTime = System.currentTimeMillis();
			scanPauseSet = new HashSet<String>(data);
		} catch (DateParsingException e) {
			statusLabel.setText("Manual entry mode");
			scanEnableCheckbox.setSelected(false);
			returnToScanning = true;
			btnAdd.setEnabled(true);
			scanDate.setBackground(Color.YELLOW);
			scanDate.requestFocusInWindow();
		}
		if (!workflow.isCompleted() && scanEnableCheckbox.isSelected()) {
			statusLabel.setText("Scanning...");
			topRowPanel.setBackground(new Color(230, 230, 250));
		}
	}

	public static String readHttpErrorMessage(WebApplicationException e) {
		String message = "UNKNOWN ERROR!";
		if (e.getResponse().getEntity() instanceof InputStream) {
			StringWriter writer = new StringWriter();
			try {
				IOUtils.copy((InputStream)e.getResponse().getEntity(), writer, "UTF-8");
			} catch (IOException e2) {
				message = "Unable to get error message";
			}
			message = writer.toString();
		}
		return message;
	}

	private void showError(String message) {
		beep(Beep.FAIL);
		errorPanel.setVisible(true);
		errorTextArea.setText(message);
		//errorTextArea.setBackground(Color.PINK);
		lastErrorTime = System.currentTimeMillis();
	}

	private synchronized void editLot() {
		if (workflow != null && workflow.isCompleted()) {
			workflow.lot.uniqueId = scanRgt.getText();
			workflow.lot.expiryDate = scanDate.getText();
			workflow.lot.lotnumber = scanLot.getText();
			statusLabel.setText("Saving...");
			topRowPanel.setBackground(new Color(250, 250, 230));
			try {
				workflow.editLot();
				scanDate.setBackground(UIManager.getColor("TextArea.background"));
				topRowPanel.setBackground(new Color(230, 250, 230));
				statusLabel.setText("✓ Lot edited");
				errorPanel.setVisible(false);
				beep(Beep.SUCCESSS);
			} catch (ProcessingException | WebApplicationException e) {
				// Catches communication errors and unexpected HTTP response codes 
				showError("Communication error");
				JOptionPane.showMessageDialog(null, "Input/Output error while communicating with the backend:\n\n" + e.toString());
				statusLabel.setText("Edit error");
			} 
		}
	}
	
	private synchronized void manualAddLot() {
		boolean saveOk = false;
		try {
			topRowPanel.setBackground(new Color(250, 250, 230));
			statusLabel.setText("Saving...");
			
			List<String> data = new ArrayList<>();
			data.add(scanRef.getText());
			data.add(scanLot.getText());
			data.add(scanRgt.getText());
			
			if (workflow == null || !workflow.ref.equals(data.get(0))) {
				statusLabel.setText("Kit lookup...");
				workflow = new ScanResultsWorkflow(apiBaseTarget, data.get(0), lblGroupValue.getText());
			}
			workflow.loadKit();
			kitNameValue.setText(workflow.kit.name);
			workflow.lotNumber = scanLot.getText();
			workflow.uniqueId = scanRgt.getText();
			workflow.setExpiryDate(scanDate.getText());
			workflow.save();
			scanRgt.setText(workflow.lot.uniqueId);
			topRowPanel.setBackground(new Color(230, 250, 230));
			statusLabel.setText("✓ Lot saved");
			errorPanel.setVisible(false);
			beep(Beep.SUCCESSS);
			scanDate.setBackground(UIManager.getColor("TextArea.background"));
			saveOk = true;
			if (returnToScanning) {
				scanPauseSet = new HashSet<String>();
				scanPauseSet.add(workflow.lotNumber);
				scanPauseSet.add(workflow.ref);
				scanPauseSet.add(workflow.uniqueId);
				prevScanTime = System.currentTimeMillis();
				scanEnableCheckbox.setSelected(true);
				scanRgt.setText(workflow.lot.assignedUniqueId);
				scanDate.setText(workflow.lot.expiryDate);
			}
			else {
				resetManualInput();
			}
		}
		catch (WebApplicationException e) {
			String message = readHttpErrorMessage(e);
			showError(message);
		} catch (KitNotFoundException | ParseException e) {
			showError(e.getMessage());
		} catch (IOException | ProcessingException e) {
			showError("Input/Output error: " + e.getMessage());
		}
		if (!saveOk) {
			statusLabel.setText("Save error");
		}
	}
		
	
	public static void main(String[] args) {
		String url = "http://localhost:5001/";
		if (args.length >= 1) {
			url = args[0];
		}
		int x = 1280, y = 1024, icam = -1;
		if (args.length >= 3) {
			x = Integer.parseInt(args[1]);
			y = Integer.parseInt(args[2]);
		}
		if (args.length >= 4) {
			icam = Integer.parseInt(args[3]);
		}
		new WebcamScanner(url, x, y, icam);
	
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

	private void showNewKitDialog() {
		if (newKitDialog == null || !newKitDialog.isDisplayable()) {
			newKitDialog = new NewKitDialog(apiBaseTarget, this, lblGroupValue.getText());
		}
		else {
			java.awt.EventQueue.invokeLater(new Runnable() {
			    @Override
			    public void run() {
			    	newKitDialog.toFront();
			    	newKitDialog.repaint();
			    }
			});
		}
		if (!scanRef.getText().isEmpty()) {
			newKitDialog.setRef(scanRef.getText());
		}
	}

	@Override
	public synchronized void kitServerStatusChanged(String ref) {
		if (workflow != null && workflow.ref.equals(ref)) {
			workflow = null;
		}
	}

	private void manualRefScanned() {
		topRowPanel.setBackground(new Color(230, 230, 250));
		statusLabel.setText("Ready");
		if (!scanRef.getText().isEmpty() && !scanEnableCheckbox.isSelected()) {
			try {
				scanLot.requestFocusInWindow(); 
				if (workflow == null || !workflow.ref.equals(scanRef.getText())) {
					workflow = new ScanResultsWorkflow(apiBaseTarget, scanRef.getText(), lblGroupValue.getText());
					workflow.loadKit();
				}
				workflow.resetLot();
				if (workflow.kit != null) {
					kitNameValue.setText(workflow.kit.name);
					scanRgt.setEnabled(workflow.kit.hasUniqueId);
					if (!scanLot.getText().isEmpty()) {
						// If we scanned the lot before we 
						manualLotScanned();
					}
				}
			}
			catch (WebApplicationException e) {
				String message = readHttpErrorMessage(e);
				showError(message);
			} catch (KitNotFoundException e) {
				// Ignore Kit not found errors here
			} catch (ProcessingException e) {
				showError("Input/Output error: " + e.getMessage());
			}
		}
	}

	private synchronized void manualLotScanned() {
		if (workflow != null && workflow.kit != null && !scanLot.getText().isEmpty() && 
				!scanLot.getText().equals(workflow.lotNumber)) {
			workflow.lotNumber = scanLot.getText();
			workflow.tryGetLotDate();
			boolean validDate = workflow.valiDate();
			if (validDate) {
				scanDate.setText(workflow.getExpiryDateString());
			}
			if (workflow.kit.hasUniqueId) {
				scanRgt.requestFocusInWindow();
			}
			else if (validDate) {
				btnAdd.requestFocusInWindow();
			}
			else {
				scanDate.requestFocusInWindow();
			}
		}
	}
	
	private void manualRgtScanned() {
		if (workflow != null && workflow.kit != null && !scanRgt.getText().isEmpty()) {
			if (workflow.valiDate()) {
				btnAdd.requestFocusInWindow();
			}
			else {
				scanDate.requestFocusInWindow();
			}
		}
	}

	private void resetManualInput() {
		scanRef.setText("");
		scanLot.setText("");
		scanRgt.setText("");
		scanDate.setText("");
		kitNameValue.setText("");
		scanRef.requestFocusInWindow();
	}
}
