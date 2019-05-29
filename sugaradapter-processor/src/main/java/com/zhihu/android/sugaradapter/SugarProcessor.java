/*
 * Copyright 2018 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhihu.android.sugaradapter;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hendraanggrian.RParser;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SupportedAnnotationTypes({SugarProcessor.ANNOTATION_TYPE_LAYOUT, SugarProcessor.ANNOTATION_TYPE_ID})
@SupportedOptions({SugarProcessor.OPTION_MODULE_NAME, SugarProcessor.OPTION_SUB_MODULES})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SugarProcessor extends AbstractProcessor {
    static final String ANNOTATION_TYPE_LAYOUT = "com.zhihu.android.sugaradapter.Layout";
    static final String ANNOTATION_TYPE_ID = "com.zhihu.android.sugaradapter.Id";
    static final String OPTION_MODULE_NAME = "moduleNameOfSugarAdapter";
    static final String OPTION_SUB_MODULES = "subModulesOfSugarAdapter";

    private static final Pattern TYPE_PARAM_PATTERN = Pattern.compile("(.*?)<(.*?)>");

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
        // not pure function but just work :/
        processId(roundEnv, processLayout(roundEnv));
        return true;
    }

    // <editor-fold desc="@Layout">

    @CheckResult
    private Map<String, Set<String>> processLayout(@NonNull RoundEnvironment roundEnv) {
        RParser parser = RParser.builder(processingEnv)
                .setSupportedAnnotations(Collections.singleton(Layout.class))
                .setSupportedTypes("layout")
                .build();
        parser.scan(roundEnv);

        Map<String, Pair> containerMap = new HashMap<>();
        Map<String, Set<String>> superclassMap = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Layout.class)) {
            if (element instanceof TypeElement) {
                String holderClass = ((TypeElement) element).getQualifiedName().toString();
                int layoutRes = element.getAnnotation(Layout.class).value();

                String dataClass = null;
                TypeMirror mirror = ((TypeElement) element).getSuperclass();
                while (mirror != null && !(mirror instanceof NoType)) {
                    Matcher matcher = TYPE_PARAM_PATTERN.matcher(mirror.toString());
                    if (matcher.matches()) {
                        // remove generic type from dataClass
                        dataClass = matcher.group(2).trim().replaceAll("<[^>]*>", "");
                        break;
                    } else {
                        mirror = ((TypeElement) processingEnv.getTypeUtils().asElement(mirror)).getSuperclass();
                    }
                }

                if (layoutRes == 0 || dataClass == null) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                String layoutResStr = null;
                String packageName = null;
                for (String path : holderClass.split("\\.")) {
                    if (packageName == null) {
                        packageName = path;
                    } else {
                        packageName = packageName + "." + path;
                    }

                    layoutResStr = parser.parse(packageName, layoutRes);
                    if (!layoutResStr.equals(String.valueOf(layoutRes))) {
                        break;
                    }
                }

                if (layoutResStr == null || layoutResStr.equals(String.valueOf(layoutRes))) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                containerMap.put(holderClass, new Pair(layoutResStr, dataClass));

                // find all available superclass for next step process @Id
                mirror = ((TypeElement) element).getSuperclass();
                while (mirror != null && !(mirror instanceof NoType)) {
                    TypeElement temp = ((TypeElement) processingEnv.getTypeUtils().asElement(mirror));
                    Set<String> set = superclassMap.get(holderClass);
                    if (set == null) {
                        set = new HashSet<>();
                        set.add(temp.getQualifiedName().toString());
                        superclassMap.put(holderClass, set);
                    } else  {
                        set.add(temp.getQualifiedName().toString());
                    }

                    mirror = temp.getSuperclass();
                }
            }
        }

        String moduleName = processingEnv.getOptions().get(OPTION_MODULE_NAME);
        String subModules = processingEnv.getOptions().get(OPTION_SUB_MODULES);
        if (moduleName != null && moduleName.length() > 0 && !containerMap.isEmpty()) {
            try {
                generateContainerDelegateImpl(containerMap);
            } catch (@NonNull Exception e) {
                throw new IllegalStateException(e);
            }
        } else if (subModules != null && subModules.length() > 0 || !containerMap.isEmpty()) {
            try {
                generateContainerDelegateImpl(containerMap);
            } catch (@NonNull Exception e) {
                // noinspection StatementWithEmptyBody
                if (e instanceof FilerException) {
                    // Attempt to recreate a file for type...
                } else {
                    throw new IllegalStateException(e);
                }
            }
        }

        return superclassMap;
    }

    private void generateContainerDelegateImpl(@NonNull Map<String, Pair> map) throws IOException {
        StringBuilder builder = new StringBuilder();
        String packageName = "com.zhihu.android.sugaradapter";
        builder.append("package ").append(packageName).append(";\n\n");

        builder.append("import androidx.annotation.LayoutRes;\n");
        builder.append("import androidx.annotation.NonNull;\n\n");

        builder.append("import java.util.HashMap;\n");
        builder.append("import java.util.Map;\n\n");

        // for module project
        String className = generateClassName(processingEnv.getOptions().get(OPTION_MODULE_NAME));
        builder.append("public final class ").append(className).append(" implements ContainerDelegate {\n");
        builder.append("    private Map<Class<? extends SugarHolder>, Integer> mLayoutResMap;\n");
        builder.append("    private Map<Class<? extends SugarHolder>, Class> mDataClassMap;\n\n");

        builder.append("    public ").append(className).append("() {\n");
        builder.append("        mLayoutResMap = new HashMap<>();\n");
        builder.append("        mDataClassMap = new HashMap<>();\n");
        if (!map.isEmpty()) {
            builder.append("\n");
        }

        for (String key : map.keySet()) {
            String layoutResStr = map.get(key).getFirst();
            String dataClass = map.get(key).getSecond();
            builder.append("        mLayoutResMap.put(").append(key).append(".class, ")
                    .append(layoutResStr).append(");\n");
            builder.append("        mDataClassMap.put(").append(key).append(".class, ")
                    .append(dataClass).append(".class);\n");
        }

        // for main project
        String subModules = processingEnv.getOptions().get(OPTION_SUB_MODULES);
        if (subModules != null && subModules.length() > 0) {
            for (String moduleName : subModules.split(",")) {
                String moduleClassName = generateClassName(moduleName);
                String moduleVariableName = moduleClassName.toLowerCase();
                builder.append("\n");
                builder.append("        ").append(moduleClassName).append(" ").append(moduleVariableName)
                        .append(" = new ").append(moduleClassName).append("();")
                        .append(" // ").append(moduleName).append("\n");
                builder.append("        mLayoutResMap.putAll(")
                        .append(moduleVariableName).append(".getLayoutResMap());\n");
                builder.append("        mDataClassMap.putAll(")
                        .append(moduleVariableName).append(".getDataClassMap());\n");
            }
        }

        builder.append("    }\n\n");

        builder.append("    @NonNull\n");
        builder.append("    public Map<Class<? extends SugarHolder>, Integer> getLayoutResMap() {\n");
        builder.append("        return mLayoutResMap;\n");
        builder.append("    }\n\n");

        builder.append("    @NonNull\n");
        builder.append("    public Map<Class<? extends SugarHolder>, Class> getDataClassMap() {\n");
        builder.append("        return mDataClassMap;\n");
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    @LayoutRes\n");
        builder.append("    public int getLayoutRes(@NonNull Class<? extends SugarHolder> holderClass) {\n");
        builder.append("        return mLayoutResMap.get(holderClass);\n");
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    @NonNull\n");
        builder.append("    public Class getDataClass(@NonNull Class<? extends SugarHolder> holderClass) {\n");
        builder.append("        return mDataClassMap.get(holderClass);\n");
        builder.append("    }\n");
        builder.append("}\n");

        JavaFileObject object = processingEnv.getFiler().createSourceFile(packageName + "." + className);
        Writer writer = object.openWriter();
        writer.write(builder.toString());
        writer.flush();
        writer.close();
    }

    @NonNull
    private String generateClassName(@Nullable String moduleName) {
        if (moduleName == null) {
            return "ContainerDelegateImpl";
        } else {
            // abs(hashCode) maybe conflict, for huge project :P
            return "ContainerDelegateImpl" + Math.abs(moduleName.trim().hashCode());
        }
    }

    // </editor-fold>

    // <editor-fold desc="@Id">

    private void processId(@NonNull RoundEnvironment roundEnv, Map<String, Set<String>> superclassMap) {
        RParser parser = RParser.builder(processingEnv)
                .setSupportedAnnotations(Collections.singleton(Id.class))
                .setSupportedTypes("id")
                .build();
        parser.scan(roundEnv);

        Map<String, Set<InjectInfo>> injectInfoMap = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Id.class)) {
            if (element instanceof VariableElement) {
                String holderClass = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();

                VariableElement ve = (VariableElement) element;
                String viewName = ve.getSimpleName().toString();
                String viewType = ve.asType().toString();

                int viewId = element.getAnnotation(Id.class).value();
                if (viewId == 0) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                String viewIdStr = null;
                String packageName = null;
                for (String path : holderClass.split("\\.")) {
                    if (packageName == null) {
                        packageName = path;
                    } else {
                        packageName = packageName + "." + path;
                    }

                    viewIdStr = parser.parse(packageName, viewId);
                    if (!viewIdStr.equals(String.valueOf(viewId))) {
                        break;
                    }
                }

                if (viewIdStr == null || viewIdStr.equals(String.valueOf(viewId))) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                Set<InjectInfo> set = injectInfoMap.get(holderClass);
                InjectInfo info = new InjectInfo(viewName, viewType, viewIdStr);
                if (set == null) {
                    set = new HashSet<>();
                    set.add(info);
                    injectInfoMap.put(holderClass, set);
                } else {
                    set.add(info);
                }
            }
        }

        // put all superclass @Id to current class
        for (String holderClass : superclassMap.keySet()) {
            for (String superclass : superclassMap.get(holderClass)) {
                if (injectInfoMap.containsKey(superclass)) {
                    Set<InjectInfo> set = injectInfoMap.get(holderClass);
                    if (set == null) {
                        set = new HashSet<>(injectInfoMap.get(superclass));
                        injectInfoMap.put(holderClass, set);
                    } else  {
                        set.addAll(injectInfoMap.get(superclass));
                    }
                }
            }
        }

        if (!injectInfoMap.isEmpty()) {
            generateInjectDelegateImpl(injectInfoMap);
        }
    }

    private void generateInjectDelegateImpl(@NonNull Map<String, Set<InjectInfo>> map) {
        for (String holderClass : map.keySet()) {
            StringBuilder builder = new StringBuilder();
            int lastIndex = holderClass.lastIndexOf(".");
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            String className = holderClass.substring(lastIndex + 1) + "$InjectDelegateImpl";
            builder.append("package ").append(holderClass, 0, lastIndex).append(";\n\n");

            builder.append("import android.annotation.SuppressLint;\n");
            builder.append("import android.view.View;\n");
            builder.append("import androidx.annotation.NonNull;\n\n");

            builder.append("import com.zhihu.android.sugaradapter.InjectDelegate;\n");
            builder.append("import com.zhihu.android.sugaradapter.SugarHolder;\n\n");

            builder.append("public final class ").append(className).append(" implements InjectDelegate {\n");
            builder.append("    @Override\n");
            builder.append("    @SuppressLint(\"ResourceType\")\n");
            builder.append("    public <SH extends SugarHolder> void injectView(@NonNull SH sh, @NonNull View view) {\n");
            builder.append("        if (sh instanceof ").append(holderClass).append(") {\n");
            builder.append("            ").append(holderClass).append(" th = (").append(holderClass).append(") sh;\n");

            for (InjectInfo info : map.get(holderClass)) {
                builder.append("            th.").append(info.getViewName())
                        .append(" = (").append(info.getViewType()).append(")")
                        .append(" view.findViewById(").append(info.getViewIdStr()).append(");\n");
            }

            builder.append("        }\n");
            builder.append("    }\n");
            builder.append("}\n");

            try {
                JavaFileObject object = processingEnv.getFiler().createSourceFile(className);
                Writer writer = object.openWriter();
                writer.write(builder.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // </editor-fold>
}
