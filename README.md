# jenius
A small executable that generate course content

### Course content Description

The course content is described using the .xumlv format (an XML format) defined by the [tipi DTD](tipi.dtd).

The document [tipi.md](tipi.md) explains the basics of the .xumlv format.

### How it works ?

jenius takes a source directory, a destination directory and a template as parameter
```bash
  jenius [options] sourceDir destinationDir template.html
```

All the files in the source directory are copied in the destination directory (if they have changed).
The files that ends with `.xumlv` are transformed to a html file using the template file.

As a convenience, the template file can be stored in the folder sourceDir as `template.html` and it will not be copied
in the destination directory.

There are two examples of templates in the [templates](src/test/resources/com/github/jenius/talc/templates) directory.

If you want to generate private files too, you can specify a private destination directory like this
```bash
  jenius [options] sourceDir destinationDir privateDestinationDir template.html
```

Options are
 - `--force` ask to update all the files even if there is no change
 - `--watch` go into a loop that watch for filesystem changes and rerun jenius on them
 - `--serve` run an http server on port 8080 to see the destinationDir (or the privateDestinationDir)

### How to get it ?

The [Releases](https://github.com/forax/jenius/releases) tab of GitHub contains several executables
- the java jar file which run with `java -jar jenius-*.jar` (requires [Java 23+](https://www.oracle.com/java/technologies/downloads/)),
- the executable for linux which run with `./jenius-*-linux` and
- the executable for macOS which run with `./jenius-*-macos`.

On macOS, executables downloaded from the Web are not trusted by default,
so you need to remove the quarantine attribute of the executable once you have downloaded it.
```bash
xattr -dr com.apple.quarantine jenius-*-macos
```
