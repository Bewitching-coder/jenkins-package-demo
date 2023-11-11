package com.t;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DemoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("DemoPlugin apply");
//        project.getTasks().register("xalChangeClassName", ChangeClassNameTask.class, changeClassPathTask -> {
//            changeClassPathTask.setProject(project);
//            changeClassPathTask.setGroup("xal");
//            changeClassPathTask.setDescription("修改classPath");
//        });
    }
}
