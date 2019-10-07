package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;

public class ANTLRv4ProjectSettings implements SearchableConfigurable {

    private ConfigANTLRPerGrammar configurationForm;

    private final Project project;

    public ANTLRv4ProjectSettings(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "ANTLR4ProjectSettings";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ANTLR4 Project Settings";
    }

    @Nullable
    public String getHelpTopic() {
        return "ANTLR4 Project Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        configurationForm = ConfigANTLRPerGrammar.getProjectSettingsForm(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
        return configurationForm.createCenterPanel();
    }

    @Override
    public boolean isModified() {
        ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
        return configurationForm.isModified(grammarProperties);
    }

    @Override
    public void apply() {
        configurationForm.saveValues(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
    }

    public void reset() {
        configurationForm.loadValues(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
    }
}
