Potaufeu
========
[![Build Status](https://travis-ci.org/argius/potaufeu.png)](https://travis-ci.org/argius/potaufeu)

Potaufeu is a find utility written in Java.

Potaufeu requires JRE 8 or later.

See [the project's wiki page](https://github.com/argius/potaufeu/wiki) for further information.



To Install
----------

Run the following command.

```sh
$ curl -fsSL http://bit.ly/instpotf | sh
```

or

```sh
$ curl -fsSL https://goo.gl/m54fCw | sh
```

Both of these urls are shortened of `https://raw.githubusercontent.com/argius/potaufeu/master/install.sh`.

To uninstall, remove `~/.potaufeu` and `$(which potf)`.


You need only to download, see [the releases page](https://github.com/argius/potaufeu/releases).



Usage
-----

Run the following command.

```
$ potf --help
```



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
