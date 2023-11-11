package com.t;

import com.android.build.gradle.BaseExtension;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClzSourceUtils {

    private static Map<String, String> classMap;

    public static void dealClass(BaseExtension extension, String variantName, Map<String, String> classPathMap) {
        Set<File> javaSourceSet = extension.getSourceSets().getByName("main").getJava().getSrcDirs();
        dealClassSet(javaSourceSet, classPathMap);
        if (variantName != null && variantName.length() > 0
                && !variantName.equals("release") && !variantName.equals("debug")) {
            String varname = "";
            if (variantName.endsWith("Debug")) {
                varname = variantName.substring(0, variantName.length() - 5);
            } else {
                varname = variantName.substring(0, variantName.length() - 7);
            }
            Set<File> variantJavaSourceSet = extension.getSourceSets().getByName(varname).getJava().getSrcDirs();
            dealClassSet(variantJavaSourceSet, classPathMap);
        }
    }

    private static void dealClassSet(Set<File> files, Map<String, String> classPathMap) {
        classMap = classPathMap;
        if (classMap==null || classMap.isEmpty()) {
            return;
        }
        // 替换引用
        replaceImport(files);
        classMap = null;
        // 移动类，替换包名
        moveClassFiles(files, classPathMap);

    }

    private static void moveClassFiles(Set<File> files, Map<String, String> classPathMap) {
        files.forEach(file -> moveClassFile(file, classPathMap));
    }

    private static void moveClassFile(File file, Map<String, String> classPathMap) {
        for (String key : classPathMap.keySet()) {
            String orgClzPath = key.replace(".", File.separator);
            String value = classPathMap.get(key);
            File orgClzFullJavaPath = new File(file, orgClzPath+".java");
            if (orgClzFullJavaPath.exists()) {
                String javaTargetPath = value.replace(".", File.separator)+".java";
                File targetFile = new File(file, javaTargetPath);
                moveFileTo(orgClzFullJavaPath, targetFile, "import " + value);
                continue;
            }
            File orgClzFullKotlinPath = new File(file, orgClzPath+".kt");
            if (orgClzFullKotlinPath.exists()) {
                String kotlinTargetPath = value.replace(".", File.separator)+".kt";
                File targetFile = new File(file, kotlinTargetPath);
                moveFileTo(orgClzFullJavaPath, targetFile, "import " + value);
                continue;
            }
        }
    }

    private static void moveFileTo(File orgFile, File targetFile, String importStr) {
        File dir = targetFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File[] files = orgFile.getParentFile().listFiles();
        for (File f : files) {
            if (f == orgFile && f == targetFile) {
                continue;
            }
            try {
                int index = 0;
                boolean hasImport = false;
                List<String> lines = FileUtils.readLines(f);
                for (int i = 0; i < lines.size(); i ++) {
                    String l = lines.get(i);
                    if (l.contains(importStr)) {
                        hasImport = true;
                        continue;
                    }
                    if (l.trim().contains("package ")) {
                        index = i;
                    }
                }
                System.out.println("index = " + index);
                if (!hasImport) {
                    lines.add(index + 1, importStr + ";");
                }
                FileUtils.writeLines(f, lines);
            } catch (Exception e) {}
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }
        try {
            FileUtils.moveFile(orgFile, targetFile);
        } catch (Exception e) {}
    }

    private static void replaceImport(Set<File> files) {
        files.forEach(file -> {
            traverseDir(file, file);
        });
    }

    private static void traverseDir(File path, File root) {
        if (!path.exists() || !path.isDirectory()) {
            doFile(path, root);
            return;
        }
        File[] subFiles = path.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (subFile.isFile()) {
                doFile(subFile, root);
            }else if (subFile.isDirectory()) {
                traverseDir(subFile, root);
            }
        }
    }

    private static final String[] sign = new String[] {
            "{", "(", " ", "\t", "\n", "<", "?", ":", ",", ">", ".",")"
    };

    private static void doFile(File file, File root) {
        if (!file.getName().endsWith(".java") && !file.getName().endsWith(".kt")){
            return;
        }
        System.out.println("file = " + file.getAbsoluteFile());
        String content = null;
        try {
            content = FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (Exception e) {
        }
        if (content == null || (content.length() == 0)) {
            return;
        }
        boolean hasChange = false;
        for (String key : classMap.keySet()) {
            String value = classMap.get(key);
            if (content.contains(key)) {
                content = content.replaceAll(key, value);
                hasChange = true;
            }
            String orgClass = getClassName(key);
            String orgPkg = getPkg(key);
            String tarClass = getClassName(value);
            String tarPkg = getPkg(value);
            for (String prefix : sign) {
                String orgSign = prefix + orgClass;
                String tarSign = prefix + tarClass;
                for (String sub : sign) {
                    orgSign = orgSign + sub;
                    tarSign = tarSign + sub;
                    content = content.replace(orgSign, tarSign);
                }
            }
//
//            if (content.contains("\t"+orgClass+" ")){
//                content = content.replace("\t"+orgClass+" ", "\t"+tarClass+" ");
//                hasChange = true;
//            }
//            if (content.contains("\n"+orgClass+" ")){
//                content = content.replace(" "+orgClass+" ", " "+tarClass+" ");
//                hasChange = true;
//            }
//            if (content.contains("("+orgClass+" ")){
//                content = content.replace("("+orgClass+" ", "("+tarClass+" ");
//                hasChange = true;
//            }
//            if (content.contains(" "+orgClass+" ")){
//                content = content.replace(" "+orgClass+" ", " "+tarClass+" ");
//                hasChange = true;
//            }
//            if (content.contains(" "+orgClass+",")){
//                content = content.replace(" "+orgClass+",", " "+tarClass+",");
//                hasChange = true;
//            }
//            if (content.contains("<"+orgClass+",")){
//                content = content.replace("<"+orgClass+",", "<"+tarClass+",");
//                hasChange = true;
//            }
//            if (content.contains("<"+orgClass+" ")){
//                content = content.replace("<"+orgClass+" ", "<"+tarClass+" ");
//                hasChange = true;
//            }
//            if (content.contains(" "+orgClass+"?")){
//                content = content.replace(" "+orgClass+"?", " "+tarClass+"?");
//                hasChange = true;
//            }
//            if (content.contains(" "+orgClass+"?")){
//                content = content.replace(" "+orgClass+"?", " "+tarClass+"?");
//                hasChange = true;
//            }
//            if (content.contains(" "+orgClass+"(")){
//                content = content.replace(" "+orgClass+"(", " "+tarClass+"(");
//                hasChange = true;
//            }
//            if (content.contains(orgClass + ".")) {
//                content = content.replace(orgClass + ".", tarClass + ".");
//                hasChange = true;
//            }


            String orgClassSign1 = "class " + orgClass + " {";
            if (content.contains(orgClassSign1)) {
                String tarClassSign1 = "class " + tarClass + " {";
                content = content.replace(orgClassSign1, tarClassSign1);
                hasChange = true;
            }
            if (content.contains("class " + orgClass + "{")) {
                content = content.replace("class " + orgClass + "{", "class " + tarClass + "{");
                hasChange = true;
            }
            if (content.contains("class " + orgClass + "\n{")) {
                content = content.replace("class " + orgClass + "\n{", "class " + tarClass + "\n{");
                hasChange = true;
            }
            //"class\\s*(?<className>[a-zA-Z][a-zA-Z\\d_\\$]*)"
//            content = content.replaceAll("class\\s*\\(?<" + orgClass + ">\\s*\\{", tarClass);

            // 需要修改的类进行包名修改
            String orgPath = key.replace(".", File.separator);
            File orgJava = new File(root, orgPath+".java");
            File orgKotlin = new File(root, orgPath+".kt");
            if (orgJava == file || orgKotlin == file) {
                boolean contains = content.contains("package " + orgPkg);
                if (orgPkg != null && contains && tarPkg != null) {
                    content = content.replace("package " + orgPkg, "package " + tarPkg);
                } else if (orgPkg != null && contains) {
                    content = content.replace("package " + orgPkg, "//package " + orgPkg);
                } else if (orgPkg == null && tarPkg != null) {
                    content = "package " + tarPkg + ";\n" + content;
                }
            }
        }
        if (hasChange) {
            try {
                FileUtils.writeStringToFile(file, content);
            } catch (Exception e) {
            }
        }

    }

    private static String getClassName(String str) {
        int nIndex = str.lastIndexOf(".");
        if (nIndex > 0) {
            return str.substring(nIndex + 1);
        } else {
            return str;
        }
    }

    private static String getPkg(String str) {
        int nIndex = str.lastIndexOf(".");
        if (nIndex > 0) {
            return str.substring(0, nIndex);
        } else {
            return null;
        }
    }
}
