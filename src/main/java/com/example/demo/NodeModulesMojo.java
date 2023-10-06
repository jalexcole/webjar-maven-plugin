package com.example.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "CopyNodeModules", defaultPhase = LifecyclePhase.VALIDATE)
public class NodeModulesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(name = "nodeModulesPath", defaultValue = "")
    String nodeModulesPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }


    public void getDependencies() {
        Set<Artifact> allArtificats = project.getArtifacts();
        List<Artifact> webjarArtifacts = new LinkedList<>();

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

}
