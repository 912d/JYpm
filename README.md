# JYpm - Java YouTube Playlist Manager [![Build Status](https://travis-ci.org/Open96/JYpm.svg?branch=master)](https://travis-ci.org/Open96/JYpm)

JYpm is a youtube-dl frontend oriented around YouTube playlist system.

Works on Windows and Linux.

MacOS - Not supported. I don't own a Mac but by Java's nature it should probably run fine. If you have access to MacOS system you can help me test it!

![Example screenshot](https://i.imgur.com/dNd7WFz.png)

# Future plans

Pretty much none. Application does what I wanted it to do,  and I learned many things while creating it.
If I had to write it again I would write it in a cleaner manner (and probably another programming language).
That doesn't mean I think JYpm is bad, it just could have been done much better but I wanted an application that
would let me download youtube playlists and convert them to have them available offline. If I or
someone else encounters any bug I will patch it, but that's all I will do in that project.

# Requirements

* JRE 9+ (JDK 9 for compiling from source)
* JavaFX (most likely bundled into JDK unless you are using Gentoo Linux, which requires adding USE flag to your Java build)
* Apache Maven


# How to convert playlists

1. Download FFmpeg (from their site, or your system's package manager)
2. In settings specify FFmpeg's location (if FFmpeg is available in your system path, ```ffmpeg``` will be enough).
3. Now you can freely convert your playlists

# How to compile

While it is strongly recommended to download executable from releases page, you still can do it yourself.

Instructions below are only for linux users. Windows users can probably do the same assuming they have proper command line tools.

* Open your terminal in location where you want to download source code and type the following:

```git clone https://github.com/Open96/JYpm.git```

* Now enter into cloned directory using cd

```cd JYpm```

* Compile code by using this command, this process may take some if it is your first time using maven because it needs to download dependencies from online repos:

````mvn clean package -DskipTests````

* Now navigate to output directory

```cd jypm-app/target/```

* You should see two .jars and bunch of other files, you are only interested in file ending with -jar-with-dependecies.jar.

* Move it to separate directory somewhere on your system as it will create some files when it's running

* You can run it by double clicking it or typing the following

```java -jar jypm-app-<version>-jar-with-dependencies.jar```

* Look into executables directory if you want to wrap your jar file into executable.


# Maven dependency graph

If you want to contribute this might be useful.

![Dependency graph](https://i.imgur.com/rQMNnlE.png)

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