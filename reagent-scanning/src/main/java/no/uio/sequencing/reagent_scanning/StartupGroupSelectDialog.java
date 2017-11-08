package no.uio.sequencing.reagent_scanning;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class StartupGroupSelectDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	
	/**
	 * Create the dialog.
	 */
	public StartupGroupSelectDialog(WebcamScanner parentWindow) {
		setModal(true);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		setBounds(100, 100, 456, 110);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel lblGroup = new JLabel("Group for scanning");
			contentPanel.add(lblGroup);
		}
		JButton nscButton = new JButton("NSC");
		contentPanel.add(nscButton);
		nscButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parentWindow.groupSelected("NSC");
				dispose();
			}
		});
		getRootPane().setDefaultButton(nscButton);
		JButton diagButton = new JButton("Diag");
		diagButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parentWindow.groupSelected("Diag");
				dispose();
			}
		});
		contentPanel.add(diagButton);
		pack();
		setVisible(true);
	}
}
