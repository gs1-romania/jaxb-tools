# JAXB3-TOOLS - Plugin for [jaxb2-maven-plugin](https://www.mojohaus.org/jaxb2-maven-plugin/Documentation/v2.4/)

This plugin generates the toString, equals and hashCode methods for classes generated with jaxb-xjc.

It works only with JAXB 2.x.
Apache commons-lang3 is required.

This library is tested only with GDSN XSD schemas. There are edge cases that are not considered.

## Usage

```xml

<dependencies>
   ...
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
   ...
</dependencies>
```

## Plugin toString/hashCode/equals

Generates the toString/hashCode/equals method for all classes.
It cannot be used in bindings file.

### Usage

```xml

<build>
   ...
   <plugins>
      ...
      <plugin>
         <groupId>org.codehaus.mojo</groupId>
         <artifactId>jaxb2-maven-plugin</artifactId>
         <version>${version.jaxb2-maven-plugin}</version>
         <executions>
            <execution>
               <id>xjc</id>
               <goals>
                  <goal>xjc</goal>
               </goals>
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
               <artifactId>jaxb3-tools</artifactId>
               <version>${version.jaxb2.tools}</version>
            </dependency>
         </dependencies>
      </plugin>
      ...
   </plugins>
   ...
</build>
```

## Plugin inheritance

Provides the ability to specify the implementation of a class.
The implementation class will not be generated and must be in the classpath.
It can be used in bindings file.

### Usage

```xml

...
   <jxb:bindings schemaLocation="../xsd/gs1/gdsn/AlcoholInformationModule.xsd">
      <jxb:bindings node="//xsd:complexType[@name='AlcoholInformationModuleType']">
         <inheritance:implements>ro.gs1.BasicExtensionType</inheritance:implements>
      </jxb:bindings>
   </jxb:bindings>
...
```


## Plugin simplify


It can be used in bindings file.

* replaceGenericList - Change the generic type of a list to the desired type.
* deleteJaxbElementList - If the list is of type List<JAXBElement<?>> then it will be reduced to List<?>

### Usage

```xml

...

   <jxb:bindings schemaLocation="../xsd/gs1/shared/SharedCommon.xsd">
      <jxb:bindings node="//xsd:complexType[@name='ExtensionType']">
         <simplify:replaceGenericList field="anies">ro.gs1.BasicExtensionType</simplify:replaceGenericList>
      </jxb:bindings>
   </jxb:bindings>
   
   <jxb:bindings schemaLocation="../xsd/sbdh/BusinessScope.xsd">
      <jxb:bindings node="//xsd:complexType[@name='Scope']//xsd:element[@ref='ScopeInformation']">
         <simplify:deleteJaxbElementList/>
      </jxb:bindings>
   </jxb:bindings>
...
```


