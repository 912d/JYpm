# JYpm - Java YouTube Playlist Manager 1.0.0 [![Build Status](https://travis-ci.org/Open96/JYpm.svg?branch=master)](https://travis-ci.org/Open96/JYpm)

This Java application lets you download YouTube playlists to your local storage and manage them in a a simple manner.

Works on Windows and Linux. On MacOS some features are disabled, if you have access to MacOS and want to contribute - you are more than welcome.

![Example screenshot](https://i.imgur.com/xkJYMmB.png)

# Requirements

* JRE 9+ (JDK 9 for compiling from source)
* JavaFX (most likely bundled into JDK unless you are using Gentoo Linux or similar distro)
* Apache Maven

# How to compile

Instructions below are for linux system. For other systems process is probably similar.

* Open your terminal in location where you want to download source code and type the following:

```git clone https://github.com/Open96/JYpm.git```

* Now enter into downloaded directory using cd

```cd jypm```

* Compile code by using this command, this process may take some time on first run because maven needs to download dependencies from online repos:

````mvn clean package -DskipTests````

* Now navigate to output directory

```cd jypm-app/target/```

* There should be a file called jypm-app-1.0.0-jar-with-dependencies.jar - this is file you can open to run Ypm by clicking on it or by typing:

```java -jar <FILENAME>```

* Move that .jar to separate directory of your choice and run it.


### Future development plans

* MacOS support
* Video format conversion
* Dumping YouTube API completely
* Playlist deletion should affect downloaded files

# Licensing

This application is distributed under Apache-2.0 license, see LICENSE.md for more details.

JYpm depends on:

* [youtube-dl](https://github.com/rg3/youtube-dl) which is distributed under The Unlicense license
* [log4j](https://logging.apache.org/log4j/2.x/) which is distributed under Apache-2.0 license
* [JUnit](http://junit.org/junit5/) which is distributed under EPL 1.0 license
* [Gson](https://github.com/google/gson) which is distributed under Apache-2.0 license
* [Retrofit](https://github.com/square/retrofit) which is distributed under Apache-2.0 license
* [Commons Lang](https://github.com/apache/commons-lang) which is distributed under Apache-2.0 license
* And their respective dependencies...