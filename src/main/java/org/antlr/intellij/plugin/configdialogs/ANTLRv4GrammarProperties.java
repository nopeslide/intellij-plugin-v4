package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;

import java.io.File;

@Tag("PerGrammarGenerationSettings")
public class ANTLRv4GrammarProperties implements Cloneable {

    public static final String PROP_LANGUAGE = "language";
    static final String PROJECT_SETTINGS_PREFIX = "*";

    @Property
    String fileName;

    @Property
    boolean autoGen;

    @Property
    String outputDir;

    @Property
    String libDir;

    @Property
    String encoding;

    @Property
    String pkg;

    @Property
    String language;

    @Property
    boolean generateListener = true;

    @Property
    boolean generateVisitor;

    public boolean shouldAutoGenerateParser() {
        return autoGen;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getLibDir() {
        return libDir;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getPackage() {
        return pkg;
    }

    public String getLanguage() {
        return language;
    }

    public boolean shouldGenerateParseTreeListener() {
        return generateListener;
    }

    public boolean shouldGenerateParseTreeVisitor() {
        return generateVisitor;
    }

    public String resolveOutputDirName(Project project, VirtualFile contentRoot, String package_) {
        String outputDirName = outputDir.isEmpty() ? RunANTLROnGrammarFile.OUTPUT_DIR_NAME : outputDir;

        outputDirName = PathMacroManager.getInstance(project).expandPath(outputDirName);

        File f = new File(outputDirName);
        if (!f.isAbsolute()) { // if not absolute file spec, it's relative to project root
            outputDirName = contentRoot.getPath() + File.separator + outputDirName;
        }
        // add package if any
        if ( !package_.equals(RunANTLROnGrammarFile.MISSING) ) {
            outputDirName += File.separator + package_.replace('.', File.separatorChar);
        }
        return outputDirName;
    }

    public String resolveLibDir(Project project, String defaultValue) {
        String libDir = getLibDir();

        if ( libDir==null || libDir.equals("") ) {
            libDir = defaultValue;
        }

        return PathMacroManager.getInstance(project).expandPath(libDir);
    }

    @Override
    public ANTLRv4GrammarProperties clone() throws CloneNotSupportedException {
        return (ANTLRv4GrammarProperties) super.clone();
    }
}
