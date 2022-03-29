package ro.gs1.jaxbtools;

import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;
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

public class XJCHashCodePlugin extends Plugin {

   private static Logger logger = LoggerFactory.getLogger(XJCHashCodePlugin.class);

   
   @Override
   public String getOptionName() {
      return "Xgs1-hashcode";
   }

   @Override
   public int parseArgument(Options opt, String[] args, int i) {
      return 1;
   }
   
   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCHashcodePlugin) Activated.");
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-hashcode    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      JCodeModel model = new JCodeModel();
      logger.debug("(XJCHashcodePlugin) Found {} classes.", outline.getClasses()
            .size());
      for (ClassOutline o : outline.getClasses()) {
         JClass hashCodeBuilderClass = model.ref(HashCodeBuilder.class);
         // remove static fields
         Set<Entry<String, JFieldVar>> fields = o.implClass.fields()
               .entrySet()
               .stream()
               .filter(aa -> (aa.getValue()
                     .mods()
                     .getValue() & STATIC) == 0)
               .collect(Collectors.toSet());

         if (fields.size() == 0) {
            logger.debug("(XJCHashcodePlugin) Skip hashCode for class {}, reason: no fields.", o.implClass.name());
            continue;
         }
         logger.debug("(XJCHashcodePlugin) Generate hashCode for class {}.", o.implClass.name());
         JMethod method = o.implClass.method(PUBLIC, int.class, "hashCode");
         method.annotate(Override.class);
         JBlock body = method.body();
         JVar equalsBuilder = body.decl(0, hashCodeBuilderClass, "hashCodeBuilderBuilder", JExpr._new(hashCodeBuilderClass).arg("17").arg("17"));
         for (Entry<String, JFieldVar> e : fields) {
            JFieldVar v = e.getValue();
            logger.debug("(XJCHashcodePlugin) Append {} for hashCode.", e.getKey());
            body.add(equalsBuilder.invoke("append")
                  .arg(v));
         }
         body._return(equalsBuilder.invoke("toHashCode"));
      }
      return true;
   }
}
