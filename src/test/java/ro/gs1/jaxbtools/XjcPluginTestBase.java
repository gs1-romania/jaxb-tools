package ro.gs1.jaxbtools;

import com.sun.tools.xjc.Driver;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class XjcPluginTestBase {

   @Rule
   public TemporaryFolder tempFolder = new TemporaryFolder();

   /**
    * Runs XJC against test.xsd with the given plugin flag and any extra arguments (e.g. "-b", bindingsPath,
    * "-extension").
    */
   protected void runXjc(String pluginFlag, String... extraArgs) throws Exception {
      Path xsdPath = Paths.get(getClass().getResource("/test.xsd")
         .toURI());
      Path outputDir = tempFolder.getRoot()
         .toPath();
      List<String> args = new ArrayList<>();
      args.add("-d");
      args.add(outputDir.toString());
      args.add("-p");
      args.add("generated");
      args.add(pluginFlag);
      Collections.addAll(args, extraArgs);
      args.add(xsdPath.toString());
      int result = Driver.run(args.toArray(new String[0]), System.out, System.err);
      assertEquals("XJC compilation should succeed", 0, result);
   }

   protected String readGeneratedClass(String className) throws Exception {
      Path file = tempFolder.getRoot()
         .toPath()
         .resolve("generated")
         .resolve(className + ".java");
      return new String(Files.readAllBytes(file));
   }

   protected String bindingsPath(String resourceName) throws Exception {
      return Paths.get(getClass().getResource(resourceName)
            .toURI())
         .toString();
   }
}
