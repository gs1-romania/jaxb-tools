package ro.gs1.jaxbtools.hashcode;

import org.junit.jupiter.api.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XJCHashCodePluginTest extends XjcPluginTestBase {

   @Test
   public void generatesHashCodeForClassWithFields() throws Exception {
      runXjc("-Xgs1-hashcode");
      String source = readGeneratedClass("PersonType");
      assertTrue(source.contains("public int hashCode()"), "PersonType should have a hashCode() method");
      assertTrue(source.contains("HashCodeBuilder"), "hashCode() should use HashCodeBuilder");
   }

   @Test
   public void skipsHashCodeForClassWithNoFields() throws Exception {
      runXjc("-Xgs1-hashcode");
      String source = readGeneratedClass("EmptyType");
      assertFalse(source.contains("public int hashCode()"), "EmptyType should not have a hashCode() method");
   }
}