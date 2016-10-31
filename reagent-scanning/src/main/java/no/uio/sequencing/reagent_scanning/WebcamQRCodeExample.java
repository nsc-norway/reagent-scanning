package no.uio.sequencing.reagent_scanning;

import java.awt.Dimension;
import java.awt.FlowLayout;
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


public class WebcamQRCodeExample extends JFrame implements Runnable {

	private static final long serialVersionUID = 6441489127408381878L;

	private Webcam webcam = null;
	private JPanel panel = null;
	private JTextArea textarea = null;

	public WebcamQRCodeExample() {
		super();
		
		setLayout(new FlowLayout());
		setTitle("Scanner (moose) application");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Dimension size = new Dimension(1920, 1080);
		webcam = Webcam.getDefault();
		if (webcam == null) {
			JOptionPane.showMessageDialog(null, "Error: No webcam detected");
			panel = new JPanel();
		}
		else {
			webcam.setCustomViewSizes(new Dimension[] {size});
			webcam.setViewSize(size);
			panel = new WebcamPanel(webcam);
		}

		//panel.setPreferredSize(size);

		textarea = new JTextArea();
		textarea.setEditable(false);
		//textarea.setPreferredSize(size);

		add(panel);
		add(textarea);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	public void run() {

		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Result result = null;
			BufferedImage image = null;

			if (webcam != null && webcam.isOpen()) {

				if ((image = webcam.getImage()) == null) {
					continue;
				}

				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					result = new MultiFormatReader().decode(bitmap);
				} catch (NotFoundException e) {
					// fall thru, it means there is no QR code in image
				}
			}

			if (result != null) {
				textarea.setText(result.getText());
			}

		} while (true);
	}

	public static void main(String[] args) {
		new WebcamQRCodeExample();
	}
}