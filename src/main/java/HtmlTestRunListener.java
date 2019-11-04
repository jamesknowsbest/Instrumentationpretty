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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes JUnit results to an HTML file for display in browser.
 */
public class HtmlTestRunListener implements ITestRunListener {

    private static final String LOG_TAG = "HtmlResultReporter";

    private static final String TEST_RESULT_FILE_SUFFIX = ".html";
    private static final String TEST_RESULT_FILE_PREFIX = "test_result";

    private File mReportDir = new File(System.getProperty("java.io.tmpdir"));

    private String mReportPath = "";

    /**
     * Test results are grouped by test class name
     */
    private Map<String, TestRunResult> mResults = new LinkedHashMap<>();
    private String mFailureMessage = null;

    /**
     * Sets the report file to use.
     */
    public void setReportDir(File file) {
        mReportDir = file;
    }

    /**
     * Returns Test results grouped by classname. Each class will be rendered as a test case
     */
    public Map<String, TestRunResult> getRunResults() {
        return mResults;
    }

    @Override
    public void testRunStarted(String runName, int numTests) {
        //Do nothing until we get test results
    }

    private TestRunResult getTestRun(TestIdentifier test) {
        TestRunResult result = mResults.get(test.getClassName());
        if (result == null) {
            result = new TestRunResult();
            mResults.put(test.getClassName(), result);
        }

        return result;
    }

    @Override
    public void testStarted(TestIdentifier test) {
        getTestRun(test).reportTestStarted(test);
    }

    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        if (status.equals(TestFailure.ERROR)) {
            getTestRun(test).reportTestFailure(test, TestResult.TestStatus.ERROR, trace);
        } else {
            getTestRun(test).reportTestFailure(test, TestResult.TestStatus.FAILURE, trace);
        }
        Log.d(LOG_TAG, String.format("%s %s: %s", test, status, trace));
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        getTestRun(test).reportTestEnded(test, testMetrics);
    }

    @Override
    public void testSkipped(TestIdentifier test, Map<String, String> testMetrics) {
        getTestRun(test).reportTestSkipped(test, testMetrics);
    }

    @Override
    public void testRunFailed(String errorMessage) {
        mFailureMessage = errorMessage;
    }

    @Override
    public void testRunStopped(long arg0) {
        // ignore
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        generateDocument(mReportDir);
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void generateDocument(File reportDir) {
        FileWriter fw = null;
        try {
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
            printTestResults(context);

            //get the velocity template
            Template template = ve.getTemplate("test_report.html.vm", "UTF-8");
            //merge the context object with the template and write to a string which can be written to a html file
            fw = new FileWriter(reportDir + "/" + TEST_RESULT_FILE_PREFIX + TEST_RESULT_FILE_SUFFIX);
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            fw.write(writer.toString());
            fw.close();
            String msg = String.format("HTML test result file generated at %s. Total tests %d, " +
                            "Failed %d, Skipped %d", getAbsoluteReportPath(), getTotalTests(),
                    getFailedTests(), getSkippedTests());

            Log.logAndDisplay(Log.LogLevel.INFO, LOG_TAG, msg);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
            e.printStackTrace();
            // TODO: consider throwing exception
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String getAbsoluteReportPath() {
        return mReportPath;
    }

    private void printTestResults(VelocityContext context) {
        //add test results to velocity context
        context.put("run_failure_message", mFailureMessage == null ? "" : mFailureMessage);
        context.put("test_count", Integer.toString(getTotalTests()));
        context.put("fail_count", getFailedTests());
        context.put("skipped_count", getSkippedTests());
        context.put("test_suites", mResults);
    }

    private int getTotalTests() {
        return mResults.values().stream().mapToInt(TestRunResult::getNumTests).sum();
    }

    private int getPassedTests() {
        return mResults.values().stream().mapToInt(TestRunResult::getNumPassedTests).sum();
    }

    private int getFailedTests() {
        return mResults.values().stream().mapToInt(
                result -> result.getNumFailedTests() + result.getNumErrorTests()
        ).sum();
    }

    private int getSkippedTests() {
        return mResults.values().stream().mapToInt(TestRunResult::getNumSkippedTests).sum();
    }
}
