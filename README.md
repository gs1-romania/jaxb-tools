# jaxb-tools

[![Maven Central](https://img.shields.io/maven-central/v/ro.gs1/jaxb-tools)](https://central.sonatype.com/artifact/ro.gs1/jaxb-tools)
[![Maven Build](https://github.com/gs1-romania/jaxb-tools/actions/workflows/build.yml/badge.svg)](https://github.com/gs1-romania/jaxb-tools/actions/workflows/build.yml)
[![Release](https://github.com/gs1-romania/jaxb-tools/actions/workflows/release.yml/badge.svg)](https://github.com/gs1-romania/jaxb-tools/actions/workflows/release.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://adoptium.net/)

XJC plugins for [Jakarta XML Binding](https://jakarta.ee/specifications/xml-binding/) that generate `toString`, `equals`, `hashCode`, inheritance declarations, and list simplifications for JAXB-generated classes.

Available on [Maven Central](https://central.sonatype.com/artifact/ro.gs1/jaxb-tools) and [GitHub Packages](https://github.com/gs1-romania/jaxb-tools/packages).

## Requirements

- Jakarta XML Binding 4.x (`jaxb-xjc` 4.x)
- Apache Commons Lang 3.x

## Installation

```xml
<dependency>
   <groupId>ro.gs1</groupId>
   <artifactId>jaxb-tools</artifactId>
   <version>VERSION</version>
</dependency>
```

Use as a plugin dependency in your XJC configuration:

```xml
<plugin>
   <groupId>org.codehaus.mojo</groupId>
   <artifactId>jaxb2-maven-plugin</artifactId>
   <version>VERSION</version>
   <executions>
      <execution>
         <id>xjc</id>
         <goals><goal>xjc</goal></goals>
      </execution>
   </executions>
   <configuration>
      <arguments>
         <argument>-Xgs1-tostring</argument>
         <argument>-Xgs1-hashcode</argument>
         <argument>-Xgs1-equals</argument>
      </arguments>
   </configuration>
   <dependencies>
      <dependency>
         <groupId>ro.gs1</groupId>
         <artifactId>jaxb-tools</artifactId>
         <version>VERSION</version>
      </dependency>
   </dependencies>
</plugin>
```

---

## Plugins

### toString / hashCode / equals

Activated via XJC arguments. Generates the corresponding method for every class that has at least one instance field. Classes with no instance fields are skipped for `toString` and `hashCode`; `equals` is always generated.

| Plugin | Argument |
|---|---|
| toString | `-Xgs1-tostring` |
| hashCode | `-Xgs1-hashcode` |
| equals | `-Xgs1-equals` |

---

### inheritance

Activated via `-Xgs1-inheritance`. Adds `implements` declarations to generated classes using XSD bindings.

**Bindings namespace:** `http://jaxb-tools.gs1.ro/inheritance`

```xml
<jaxb:bindings version="3.0"
               xmlns:jaxb="https://jakarta.ee/xml/ns/jaxb"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:inheritance="http://jaxb-tools.gs1.ro/inheritance">

   <jaxb:bindings schemaLocation="your-schema.xsd" node="/xs:schema">
      <jaxb:bindings node="xs:complexType[@name='AlcoholInformationModuleType']">
         <inheritance:implements>ro.gs1.BasicExtensionType</inheritance:implements>
      </jaxb:bindings>
   </jaxb:bindings>

</jaxb:bindings>
```

The specified class must be present on the classpath — it is not generated.

---

### simplify

Activated via `-Xgs1-simplify`. Simplifies generated list fields using XSD bindings.

**Bindings namespace:** `http://jaxb-tools.gs1.ro/simplify`

#### replaceGenericList

Changes the generic type of a list field to a specific type.

```xml
<jaxb:bindings version="3.0"
               xmlns:jaxb="https://jakarta.ee/xml/ns/jaxb"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:simplify="http://jaxb-tools.gs1.ro/simplify">

   <jaxb:bindings schemaLocation="your-schema.xsd" node="/xs:schema">
      <jaxb:bindings node="xs:complexType[@name='ExtensionType']">
         <simplify:replaceGenericList field="anies">ro.gs1.BasicExtensionType</simplify:replaceGenericList>
      </jaxb:bindings>
   </jaxb:bindings>

</jaxb:bindings>
```

#### deleteJaxbElementList

Replaces a `List<JAXBElement<?>>` field with a typed list based on the lowest common ancestor of the referenced types. If the types share no common ancestor (other than `Object`), the field is removed and replaced with individual typed fields.

```xml
<jaxb:bindings schemaLocation="your-schema.xsd" node="/xs:schema">
   <jaxb:bindings node="xs:complexType[@name='Scope']//xs:element[@ref='ScopeInformation']">
      <simplify:deleteJaxbElementList/>
   </jaxb:bindings>
</jaxb:bindings>
```
