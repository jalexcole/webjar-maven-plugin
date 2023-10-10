package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "copyNodeModules", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class NodeModulesMojo extends AbstractMojo {

    private static final Logger logger = Logger.getLogger(NodeModulesMojo.class.getName());

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(name = "nodeModulesPath", defaultValue = "")
    String nodeModulesPath;

    private final static String NODE_MODULES_DIR = "node_modules";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.info("Running execute");
        makeNodeModules();
        getDependencies();

    }

    public void getDependencies() {
        logger.info(project.getDependencies().toString());

        var dependencies = project.getDependencies();
        Set<Dependency> webJars = new HashSet<>();

        dependencies.forEach(dependency -> {
            if (dependency.getGroupId().contains("org.webjar")) {
                webJars.add(dependency);
            }

            logger.info("SystemPath: " + dependency.getSystemPath());
        });

        Set<Artifact> allArtificats = project.getArtifacts();
        List<Artifact> webjarArtifacts = new LinkedList<>();
        logger.info(allArtificats.toString());
        allArtificats.forEach(artifact -> {
            if (artifact.getGroupId().contains("org.webjars")) {
                webjarArtifacts.add(artifact);
            }
        });

        webjarArtifacts.forEach(artifact -> {
            if (artifact.getArtifactId().equals("webjar-locator")) {
                webjarArtifacts.remove(artifact);
            } else {
                processArtifact(artifact);
            }

        });

    }

    public void processArtifact(Artifact artifact) {

        Path artifactPath = artifact.getFile().toPath();
        try {
            Path tempPath = File.createTempFile("Node-modules-temp", artifact.toString()).toPath();
            unzipFolder(artifactPath, tempPath);

            List<String> expectedPath = new LinkedList<>();
            expectedPath.add(tempPath.toFile().getAbsolutePath());
            expectedPath.add("META-INF");
            expectedPath.add("resources");
            expectedPath.add("webjars");
            expectedPath.add(artifact.getArtifactId());
            expectedPath.add(artifact.getVersion());

            List<String> directories = new ArrayList<>();
            Arrays.asList(nodeModulesPath.split("/")).forEach(directories::add);
            directories.add(NODE_MODULES_DIR);
            directories.add(artifact.getArtifactId());
            new File(String.join("/", directories)).mkdir();
            if (tempPath.toFile().list().length != 0) {
                FileUtils.copyDirectory(new File(String.join("/", expectedPath)),
                        new File(String.join("/", directories)));
            }

            tempPath.toFile().delete();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
        }

    }

    public void makeNodeModules() throws MojoFailureException {
        File baseDir = project.getBasedir();
        if (nodeModulesPath.isEmpty()) {
            throw new MojoFailureException("The path for \"nodeModulesPath\" is empty");
        }
        List<String> directories = new ArrayList<>();

        directories.add(baseDir.getName());
        Arrays.asList(nodeModulesPath.split("/")).forEach(directories::add);
        directories.add(NODE_MODULES_DIR);

        File nodeModules = new File(String.join("/", directories));

        if (!nodeModules.exists()) {
            logger.info("Node modules exists: " + nodeModules.exists());

            logger.info("Making directory: " + nodeModules.mkdirs());
            new File(baseDir.getName() + "/src").mkdir();
            logger.info("Path Location: " + nodeModules.getPath());
        }

        // verify if node modules is there
        // If not there, make it there
    }

    public static void unzipFolder(Path source, Path target) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

            // list files in zip
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {

                boolean isDirectory = false;
                // example 1.1
                // some zip stored files and folders separately
                // e.g data/
                // data/folder/
                // data/folder/file.txt
                if (zipEntry.getName().endsWith(File.separator)) {
                    isDirectory = true;
                }

                Path newPath = zipSlipProtect(zipEntry, target);

                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {

                    // example 1.2
                    // some zip stored file path only, need create parent directories
                    // e.g data/folder/file.txt
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // copy files, nio
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy files, classic
                    /*
                     * try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                     * byte[] buffer = new byte[1024];
                     * int len;
                     * while ((len = zis.read(buffer)) > 0) {
                     * fos.write(buffer, 0, len);
                     * }
                     * }
                     */
                }

                zipEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        }

    }

    // protect zip slip attack
    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
            throws IOException {

        // test zip slip vulnerability
        // Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // make sure normalized file still has targetDir as its prefix
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }

        return normalizePath;
    }

}
