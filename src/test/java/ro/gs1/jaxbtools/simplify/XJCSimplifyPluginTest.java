package ro.gs1.jaxbtools.simplify;

import org.junit.jupiter.api.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XJCSimplifyPluginTest extends XjcPluginTestBase {

   @Test
   public void replacesGenericListType() throws Exception {
      runXjc("-Xgs1-simplify", "-extension", "-b", bindingsPath("/simplify-bindings.xjb"));
      String source = readGeneratedClass("ContainerType");
      assertTrue(source.contains("ArrayList"), "ContainerType item field should be replaced with ArrayList");
      assertTrue(source.contains("Number"), "ContainerType item field should use Number as type argument");
   }

   @Test
   public void doesNotModifyUnboundClass() throws Exception {
      runXjc("-Xgs1-simplify", "-extension", "-b", bindingsPath("/simplify-bindings.xjb"));
      String source = readGeneratedClass("PersonType");
      assertFalse(source.contains("ArrayList"), "PersonType should not have ArrayList");
   }
}