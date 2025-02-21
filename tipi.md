# Course Content Description Format

A format designed for educational institutions managing course content.
It provides support for structured documents, indexes, exercises, team description, and various types of educational materials.
The structure allows for both content organization and presentation formatting.

Here is the full DTD is [tipi.dtd](tipi.dtd).

## Main Container Elements

### td
- Teaching Document container
- Attribute `draft`, true or false
- Must contain a **title** and an optional **subtitle**
- Can contain an **abstract**, **exercise**, **section**, **paragraph**, **list**, **code**, **answer**.

### index
- Directory or index page container
- Contains **title** and **info** sections
- Can include standard content elements **section**, **paragraph**, **list**, **dir** or **tdref**.

### project
- Project documentation container
- Attribute `draft`, true or false
- Requires a **title**, and an optional **subtitle**
- Can contain an **abstract**, **section**, **paragraph**, **list**, **code**, **answer**.

### doc and cours
- General document and course containers
- Both have a **title**, an optional **subtitle**, and content elements

## Content Elements

### Educational Elements

#### exercise
- Exercise container with a required `title`
- Can contain **paragraph**, **code**, **list**, **item**, **answer**

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
- **keydate**: Important date marker with `title` and `date`

### reference elements
- **projectref**: Project reference (`name` required)
- **projectlink**: Project URL (`url` and `title` required)
- **photolink**: Photo URL reference (`url` required)

### Media

#### image
- Required `src` attribute
- Optional `width`, `height` and `align` attributes

#### link
- Hyperlink element with required `href` attribute

#### srcref
- Source code reference
- Required `name` attribute
- Optional `link` attribute

## Usage Examples

```xml
<!-- Year index -->
<index>
  <title>2024-2025</title>

  <list>
    <dir name="Java"/>
    <dir name="System"/>
  </list>
</index>

<!-- Course index -->
<index>
  <title>Java 101</title>
  <infos>
    <team>
      <leader name="John Doe" mail="john@example.com" www="example.com/john" />
      <member name="Jane Smith" mail="jane@example.com" www="example.com/jane" />
    </team>
  </infos>

  <list>
    <tdref name="td01.xumlv"/>
    <tdref name="td02.xumlv"/>
  </list>  
</index>

<!-- Td example -->
<td>
  <title>Introduction to Java</title>
  <abstract>main, compilation, etc</abstract>
  
  <section title="Overview">
    <paragraph>This course covers fundamental programming concepts...</paragraph>
  </section>
  
  <exercise title="Hello World Program">
    <paragraph>Write a program that prints "Hello, World!" to the console.</paragraph>
    <code>
      System.out.println("Hello, World!");
    </code>
    <answer>
      <paragraph>The solution demonstrates basic output operations...</paragraph>
    </answer>
  </exercise>

  <exercise title="Exercise 1">
    ...
  </exercise>
</td>
```
