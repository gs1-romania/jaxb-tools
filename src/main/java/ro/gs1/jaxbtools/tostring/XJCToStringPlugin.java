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

   private static Logger logger = LoggerFactory.getLogger(XJCToStringPlugin.class);

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
      JClass equalsBuilderClass = model.ref(ToStringBuilder.class);
      logger.debug("(XJCToStringPlugin) Found {} classes.", outline.getClasses()
            .size());
      for (ClassOutline o : outline.getClasses()) {
         logger.debug("(XJCToStringPlugin) Generate toString for class {}.", o.implClass.name());
         // remove static fields
         Set<Entry<String, JFieldVar>> fields = o.implClass.fields()
               .entrySet()
               .stream()
               .filter(aa -> (aa.getValue()
                     .mods()
                     .getValue() & STATIC) == 0)
               .collect(Collectors.toSet());
         if (fields.size() == 0) {
            logger.debug("(XJCHashCodePlugin) Skip toString for class {}, reason: no fields.", o.implClass.name());
            continue;
         }
         JMethod method = o.implClass.method(PUBLIC, String.class, "toString");
         method.annotate(Override.class);
         JBlock body = method.body();
         JVar toStringBuilder = body.decl(0, equalsBuilderClass, "toStringBuilder", JExpr._new(equalsBuilderClass)
               .arg(JExpr._this()));
         for (Entry<String, JFieldVar> e : fields) {
            JFieldVar v = e.getValue();
            logger.debug("(XJCToStringPlugin) Append {} for toString.", e.getKey());
            body.add(toStringBuilder.invoke("append")
                  .arg(e.getKey())
                  .arg(v));
         }
         body._return(toStringBuilder.invoke("toString"));
      }
      return true;
   }
}
