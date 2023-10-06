package com.example.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "copyNodeModules", defaultPhase = LifecyclePhase.INITIALIZE)
public class NodeModulesMojo extends AbstractMojo {

    private static final Logger logger = Logger.getLogger(NodeModulesMojo.class.getName());

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(name = "nodeModulesPath", defaultValue = "")
    String nodeModulesPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.info("Running execute");
        getDependencies();
        makeNodeModules();
    }


    public void getDependencies() {
        logger.info(project.getDependencies().toString());

        var dependencies = project.getDependencies();

        
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
            }
        });

    }

    public void makeNodeModules() {
        File file = new File();
        // verify if node modules is there
        // If not there, make it there
    }

}
