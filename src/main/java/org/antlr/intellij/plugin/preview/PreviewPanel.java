package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.tool.Rule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The top level contents of the preview tool window created by
 *  intellij automatically. Since we need grammars to interpret,
 *  this object creates and caches lexer/parser grammars for
 *  each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel {
	//com.apple.eawt stuff stopped working correctly in java 7 and was only recently fixed in java 9;
	//perhaps in a few more years they will get around to backporting whatever it was they fixed.
	// until then,  the zoomable tree viewer will only be installed if the user is running java 1.6
	private static final boolean isTrackpadZoomSupported =
		SystemInfo.isMac &&
		(SystemInfo.JAVA_VERSION.startsWith("1.6") || SystemInfo.JAVA_VERSION.startsWith("1.9"));

	public static final Logger LOG = Logger.getInstance("ANTLR PreviewPanel");

	public Project project;

	public InputPanel inputPanel;

	private UberTreeViewer treeViewer;
	public HierarchyViewer hierarchyViewer;

	public ProfilerPanel profilerPanel;

	/**
	 * Indicates if the preview should be automatically refreshed after grammar changes.
	 */
	private boolean autoRefresh = true;

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	private void createGUI() {
		this.setLayout(new BorderLayout());

		// Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
		Splitter splitPane = new Splitter();
		inputPanel = getEditorPanel();
		splitPane.setFirstComponent(inputPanel.getComponent());
		splitPane.setSecondComponent(createParseTreeAndProfileTabbedPanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	private InputPanel getEditorPanel() {
		LOG.info("createEditorPanel"+" "+project.getName());
		return new InputPanel(this);
	}

	public ProfilerPanel getProfilerPanel() {
		return profilerPanel;
	}

	private JTabbedPane createParseTreeAndProfileTabbedPanel() {
		JBTabbedPane tabbedPane = new JBTabbedPane();

		LOG.info("createParseTreePanel" + " " + project.getName());
		Pair<UberTreeViewer, JPanel> pair = createParseTreePanel();
		treeViewer = pair.a;
		setupContextMenu(treeViewer);
		tabbedPane.addTab("Parse tree", pair.b);

		hierarchyViewer = new HierarchyViewer(null, this);
		tabbedPane.addTab("Hierarchy", hierarchyViewer);

		profilerPanel = new ProfilerPanel(project, this);
		tabbedPane.addTab("Profiler", profilerPanel.getComponent());

		return tabbedPane;
	}

	private static void setupContextMenu(final UberTreeViewer treeViewer) {
		treeViewer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					ParseTreeContextualMenu.showPopupMenu(treeViewer, e);
				}
			}
		});
	}

	private static Pair<UberTreeViewer,JPanel> createParseTreePanel() {
		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0, 0));
		treePanel.setBackground(JBColor.white);

		final UberTreeViewer viewer =
			isTrackpadZoomSupported ?
				new TrackpadZoomingTreeView(null, null, false) :
				new UberTreeViewer(null, null, false);

		JSlider scaleSlider = createTreeViewSlider(viewer);

		// Wrap tree viewer component in scroll pane
		JScrollPane scrollPane = new JBScrollPane(viewer); // use Intellij's scroller

		treePanel.add(scrollPane, BorderLayout.CENTER);

		treePanel.add(scaleSlider, BorderLayout.SOUTH);

		return new Pair<>(viewer, treePanel);
	}

	@NotNull
	private static JSlider createTreeViewSlider(final UberTreeViewer viewer) {
		JSlider scaleSlider;
		if ( isTrackpadZoomSupported ) {
			scaleSlider = new JSlider();
			scaleSlider.setModel(((TrackpadZoomingTreeView) viewer).scaleModel);
		}
		else {
			int sliderValue = (int) ((viewer.getScale() - 1.0) * 1000);
			scaleSlider = new JSlider(JSlider.HORIZONTAL, -999, 1000, sliderValue);
			scaleSlider.addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						int v = ((JSlider) e.getSource()).getValue();
						if ( viewer.hasTree() ) {
							viewer.setScale(v/1000.0+1.0);
						}
					}
				});
		}
		return scaleSlider;
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("grammarFileSaved " + grammarFileName+" "+project.getName());
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);

		ensureStartRuleExists(grammarFile);
		inputPanel.grammarFileSaved();

		// if the saved grammar is not a pure lexer and there is a start rule, reparse
		// means that switching grammars must refresh preview
		if ( previewState.g!=null && previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(previewState.grammarFile);
		}
		else {
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}

		profilerPanel.grammarFileSaved(previewState, grammarFile);
	}

	private void ensureStartRuleExists(VirtualFile grammarFile) {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFile);
		// if start rule no longer exists, reset display/state.
		if ( previewState.g!=null &&
			 previewState.g!=ParsingUtils.BAD_PARSER_GRAMMAR &&
			 previewState.startRuleName!=null )
		{
			Rule rule = previewState.g.getRule(previewState.startRuleName);
			if ( rule==null ) {
				previewState.startRuleName = null;
				inputPanel.resetStartRuleLabel();
			}
		}
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammar(oldFile, newFile);
	}

	/** Load grammars and set editor component. */
	private void switchToGrammar(VirtualFile oldFile, VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar " + grammarFileName+" "+project.getName());
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);

		inputPanel.switchToGrammar(previewState, grammarFile);
		profilerPanel.switchToGrammar(previewState, grammarFile);

		if ( previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(grammarFile); // regens tree and profile data
		}
		else {
			setParseTree(Collections.emptyList(), null); // blank tree
		}

		if ( previewState.g==null && previewState.lg!=null ) {
			setEnabled(false);
		}
		else {
			setEnabled(true);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.setEnabledRecursive(this, enabled);
	}

	private void setEnabledRecursive(Component component, boolean enabled) {
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				child.setEnabled(enabled);
				setEnabledRecursive(child, enabled);
			}
		}
	}

	public void closeGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("closeGrammar "+grammarFileName+" "+project.getName());

		inputPanel.resetStartRuleLabel();
		inputPanel.clearErrorConsole();
		clearParseTree(); // wipe tree

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);
		inputPanel.releaseEditor(previewState);
	}

	private void setParseTree(final List<String> ruleNames, final ParseTree tree) {
		ApplicationManager.getApplication().invokeLater(() -> {
			treeViewer.setRuleNames(ruleNames);
			treeViewer.setTree(tree);
		});
	}

	private void updateTreeViewer(final PreviewState preview, final ParsingResult result) {
		ApplicationManager.getApplication().invokeLater(() -> {
			if (result.parser instanceof PreviewParser) {
				AltLabelTextProvider provider = new AltLabelTextProvider(result.parser, preview.g);
				treeViewer.setTreeTextProvider(provider);
				treeViewer.setTree(result.tree);
				hierarchyViewer.setTreeTextProvider(provider);
				hierarchyViewer.setTree(result.tree);
			}
			else {
				treeViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
				treeViewer.setTree(result.tree);
				hierarchyViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
				hierarchyViewer.setTree(result.tree);
			}
		});
	}


	void clearParseTree() {
		setParseTree(Collections.emptyList(), null);
	}

	private void indicateInvalidGrammarInParseTreePane() {
		setParseTree(Collections.emptyList(),
					 new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE,
														  "Issues with parser and/or lexer grammar(s) prevent preview; see ANTLR 'Tool Output' pane")));
	}

	private void indicateNoStartRuleInParseTreePane() {
		setParseTree(Collections.emptyList(),
					 new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE,
														  "No start rule is selected")));
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		if ( controller==null ) return;
		PreviewState previewState = controller.getPreviewState(grammarFile);
		LOG.info("updateParseTreeFromDoc "+grammarFile+" rule "+previewState.startRuleName);
		if ( previewState.g==null || previewState.lg==null ) {
			// likely error in grammar prevents it from loading properly into previewState; bail
			indicateInvalidGrammarInParseTreePane();
			return;
		}
		try {
			Editor editor = inputPanel.getInputEditor();
			if ( editor==null ) return;
			final String inputText = editor.getDocument().getText();
			ParsingResult results = controller.parseText(grammarFile, inputText);
			if ( results!=null) {
				updateTreeViewer(previewState,results);
			}
			else if ( previewState.startRuleName==null ) {
				indicateNoStartRuleInParseTreePane();
			}
			else {
				indicateInvalidGrammarInParseTreePane();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}

	public boolean isAutoRefresh() {
		return autoRefresh;
	}

	public void setAutoRefresh(boolean autoRefresh) {
		this.autoRefresh = autoRefresh;
	}

	public void autoRefreshPreview(VirtualFile virtualFile) {
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);

		if (autoRefresh
				&& controller != null
				&& inputPanel.previewState.startRuleName != null) {
			ApplicationManager.getApplication().invokeLater(() -> controller.grammarFileSavedEvent(virtualFile));
		}
	}
}
