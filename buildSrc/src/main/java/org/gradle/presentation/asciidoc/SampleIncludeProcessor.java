/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.presentation.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.DocumentRuby;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SampleIncludeProcessor extends IncludeProcessor {
    private static final String SAMPLE = "sample";
    private static final Map<String, String> FILE_SUFFIX_TO_SYNTAX = initializeSyntaxMap();

    // Map file suffixes to syntax highlighting where they differ
    private static Map<String, String> initializeSyntaxMap() {
        Map<String, String> map = new HashMap<>();
        map.put("gradle", "groovy");
        map.put("kt", "kotlin");
        map.put("kts", "kotlin");
        map.put("py", "python");
        map.put("sh", "bash");
        map.put("rb", "ruby");
        return Collections.unmodifiableMap(map);
    }

    // Even though these are unused, these constructors are necessary to prevent
    // "(ArgumentError) asciidoctor: FAILED: Failed to load AsciiDoc document - wrong number of arguments (1 for 0)"
    // See https://github.com/asciidoctor/asciidoctorj/issues/451#issuecomment-210914940
    // This is fixed in asciidoctorj 1.6.0
    public SampleIncludeProcessor() {
        super(new HashMap<>());
    }

    public SampleIncludeProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public boolean handles(String target) {
        return target.equals(SAMPLE);
    }

    @Override
    public void process(DocumentRuby document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        this.process(document(document), reader, target, attributes);
    }

    // This method adheres the asciidoctorj 1.6.0 API
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        if (!attributes.containsKey("dir") || !attributes.containsKey("files")) {
            throw new IllegalStateException("Both the 'dir' and 'files' attributes are required to include a sample");
        }

        final String sampleBaseDir = document.getAttr("samples-dir", ".").toString();
        final String sampleDir = attributes.get("dir").toString();
        final List<String> files = Arrays.asList(attributes.get("files").toString().split(";"));

        final String sampleContent = getSampleContent(sampleBaseDir, sampleDir, files);
        reader.push_include(sampleContent, target, target, 1, attributes);
    }

    private String getSourceSyntax(String fileName) {
        String syntax = "txt";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            String substring = fileName.substring(i + 1);
            syntax = FILE_SUFFIX_TO_SYNTAX.getOrDefault(substring, substring);
        }
        return syntax;
    }

    private String getSampleContent(String sampleBaseDir, String sampleDir, List<String> files) {
        final StringBuilder builder = new StringBuilder(String.format("%n[.testable-sample.multi-language-sample,dir=\"%s\"]%n=====%n", sampleDir));
        for (String fileDeclaration : files) {
            final String sourceRelativeLocation = parseSourceFilePath(fileDeclaration);
            final List<String> tags = parseTags(fileDeclaration);
            final String sourceSyntax = getSourceSyntax(sourceRelativeLocation);
            String sourcePath = String.format("%s/%s/%s", sampleBaseDir, sampleDir, sourceRelativeLocation);
            String source = getContent(sourcePath);
            if (!tags.isEmpty()) {
                source = filterByTag(source, sourceSyntax, tags);
            }
            builder.append(String.format(".%s%n[source,%s]%n----%n%s%n----%n", sourceRelativeLocation, sourceSyntax, source));
        }

        builder.append(String.format("=====%n"));
        return builder.toString();
    }

    private String getContent(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read source file " + Paths.get(filePath).toAbsolutePath().toFile().getAbsolutePath());
        }
    }

    protected String parseSourceFilePath(String fileDeclaration) {
        return fileDeclaration.replaceAll("\\[[^]]*]", "");
    }

    protected List<String> parseTags(String fileDeclaration) {
        final List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile(".*\\[tags?=(.*)].*");
        Matcher matcher = pattern.matcher(fileDeclaration);
        if (matcher.matches()) {
            tags.addAll(Arrays.asList(matcher.group(1).split(",")));
        }
        return tags;
    }

    private String filterByTag(String source, String syntax, List<String> tags) {
        Pattern tagPattern;
        if (syntax.equals("html") || syntax.equals("xml")) {
            tagPattern = Pattern.compile("\\s*<!--\\s*(tag|end)::(\\S+)\\[]\\s*-->");
        } else {
            tagPattern = Pattern.compile(".*(tag|end)::(\\S+)\\[]\\s*");
        }

        StringBuilder result = new StringBuilder(source.length());
        String activeTag = null;
        BufferedReader reader = new BufferedReader(new StringReader(source));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (activeTag != null) {
                    if (line.contains("end::" + activeTag + "[]")) {
                        activeTag = null;
                    } else if (!tagPattern.matcher(line).matches()) {
                        result.append(line).append("\n");
                    }
                } else {
                    activeTag = determineActiveTag(line, tags);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception while filtering tagged content");
        }

        return result.toString();
    }

    private String determineActiveTag(String line, List<String> tags) {
        for (String tag : tags) {
            if (line.contains("tag::" + tag + "[]")) {
                return tag;
            }
        }
        return null;
    }
}
