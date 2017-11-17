# JYpm - Java YouTube Playlist Manager [![Build Status](https://travis-ci.org/Open96/JYpm.svg?branch=master)](https://travis-ci.org/Open96/JYpm)

This Java application lets you download YouTube playlists to your local storage and manage them in a a simple manner.

Works on Windows and Linux. On MacOS some features are disabled, if you have access to MacOS and want to contribute - you are more than welcome.

![Example screenshot](https://i.imgur.com/dNd7WFz.png)

# Requirements

* JRE 9+ (JDK 9 for compiling from source)
* JavaFX (most likely bundled into JDK unless you are using Gentoo Linux or similar distro)
* Apache Maven

# How to compile

While it is strongly recommended to download executable from releases page, you still can do it yourself.

Instructions below are for linux system. For other systems process is probably similar.

* Open your terminal in location where you want to download source code and type the following:

```git clone https://github.com/Open96/JYpm.git```

* Now enter into downloaded directory using cd

```cd jypm```

* Compile code by using this command, this process may take some time on first run because maven needs to download dependencies from online repos:

````mvn clean package -DskipTests````

* Now navigate to output directory

```cd jypm-app/target/```

* There should be a file called jypm-app-<version>-jar-with-dependencies.jar - this is file you can open to run JYpm by clicking on it or by typing:

```java -jar <FILENAME>```

* Move that .jar to separate directory of your choice and run it.

* (ADDITIONAL) Look into executables directory if you want to wrap your jar file into executable.


### Future development plans

* Video format conversion
* Ditch Google's GSON in favor of Jackson library
* If any macOS user wants to have this application usable in macOS please contact me as I don't have macOS system to test it myself

# Maven dependency graph

If you want to contribute it might be useful.

![Dependency graph](https://i.imgur.com/F9MXNOC.png)

# Licensing

This application is distributed under Apache-2.0 license, see LICENSE.md for more details.

JYpm depends on:

* [youtube-dl](https://github.com/rg3/youtube-dl) which is distributed under The Unlicense license
* [log4j](https://logging.apache.org/log4j/2.x/) which is distributed under Apache-2.0 license
* [JUnit](http://junit.org/junit5/) which is distributed under EPL 1.0 license
* [Gson](https://github.com/google/gson) which is distributed under Apache-2.0 license
* [Retrofit](https://github.com/square/retrofit) which is distributed under Apache-2.0 license
* [Commons Lang](https://github.com/apache/commons-lang) which is distributed under Apache-2.0 license
* [jsoup](https://github.com/jhy/jsoup) which is distributed under MIT license
* And their respective dependencies...