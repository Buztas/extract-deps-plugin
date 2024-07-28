package mps.sample;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name="mps-deps", defaultPhase = LifecyclePhase.INITIALIZE)
public class ExtractDeps extends AbstractMojo {

    @Parameter(property = "project-directory", required = true, readonly = true)
    private File directory;

    @Parameter(property = "mps-directory", required = true, readonly = true)
    private File mpsDirectory;

    @Parameter(property = "install-dir", required = true, readonly = true)
    private File installDir;

    private Set<String> printedDependencies = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (directory == null || !directory.exists() || !directory.isDirectory()) {
                throw new MojoFailureException("Directory does not exist or is not a directory: " + directory.getAbsolutePath());
            }

            List<File> xmlFiles = new ArrayList<>();
            findXmlFiles(directory, xmlFiles);

            for (File file : xmlFiles) {
                printDeps(file);
            }

            checkPluginExistence();

        } catch (Exception e) {
            throw new MojoExecutionException("Error reading MPS files", e);
        }
    }

    private void findXmlFiles(File dir, List<File> xmlFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findXmlFiles(file, xmlFiles);
                } else if (file.getName().endsWith(".xml")) {
                    xmlFiles.add(file);
                }
            }
        }
    }

    private void printDeps(File file) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            dbFactory.setFeature("http://xml.org/sax/features/validation", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(file.getAbsolutePath()));

            NodeList deps = doc.getElementsByTagName("depends");

            for (int i = 0; i < deps.getLength(); i++) {
                String dependency = deps.item(i).getTextContent();
                if (!printedDependencies.contains(dependency)) {
                    printedDependencies.add(dependency);
                    getLog().info("Dependency: " + dependency);
                }
            }

            for (int i = 0; i < deps.getLength(); i++) {
                String dep = deps.item(i).getTextContent();
                File depFile = new File(directory, dep + ".xml");
                if (depFile.exists() && !printedDependencies.contains(dep)) {
                    printDeps(depFile);
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            getLog().error("Error parsing file: " + file.getAbsolutePath(), e);
        }
    }

    private void checkPluginExistence() throws IOException {
        File parentDir = new File(mpsDirectory, "/plugins");

        if (!parentDir.exists() || !parentDir.isDirectory()) {
            getLog().warn("Parent plugins directory not found or is not a directory.");
            return;
        }

        for (String dependency : printedDependencies) {
            String folderName = dependency;

            if (dependency.startsWith("com.intellij")) {

                continue;
            }

            if (dependency.startsWith("jetbrains.")) {
                folderName = dependency.substring(dependency.indexOf('.') + 1);
                folderName = folderName.replace('.', '-');
            }


            File depDir = new File(parentDir, folderName);
            if (!depDir.exists() || !depDir.isDirectory()) {
                resolveMissingDependency(folderName);
            }
        }
    }

    private void resolveMissingDependency(String folderName) throws IOException {
        File sourceDir = new File(installDir, folderName + "/build/artifacts/" + folderName + "/" + folderName);
        File targetDir = new File(directory.getParentFile(), folderName);


        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            getLog().warn("Dependency not found in mps-install-plugins directory: " + folderName);
            return;
        }

        try {
            copyDirectory(sourceDir, targetDir);
            getLog().info("Copied " + folderName + " from installation-dir to plugins directory.");
        } catch (IOException e) {
            getLog().error("Failed to copy " + folderName + " from installation-dir to plugins directory.", e);
        }
    }

    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        for (String child : Objects.requireNonNull(sourceDir.list())) {
            File sourceChild = new File(sourceDir, child);
            File targetChild = new File(targetDir, child);

            if (sourceChild.isDirectory()) {
                copyDirectory(sourceChild, targetChild);
            } else {
                Files.copy(sourceChild.toPath(), targetChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

    }
}
