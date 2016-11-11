package no.uio.sequencing.reagent_scanning;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSplitPane;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JTextField;
import java.awt.Component;


public class WebcamScanner extends JFrame implements Runnable {

	private static final long serialVersionUID = 6441489127408381878L;

	private Webcam webcam = null;
	private JTextField scanRef;
	private JTextField scanLot;
	private JTextField scanRgt;
	private JTextField scanDate;

	public WebcamScanner() {
		super();
		setTitle("Scanner (moose) application");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Dimension res = new Dimension(1024, 768);
		webcam = Webcam.getDefault();
		if (webcam == null) {
			JOptionPane.showMessageDialog(null, "Error: No webcam detected");
			System.exit(1);
		}
		else {
			webcam.setCustomViewSizes(new Dimension[] {res});
			webcam.setViewSize(res);
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
		
		JTextArea scanningText = new JTextArea();
		scanningText.setAlignmentY(0.0f);
		scanningText.setColumns(8);
		scanningText.setEditable(false);
		scanningText.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		scanningText.setText("Scanning...");
		scanningText.setBackground(Color.BLUE);
		scanningText.setForeground(Color.WHITE);
		scanningPanel.add(scanningText);
		
		JPanel scanBox = new JPanel();
		scanBox.setAlignmentY(0.0f);
		scanningPanel.add(scanBox);
		scanBox.setBackground(Color.BLUE);
		scanBox.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblRefBox = new JLabel("REF");
		lblRefBox.setForeground(Color.WHITE);
		scanBox.add(lblRefBox);
		
		scanRef = new JTextField();
		scanBox.add(scanRef);
		scanRef.setColumns(10);
		
		JLabel lblLot = new JLabel("LOT");
		lblLot.setForeground(Color.WHITE);
		scanBox.add(lblLot);
		
		scanLot = new JTextField();
		scanBox.add(scanLot);
		scanLot.setColumns(10);
		
		JLabel lblRgt = new JLabel("RGT");
		lblRgt.setForeground(Color.WHITE);
		scanBox.add(lblRgt);
		
		scanRgt = new JTextField();
		scanBox.add(scanRgt);
		scanRgt.setColumns(10);
		
		JLabel lblDate = new JLabel("Date");
		lblDate.setForeground(Color.WHITE);
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

		MultiFormatReader mfReader = new MultiFormatReader();
		GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(mfReader);
		
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Result [] results = {};
			BufferedImage image = null;

			if (webcam != null && webcam.isOpen()) {

				if ((image = webcam.getImage()) == null) {
					continue;
				}

				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					results = reader.decodeMultiple(bitmap);
				} catch (NotFoundException e) {
					// fall thru, it means there is no QR code in image
				}
			}

			int pointer = 0;
			final JTextField[] destination = {scanRef, scanLot, scanRgt};
			for (Result r : results) {
				if (pointer < destination.length) {
					destination[pointer++].setText(r.getText());
				}
			}

		} while (true);
	}

	public static void main(String[] args) {
		new WebcamScanner();
	}
}
