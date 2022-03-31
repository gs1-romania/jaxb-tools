# JAXB3-TOOLS - Plugin for [jaxb2-maven-plugin](https://www.mojohaus.org/jaxb2-maven-plugin/Documentation/v2.4/)

This plugin generates the toString, equals and hashCode methods for classes generated with jaxb-xjc.

It works only with JAXB 3.x and Jakarta EE 9.x.
Apache commons-lang3 is required.

## Usage

```xml

   <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-lang3</artifactId>
      <version>XXX</version>
   </dependency>
   <dependency>
      <groupId>ro.gs1</groupId>
      <artifactId>jaxb3-tools</artifactId>
      <version>XXX</version>
   </dependency>

```
