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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hendraanggrian.RParser;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({"com.zhihu.android.sugaradapter.Layout", "com.zhihu.android.sugaradapter.Id"})
@SupportedOptions({ContainerProcessor.OPTION_MODULE_NAME, ContainerProcessor.OPTION_SUB_MODULES})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ContainerProcessor extends AbstractProcessor {
    static final String OPTION_MODULE_NAME = "moduleNameOfSugarAdapter";
    static final String OPTION_SUB_MODULES = "subModulesOfSugarAdapter";

    private static final Pattern TYPE_PARAM_PATTERN = Pattern.compile("(.*?)<(.*?)>");
    private RParser mLayoutParser;
    private RParser mIdParser;

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        mLayoutParser = RParser.builder(processingEnvironment)
                .setSupportedAnnotations(Collections.singleton(Layout.class))
                .setSupportedTypes("layout")
                .build();

        mIdParser = RParser.builder(processingEnvironment)
                .setSupportedAnnotations(Collections.singleton(Id.class))
                .setSupportedTypes("id")
                .build();
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
        processLayout(roundEnv);
        processId(roundEnv);
        return true;
    }

    // <editor-fold desc="@Layout">

    private void processLayout(@NonNull RoundEnvironment roundEnv) {
        Map<String, Pair> map = new HashMap<>();
        mLayoutParser.scan(roundEnv);

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

                    layoutResStr = mLayoutParser.parse(packageName, layoutRes);
                    if (!layoutResStr.equals(String.valueOf(layoutRes))) {
                        break;
                    }
                }

                if (layoutResStr == null || layoutResStr.equals(String.valueOf(layoutRes))) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                map.put(holderClass, new Pair(layoutResStr, dataClass));
            }
        }

        String moduleName = processingEnv.getOptions().get(OPTION_MODULE_NAME);
        String subModules = processingEnv.getOptions().get(OPTION_SUB_MODULES);
        if (moduleName != null && moduleName.length() > 0 && !map.isEmpty()) {
            try {
                generateContainerDelegateImpl(map);
            } catch (@NonNull Exception e) {
                throw new IllegalStateException(e);
            }
        } else if (subModules != null && subModules.length() > 0 || !map.isEmpty()) {
            try {
                generateContainerDelegateImpl(map);
            } catch (@NonNull Exception e) {
                // noinspection StatementWithEmptyBody
                if (e instanceof FilerException) {
                    // Attempt to recreate a file for type...
                } else {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private void generateContainerDelegateImpl(@NonNull Map<String, Pair> map) throws IOException {
        StringBuilder builder = new StringBuilder();
        String packageName = "com.zhihu.android.sugaradapter";
        builder.append("package ").append(packageName).append(";\n\n");

        builder.append("import android.support.annotation.LayoutRes;\n");
        builder.append("import android.support.annotation.NonNull;\n\n");

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
            builder.append("\n");
            for (String moduleName : subModules.split(",")) {
                String moduleClassName = generateClassName(moduleName);
                builder.append("        mLayoutResMap.putAll(new ")
                        .append(moduleClassName).append("().getLayoutResMap());")
                        .append(" // ").append(moduleName).append("\n");
                builder.append("        mDataClassMap.putAll(new ")
                        .append(moduleClassName).append("().getDataClassMap());")
                        .append(" // ").append(moduleName).append("\n");
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

    private void processId(@NonNull RoundEnvironment roundEnv) {
        Map<String, Set<InjectInfo>> map = new HashMap<>();
        mIdParser.scan(roundEnv);

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

                    viewIdStr = mIdParser.parse(packageName, viewId);
                    if (!viewIdStr.equals(String.valueOf(viewId))) {
                        break;
                    }
                }

                if (viewIdStr == null || viewIdStr.equals(String.valueOf(viewId))) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                Set<InjectInfo> set = map.get(holderClass);
                InjectInfo info = new InjectInfo(viewName, viewType, viewIdStr);
                if (set == null) {
                    set = new HashSet<>();
                    set.add(info);
                    map.put(holderClass, set);
                } else {
                    set.add(info);
                }
            }
        }

        if (!map.isEmpty()) {
            generateInjectDelegateImpl(map);
        }
    }

    private void generateInjectDelegateImpl(@NonNull Map<String, Set<InjectInfo>> map) {
        for (String holderClass : map.keySet()) {
            StringBuilder builder = new StringBuilder();
            int lastIndex = holderClass.lastIndexOf(".");
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            String className = holderClass.substring(lastIndex + 1, holderClass.length()) + "$InjectDelegateImpl";
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
