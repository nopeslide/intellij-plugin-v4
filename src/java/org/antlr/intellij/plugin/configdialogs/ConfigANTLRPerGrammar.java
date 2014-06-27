package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	private JPanel dialogContents;
	private JCheckBox generateParseTreeListenerCheckBox;
	private JCheckBox generateParseTreeVisitorCheckBox;
	private JTextField packageField;
	private TextFieldWithBrowseButton outputDirField;
	private TextFieldWithBrowseButton libDirField;
	private JTextField fileEncodingField;

	public ConfigANTLRPerGrammar(final Project project, String qualFileName) {
		super(project, false);
		init();

		FileChooserDescriptor dirChooser =
			FileChooserDescriptorFactory.createSingleFolderDescriptor();
		outputDirField.addBrowseFolderListener("Select output dir", null, project, dirChooser);
		outputDirField.setTextFieldPreferredWidth(50);

		dirChooser =
			FileChooserDescriptorFactory.createSingleFolderDescriptor();
		libDirField.addBrowseFolderListener("Select lib dir", null, project, dirChooser);
		libDirField.setTextFieldPreferredWidth(50);

		loadValues(project, qualFileName);
	}

	public void loadValues(Project project, String qualFileName) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		String s;
		s = props.getValue(getPropNameForFile(qualFileName, "output-dir"), "");
		outputDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "lib-dir"), "");
		libDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "encoding"), "");
		fileEncodingField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, "package"), "");
		packageField.setText(s);

		boolean b;
		b = props.getBoolean(getPropNameForFile(qualFileName, "gen-listener"), true);
		generateParseTreeListenerCheckBox.setSelected(b);
		b = props.getBoolean(getPropNameForFile(qualFileName, "gen-visitor"), false);
		generateParseTreeVisitorCheckBox.setSelected(b);
	}

	public void saveValues(Project project, String qualFileName) {
		String v;
		PropertiesComponent props = PropertiesComponent.getInstance(project);

		v = outputDirField.getText();
		if (v.trim().length() > 0) {
			props.setValue(getPropNameForFile(qualFileName, "output-dir"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "output-dir"));
		}

		v = libDirField.getText();
		if (v.trim().length() > 0) {
			props.setValue(getPropNameForFile(qualFileName, "lib-dir"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "lib-dir"));
		}

		v = fileEncodingField.getText();
		if (v.trim().length() > 0) {
			props.setValue(getPropNameForFile(qualFileName, "encoding"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "encoding"));
		}

		v = packageField.getText();
		if (v.trim().length() > 0) {
			props.setValue(getPropNameForFile(qualFileName, "package"), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, "package"));
		}

		props.setValue(getPropNameForFile(qualFileName, "gen-listener"),
			String.valueOf(generateParseTreeListenerCheckBox.isSelected()));
		props.setValue(getPropNameForFile(qualFileName, "gen-visitor"),
			String.valueOf(generateParseTreeVisitorCheckBox.isSelected()));
	}

	public static String getPropNameForFile(String qualFileName, String prop) {
		return qualFileName + "::/" + prop;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return dialogContents;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}


	@Override
	public String toString() {
		return "ConfigANTLRPerGrammar{" +
			"textField3=" +
			", generateParseTreeListenerCheckBox=" + generateParseTreeListenerCheckBox +
			", generateParseTreeVisitorCheckBox=" + generateParseTreeVisitorCheckBox +
			", packageField=" + packageField +
			", outputDirField=" + outputDirField +
			", libDirField=" + libDirField +
			'}';
	}

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		dialogContents = new JPanel();
		dialogContents.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
		final JLabel label1 = new JLabel();
		label1.setText("Location of grammars, tokens files");
		dialogContents.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		final JLabel label2 = new JLabel();
		label2.setText("Grammar file encoding; e.g., euc-jp");
		dialogContents.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		fileEncodingField = new JTextField();
		dialogContents.add(fileEncodingField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		generateParseTreeVisitorCheckBox = new JCheckBox();
		generateParseTreeVisitorCheckBox.setText("generate parse tree visitor");
		dialogContents.add(generateParseTreeVisitorCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		generateParseTreeListenerCheckBox = new JCheckBox();
		generateParseTreeListenerCheckBox.setSelected(true);
		generateParseTreeListenerCheckBox.setText("generate parse tree listener (default)");
		dialogContents.add(generateParseTreeListenerCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Package/namespace for the generated code");
		dialogContents.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		packageField = new JTextField();
		dialogContents.add(packageField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Output directory where all output is generated");
		dialogContents.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		outputDirField = new TextFieldWithBrowseButton();
		dialogContents.add(outputDirField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		libDirField = new TextFieldWithBrowseButton();
		dialogContents.add(libDirField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return dialogContents;
	}
}
