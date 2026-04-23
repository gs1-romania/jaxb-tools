package ro.gs1.jaxbtools.simplify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import ro.gs1.jaxbtools.AbstractPlugin;

public class XJCSimplifyPlugin extends AbstractPlugin {

   private static final Logger logger = LoggerFactory.getLogger(XJCSimplifyPlugin.class);

   public static final String FIELD_PROPERTY = "field";

   public static final String NAMESPACE_URI = "http://jaxb-tools.gs1.ro/simplify";

   public static final QName LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "list");

   public static final QName DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "deleteJaxbElementList");

   public static final QName REPLACE_GENERIC_LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "replaceGenericList");

   private static final List<QName> QNAMES = List.of(
         LIST_ELEMENT_NAME,
         DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME,
         REPLACE_GENERIC_LIST_ELEMENT_NAME);

   @Override
   public String getOptionName() {
      return "Xgs1-simplify";
   }

   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCSimplifyPlugin) Activated.");
   }

   @Override
   public List<String> getCustomizationURIs() {
      return List.of(NAMESPACE_URI);
   }

   @Override
   public boolean isCustomizationTagName(String namespaceURI, String localName) {
      return QNAMES.contains(new QName(namespaceURI, localName));
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-simplify    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      logger.debug("(XJCSimplifyPlugin) Found {} classes.", outline.getClasses().size());
      JCodeModel model = new JCodeModel();
      fieldCustomizations(outline, model);
      classCustomizations(outline, model);
      return true;
   }

   private void classCustomizations(Outline outline, JCodeModel model) {
      for (ClassOutline classOutline : outline.getClasses()) {
         replaceGenericList(model, classOutline, classOutline.target.getCustomizations());
      }
   }

   private void fieldCustomizations(Outline outline, JCodeModel model) {
      for (ClassOutline classOutline : outline.getClasses()) {
         for (CPropertyInfo property : classOutline.target.getProperties()) {
            deleteJaxbElementList(outline, model, classOutline, property, property.getCustomizations());
         }
      }
   }

   private void replaceGenericList(JCodeModel model, ClassOutline classOutline, CCustomizations customizations) {
      CPluginCustomization customization = customizations.find(
            REPLACE_GENERIC_LIST_ELEMENT_NAME.getNamespaceURI(),
            REPLACE_GENERIC_LIST_ELEMENT_NAME.getLocalPart());
      if (customization == null) {
         return;
      }
      if (!customization.element.hasAttribute(FIELD_PROPERTY)) {
         logger.warn("(XJCSimplifyPlugin) replaceGenericList: missing 'field' attribute, skipping.");
         return;
      }
      String fieldName = customization.element.getAttribute(FIELD_PROPERTY);
      JFieldVar field = classOutline.implClass.fields().get(fieldName);
      if (field == null) {
         logger.warn("(XJCSimplifyPlugin) replaceGenericList: field '{}' not found in class {}, skipping.",
               fieldName, classOutline.implClass.name());
         return;
      }
      String replacementClassName = customization.element.getTextContent();
      JClass replacementType = model.ref(replacementClassName);
      field.type(model.ref(ArrayList.class).narrow(replacementType));
      JClass xmlAnyElementType = model.ref(XmlAnyElement.class);
      field.annotations().stream()
            .filter(a -> a.getAnnotationClass().compareTo(xmlAnyElementType) == 0)
            .findFirst()
            .ifPresent(ann -> ann.param("lax", true));
      generateGetter(classOutline, field);
      customization.markAsAcknowledged();
      logger.debug("(XJCSimplifyPlugin) Replaced generic type of field '{}' with ArrayList<{}>.",
            field.name(), replacementType.fullName());
   }

   private void deleteJaxbElementList(Outline outline, JCodeModel model, ClassOutline classOutline,
                                      CPropertyInfo property, CCustomizations customizations) {
      CPluginCustomization customization = customizations.find(
            DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME.getNamespaceURI(),
            DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME.getLocalPart());
      if (customization == null) {
         return;
      }
      logger.debug("(XJCSimplifyPlugin) deleteJaxbElementList: field '{}' in class '{}'.",
            property.getName(false), classOutline.implClass.name());
      if (property.ref().isEmpty()) {
         logger.debug("(XJCSimplifyPlugin) Field has no type references, skipping.");
         return;
      }
      JFieldVar field = classOutline.implClass.fields().get(property.getName(false));
      logger.debug("(XJCSimplifyPlugin) Type reference count: {}", property.ref().size());
      JClass lca = findLowestCommonAncestorClass(outline, property.ref());
      if (lca == null) {
         logger.debug("(XJCSimplifyPlugin) Cannot compute LCA, skipping field '{}'.", field.name());
         return;
      }
      if (lca.fullName().equals("java.lang.Object")) {
         logger.debug("(XJCSimplifyPlugin) LCA is Object for field '{}': expanding to individual fields.", field.name());
         classOutline.implClass.removeField(field);
         removeGetterIfExists(classOutline, field);
         breakRefsInFields(outline, model, classOutline, property, field);
      } else {
         logger.debug("(XJCSimplifyPlugin) LCA is {} for field '{}': replacing JAXBElement list.", lca.name(), field.name());
         field.type(model.ref(ArrayList.class).narrow(lca));
         generateGetter(classOutline, field);
         JClass xmlElementRefType = model.ref(XmlElementRef.class);
         JAnnotationUse xmlElementRefAnnotation = field.annotations().stream()
               .filter(a -> a.getAnnotationClass().compareTo(xmlElementRefType) == 0)
               .findFirst()
               .orElse(null);
         if (xmlElementRefAnnotation != null) {
            try {
               Field memberValues = JAnnotationUse.class.getDeclaredField("memberValues");
               memberValues.setAccessible(true);
               ((Map<?, ?>) memberValues.get(xmlElementRefAnnotation)).remove("type");
               xmlElementRefAnnotation.param("type", lca);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
               logger.error("(XJCSimplifyPlugin) Failed to update @XmlElementRef type parameter.", e);
            }
         }
      }
      customization.markAsAcknowledged();
   }

   private void breakRefsInFields(Outline outline, JCodeModel model, ClassOutline classOutline,
                                  CPropertyInfo property, JFieldVar originalField) {
      Collection<? extends CTypeInfo> propertyRefs = property.ref();
      JClass xmlElementType = model.ref(XmlElement.class);
      Collection<JAnnotationUse> annotations = originalField.annotations();
      Field valuesField;
      Class<?> annotationStringValueClass;
      Field valueFieldAnnotation;
      try {
         valuesField = JAnnotationArrayMember.class.getDeclaredField("values");
         valuesField.setAccessible(true);
         annotationStringValueClass = Class.forName("com.sun.codemodel.JAnnotationStringValue");
         valueFieldAnnotation = annotationStringValueClass.getDeclaredField("value");
         valueFieldAnnotation.setAccessible(true);
      } catch (NoSuchFieldException | ClassNotFoundException e) {
         throw new RuntimeException(e);
      }

      for (CTypeInfo cTypeInfo : propertyRefs) {
         JType jType = cTypeInfo.toType(outline, Aspect.IMPLEMENTATION);
         logger.debug("(XJCSimplifyPlugin) breakRefsInFields: field '{}', type '{}'.", originalField.name(), jType.name());
         jType.boxify().getTypeParameters().stream()
               .filter(t -> t instanceof JDefinedClass)
               .forEach(t -> {
                  JFieldVar newField = classOutline.ref.field(JMod.PROTECTED, t, StringUtils.uncapitalize(t.name()));
                  logger.debug("(XJCSimplifyPlugin) Added field '{}'.", newField.name());
                  JAnnotationUse xmlTypeAnnotation = classOutline.implClass.annotations().stream()
                        .filter(a -> a.getAnnotationClass().compareTo(model.ref(XmlType.class)) == 0)
                        .findFirst()
                        .orElse(null);
                  Entry<String, JAnnotationValue> propOrderEntry = xmlTypeAnnotation.getAnnotationMembers()
                        .entrySet().stream()
                        .filter(e -> StringUtils.equalsIgnoreCase(e.getKey(), "propOrder"))
                        .findFirst()
                        .orElse(null);
                  JAnnotationArrayMember propOrder = (JAnnotationArrayMember) propOrderEntry.getValue();
                  try {
                     List<?> propOrderValues = (List<?>) valuesField.get(propOrder);
                     propOrderValues.removeIf(v -> {
                        try {
                           JStringLiteral literal = (JStringLiteral) valueFieldAnnotation.get(v);
                           return StringUtils.equalsIgnoreCase(literal.str, originalField.name());
                        } catch (IllegalAccessException e) {
                           throw new RuntimeException(e);
                        }
                     });
                  } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                     logger.error("(XJCSimplifyPlugin) Failed to update @XmlType propOrder.", e);
                  }
                  propOrder.param(StringUtils.uncapitalize(t.name()));
                  JAnnotationUse xmlElementRefAnnotation = annotations.stream()
                        .filter(a -> StringUtils.equals(a.getAnnotationClass().name(), "XmlElementRef"))
                        .findFirst()
                        .orElse(null);
                  Entry<String, JAnnotationValue> namespaceEntry = xmlElementRefAnnotation.getAnnotationMembers()
                        .entrySet().stream()
                        .filter(e -> StringUtils.equalsIgnoreCase(e.getKey(), "namespace"))
                        .findFirst()
                        .orElse(null);
                  JAnnotationUse newAnnotation = newField.annotate(xmlElementType);
                  newAnnotation.param("name", t.name());
                  try {
                     JStringLiteral namespaceLiteral = (JStringLiteral) valueFieldAnnotation.get(namespaceEntry.getValue());
                     newAnnotation.param("namespace", namespaceLiteral);
                  } catch (IllegalAccessException e) {
                     throw new RuntimeException(e);
                  }
                  generateGetter(classOutline, newField);
                  generateSetter(classOutline, newField);
               });
      }
   }
}
