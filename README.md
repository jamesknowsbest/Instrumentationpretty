# Instrumentationpretty
  This is a short java program implemented to accept the standard instrumentation output from Android test and create HTML and  junit XML reports

## Why would someone need this? 
  [In some cases](https://stackoverflow.com/q/50765454/8016330), you may only need to run an adb command to execute the tests. In this case, the only output you would receive is the standard instrumentation output which isn't easily human readable. It also doens't produce a junit XML report which you can us in [Jenkins](https://plugins.jenkins.io/test-results-analyzer) or similar software. 

## Solution
  The [android tools for gradle](https://android.googlesource.com/platform/tools/base/) has actually already implemented a parser [here](https://android.googlesource.com/platform/tools/base/+/android-5.1.1_r6/ddmlib/src/main/java/com/android/ddmlib/testrunner/InstrumentationResultParser.java). However, it's built for listening to tests and not reading a file. This code repository is a slight modification of that code base and is inteded to be used with the standard output from an instrumentation command which is written to a file.  

## example usage
 TODO

