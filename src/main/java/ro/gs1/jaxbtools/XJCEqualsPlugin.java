package ro.gs1.jaxbtools;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;

public class XJCEqualsPlugin extends Plugin {

   @Override
   public String getOptionName() {
      return "Xgs1-equals";
   }

   @Override
   public int parseArgument(Options opt, String[] args, int i) {
      return 1;
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-equals    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      return false;
   }
}
