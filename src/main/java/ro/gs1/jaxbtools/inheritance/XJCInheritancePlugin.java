package ro.gs1.jaxbtools.inheritance;

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
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

public class XJCInheritancePlugin extends Plugin {

   private static final Logger logger = LoggerFactory.getLogger(XJCInheritancePlugin.class);

   public static final String NAMESPACE_URI = "http://jaxb-tools.gs1.ro/inheritance";

   public static final QName IMPLEMENTS_ELEMENT_NAME = new QName(NAMESPACE_URI, "implements");

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
      return List.of(NAMESPACE_URI);
   }

   @Override
   public boolean isCustomizationTagName(String namespaceURI, String localName) {
      return IMPLEMENTS_ELEMENT_NAME.equals(new QName(namespaceURI, localName));
   }

   @Override
   public String getUsage() {
      return "  -Xgs1-inheritance    :  jxc gs1 tools plugin";
   }

   @Override
   public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
      logger.debug("(XJCInheritancePlugin) Found {} classes.", outline.getClasses().size());
      JCodeModel model = new JCodeModel();
      for (ClassOutline classOutline : outline.getClasses()) {
         CPluginCustomization customization = classOutline.target.getCustomizations()
               .find(IMPLEMENTS_ELEMENT_NAME.getNamespaceURI(), IMPLEMENTS_ELEMENT_NAME.getLocalPart());
         if (customization == null) {
            continue;
         }
         String className = customization.element.getTextContent();
         if (className == null) {
            throw new SAXException("<inheritance:implements> must have a non-empty class name.");
         }
         className = className.trim();
         customization.markAsAcknowledged();
         JClass interfaceRef = model.ref(className);
         classOutline.ref._implements(interfaceRef);
         logger.debug("(XJCInheritancePlugin) {} implements {}", classOutline.ref.name(), interfaceRef.name());
      }
      return true;
   }
}
