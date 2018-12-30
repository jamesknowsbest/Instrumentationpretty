# InstrumentationPretty
  This is a short java program implemented to accept the standard instrumentation output from Android test and create HTML(WIP) and  junit XML reports.

  **WARNING**: This project is a work in progress(and mostly a proof of concept) and this software is used as-is without any warranty. Please use your own judgement when using this for your project. 

## Why would someone need this? 
  [In some cases](https://stackoverflow.com/q/50765454/8016330), you may only need to run an adb command to execute the tests. In this case, the only output you would receive is the standard instrumentation output which isn't easily human readable. It also doesn't produce a junit XML report which you can us in [Jenkins](https://plugins.jenkins.io/test-results-analyzer) or similar software. 

## Solution
  The [android tools for gradle](https://android.googlesource.com/platform/tools/base/) has actually already implemented a parser [here](https://android.googlesource.com/platform/tools/base/+/android-5.1.1_r6/ddmlib/src/main/java/com/android/ddmlib/testrunner/InstrumentationResultParser.java). However, it's built for listening to tests and not for reading from STDIN. This code repository is a slight modification of that code base and is intended to be used with the standard output from an instrumentation command which is written to a file.  

## example usage

### Building
 `./gradlew clean shadowJar`

### Running

#### Using with a ADB instrument command 
 `adb shell am instrument -w <test_package_name>/<runner_class> | java -classpath /path/to/Instrumentationpretty-all.jar InstrumentationPretty`

#### Using with an existing file
`cat /path/to/instrument/log/file | java -classpath /path/to/Instrumentationpretty-all.jar InstrumentationPretty`

#### Specify output directory 
`cat /path/to/instrument/log/file | java -classpath /path/to/Instrumentationpretty-all.jar InstrumentationPretty -o <yourDirectoryHere>`

**Note**: if the directory doesn't exist it will be created. By default the output directory is the current working directory/reports as shown by this line in InstrumentationPretty.java

`reportDir = new File(System.getProperty("user.dir") + "/reports");`

