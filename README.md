# jenius
A small executable to publish HTML files defined using the .xumlv format

### The .xumlv format (a course content description format)

This document [tipi.md](tipi.md) explains the basics of the .xumlv format.

Here is the full DTD: [tipi.dtd](tipi.dtd)

### How it works ?

jenius takes a source directory, a destination directory and a template as parameter
```bash
  jenius sourceDir destinationDir template.html
```

All the files in the source directory are copied in the destination directory (if they have changed).
The files that ends with `.xumlv` are transformed to a html file using the template file.

As a convenience, the template file can be stored in the folder sourceDir as `template.html` and it will not be copied
in the destination directory.

If you want to generate private files too, you can specify a private destination directory like this
```bash
  jenius sourceDir destinationDir privateDestinationDir template.html
```

There are two examples of templates in the [templates](src/test/resources/com/github/jenius/talc/templates) directory.


### How to get it ?

The [Releases](https://github.com/forax/jenius/releases) tab of Github contains several executables
- the java jar file which run with `java -jar jenius-*.jar`,
- the executable for linux which run with `./jenius-*-linux` and
- the executable for macOS which run with `./jenius-*-macos`.

On macOS, executables downloaded from the Web are not trusted by default,
so you need to remove the quarantine attribute of the executable once you have downloaded it.
```bash
xattr -dr com.apple.quarantine jenius-*-macos
```
