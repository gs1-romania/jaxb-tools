package ro.gs1.jaxbtools;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElement;
import java.util.*;
import java.util.Map.Entry;

import static com.sun.codemodel.JMod.PUBLIC;


public abstract class AbstractPlugin extends Plugin {

   private static Logger logger = LoggerFactory.getLogger(AbstractPlugin.class);

   protected boolean removeGetterIfExists(ClassOutline classOutline, JFieldVar customizedJField) {
      return classOutline.implClass.methods()
            .removeIf(aa -> {
               if (("get" + customizedJField.name()).equalsIgnoreCase(((JMethod) aa).name())) {
                  return true;
               }
               return false;
            });
   }

   protected boolean removeSetterIfExists(ClassOutline classOutline, JFieldVar customizedJField) {
      return classOutline.implClass.methods()
            .removeIf(aa -> {
               if (("set" + customizedJField.name()).equalsIgnoreCase(((JMethod) aa).name())) {
                  return true;
               }
               return false;
            });
   }

   protected void generateGetter(ClassOutline classOutline, JFieldVar customizedJField) {
      boolean removeGetterIfExists = removeGetterIfExists(classOutline, customizedJField);
      logger.debug("(AbstractPlugin) getter removed for field ({}): {}", customizedJField.name(), removeGetterIfExists);
      JMethod getterMethod = classOutline.implClass.method(PUBLIC, customizedJField.type(),
            "get" + StringUtils.capitalize(customizedJField.name()));
      JBlock getterBody = getterMethod.body();
      if (customizedJField.type()
            .boxify()
            .isParameterized()
            && StringUtils.containsIgnoreCase(customizedJField.type()
            .name(), "list")) {
         getterBody._if(JExpr._this()
                     .ref(customizedJField)
                     .eq(JExpr._null()))
               ._then()
               .assign(JExpr._this()
                     .ref(customizedJField), JExpr._new(customizedJField.type()));
      }
      getterBody._return(customizedJField);
      logger.debug("(AbstractPlugin) generated getter for field", customizedJField.name());
   }

   protected void generateSetter(ClassOutline classOutline, JFieldVar customizedJField) {
      boolean removeSetterIfExists = removeSetterIfExists(classOutline, customizedJField);
      logger.debug("(AbstractPlugin) setter removed for field ({}): {}", customizedJField.name(), removeSetterIfExists);
      JMethod setterMethod = classOutline.implClass.method(PUBLIC, void.class,
            "set" + StringUtils.capitalize(customizedJField.name()));
      JVar that = setterMethod.param(customizedJField.type(), customizedJField.name());
      JBlock setterBody = setterMethod.body();
      setterBody.assign(JExpr._this()
            .ref(customizedJField), that);
      logger.debug("(AbstractPlugin) generated setter for field", customizedJField.name());
   }

   protected JClass findLowestCommonAncestorClass(Outline outline, Collection<? extends CTypeInfo> cPropertyInfo) {
      List<JClass> classes = new ArrayList<>();
      for (CTypeInfo cTypeInfo : cPropertyInfo) {
         JType jType = cTypeInfo.toType(outline, Aspect.IMPLEMENTATION);
         classes.addAll(jType.boxify()
               .getTypeParameters());
      }
      return findLowestCommonAncestorClass(outline, classes);
   }

   protected JClass findLowestCommonAncestorClass(Outline outline, List<JClass> classes) {
      // because JClass does not have equlas we cannot use it for searching
      // <full name of the class, list of full name extended classes>
      Map<String, List<String>> classesMatrix = new HashMap<>();
      for (JClass jClass : classes) {
         logger.debug("(AbstractPlugin) - find class: {}", jClass.name());
         if (!classesMatrix.containsKey(jClass.fullName())) {
            classesMatrix.put(jClass.fullName(), new ArrayList<>());
         }
         // put the classes in order, it is important for searching the first
         // class, otherwise Object.class is always selected
         List<String> parentsList = classesMatrix.get(jClass.fullName());
         parentsList.add(jClass.fullName());
         JClass jExtends = jClass._extends();
         while (jExtends != null) {
            logger.debug("(AbstractPlugin) - class: {} extends: {}", jClass.name(), jExtends.name());
            parentsList.add(jExtends.fullName());
            jExtends = jExtends._extends();
         }
      }
      logger.debug("(AbstractPlugin) - classesMatrix: {}", classesMatrix);
      Optional<Entry<String, List<String>>> classWithMostParents = classesMatrix.entrySet()
            .stream()
            .max((o1, o2) -> {
               return o1.getValue()
                     .size()
                     - o2.getValue()
                     .size();
            });
      if (classWithMostParents.isEmpty()) {
         return null;
      }
      Entry<String, List<String>> maxParentsClass = classWithMostParents.get();
      int pointerOfList = 0;
      logger.debug("(AbstractPlugin) - max parents class: {}", maxParentsClass.getKey());
      while (pointerOfList < maxParentsClass.getValue()
            .size()) {
         int classFoundIn = 0;
         for (Entry<String, List<String>> classMatrix : classesMatrix.entrySet()) {
            List<String> classExtendList = classMatrix.getValue();
            String foundParentClass = null;
            for (String classParent : classExtendList) {
               if (classParent.equals(maxParentsClass.getValue()
                     .get(pointerOfList))) {
                  foundParentClass = classParent;
                  break;
               }
            }
            if (foundParentClass != null) {
               logger.debug("(AbstractPlugin) - parent class found: {} in list of: {}", foundParentClass,
                     classMatrix.getKey());
               classFoundIn++;
            }
         }
         logger.debug("(AbstractPlugin) - class {} found in {} list parent classes", maxParentsClass.getValue()
               .get(pointerOfList), classFoundIn);
         if (classFoundIn == classesMatrix.size()) {
            logger.debug("(AbstractPlugin) - LCA found at pointer: {}, {}", pointerOfList, maxParentsClass.getValue()
                  .get(pointerOfList));
            break;
         }
         pointerOfList++;
      }
      String lca = maxParentsClass.getValue()
            .get(pointerOfList);
      return classes.stream()
            .filter(aa -> aa.fullName()
                  .equals(lca))
            .findFirst()
            .orElse(null);
   }

   protected void replaceXmlElementRefAnnotation(JCodeModel model, JFieldVar customizedJField) {
      JClass listRef = model.ref(List.class);
      JClass objectRef = model.ref(Object.class);
      JClass listOfObjectRef = listRef.narrow(objectRef);
      customizedJField.type(listOfObjectRef);
      JClass xmlRef = model.ref(XmlElement.class);
      Collection<JAnnotationUse> annotations = customizedJField.annotations();
      JAnnotationUse xmlElementRefAnnotation = annotations.stream()
            .filter(aa -> {
               JClass annotationClass = aa.getAnnotationClass();
               if (StringUtils.equals(annotationClass.name(), "XmlElementRef")) {
                  return true;
               }
               return false;
            })
            .findFirst()
            .orElse(null);
      if (xmlElementRefAnnotation != null) {
         logger.debug("(AbstractPlugin) - XmlElementRef found");
         JAnnotationUse annotate = customizedJField.annotate(xmlRef);
         Map<String, JAnnotationValue> annotationMembers = xmlElementRefAnnotation.getAnnotationMembers();
         /*
         for (Entry<String, JAnnotationValue> annotationEntry : annotationMembers.entrySet()) {
            if (annotationEntry.getKey()
                  .equalsIgnoreCase("type")) {
               continue;
            }
            if (annotationEntry.getValue() instanceof JAnnotationStringValue) {
               JAnnotationStringValue annotationEntryCast = (JAnnotationStringValue) annotationEntry.getValue();
               if (StringUtils.equalsAny(annotationEntryCast.toString(), "true", "false")) {
                  annotate.param(annotationEntry.getKey(), BooleanUtils.toBoolean(annotationEntryCast.toString()));
               } else {
                  annotate.param(annotationEntry.getKey(), annotationEntryCast.toString());
               }
            }
         }
         */
         customizedJField.removeAnnotation(xmlElementRefAnnotation);
      }
   }
}
