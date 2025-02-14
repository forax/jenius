# Course Content Management Description Format

This DTD is designed for educational institutions managing course content.
It provides support for structured documents, indexes, exercises, team description, and various types of educational materials.
The structure allows for both content organization and presentation formatting.

Here is the full DTD is [tipi.dtd](tipi.dtd).

## Main Container Elements

### td
- Teaching Document container
- Attribute `draft`, true or false
- Must contain a **title** and an optional **subtitle**
- Can contain an **abstract**, **exercise**, **section**, **paragraph**, **list**, **code**, **answer**.

### project
- Project documentation container
- Attribute `draft`, true or false
- Requires a **title**, and an optional **subtitle**
- Can contain an **abstract**, **section**, **paragraph**, **list**, **code**, **answer**.

### index
- Index page container
- Contains **title** and **info** sections
- Can include standard content elements **section**, **paragraph**, **list**, **tdref**.

### doc and cours
- General document and course containers
- Both have a **title**, an optional **subtitle**, and content elements

## Content Elements

### Educational Elements

#### exercise
- Exercise container with a required `title`
- Can contain **paragraph**, **code**, **list**, **answer**

#### answer
- Answer containers for professors
- Can include source references (**srcref**) and content elements

### Text and Formatting

#### Basic Elements
- **abstract**: For summary text
- **paragraph**: Basic text container with formatted text
- **section**: Content with required `title` attribute
- **code**: For code snippets, can contain formatted text and lists
- **list**: Ordered or unordered lists with style attribute

#### Text Styling
- **tt**: Teletype text
- **bold**: Bold text
- **italic**: Italic text
- **underline**: Underlined text
- **sup**: Superscript
- **sub**: Subscript
- **font**: Text with `color` formatting

### Team and Calendar Information

#### infos
- Container for metadata
- Holds **team** info, **calendar**, project reference (**projectref**), **key-date**

#### team
- Contains team member information
- Elements: **leader**, **member**, **url**

#### calendar and keydate
- **calendar**: URL reference for calendar
- **keydate**: Important date marker with title and date

### Media and References

#### image
- Required src attribute
- Optional width, height, align attributes

#### link
- Hyperlink element with required href attribute

#### srcref
- Source code reference
- Required `name` attribute
- Optional `link` attribute

### Reference Elements
- **projectref**: Project reference (name required)
- **projectlink**: Project URL (url and title required)
- **photolink**: Photo URL reference (url required)

## Usage Examples

```xml
<!-- Course index -->
<index>
  <title>Java 101</title>
  <infos>
   ...
  </infos>

  <list>
    <tdref name="td01.xumlv"/>
    <tdref name="td02.xumlv"/>
  </list>  
</index>

<!-- Team information -->
<infos>
  <team>
    <leader name="John Doe" mail="john@example.com" www="example.com/john" />
    <member name="Jane Smith" mail="jane@example.com" www="example.com/jane" />
  </team>
</infos>

<!-- Year index -->
<index>
  <title>2024-2025</title>

  <list>
    <dir name="Java"/>
    <dir name="System"/>
  </list>
</index> 

<!-- Basic td -->
<td>
  <title>Introduction to Java</title>
  <abstract>main, compilation, etc</abstract>
  
  <section title="Overview">
    <paragraph>This course covers fundamental programming concepts...</paragraph>
  </section>
  
  <exercice title="Exercise 1">
    ...
  </exercise>
  
  <exercice title="Exercise 2">
    ...
  </exercise>
</td>

<!-- Exercise example -->
<exercise title="Hello World Program">
  <paragraph>Write a program that prints "Hello, World!" to the console.</paragraph>
  <code>
    System.out.println("Hello, World!");
  </code>
  <answer>
    <paragraph>The solution demonstrates basic output operations...</paragraph>
  </answer>
</exercise>
```
