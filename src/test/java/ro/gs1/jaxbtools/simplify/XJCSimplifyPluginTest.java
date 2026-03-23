package ro.gs1.jaxbtools.simplify;

import org.junit.Test;
import ro.gs1.jaxbtools.XjcPluginTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XJCSimplifyPluginTest extends XjcPluginTestBase {

   @Test
   public void replacesGenericListType() throws Exception {
      runXjc("-Xgs1-simplify", "-extension", "-b", bindingsPath("/simplify-bindings.xjb"));
      String source = readGeneratedClass("ContainerType");
      assertTrue("ContainerType item field should be replaced with ArrayList", source.contains("ArrayList"));
      assertTrue("ContainerType item field should use Number as type argument", source.contains("Number"));
   }

   @Test
   public void doesNotModifyUnboundClass() throws Exception {
      runXjc("-Xgs1-simplify", "-extension", "-b", bindingsPath("/simplify-bindings.xjb"));
      // PersonType has no simplify customization — its fields should remain as-is
      String source = readGeneratedClass("PersonType");
      assertFalse("PersonType should not have ArrayList", source.contains("ArrayList"));
   }
}
