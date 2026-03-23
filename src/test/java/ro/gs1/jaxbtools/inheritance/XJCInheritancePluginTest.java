package ro.gs1.jaxbtools.inheritance;

import org.junit.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XJCInheritancePluginTest extends XjcPluginTestBase {

   @Test
   public void addsImplementsFromBindings() throws Exception {
      runXjc("-Xgs1-inheritance", "-extension", "-b", bindingsPath("/inheritance-bindings.xjb"));
      String source = readGeneratedClass("PersonType");
      assertTrue("PersonType should implement Serializable", source.contains("Serializable"));
      assertTrue("PersonType class declaration should have implements", source.contains("implements"));
   }

   @Test
   public void doesNotModifyClassWithNoBindings() throws Exception {
      runXjc("-Xgs1-inheritance", "-extension", "-b", bindingsPath("/inheritance-bindings.xjb"));
      // EmptyType has no inheritance customization in the bindings file
      String source = readGeneratedClass("EmptyType");
      assertFalse("EmptyType should not implement Serializable", source.contains("Serializable"));
   }
}
