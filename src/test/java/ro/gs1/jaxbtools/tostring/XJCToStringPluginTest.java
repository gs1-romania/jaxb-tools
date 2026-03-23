package ro.gs1.jaxbtools.tostring;

import org.junit.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XJCToStringPluginTest extends XjcPluginTestBase {

   @Test
   public void generatesToStringForClassWithFields() throws Exception {
      runXjc("-Xgs1-tostring");
      String source = readGeneratedClass("PersonType");
      assertTrue("PersonType should have a toString() method", source.contains("public String toString()"));
      assertTrue("toString() should use ToStringBuilder", source.contains("ToStringBuilder"));
      assertTrue("toString() should append 'name' field", source.contains("\"name\""));
      assertTrue("toString() should append 'age' field", source.contains("\"age\""));
   }

   @Test
   public void skipsToStringForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-tostring");
      String source = readGeneratedClass("EmptyType");
      assertFalse("EmptyType should not have a toString() method", source.contains("public String toString()"));
   }
}
