package ro.gs1.jaxbtools.equals;

import org.junit.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.Assert.assertTrue;

public class XJCEqualsPluginTest extends XjcPluginTestBase {

   @Test
   public void generatesEqualsForClassWithFields() throws Exception {
      runXjc("-Xgs1-equals");
      String source = readGeneratedClass("PersonType");
      assertTrue("PersonType should have an equals() method", source.contains("public boolean equals(Object"));
      assertTrue("equals() should use EqualsBuilder", source.contains("EqualsBuilder"));
   }

   @Test
   public void generatesEqualsForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-equals");
      // equals is always generated, even for empty classes (returns true)
      String source = readGeneratedClass("EmptyType");
      assertTrue("EmptyType should still have an equals() method", source.contains("public boolean equals(Object"));
   }
}
