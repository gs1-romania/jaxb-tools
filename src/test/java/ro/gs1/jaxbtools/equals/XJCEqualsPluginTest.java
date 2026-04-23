package ro.gs1.jaxbtools.equals;

import org.junit.jupiter.api.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XJCEqualsPluginTest extends XjcPluginTestBase {

   @Test
   public void generatesEqualsForClassWithFields() throws Exception {
      runXjc("-Xgs1-equals");
      String source = readGeneratedClass("PersonType");
      assertTrue(source.contains("public boolean equals(Object"), "PersonType should have an equals() method");
      assertTrue(source.contains("EqualsBuilder"), "equals() should use EqualsBuilder");
   }

   @Test
   public void generatesEqualsForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-equals");
      String source = readGeneratedClass("EmptyType");
      assertTrue(source.contains("public boolean equals(Object"), "EmptyType should still have an equals() method");
   }
}