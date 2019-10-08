package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Property;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores settings related to code generation per grammar file.
 */
public class ANTLRv4GrammarPropertiesStore {

	private static final ANTLRv4GrammarProperties DEFAULT_GRAMMAR_PROPERTIES = initDefaultGrammarProperties();

	@Property
	private List<ANTLRv4GrammarProperties> perGrammarGenerationSettings = new ArrayList<>();

	public void add(ANTLRv4GrammarProperties properties) {
		perGrammarGenerationSettings.add(properties);
	}

	public ANTLRv4GrammarProperties getGrammarProperties(String grammarFile) {
		ANTLRv4GrammarProperties grammarSettings = findSettingsForFile(grammarFile);

		if ( grammarSettings==null ) {
			ANTLRv4GrammarProperties projectSettings = findSettingsForFile("*");

			if ( projectSettings==null ) {
				return ANTLRv4GrammarPropertiesStore.DEFAULT_GRAMMAR_PROPERTIES;
			}

			return projectSettings;
		}

		return grammarSettings;
	}

	public ANTLRv4GrammarProperties getOrCreateGrammarProperties(String grammarFile) {

		ANTLRv4GrammarProperties properties = getGrammarProperties(grammarFile);

		if ( Objects.equals(properties.fileName, grammarFile) ) {
			return properties;
		}

		ANTLRv4GrammarProperties newProperties = ObjectUtils.clone(properties);
		newProperties.fileName = grammarFile;

		add(newProperties);

		return newProperties;
	}

	@Nullable
	private ANTLRv4GrammarProperties findSettingsForFile(String fileName) {
		for (ANTLRv4GrammarProperties settings : perGrammarGenerationSettings) {
			if (settings.fileName.equals(fileName)) {
				return settings;
			}
		}

		return null;
	}

	public static ANTLRv4GrammarProperties getGrammarProperties(Project project, VirtualFile grammarFile) {
		return getGrammarProperties(project, grammarFile.getPath());
	}

	/**
	 * Defaults to settings defined in the project if they exist, or to empty settings.
	 */
	public static ANTLRv4GrammarProperties getGrammarProperties(Project project, String grammarFile) {
		ANTLRv4GrammarPropertiesStore store = ANTLRv4GrammarPropertiesComponent.getInstance(project).getState();
		return store.getGrammarProperties(grammarFile);
	}

	/**
	 * Get the properties for this grammar, or create a new properties object derived from the project settings if
	 * they exist, or from the default empty settings otherwise.
	 */
	public static ANTLRv4GrammarProperties getOrCreateGrammarProperties(Project project, String grammarFile) {
		ANTLRv4GrammarPropertiesStore store = ANTLRv4GrammarPropertiesComponent.getInstance(project).getState();
		return store.getOrCreateGrammarProperties(grammarFile);
	}

	private static ANTLRv4GrammarProperties initDefaultGrammarProperties() {
		ANTLRv4GrammarProperties defaultSettings = new ANTLRv4GrammarProperties();

		defaultSettings.fileName = "**";
		defaultSettings.autoGen = true;
		defaultSettings.outputDir = "";
		defaultSettings.libDir = "";
		defaultSettings.encoding = "";
		defaultSettings.pkg = "";
		defaultSettings.language = "";
		defaultSettings.generateListener = true;
		defaultSettings.generateVisitor = true;

		return defaultSettings;
	}
}
