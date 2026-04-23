package ro.gs1.jaxbtools;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import jakarta.xml.bind.annotation.XmlElement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

import static com.sun.codemodel.JMod.PUBLIC;


public abstract class AbstractPlugin extends Plugin {

   private static final Logger logger = LoggerFactory.getLogger(AbstractPlugin.class);

   protected boolean removeGetterIfExists(ClassOutline classOutline, JFieldVar field) {
      return classOutline.implClass.methods()
            .removeIf(m -> ("get" + field.name()).equalsIgnoreCase(m.name()));
   }

   protected boolean removeSetterIfExists(ClassOutline classOutline, JFieldVar field) {
      return classOutline.implClass.methods()
            .removeIf(m -> ("set" + field.name()).equalsIgnoreCase(m.name()));
   }

   protected void generateGetter(ClassOutline classOutline, JFieldVar field) {
      boolean removed = removeGetterIfExists(classOutline, field);
      logger.debug("(AbstractPlugin) getter removed for field ({}): {}", field.name(), removed);
      JMethod getter = classOutline.implClass.method(PUBLIC, field.type(),
            "get" + StringUtils.capitalize(field.name()));
      JBlock body = getter.body();
      if (field.type().boxify().isParameterized()
            && StringUtils.containsIgnoreCase(field.type().name(), "list")) {
         body._if(JExpr._this().ref(field).eq(JExpr._null()))
               ._then()
               .assign(JExpr._this().ref(field), JExpr._new(field.type()));
      }
      body._return(field);
      logger.debug("(AbstractPlugin) generated getter for field {}", field.name());
   }

   protected void generateSetter(ClassOutline classOutline, JFieldVar field) {
      boolean removed = removeSetterIfExists(classOutline, field);
      logger.debug("(AbstractPlugin) setter removed for field ({}): {}", field.name(), removed);
      JMethod setter = classOutline.implClass.method(PUBLIC, void.class,
            "set" + StringUtils.capitalize(field.name()));
      JVar param = setter.param(field.type(), field.name());
      setter.body().assign(JExpr._this().ref(field), param);
      logger.debug("(AbstractPlugin) generated setter for field {}", field.name());
   }

   protected JClass findLowestCommonAncestorClass(Outline outline, Collection<? extends CTypeInfo> cPropertyInfo) {
      List<JClass> classes = new ArrayList<>();
      for (CTypeInfo cTypeInfo : cPropertyInfo) {
         JType jType = cTypeInfo.toType(outline, Aspect.IMPLEMENTATION);
         classes.addAll(jType.boxify().getTypeParameters());
      }
      return findLowestCommonAncestorClass(outline, classes);
   }

   protected JClass findLowestCommonAncestorClass(Outline outline, List<JClass> classes) {
      // JClass does not implement equals, so use full names for comparison.
      // Map: full class name -> list of full names of itself and all ancestors (in order)
      Map<String, List<String>> ancestorMap = new HashMap<>();
      for (JClass jClass : classes) {
         logger.debug("(AbstractPlugin) - find class: {}", jClass.name());
         if (!ancestorMap.containsKey(jClass.fullName())) {
            ancestorMap.put(jClass.fullName(), new ArrayList<>());
         }
         // Order matters: self first, then ancestors — avoids always selecting Object
         List<String> ancestors = ancestorMap.get(jClass.fullName());
         ancestors.add(jClass.fullName());
         JClass parent = jClass._extends();
         while (parent != null) {
            logger.debug("(AbstractPlugin) - class: {} extends: {}", jClass.name(), parent.name());
            ancestors.add(parent.fullName());
            parent = parent._extends();
         }
      }
      logger.debug("(AbstractPlugin) - ancestorMap: {}", ancestorMap);
      Optional<Entry<String, List<String>>> deepest = ancestorMap.entrySet()
            .stream()
            .max(Comparator.comparingInt(e -> e.getValue().size()));
      if (deepest.isEmpty()) {
         return null;
      }
      Entry<String, List<String>> deepestEntry = deepest.get();
      logger.debug("(AbstractPlugin) - deepest class: {}", deepestEntry.getKey());
      int pointer = 0;
      while (pointer < deepestEntry.getValue().size()) {
         String candidate = deepestEntry.getValue().get(pointer);
         long matchCount = ancestorMap.values().stream()
               .filter(ancestors -> ancestors.contains(candidate))
               .count();
         logger.debug("(AbstractPlugin) - class {} found in {} ancestor lists", candidate, matchCount);
         if (matchCount == ancestorMap.size()) {
            logger.debug("(AbstractPlugin) - LCA found: {}", candidate);
            break;
         }
         pointer++;
      }
      String lca = deepestEntry.getValue().get(pointer);
      return classes.stream()
            .filter(c -> c.fullName().equals(lca))
            .findFirst()
            .orElse(null);
   }

   protected void replaceXmlElementRefAnnotation(JCodeModel model, JFieldVar field) {
      field.type(model.ref(List.class).narrow(model.ref(Object.class)));
      JAnnotationUse xmlElementRefAnnotation = field.annotations()
            .stream()
            .filter(a -> StringUtils.equals(a.getAnnotationClass().name(), "XmlElementRef"))
            .findFirst()
            .orElse(null);
      if (xmlElementRefAnnotation != null) {
         logger.debug("(AbstractPlugin) - XmlElementRef found, replacing with XmlElement");
         field.annotate(model.ref(XmlElement.class));
         field.removeAnnotation(xmlElementRefAnnotation);
      }
   }
}
