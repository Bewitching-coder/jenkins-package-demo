package com.t;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryExtension;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChangeClassNameTask extends org.gradle.api.DefaultTask{

    private Project project;

    private final Map<String, String> classPathMap = new HashMap<>();

    public void setProject(Project p) {
        project = p;
    }

    @TaskAction
    public void execute() {
        System.out.println("[ChangeClassNameTask], changeClassName");
        File path = new File(project.getProjectDir(), "class.change-name");
        if (!path.exists()) {
            System.out.println("[ChangeClassNameTask] change name file is not exist : " + path.getAbsolutePath());
            return;
        }
        try {
            List<String> values = FileUtils.readLines(path, "UTF-8");
            if (values.size() == 0) {
                System.out.println("[ChangeClassPathTask] change path file is empty");
                return;
            }
            for (String value : values) {
                if (value.isEmpty() || value.trim().startsWith("#")) {
                    continue;
                }
                String[] split = value.split(":");
                if (split.length != 2) {
                    continue;
                }
                classPathMap.put(split[0].trim(), split[1].trim());
            }
            project.getRootProject().getAllprojects().forEach(this::projectConsumer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void projectConsumer(Project project) {
        System.out.println("[ChangeClassPathTask] projectConsumer = " + project.getName());
        BaseExtension android = (BaseExtension) project.getExtensions().findByName("android");
        if (android == null) {
            project.afterEvaluate(p -> {
                BaseExtension andr = (BaseExtension) p.getExtensions().findByName("android");
                doExtension(project, andr);
            });
        } else {
            doExtension(project, android);
        }
    }

    private void doExtension(Project p, BaseExtension baseExtension) {
        if (baseExtension instanceof AppExtension) {
            ((AppExtension) baseExtension).getApplicationVariants().all(applicationVariant -> {
                System.out.println("[ChangeClassPathTask] doExtension applicationVariant = " + applicationVariant.getName());
                onVariant(p, baseExtension, applicationVariant.getName());
            });
        } else if (baseExtension instanceof LibraryExtension){
            ((LibraryExtension) baseExtension).getLibraryVariants().all(libraryVariant -> {
                System.out.println("[ChangeClassPathTask] doExtension libraryVariant = " + libraryVariant.getName());
                onVariant(p, baseExtension, libraryVariant.getName());
            });
        }
    }

    private void onVariant(Project p, BaseExtension extension, String variantName) {
        //1、遍历res下的xml文件，找到自定义的类(View/Fragment/四大组件等)，并将混淆结果同步到xml文件内
        XmlUtils.dealXml(p, extension, variantName, classPathMap);
        //2、仅修改文件名及文件路径，
        ClzSourceUtils.dealClass(extension, variantName, classPathMap);
    }

//    private List<ClassInfo> replaceNavigationFragmentName(String text) {
//        List<ClassInfo> classInfos = new ArrayList<>();
//        try {
//            Node node = new XmlParser(false, false).parse(text);
//            for (Object child : node.children()) {
//                if (!(child instanceof Node)) {
//                    continue;
//                }
//                Node children = (Node) child;
//                String childName = (String) children.name();
//                if ("fragment".equals(childName)) {
//                    String className = children.attribute("android:name").toString();
//                    classInfos.add(new ClassInfo(className, children.children() != null, false));
//                }
//            }
//        } catch (Exception e) {}
//        return classInfos;
//    }
//
//    private List<ClassInfo> findClassInLayout(String text, String pkg) {
//        List<ClassInfo> classInfos = new ArrayList<>();
//        try {
//            String[] destAttributes = new String[]{"tools:context", "app:layout_behavior", "app:layoutManager", "android:name"};
//            List childrenList = new XmlParser(false, false).parseText(text).breadthFirst();
//            for (Object child : childrenList) {
//                if (!(child instanceof Node)) {
//                    continue;
//                }
//                Node childNode = (Node) child;
//                for (String attr : destAttributes) {
//                    String attributeValue = childNode.attribute(attr).toString();
//                    if (attributeValue == null || attributeValue.isEmpty()) {
//                        continue;
//                    }
//                    String classname = attributeValue.startsWith(".") ? pkg + attributeValue : attributeValue;
//                    classInfos.add(new ClassInfo(classname, false, false));
//                }
//                String nodeName = childNode.name().toString();
//                if ("variable".equals(nodeName) || "import".equals(nodeName)) {
//                    String typeValue = childNode.attribute("type").toString();
//                    classInfos.add(new ClassInfo(typeValue, false,nodeName.equals("import")));
//                } else {
//                    classInfos.add(new ClassInfo(nodeName, false, false));
//                }
//            }
//        } catch (Exception e) {}
//        return classInfos;
//    }
//


}
