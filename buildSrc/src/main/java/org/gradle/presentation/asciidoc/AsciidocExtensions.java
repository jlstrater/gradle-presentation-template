package org.gradle.presentation.asciidoc;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.extension.spi.ExtensionRegistry;

public class AsciidocExtensions implements ExtensionRegistry {
    @Override
    public void register(Asciidoctor asciidoctor) {
        JavaExtensionRegistry registry = asciidoctor.javaExtensionRegistry();

        registry.docinfoProcessor(new MultiLanguageSamplesDocinfoProcessor());
        registry.includeProcessor(SampleIncludeProcessor.class);
        registry.inlineMacro(new ScreencastAsciidoctorExtension());
    }
}