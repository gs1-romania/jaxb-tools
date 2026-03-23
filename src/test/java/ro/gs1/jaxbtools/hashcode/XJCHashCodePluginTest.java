package ro.gs1.jaxbtools.hashcode;

import org.junit.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XJCHashCodePluginTest extends XjcPluginTestBase {

   @Test
   public void generatesHashCodeForClassWithFields() throws Exception {
      runXjc("-Xgs1-hashcode");
      String source = readGeneratedClass("PersonType");
      assertTrue("PersonType should have a hashCode() method", source.contains("public int hashCode()"));
      assertTrue("hashCode() should use HashCodeBuilder", source.contains("HashCodeBuilder"));
   }

   @Test
   public void skipsHashCodeForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-hashcode");
      String source = readGeneratedClass("EmptyType");
      assertFalse("EmptyType should not have a hashCode() method", source.contains("public int hashCode()"));
   }
}
