import org.apache.oro.io.GlobFilenameFilter;
import org.junit.*;
import org.junit.rules.*;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class InstrumentationPrettyTest {
    @Rule
    public TemporaryFolder testOutputFolder = new TemporaryFolder();

    @Test public void testXMLReportGeneration() throws IOException {
        InstrumentationPretty ip = new InstrumentationPretty(testOutputFolder.getRoot().getAbsolutePath());

        ip.processInsturmentationOutput();

        final FilenameFilter xmlResultFilter = new GlobFilenameFilter("test_result_*.xml");
        File[] xmlReports = testOutputFolder.getRoot().listFiles(xmlResultFilter);
        assertEquals("processInsturmentationOutput should generate a file ", 1, xmlReports.length);
    }
}
