package ro.gs1.jaxbtools.simplify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationStringValue;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JAnnotationValue;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import ro.gs1.jaxbtools.AbstractPlugin;

public class XJCSimplifyPlugin extends AbstractPlugin {

   private static Logger logger = LoggerFactory.getLogger(XJCSimplifyPlugin.class);

   public static String FIELD_PROPERTY = "field";

   public static String NAMESPACE_URI = "http://jaxb3-tools.gs1.ro/simplify";

   public static QName LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "list");

   public static QName DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "deleteJaxbElementList");

   public static QName REPLACE_GENERIC_LIST_ELEMENT_NAME = new QName(NAMESPACE_URI, "replaceGenericList");

   private List<QName> qNames = Arrays.asList(LIST_ELEMENT_NAME, DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME,
         REPLACE_GENERIC_LIST_ELEMENT_NAME);

   private List<String> uris;

   private Set<QName> names;

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
      if (this.uris == null) {
         this.uris = new ArrayList<String>(qNames.size());
         for (QName qName : qNames) {
            final String namespaceURI = qName.getNamespaceURI();
            if (!(namespaceURI == null || namespaceURI.length() == 0)) {
               this.uris.add(namespaceURI);
            }
         }
      }
      return this.uris;
   }

   @Override
   public boolean isCustomizationTagName(String namespaceURI, String localName) {
      if (this.names == null) {
         this.names = new HashSet<QName>(qNames);
      }
      return this.names.contains(new QName(namespaceURI, localName));
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-simplify    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      logger.debug("(XJCSimplifyPlugin) Found {} classes.", outline.getClasses()
            .size());
      JCodeModel model = new JCodeModel();
      fieldCustomizations(outline, model);
      classCustomizations(outline, model);
      return true;
   }

   private void classCustomizations(Outline outline, JCodeModel model) {
      for (ClassOutline classOutline : outline.getClasses()) {
         CCustomizations customizations = classOutline.target.getCustomizations();
         replaceGenericList(outline, model, classOutline, customizations);
      }
   }

   private void fieldCustomizations(Outline outline, JCodeModel model) {
      for (ClassOutline classOutline : outline.getClasses()) {
         for (CPropertyInfo cPropertyInfo : classOutline.target.getProperties()) {
            CCustomizations customizations = cPropertyInfo.getCustomizations();
            deleteJaxbElementList(outline, model, classOutline, cPropertyInfo, customizations);
         }
      }
   }

   private void replaceGenericList(Outline outline, JCodeModel model, ClassOutline classOutline,
         CCustomizations customizations) {
      CPluginCustomization customization = customizations.find(REPLACE_GENERIC_LIST_ELEMENT_NAME.getNamespaceURI(),
            REPLACE_GENERIC_LIST_ELEMENT_NAME.getLocalPart());
      if (customization != null) {
         String classReplacementString = customization.element.getTextContent();
         JClass classReplacement = model.ref(classReplacementString);
         if (!customization.element.hasAttribute(FIELD_PROPERTY)) {
            logger.warn("(XJCSimplifyPlugin) - field attribute missing, skip");
            return;
         }
         String attribute = customization.element.getAttribute(FIELD_PROPERTY);
         JFieldVar customizedJField = classOutline.implClass.fields()
               .get(attribute);
         if (customizedJField == null) {
            logger.warn("(XJCSimplifyPlugin) - field missing from class, skip");
            return;
         }
         JClass listRef = model.ref(ArrayList.class);
         JClass listOfObjectRef = listRef.narrow(classReplacement);
         customizedJField.type(listOfObjectRef);
         generateGetter(classOutline, customizedJField);
         customization.markAsAcknowledged();
         logger.debug("(XJCSimplifyPlugin) - replaced generic type of field {} with: {}", customizedJField.name(),
               listOfObjectRef.fullName());
      }
   }

   private void deleteJaxbElementList(Outline outline, JCodeModel model, ClassOutline classOutline,
         CPropertyInfo cPropertyInfo, CCustomizations customizations) {
      CPluginCustomization customization = customizations.find(DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME.getNamespaceURI(),
            DELETE_JAXB_ELEMENT_LIST_ELEMENT_NAME.getLocalPart());
      if (customization != null) {
         logger.debug("(XJCSimplifyPlugin) - found customization for field: {}", cPropertyInfo.getName(false));
         if (cPropertyInfo.ref()
               .isEmpty()) {
            logger.debug("(XJCSimplifyPlugin) - field without reference to any type, skip");
            return;
         }
         JFieldVar customizedJField = classOutline.implClass.fields()
               .get(cPropertyInfo.getName(false));
         classOutline.implClass.removeField(customizedJField);
         if (cPropertyInfo.ref()
               .size() == 1) {
            logger.debug("(XJCSimplifyPlugin) - one reference found, removing JAXBElement");
            // CTypeInfo firstClass = cPropertyInfo.ref()
            // .stream()
            // .findFirst()
            // .orElse(null);
            // JType type = firstClass.toType(outline, Aspect.IMPLEMENTATION);
            // JClass boxify = type.boxify();
            logger.debug("(XJCSimplifyPlugin) - unimplemented");
            // JType unboxify = type.unboxify();
            // boxify.getTypeParameters();
            // cPropertyInfo.ref().clear();
         } else {
            logger.debug(
                  "(XJCSimplifyPlugin) - more than one reference found for field: {}, replace with java.lang.Object",
                  customizedJField);
            Collection<? extends CTypeInfo> propertyRefs = cPropertyInfo.ref();
            JClass xmlElementRef = model.ref(XmlElement.class);
            Collection<JAnnotationUse> annotations = customizedJField.annotations();
            for (CTypeInfo cTypeInfo : propertyRefs) {
               JType jType = cTypeInfo.toType(outline, Aspect.IMPLEMENTATION);
               List<JClass> typeParameters = jType.boxify()
                     .getTypeParameters();
               typeParameters.stream()
                     .filter(aa -> aa instanceof JDefinedClass)
                     .forEach(aa -> {
                        JFieldVar newField = classOutline.ref.field(JMod.PROTECTED, aa,
                              StringUtils.uncapitalize(aa.name()));
                        logger.debug("(XJCSimplifyPlugin) - Added field {}", newField);
                        JClass xmlTypeRef = model.ref(XmlType.class);
                        JAnnotationUse xmlTypeAnnotation = classOutline.implClass.annotations()
                              .stream()
                              .filter(bb -> bb.getAnnotationClass()
                                    .compareTo(xmlTypeRef) == 0)
                              .findFirst()
                              .orElse(null);
                        Entry<String, JAnnotationValue> propOrder = xmlTypeAnnotation.getAnnotationMembers()
                              .entrySet()
                              .stream()
                              .filter(bb -> StringUtils.equalsIgnoreCase(bb.getKey(), "propOrder"))
                              .findFirst()
                              .orElse(null);
                        JAnnotationValue propOrderValue = propOrder.getValue();
                        JAnnotationArrayMember propOrderCast = (JAnnotationArrayMember) propOrderValue;
                        Field values;
                        try {
                           values = JAnnotationArrayMember.class.getDeclaredField("values");
                           values.setAccessible(true);
                           List<?> valuesList = (List<?>) values.get(propOrderCast);
                           valuesList.removeIf(bb -> StringUtils
                                 .equalsIgnoreCase(((JAnnotationStringValue) bb).toString(), customizedJField.name()));
                        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                              | IllegalAccessException e) {
                           logger.error(e.getMessage());
                        }
                        propOrderCast.param(StringUtils.uncapitalize(aa.name()));
                        JAnnotationUse xmlElementRefAnnotation = annotations.stream()
                              .filter(bb -> {
                                 JClass annotationClass = bb.getAnnotationClass();
                                 if (StringUtils.equals(annotationClass.name(), "XmlElementRef")) {
                                    return true;
                                 }
                                 return false;
                              })
                              .findFirst()
                              .orElse(null);
                        Entry<String, JAnnotationValue> namespace = xmlElementRefAnnotation.getAnnotationMembers()
                              .entrySet()
                              .stream()
                              .filter(bb -> StringUtils.equalsIgnoreCase(bb.getKey(), "namespace"))
                              .findFirst()
                              .orElse(null);
                        JAnnotationStringValue namespaceCast = (JAnnotationStringValue) namespace.getValue();
                        JAnnotationUse annotate = newField.annotate(xmlElementRef);
                        annotate.param("name", aa.name());
                        annotate.param("namespace", namespaceCast.toString());
                        generateGetter(classOutline, newField);
                        generateSetter(classOutline, newField);
                     });
            }
         }
         customization.markAsAcknowledged();
      }
   }

}
