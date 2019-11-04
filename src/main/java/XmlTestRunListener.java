/*
 * Modified for https://github.com/jamesknowsbest/Instrumentationpretty
 * Original license follows
 */
/*
 * Copyright (C) 2009 The Android Open Source Project
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

import org.kxml2.io.KXmlSerializer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * Writes JUnit results to an XML files in a format consistent with
 * Ant's XMLJUnitResultFormatter.
 * <p/>
 * Creates a separate XML file per test run.
 * <p/>
 */
public class XmlTestRunListener implements ITestRunListener {

    private static final String LOG_TAG = "XmlResultReporter";

    private static final String TEST_RESULT_FILE_SUFFIX = ".xml";
    private static final String TEST_RESULT_FILE_PREFIX = "test_result_";

    private static final String TESTSUITE = "testsuite";
    private static final String TESTCASE = "testcase";
    private static final String ERROR = "error";
    private static final String FAILURE = "failure";
    private static final String SKIPPED = "skipped";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_ERRORS = "errors";
    private static final String ATTR_FAILURES = "failures";
    private static final String ATTR_SKIPPED = "skipped";
    private static final String ATTR_TESTS = "tests";
    //private static final String ATTR_TYPE = "type";
    //private static final String ATTR_MESSAGE = "message";
    private static final String PROPERTIES = "properties";
    private static final String ATTR_CLASSNAME = "classname";
    private static final String TIMESTAMP = "timestamp";
    private static final String HOSTNAME = "hostname";

    /** the XML namespace */
    private static final String ns = null;

    private String mHostName = "localhost";

    private File mReportDir = new File(System.getProperty("java.io.tmpdir"));

    private String mReportPath = "";

    private TestRunResult mRunResult = new TestRunResult();

    /**
     * Sets the report file to use.
     */
    public void setReportDir(File file) {
        mReportDir = file;
    }

    public void setHostName(String hostName) {
        mHostName = hostName;
    }

    /**
     * Returns the {@link TestRunResult}
     * @return the test run results.
     */
    public TestRunResult getRunResult() {
        return mRunResult;
    }

    @Override
    public void testRunStarted(String runName, int numTests) {
        mRunResult = new TestRunResult(runName);
    }

    @Override
    public void testStarted(TestIdentifier test) {
       mRunResult.reportTestStarted(test);
    }

    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        if (status.equals(TestFailure.ERROR)) {
            mRunResult.reportTestFailure(test, TestResult.TestStatus.ERROR, trace);
        } else {
            mRunResult.reportTestFailure(test, TestResult.TestStatus.FAILURE, trace);
        }
        Log.d(LOG_TAG, String.format("%s %s: %s", test, status, trace));
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        mRunResult.reportTestEnded(test, testMetrics);
    }

    @Override
    public void testSkipped(TestIdentifier test, Map<String, String> testMetrics) {
        mRunResult.reportTestSkipped(test, testMetrics);
    }

    @Override
    public void testRunFailed(String errorMessage) {
        mRunResult.setRunFailureError(errorMessage);
    }

    @Override
    public void testRunStopped(long arg0) {
        // ignore
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        mRunResult.setRunComplete(true);
        generateDocument(mReportDir, elapsedTime);
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void generateDocument(File reportDir, long elapsedTime) {
        String timestamp = getTimestamp();

        OutputStream stream = null;
        try {
            stream = createOutputResultStream(reportDir);
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(stream, "UTF-8");
            serializer.startDocument("UTF-8", null);
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);

            //create velocity object 
            VelocityEngine ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

            ve.init();

            //get template from jar and confirm it's there
            InputStream input = this.getClass().getClassLoader().getResourceAsStream("test_report.html.vm");
            if (input == null) {
                throw new IOException("Template file doesn't exist");
            }
            //close the stream as the file's confirmed to be present and not needed now
            input.close();
            //create context object to hold test results and map to variables
            VelocityContext context = new VelocityContext();
            // TODO: insert build info
            printTestResults(serializer,context, timestamp, elapsedTime);
            serializer.endDocument();
            //get the velocity template
            Template template = ve.getTemplate("test_report.html.vm", "UTF-8");
            //merge the context object with the template and write to a string which can be written to a html file
            FileWriter fw = new FileWriter(this.mReportDir + "/" + TEST_RESULT_FILE_PREFIX + ".html");
            StringWriter writer = new StringWriter();
            template.merge( context, writer );
            fw.write(writer.toString());
            fw.close();
            String msg = String.format("XML test result file generated at %s. Total tests %d, " +
                    "Failed %d, Error %d, Skipped %d", getAbsoluteReportPath(), mRunResult.getNumTests(),
                    mRunResult.getNumFailedTests(), mRunResult.getNumErrorTests(), mRunResult.getNumSkippedTests());

            Log.logAndDisplay(Log.LogLevel.INFO, LOG_TAG, msg);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
            e.printStackTrace();
            // TODO: consider throwing exception
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String getAbsoluteReportPath() {
        return mReportPath ;
    }

    /**
     * Return the current timestamp as a {@link String}.
     */
    String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault());
        TimeZone gmt = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(gmt);
        dateFormat.setLenient(true);
        String timestamp = dateFormat.format(new Date());
        return timestamp;
    }

    /**
     * Creates a {@link File} where the report will be created.
     * @param reportDir the root directory of the report.
     * @return a file
     * @throws IOException
     */
    protected File getResultFile(File reportDir) throws IOException {
        File reportFile = File.createTempFile(TEST_RESULT_FILE_PREFIX, TEST_RESULT_FILE_SUFFIX,
                reportDir);
        Log.i(LOG_TAG, String.format("Created xml report file at %s",
                reportFile.getAbsolutePath()));

        return reportFile;
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     */
    OutputStream createOutputResultStream(File reportDir) throws IOException {
        File reportFile = getResultFile(reportDir);
        mReportPath = reportFile.getAbsolutePath();
        return new BufferedOutputStream(new FileOutputStream(reportFile));
    }

    protected String getTestSuiteName() {
        return mRunResult.getName();
    }

    void printTestResults(KXmlSerializer serializer, VelocityContext context, String timestamp, long elapsedTime)
            throws IOException {
        serializer.startTag(ns, TESTSUITE);
        String name = getTestSuiteName();
        if (name != null) {
            serializer.attribute(ns, ATTR_NAME, name);
        }
        serializer.attribute(ns, ATTR_TESTS, Integer.toString(mRunResult.getNumTests()));
        serializer.attribute(ns, ATTR_FAILURES, Integer.toString(mRunResult.getNumFailedTests()));
        serializer.attribute(ns, ATTR_ERRORS, Integer.toString(mRunResult.getNumErrorTests()));
        serializer.attribute(ns, ATTR_SKIPPED, Integer.toString(mRunResult.getNumSkippedTests()));
        serializer.attribute(ns, ATTR_TIME, Double.toString((double) elapsedTime / 1000.f));
        serializer.attribute(ns, TIMESTAMP, timestamp);
        serializer.attribute(ns, HOSTNAME, mHostName);

        //add test results to velocity context 
        context.put("test_count", Integer.toString(mRunResult.getNumTests()));
        context.put("fail_count", Integer.toString(mRunResult.getNumFailedTests()));
        context.put("skipped_count", Integer.toString(mRunResult.getNumSkippedTests()));
        context.put("test_suites",mRunResult.getTestResults());
        context.put("suite_result",mRunResult.getSuiteResult());

        serializer.startTag(ns, PROPERTIES);
        setPropertiesAttributes(serializer, ns);
        serializer.endTag(ns, PROPERTIES);

        Map<TestIdentifier, TestResult> testResults = mRunResult.getTestResults();
        for (Map.Entry<TestIdentifier, TestResult> testEntry : testResults.entrySet()) {
            print(serializer, testEntry.getKey(), testEntry.getValue());
        }

        serializer.endTag(ns, TESTSUITE);
    }

    /**
     * Sets the attributes on properties.
     * @param serializer the serializer
     * @param namespace the namespace
     * @throws IOException
     */
    protected void setPropertiesAttributes(KXmlSerializer serializer, String namespace)
            throws IOException {
    }

    protected String getTestName(TestIdentifier testId) {
        return testId.getTestName();
    }

    void print(KXmlSerializer serializer, TestIdentifier testId, TestResult testResult)
            throws IOException {

        serializer.startTag(ns, TESTCASE);
        serializer.attribute(ns, ATTR_NAME, getTestName(testId));
        serializer.attribute(ns, ATTR_CLASSNAME, testId.getClassName());
        long elapsedTimeMs = testResult.getEndTime() - testResult.getStartTime();
        serializer.attribute(ns, ATTR_TIME, Double.toString((double) elapsedTimeMs / 1000.f));

        if (!TestResult.TestStatus.PASSED.equals(testResult.getStatus())) {

            String result;
            switch (testResult.getStatus()) {
                case FAILURE:
                    result = FAILURE;
                    break;
                case SKIPPED:
                    result = SKIPPED;
                    break;
                default:
                    result = ERROR;
            }

//            String result = testResult.getStatus().equals(TestResult.TestStatus.FAILURE) ? FAILURE : ERROR;
            serializer.startTag(ns, result);
            // TODO: get message of stack trace ?
//            String msg = testResult.getStackTrace();
//            if (msg != null && msg.length() > 0) {
//                serializer.attribute(ns, ATTR_MESSAGE, msg);
//            }
           // TODO: get class name of stackTrace exception
            //serializer.attribute(ns, ATTR_TYPE, testId.getClassName());
            String stackTrace = testResult.getStackTrace();
            String stackText = stackTrace == null ? "" : sanitize(stackTrace);
            serializer.text(stackText);
            serializer.endTag(ns, result);
        }

        serializer.endTag(ns, TESTCASE);
     }

    /**
     * Returns the text in a format that is safe for use in an XML document.
     */
    private String sanitize(String text) {
        return text.replace("\0", "<\\0>");
    }
}
