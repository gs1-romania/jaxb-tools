package ro.gs1.jaxbtools.inheritance;

import org.junit.jupiter.api.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XJCInheritancePluginTest extends XjcPluginTestBase {

   @Test
   public void addsImplementsFromBindings() throws Exception {
      runXjc("-Xgs1-inheritance", "-extension", "-b", bindingsPath("/inheritance-bindings.xjb"));
      String source = readGeneratedClass("PersonType");
      assertTrue(source.contains("Serializable"), "PersonType should implement Serializable");
      assertTrue(source.contains("implements"), "PersonType class declaration should have implements");
   }

   @Test
   public void doesNotModifyClassWithNoBindings() throws Exception {
      runXjc("-Xgs1-inheritance", "-extension", "-b", bindingsPath("/inheritance-bindings.xjb"));
      String source = readGeneratedClass("EmptyType");
      assertFalse(source.contains("Serializable"), "EmptyType should not implement Serializable");
   }
}