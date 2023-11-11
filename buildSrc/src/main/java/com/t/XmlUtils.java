package com.t;

import com.android.build.gradle.BaseExtension;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import groovy.util.Node;
import groovy.util.XmlParser;

public class XmlUtils {

    public static void dealXml(Project project, BaseExtension extension, String variantName, Map<String, String> classPathMap) {
        File file = extension.getSourceSets().getByName("main").getManifest().getSrcFile();
        try {
            Node rootNode = new XmlParser(false, false).parse(file);
            //查找清单文件里的package属性值
            String packageName = rootNode.attribute("package").toString();
            // System.out.println("packageName = " + packageName);
            List<File> dirs = new ArrayList<>();
            // 查找res中布局目录
            Set<File> resSourceSet = extension.getSourceSets().getByName("main").getRes().getSrcDirs();
            dealXmlFileSourceSet(resSourceSet, classPathMap);
            if (variantName!= null && variantName.length() > 0
                    && !variantName.equals("release") && !variantName.equals("debug")) {
                String varname = "";
                if (variantName.endsWith("Debug")) {
                    varname = variantName.substring(0, variantName.length() - 5);
                } else {
                    varname = variantName.substring(0, variantName.length() - 7);
                }
                Set<File> variantResSourceSet = extension.getSourceSets().getByName(varname).getRes().getSrcDirs();
                dealXmlFileSourceSet(variantResSourceSet, classPathMap);
            }

            // 查找manifest
            dealManifest(file, classPathMap, packageName);
        } catch (Exception e) {}
    }

    private static void dealXmlFileSourceSet(Set<File> fileSet, Map<String, String> classPathMap) {
        for (File f : fileSet) {
            File[] fs = f.listFiles();
            if (fs == null || fs.length == 0) {
                continue;
            }
            for (File dir : fs) {
                if (dir == null || !dir.exists() || dir.isFile()) {
                    continue;
                }
                String xmlDir = dir.getName();
                if (xmlDir.startsWith("layout")
                        || xmlDir.startsWith("navigation")
                        || xmlDir.startsWith("xml")) {
                    //System.out.println("res dir = " + dir.getAbsolutePath());
                    File[] files = dir.listFiles();
                    if (files == null || files.length == 0) {
                        continue;
                    }
                    for (File file : files) {
                        if (file == null || !file.exists() || file.isDirectory()) {
                            continue;
                        }
                        //System.out.println("res path = " + file.getAbsolutePath());
                        resContentReplace(file, classPathMap);
                    }
                }
            }
        }
    }

    private static void resContentReplace(File file, Map<String, String> classPathMap) {
        try {
            String content = FileUtils.readFileToString(file, Charset.defaultCharset());
            boolean hasModify = false;
            for (String key : classPathMap.keySet()) {
                String value = classPathMap.get(key);
                if (content.contains(key)) {
                    content = content.replaceAll(key, value);
                    hasModify = true;
                }
            }
            if (hasModify) {
                FileUtils.writeStringToFile(file, content);
            }
        } catch (Exception ignored) {}
    }

    private static void dealManifest(File file, Map<String, String> classPathMap, String pkg) {
        //System.out.println("manifest path = " + file.getAbsolutePath());
        try {
            // 2.DocumentBuilderFactory 对象，用来创建 DocumentBuilder 对象
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            // 3、创建 DocumentBuilder 对象，用来将 XML 文件 转化为 Document 对象
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            // 4、创建 Document 对象，解析 XML 文件
            Document document = documentBuilder.parse(file);
            NodeList nodeList = document.getElementsByTagName("application");
            boolean hasChange = false;
            if (nodeList == null || nodeList.getLength() <= 0) {
                return;
            }
            org.w3c.dom.Node appNode = nodeList.item(0);
            if (appNode == null) {
                return;
            }
            org.w3c.dom.Node appNameNode = appNode.getAttributes().getNamedItem("android:name");
            if (appNameNode != null) {
                String appName = appNameNode.getNodeValue();
                String appClassPath = appName.startsWith(".") ? pkg + appName : appName;
                //System.out.println("manifest application name = " + appClassPath);
                if (classPathMap.containsKey(appClassPath)) {
                    appNameNode.setNodeValue(classPathMap.get(appClassPath));
                    hasChange = true;
                }
            }
            NodeList components = appNode.getChildNodes();
            for (int i = 0; i < components.getLength(); i ++) {
                org.w3c.dom.Node component = components.item(i);
                if (component == null) {
                    continue;
                }
                String componentName = component.getNodeName();
                if ("activity".equals(componentName)
                    || "service".equals(componentName)
                    || "receiver".equals(componentName)
                    || "provider".equals(componentName)
                    || "activity-alise".equals(componentName)) {
                    org.w3c.dom.Node componentNameAttr = component.getAttributes().getNamedItem("android:name");
                    if (componentNameAttr == null) {
                        continue;
                    }
                    String name = componentNameAttr.getNodeValue();
                    String componentClassPath = name.startsWith(".") ? pkg + name : name;
                    if (classPathMap.containsKey(componentClassPath)) {
                        componentNameAttr.setNodeValue(classPathMap.get(componentClassPath));
                        hasChange = true;
                    }
                }
            }
            if (hasChange) {
                writeXml(document, file);
            }
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
    }

    private static void writeXml(Document document, File output) throws FileNotFoundException, TransformerException {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.transform(new DOMSource(document), new StreamResult(new FileOutputStream(output)));
    }
}
