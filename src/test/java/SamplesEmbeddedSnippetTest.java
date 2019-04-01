import org.gradle.samples.test.normalizer.FileSeparatorOutputNormalizer;
import org.gradle.samples.test.normalizer.JavaObjectSerializationOutputNormalizer;
import org.gradle.samples.test.runner.EmbeddedSamplesRunner;
import org.gradle.samples.test.runner.SamplesOutputNormalizers;
import org.gradle.samples.test.runner.SamplesRoot;
import org.junit.runner.RunWith;

@RunWith(EmbeddedSamplesRunner.class)
@SamplesRoot("src/docs/asciidoc")
@SamplesOutputNormalizers({JavaObjectSerializationOutputNormalizer.class, FileSeparatorOutputNormalizer.class})
public class SamplesEmbeddedSnippetTest {

}