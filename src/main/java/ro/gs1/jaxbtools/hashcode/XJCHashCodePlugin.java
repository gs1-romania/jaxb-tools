package ro.gs1.jaxbtools.hashcode;

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

   private static final Logger logger = LoggerFactory.getLogger(XJCHashCodePlugin.class);

   @Override
   public String getOptionName() {
      return "Xgs1-hashcode";
   }

   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCHashCodePlugin) Activated.");
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-hashcode    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      JCodeModel model = new JCodeModel();
      JClass hashCodeBuilderClass = model.ref(HashCodeBuilder.class);
      logger.debug("(XJCHashCodePlugin) Found {} classes.", outline.getClasses().size());
      for (ClassOutline classOutline : outline.getClasses()) {
         Set<Entry<String, JFieldVar>> fields = classOutline.implClass.fields().entrySet().stream()
               .filter(e -> (e.getValue().mods().getValue() & STATIC) == 0)
               .collect(Collectors.toSet());
         if (fields.isEmpty()) {
            logger.debug("(XJCHashCodePlugin) Skip hashCode for class {}: no instance fields.", classOutline.implClass.name());
            continue;
         }
         logger.debug("(XJCHashCodePlugin) Generating hashCode for class {}.", classOutline.implClass.name());
         JMethod method = classOutline.implClass.method(PUBLIC, int.class, "hashCode");
         method.annotate(Override.class);
         JBlock body = method.body();
         JVar hashCodeBuilder = body.decl(0, hashCodeBuilderClass, "hashCodeBuilder",
               JExpr._new(hashCodeBuilderClass).arg(JExpr.lit(17)).arg(JExpr.lit(31)));
         for (Entry<String, JFieldVar> e : fields) {
            logger.debug("(XJCHashCodePlugin) Appending field {} to hashCode.", e.getKey());
            body.add(hashCodeBuilder.invoke("append").arg(e.getValue()));
         }
         body._return(hashCodeBuilder.invoke("toHashCode"));
      }
      return true;
   }
}
