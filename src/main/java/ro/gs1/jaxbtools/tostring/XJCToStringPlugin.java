package ro.gs1.jaxbtools.tostring;

import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

public class XJCToStringPlugin extends Plugin {

   private static final Logger logger = LoggerFactory.getLogger(XJCToStringPlugin.class);

   @Override
   public String getOptionName() {
      return "Xgs1-tostring";
   }

   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCToStringPlugin) Activated.");
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-tostring    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      JCodeModel model = new JCodeModel();
      JClass toStringBuilderClass = model.ref(ToStringBuilder.class);
      logger.debug("(XJCToStringPlugin) Found {} classes.", outline.getClasses().size());
      for (ClassOutline classOutline : outline.getClasses()) {
         Set<Entry<String, JFieldVar>> fields = classOutline.implClass.fields().entrySet().stream()
               .filter(e -> (e.getValue().mods().getValue() & STATIC) == 0)
               .collect(Collectors.toSet());
         if (fields.isEmpty()) {
            logger.debug("(XJCToStringPlugin) Skip toString for class {}: no instance fields.", classOutline.implClass.name());
            continue;
         }
         logger.debug("(XJCToStringPlugin) Generating toString for class {}.", classOutline.implClass.name());
         JMethod method = classOutline.implClass.method(PUBLIC, String.class, "toString");
         method.annotate(Override.class);
         JBlock body = method.body();
         JVar builder = body.decl(0, toStringBuilderClass, "builder",
               JExpr._new(toStringBuilderClass).arg(JExpr._this()));
         for (Entry<String, JFieldVar> e : fields) {
            logger.debug("(XJCToStringPlugin) Appending field {} to toString.", e.getKey());
            body.add(builder.invoke("append").arg(e.getKey()).arg(e.getValue()));
         }
         body._return(builder.invoke("toString"));
      }
      return true;
   }
}
