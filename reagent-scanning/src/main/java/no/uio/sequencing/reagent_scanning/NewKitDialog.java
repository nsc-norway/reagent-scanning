package no.uio.sequencing.reagent_scanning;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
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
	private JTextField kitVersionField;
	private JTextField numCyclesField;
	private final JComboBox<String> sequencingTypeField;
	private final JComboBox<String> nextseqField;
	private final JComboBox<String> otherPropertiesField;
	private JTextField numReactionsField;
	
	public NewKitDialog(WebTarget apiBase, KitInvalidationListener kil, String group) {
		this.apiBase = apiBase;
		this.kil = kil;
		this.group = group;
		getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
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
				RowSpec.decode("default:grow"),
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
		
		final DocumentListener typeCodeDocListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateTypeCode();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateTypeCode();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateTypeCode();
			}
		};
		final ActionListener actionMan = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateTypeCode();
			}
		};
		
		JLabel lblAddANew = new JLabel("Add a new kit type to the scanner program");
		lblAddANew.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		getContentPane().add(lblAddANew, "2, 2, 3, 1");
		
		JLabel lblRef = new JLabel("REF on box");
		getContentPane().add(lblRef, "2, 6, right, default");
		
		refField = new JTextField();
		getContentPane().add(refField, "4, 6, 2, 1, fill, default");
		refField.setColumns(10);
		
		JLabel lblExactJitName = new JLabel("Kit name in LIMS without group");
		getContentPane().add(lblExactJitName, "2, 8, right, default");
		
		kitNameField = new JTextField();
		getContentPane().add(kitNameField, "4, 8, 2, 1, fill, default");
		kitNameField.setColumns(10);
		
		JLabel lblKitVersion = new JLabel("Kit version");
		lblKitVersion.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblKitVersion, "2, 10, right, default");
		
		kitVersionField = new JTextField();
		kitVersionField.setText("1");
		getContentPane().add(kitVersionField, "4, 10, fill, default");
		kitVersionField.setColumns(5);
		kitVersionField.getDocument().addDocumentListener(typeCodeDocListener);
		
		JLabel lblNumberOfCycles = new JLabel("Number of cycles");
		lblNumberOfCycles.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblNumberOfCycles, "2, 12, right, default");
		
		numCyclesField = new JTextField();
		getContentPane().add(numCyclesField, "4, 12, fill, default");
		numCyclesField.setColumns(5);
		numCyclesField.getDocument().addDocumentListener(typeCodeDocListener);
		
		JLabel lblNumberOfSamplesreactions = new JLabel("Number of reactions/samples");
		lblNumberOfSamplesreactions.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblNumberOfSamplesreactions, "2, 14, right, default");
		
		numReactionsField = new JTextField();
		getContentPane().add(numReactionsField, "4, 14, fill, default");
		numReactionsField.setColumns(10);
		numReactionsField.getDocument().addDocumentListener(typeCodeDocListener);
		
		JLabel lblSequencingMethod = new JLabel("Sequencing method");
		lblSequencingMethod.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblSequencingMethod, "2, 16, right, default");
		
		sequencingTypeField = new JComboBox<String>();
		sequencingTypeField.setModel(new DefaultComboBoxModel<String>(new String[] {"N/A", "Paired end", "Single read"}));
		sequencingTypeField.setSelectedIndex(0);
		getContentPane().add(sequencingTypeField, "4, 16, fill, default");
		sequencingTypeField.addActionListener(actionMan);
		
		JLabel lblNextseq = new JLabel("NextSeq");
		lblNextseq.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblNextseq, "2, 18, right, default");
		
		nextseqField = new JComboBox<String>();
		nextseqField.setModel(new DefaultComboBoxModel<String>(new String[] {"N/A", "High output", "Mid output"}));
		getContentPane().add(nextseqField, "4, 18, fill, default");
		nextseqField.addActionListener(actionMan);
		
		JLabel lblOtherProperties = new JLabel("Other properties, if any");
		lblOtherProperties.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblOtherProperties, "2, 20, right, default");
		
		otherPropertiesField = new JComboBox<String>();
		otherPropertiesField.setModel(new DefaultComboBoxModel<String>(new String[] {"", "BC", "RC", "FC", "RAPID"}));
		otherPropertiesField.setEditable(true);
		getContentPane().add(otherPropertiesField, "4, 20, fill, default");
		((JTextComponent)otherPropertiesField.getEditor().getEditorComponent()).getDocument().addDocumentListener(typeCodeDocListener);
		
		JLabel lblVersionSubtype = new JLabel("Version / subtype code");
		getContentPane().add(lblVersionSubtype, "2, 22, right, default");
		
		versionSubtypeField = new JTextField();
		versionSubtypeField.setEditable(false);
		getContentPane().add(versionSubtypeField, "4, 22, 2, 1, fill, default");
		versionSubtypeField.setColumns(10);
		
		JLabel lblegBc = new JLabel("(generated based on the above)");
		getContentPane().add(lblegBc, "4, 24, 2, 1");
		
		JLabel lblHasUniqueId = new JLabel("Has unique ID?");
		getContentPane().add(lblHasUniqueId, "2, 26, right, default");
		
		uniqueIdCheck = new JCheckBox("(3 barcodes, including \"RGT\")");
		uniqueIdCheck.setSelected(true);
		getContentPane().add(uniqueIdCheck, "4, 26, 2, 1");
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		JLabel lblNewLabel = new JLabel("Set ACTIVE on scan?");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		getContentPane().add(lblNewLabel, "2, 28");
		
		setActiveCheck = new JCheckBox("");
		setActiveCheck.setSelected(true);
		getContentPane().add(setActiveCheck, "4, 28");
		getContentPane().add(btnCancel, "4, 30");
		
		JButton btnOk = new JButton("Add new kit");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submitKit();
			}
		});
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().add(btnOk, "5, 30");
		getRootPane().setDefaultButton(btnOk);
		pack();
		setVisible(true);
		updateTypeCode();
	}
	/**
	 * 	private JTextField kitVersionField;
	private JTextField numCyclesField;
	private final JComboBox<String> sequencingTypeField;
	private final JComboBox<String> nextseqField;
	private final JComboBox<String> otherPropertiesField;
	 */
	private void updateTypeCode() {
		String code = "";
		String otherProp = (String)otherPropertiesField.getSelectedItem();
		if (!otherProp.isEmpty()) {
			code += otherProp + "-";
		}
		String nextseq = (String)nextseqField.getSelectedItem();
		if ("High output".equals(nextseq)) {
			code += "HO-";
		}
		else if ("Mid output".equals(nextseq)) {
			code += "MO-";
		}
		String seqmethod = (String)sequencingTypeField.getSelectedItem();
		if ("Paired end".equals(seqmethod)) {
			code += "PE-";
		}
		else if ("Single read".equals(seqmethod)) {
			code += "SR-";
		}
		if (!numCyclesField.getText().isEmpty()) {
			code += numCyclesField.getText() + "-";
		}
		// There's always a "version", so this is the end, but possibly a 
		// -R for reactions below
		if (kitVersionField.getText().isEmpty())
			code += "V1";
		else
			code += "V" + kitVersionField.getText();
		if (!numReactionsField.getText().isEmpty()) {
			code += "-" + numReactionsField.getText() + "R";
		}
		versionSubtypeField.setText(code);
	}
	
	public void submitKit() {
		updateTypeCode();
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
