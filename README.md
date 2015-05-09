Potaufeu
========
[![Build Status](https://travis-ci.org/argius/potaufeu.png)](https://travis-ci.org/argius/potaufeu)

Potaufeu is a find utility written in Java.

Potaufeu requires JRE 8 or later.



Download
--------

Download the latest Jar file from the release page in GitHub.



Usage
-----

Run the following command.

```
$ java -jar /path/to/potaufeu.jar --help
```

`potf` is the proposed command name.
Set up alias if you need.



Examples
--------

If you want to find some files which have .java or .xml extension and file size larger than 50KB,
run the command below.

```
$ potf .java,xml -s 50KB-
```


Run the following command to show posix-like file attributes (similar to ls -l).

```
$ potf .java,xml --list-posix
```



License
-------

Apache 2.0 License.
