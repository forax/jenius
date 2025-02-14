# jenius
A small executable to publish HTML files defined using the .xumlv format

### The .xumlv format

TODO

Here is the full DTD is [tipi.dtd](tipi.dtd)

### How it works ?

TODO

### How to get it ?

The [Release](https://github.com/forax/jenius/releases) tab of Github contains several executables
- the java jar file (jenius-*.jar)
- the executable for linux (jenius-*-linux) and
- the executable for macOS (jenius-*-macos).

On macOS, executables downloaded from the Web are not trusted by default,
so you need to remove the quarantine attribute of the executable once you have downloaded it.
```bash
xattr -dr com.apple.quarantine path/to/jenius-*-macos
```
