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

import com.hendraanggrian.RParser;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.zhihu.android.sugaradapter.Id")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectProcessor extends AbstractProcessor {
    private RParser mRParser;

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mRParser = RParser.builder(processingEnvironment)
                .setSupportedAnnotations(Collections.singleton(Id.class))
                .setSupportedTypes("id")
                .build();
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
        Map<String, List<InjectInfo>> map = new HashMap<>();
        mRParser.scan(roundEnv);

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

                    viewIdStr = mRParser.parse(packageName, viewId);
                    if (!viewIdStr.equals(String.valueOf(viewId))) {
                        break;
                    }
                }

                if (viewIdStr == null || viewIdStr.equals(String.valueOf(viewId))) {
                    throw new IllegalStateException("process " + holderClass + " failed!");
                }

                List<InjectInfo> list = map.get(holderClass);
                InjectInfo info = new InjectInfo(viewName, viewType, viewIdStr);
                if (list == null) {
                    list = new ArrayList<>();
                    list.add(info);
                    map.put(holderClass, list);
                } else {
                    list.add(info);
                }
            }
        }

        if (!map.isEmpty()) {
            generateInjectDelegateImpl(map);
        }

        return true;
    }

    private void generateInjectDelegateImpl(@NonNull Map<String, List<InjectInfo>> map) {
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
            builder.append("import android.support.annotation.NonNull;\n\n");

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
}
