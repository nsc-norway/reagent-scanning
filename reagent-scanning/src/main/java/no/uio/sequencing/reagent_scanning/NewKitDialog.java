package no.uio.sequencing.reagent_scanning;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class NewKitDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final WebTarget apiBase;
	private final KitInvalidationListener kil;
	private final String group;
	private JTextField refField;
	private JTextField kitNameField;
	private JTextField versionSubtypeField;
	private JCheckBox uniqueIdCheck;
	private JCheckBox setActiveCheck;
	
	public NewKitDialog(WebTarget apiBase, KitInvalidationListener kil, String group) {
		this.apiBase = apiBase;
		this.kil = kil;
		this.group = group;
		getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,}));
		
		JLabel lblAddANew = new JLabel("Add a new kit type to the scanner program");
		lblAddANew.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		getContentPane().add(lblAddANew, "2, 2, 3, 1");
		
		JLabel lblRef = new JLabel("REF on box");
		getContentPane().add(lblRef, "2, 6, right, default");
		
		refField = new JTextField();
		getContentPane().add(refField, "4, 6, 2, 1, fill, default");
		refField.setColumns(10);
		
		JLabel lblExactJitName = new JLabel("Exact kit name in LIMS");
		getContentPane().add(lblExactJitName, "2, 8, right, default");
		
		kitNameField = new JTextField();
		getContentPane().add(kitNameField, "4, 8, 2, 1, fill, default");
		kitNameField.setColumns(10);
		
		JLabel lblVersionSubtype = new JLabel("Version / subtype code");
		getContentPane().add(lblVersionSubtype, "2, 10, right, default");
		
		versionSubtypeField = new JTextField();
		getContentPane().add(versionSubtypeField, "4, 10, 2, 1, fill, default");
		versionSubtypeField.setColumns(10);
		
		JLabel lblegBc = new JLabel("(examples: v3-600C, BC-v2, RC-v2-HO-75C)");
		getContentPane().add(lblegBc, "4, 12, 2, 1");
		
		JLabel lblHasUniqueId = new JLabel("Has unique ID?");
		getContentPane().add(lblHasUniqueId, "2, 14, right, default");
		
		uniqueIdCheck = new JCheckBox("(3 barcodes)");
		uniqueIdCheck.setSelected(true);
		getContentPane().add(uniqueIdCheck, "4, 14, 2, 1");
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		JLabel lblNewLabel = new JLabel("Set ACTIVE?");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblNewLabel, "2, 16");
		
		setActiveCheck = new JCheckBox("");
		setActiveCheck.setSelected(true);
		getContentPane().add(setActiveCheck, "4, 16");
		getContentPane().add(btnCancel, "4, 18");
		
		JButton btnOk = new JButton("Add new kit");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submitKit();
			}
		});
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().add(btnOk, "5, 18");
		getRootPane().setDefaultButton(btnOk);
		pack();
		setVisible(true);

	}

	public void submitKit() {
		Kit kit = new Kit();
		kit.ref = refField.getText();
		kit.name = kitNameField.getText();
		kit.lotcode = versionSubtypeField.getText();
		kit.hasUniqueId = uniqueIdCheck.isSelected();
		kit.setActive = setActiveCheck.isSelected();
		try {
			apiBase.path("kits").path(group)
				.request(MediaType.TEXT_PLAIN_TYPE)
				.post(Entity.entity(kit, MediaType.APPLICATION_JSON_TYPE), String.class);
			JOptionPane.showMessageDialog(this, "Kit added successfully", "Add new kit", JOptionPane.INFORMATION_MESSAGE);
			kil.kitServerStatusChanged(kit.ref);
			dispose();
		} catch (WebApplicationException e) {
			String errorMessage = WebcamScanner.readHttpErrorMessage(e);
			JOptionPane.showMessageDialog(this, errorMessage, "Add new kit", JOptionPane.ERROR_MESSAGE);
		} catch (ProcessingException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Add new kit", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void setRef(String str) {
		refField.setText(str);
	}
}
