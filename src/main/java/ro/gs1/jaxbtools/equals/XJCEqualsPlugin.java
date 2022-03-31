package ro.gs1.jaxbtools.equals;

import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
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

public class XJCEqualsPlugin extends Plugin {

   private static Logger logger = LoggerFactory.getLogger(XJCEqualsPlugin.class);

   @Override
   public String getOptionName() {
      return "Xgs1-equals";
   }

   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCEqualsPlugin) Activated.");
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-equals    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      JCodeModel model = new JCodeModel();
      JClass equalsBuilderClass = model.ref(EqualsBuilder.class);
      logger.debug("(XJCEqualsPlugin) Found {} classes.", outline.getClasses()
            .size());
      for (ClassOutline o : outline.getClasses()) {
         logger.debug("(XJCEqualsPlugin) Generate equals for class {}.", o.implClass.name());
         JMethod method = o.implClass.method(PUBLIC, boolean.class, "equals");
         method.annotate(Override.class);
         JVar that = method.param(Object.class, "that");
         JBlock body = method.body();
         body._if(JExpr._this()
               .eq(that))
               ._then()
               ._return(JExpr.lit(true));
         body._if(that.eq(JExpr._null()))
               ._then()
               ._return(JExpr.lit(false));
         body._if(JExpr.invoke("getClass")
               .ne(JExpr.invoke(that, "getClass")))
               ._then()
               ._return(JExpr.lit(false));
         // remove static fields
         Set<Entry<String, JFieldVar>> fields = o.implClass.fields()
               .entrySet()
               .stream()
               .filter(aa -> (aa.getValue()
                     .mods()
                     .getValue() & STATIC) == 0)
               .collect(Collectors.toSet());
         if (fields.size() == 0) {
            body._return(JExpr.lit(true));
            continue;
         }
         JVar equalsBuilder = body.decl(0, equalsBuilderClass, "equalsBuilder", JExpr._new(equalsBuilderClass));
         JVar other = body.decl(0, o.implClass, "other", JExpr.cast(o.implClass, that));
         for (Entry<String, JFieldVar> e : fields) {
            JFieldVar v = e.getValue();
            logger.debug("(XJCEqualsPlugin) Append {} for equals.", e.getKey());
            body.add(equalsBuilder.invoke("append")
                  .arg(v)
                  .arg(other.ref(v)));
         }
         body._return(equalsBuilder.invoke("isEquals"));
      }
      return true;
   }
}
