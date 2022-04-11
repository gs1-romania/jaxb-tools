package ro.gs1.jaxbtools.inheritance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

public class XJCInheritancePlugin extends Plugin {

   private static Logger logger = LoggerFactory.getLogger(XJCInheritancePlugin.class);

   public static String NAMESPACE_URI = "http://jaxb3-tools.gs1.ro/inheritance";

   public static QName IMPLEMENTS_ELEMENT_NAME = new QName(NAMESPACE_URI, "implements");

   private List<QName> qNames = Arrays.asList(IMPLEMENTS_ELEMENT_NAME);

   private List<String> uris;

   private Set<QName> names;

   @Override
   public String getOptionName() {
      return "Xgs1-inheritance";
   }

   @Override
   public void onActivated(Options opts) {
      logger.info("(XJCInheritancePlugin) Activated.");
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
      return "  -Xgs1-inheritance    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      logger.debug("(XJCInheritancePlugin) Found {} classes.", outline.getClasses()
            .size());
      JCodeModel model = new JCodeModel();
      for (ClassOutline classOutline : outline.getClasses()) {
         if (!classOutline.target.getCustomizations()
               .isEmpty()) {
            CCustomizations customizations = classOutline.target.getCustomizations();
            CPluginCustomization customization = customizations.find(IMPLEMENTS_ELEMENT_NAME.getNamespaceURI(),
                  IMPLEMENTS_ELEMENT_NAME.getLocalPart());
            if (customization != null) {
               String classToBeImplemented = customization.element.getTextContent();
               if (classToBeImplemented == null) {
                  throw new SAXException("Tag implements must not be text content empty, a class must be provided!");
               }
               classToBeImplemented = classToBeImplemented.trim();
               customization.markAsAcknowledged();
               JClass classToBeExtendedRef = model.ref(classToBeImplemented);
               classOutline.ref._implements(classToBeExtendedRef);
               logger.debug("(XJCInheritancePlugin) - {} implements {}", classOutline.ref.name(),
                     classToBeExtendedRef.name());
            }
         }
      }
      return true;
   }
}
