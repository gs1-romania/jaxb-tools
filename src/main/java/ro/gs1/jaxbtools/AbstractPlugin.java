package ro.gs1.jaxbtools;

import static com.sun.codemodel.JMod.PUBLIC;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JAnnotationStringValue;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JAnnotationValue;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;

import jakarta.xml.bind.annotation.XmlElement;

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
         customizedJField.removeAnnotation(xmlElementRefAnnotation);
      }
   }
}
