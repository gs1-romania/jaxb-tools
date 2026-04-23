package ro.gs1.jaxbtools.tostring;

import org.junit.jupiter.api.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XJCToStringPluginTest extends XjcPluginTestBase {

   @Test
   public void generatesToStringForClassWithFields() throws Exception {
      runXjc("-Xgs1-tostring");
      String source = readGeneratedClass("PersonType");
      assertTrue(source.contains("public String toString()"), "PersonType should have a toString() method");
      assertTrue(source.contains("ToStringBuilder"), "toString() should use ToStringBuilder");
      assertTrue(source.contains("\"name\""), "toString() should append 'name' field");
      assertTrue(source.contains("\"age\""), "toString() should append 'age' field");
   }

   @Test
   public void skipsToStringForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-tostring");
      String source = readGeneratedClass("EmptyType");
      assertFalse(source.contains("public String toString()"), "EmptyType should not have a toString() method");
   }
}