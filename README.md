# robotstxt (java)

A java clone of [Google's robotst.txt parser](https://github.com/google/robotstxt), passing all unit tests.

**Disclaimer:**
- The author of this repository is not affiliated to Google by any means.

# Features

- With google specific optimizations, compared with other implmentations (credit to Google authors)
- No extra dependency other than JDK

# How to use

- Add this package to your build dependency, based on your build tool:
  - Maven
    ```xml
    <dependency>
      <groupId>com.github.itechbear</groupId>
      <artifactId>robotstxt</artifactId>
      <version>0.0.1</version>
    </dependency>
    ```
  - Gradle(Groovy)
    ```groovy
    implementation 'com.github.itechbear:robotstxt:0.0.1'
    ```
  - SBT
    ```scale
    libraryDependencies += "com.github.itechbear" % "robotstxt" % "0.0.1"
    ```
  - For any other build tool, please refer to [https://search.maven.org/artifact/com.github.itechbear/robotstxt/0.0.1/jar](https://search.maven.org/artifact/com.github.itechbear/robotstxt/0.0.1/jar)

- Code sample
```java
String robotstxt = "allow: /foo/bar/\n" +
                "\n" +
                "user-agent: FooBot\n" +
                "disallow: /\n" +
                "allow: /x/\n" +
                "user-agent: BarBot\n" +
                "disallow: /\n" +
                "allow: /y/\n" +
                "\n" +
                "\n" +
                "allow: /w/\n" +
                "user-agent: BazBot\n" +
                "\n" +
                "user-agent: FooBot\n" +
                "allow: /z/\n" +
                "disallow: /\n";
String url = "http://test.com/x";

RobotsMatcher matcher = new RobotsMatcher();
// check whether FooBot is allowed to crawl url.
matcher.OneAgentAllowedByRobots(robotstxt, "FooBot", url);
// check whether any of (FooBot,BarBot) is allowed to crawl url
matcher.AllowedByRobots(robotstxt, Arrays.asList("FooBot", "BarBot"), url);
```

# Change log

- 0.0.1 Initial release, based on [google/robotstxt@750aec7](https://github.com/google/robotstxt/tree/750aec7933648c816d6d5bb2f4fe5c30f2485ccf)  
