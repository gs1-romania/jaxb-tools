package ro.gs1.jaxbtools;

import com.sun.tools.xjc.Driver;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class XjcPluginTestBase {

   @TempDir
   Path tempFolder;

   protected void runXjc(String pluginFlag, String... extraArgs) throws Exception {
      Path xsdPath = Paths.get(getClass().getResource("/test.xsd")
         .toURI());
      List<String> args = new ArrayList<>();
      args.add("-d");
      args.add(tempFolder.toString());
      args.add("-p");
      args.add("generated");
      args.add(pluginFlag);
      Collections.addAll(args, extraArgs);
      args.add(xsdPath.toString());
      int result = Driver.run(args.toArray(new String[0]), System.out, System.err);
      assertEquals(0, result, "XJC compilation should succeed");
   }

   protected String readGeneratedClass(String className) throws Exception {
      Path file = tempFolder.resolve("generated").resolve(className + ".java");
      return new String(Files.readAllBytes(file));
   }

   protected String bindingsPath(String resourceName) throws Exception {
      return Paths.get(getClass().getResource(resourceName).toURI()).toString();
   }
}