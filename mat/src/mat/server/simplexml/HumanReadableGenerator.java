package mat.server.simplexml;

import javax.xml.xpath.XPathExpressionException;

import mat.server.util.XmlProcessor;

import org.jsoup.nodes.Element;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HumanReadableGenerator {

	private static final String DISPLAY_NAME = "displayName";
	private static final String ELEMENT_REF = "elementRef";
	private static final String RELATIONAL_OP = "relationalOp";
	private static final String HTML_LI = "li";
	private static final String HTML_UL = "ul";
	private static final String SET_OP = "setOp";
	private static final String SUB_TREE = "subTree";
	private static final String COMMENT = "comment";
	private static final String LOGICAL_OP = "logicalOp";

	public static String generateHTMLForPopulation(String measureId, String populationSubXML,
			String measureXML) {
		org.jsoup.nodes.Document htmlDocument = null;
		//replace the <subTree> tags in 'populationSubXML' with the appropriate subTree tags from 'simpleXML'.
		try {
			XmlProcessor populationXMLProcessor = expandSubTrees(populationSubXML, measureXML);
			String name = getPopulationClauseName(populationXMLProcessor);
			
			htmlDocument = createBaseHumanReadableDocument();
			Element bodyElement = htmlDocument.body();
			Element mainDivElement = bodyElement.appendElement("div");
			Element mainListElement = mainDivElement.appendElement(HTML_UL);
			Element populationListElement = mainListElement.appendElement(HTML_LI);
			
			Element boldPopulationNameElement = populationListElement.appendElement("b");
			boldPopulationNameElement.appendText(name + " =");
			
			parsePopulationAndBuildHTML(populationXMLProcessor, populationListElement);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(htmlDocument);
		String returnHTML = "";
		if(htmlDocument != null){
			returnHTML = htmlDocument.toString();
		}
		return returnHTML;
	}

	private static XmlProcessor expandSubTrees(String populationSubXML, String measureXML) throws XPathExpressionException {
		
		XmlProcessor populationXMLProcessor = new XmlProcessor(populationSubXML);
		XmlProcessor measureXMLProcessor = new XmlProcessor(measureXML);
		
		//find all <subTreeRef> tags in 'populationSubXML'
		NodeList subTreeRefNodeList = populationXMLProcessor.findNodeList(populationXMLProcessor.getOriginalDoc(), "//subTreeRef");
		
		if(subTreeRefNodeList.getLength() > 0){
			
			//For each <subTreeRef> node replace it by actual <subTree> node from 'simpleXML' 
			for(int i=0;i<subTreeRefNodeList.getLength();i++){
				Node subTreeRefNode = subTreeRefNodeList.item(i);
				String subTreeId = subTreeRefNode.getAttributes().getNamedItem("id").getNodeValue();
				System.out.println("subTreeId:"+subTreeId);
				
				Node subTreeNode = measureXMLProcessor.findNode(measureXMLProcessor.getOriginalDoc(), "/measure/subTreeLookUp/subTree[@uuid='"+subTreeId+"']");
				
				//replace the 'subTreeRefNode' with 'subTreeNode'
				Node subTreeRefNodeParent = subTreeRefNode.getParentNode();								
				Node subTreeNodeImportedClone = populationXMLProcessor.getOriginalDoc().importNode(subTreeNode, true);
				subTreeRefNodeParent.replaceChild(subTreeNodeImportedClone, subTreeRefNode);
			}
		}
		
		System.out.println("Inflated popualtion tree: "+populationXMLProcessor.transform(populationXMLProcessor.getOriginalDoc()));	
		return populationXMLProcessor;
	}
	
	private static String getPopulationClauseName(
			XmlProcessor populationXMLProcessor) {
		String name = "";
		try {
			name = populationXMLProcessor.findNode(populationXMLProcessor.getOriginalDoc(), "//clause/@displayName").getNodeValue();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}
	
	private static void parsePopulationAndBuildHTML(
			XmlProcessor populationXMLProcessor, Element populationListElement) {
		
		try {
			Node clauseNode = populationXMLProcessor.findNode(populationXMLProcessor.getOriginalDoc(), "//clause");
			NodeList childNodes = clauseNode.getChildNodes();
			for(int i = 0;i < childNodes.getLength(); i++){
				parseChild(childNodes.item(i),populationListElement,clauseNode);
			}			
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void parseChild(Node item, Element parentListElement, Node parentNode) {
		String nodeName = item.getNodeName();
		
		if(LOGICAL_OP.equals(nodeName)){
			
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				Element liElement = parentListElement.appendElement(HTML_LI);
				liElement.appendText(parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": ");
			}
			
			Element ulElement = parentListElement.appendElement(HTML_UL);
			NodeList childNodes = item.getChildNodes();
			for (int i=0; i< childNodes.getLength(); i++){
				parseChild(childNodes.item(i), ulElement, item);				
			}
		}else if(COMMENT.equals(nodeName)){
			return;
		}else if(SUB_TREE.equals(nodeName)){
			NodeList childNodes = item.getChildNodes();
			for (int i=0; i< childNodes.getLength(); i++){
				parseChild(childNodes.item(i), parentListElement,parentNode);				
			}
		}else if(SET_OP.equals(nodeName)){
			Element liElement = parentListElement.appendElement(HTML_LI);
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				liElement.appendText(parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": ");
			}
			Element ulElement = liElement.appendElement(HTML_UL);
			NodeList childNodes = item.getChildNodes();
			for (int i=0; i< childNodes.getLength(); i++){
				parseChild(childNodes.item(i), ulElement,item);				
			}
		}else if(RELATIONAL_OP.equals(nodeName)){
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				Element liElement = parentListElement.appendElement(HTML_LI);
				liElement.appendText(parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": ");
				/**
				 * A relationalOp can have 2 children. First evaluate the LHS child, then add the name of the relationalOp and finally
				 * evaluate the RHS child.
				 */
				NodeList childNodes = item.getChildNodes();
				parseChild(childNodes.item(0),liElement,item);
				liElement.appendText(item.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue()+" ");
				parseChild(childNodes.item(1),liElement,item);
			}else{
				/**
				 * A relationalOp can have 2 children. First evaluate the LHS child, then add the name of the relationalOp and finally
				 * evaluate the RHS child.
				 */
				NodeList childNodes = item.getChildNodes();
				parseChild(childNodes.item(0),parentListElement,item);
				parentListElement.appendText(item.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue()+" ");
				parseChild(childNodes.item(1),parentListElement,item);
			}
			
		}else if(ELEMENT_REF.equals(nodeName)){
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				Element liElement = parentListElement.appendElement(HTML_LI);
				liElement.appendText(parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": "+getQDMText(item));
			}else{
				parentListElement.appendText(getQDMText(item));
			}
		}else if("functionalOp".equals(nodeName)){
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				Element liElement = parentListElement.appendElement(HTML_LI);
				liElement.appendText(" "+parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": ");
				liElement.appendText(getFunctionText(item));
				NodeList childNodes = item.getChildNodes();
				for (int i=0; i< childNodes.getLength(); i++){
					parseChild(childNodes.item(i), liElement,item);				
				}
			}else{
				parentListElement.appendText(getFunctionText(item));
				NodeList childNodes = item.getChildNodes();
				for (int i=0; i< childNodes.getLength(); i++){
					parseChild(childNodes.item(i), parentListElement,item);				
				}
			}
			
		}
		else {
			Element liElement = parentListElement.appendElement(HTML_LI);
			
			if(LOGICAL_OP.equals(parentNode.getNodeName()) || SET_OP.equals(parentNode.getNodeName())){
				liElement.appendText(" "+parentNode.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue().toUpperCase()+": ");
			}
			liElement.appendText(item.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue() + " ");
		}
	}

	private static String getFunctionText(Node item) {
		if(!"functionalOp".equals(item.getNodeName())){
			return "";
		}
		
		return item.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue() + " of ";
	}

	/**
	 * This will look at the elementRef node and return an appropriate 
	 * display text for human readable HTML for that QDM.
	 * TODO: Write code to take <attributes> into account.
	 * @param item
	 * @return
	 */
	private static String getQDMText(Node item) {
		if(!ELEMENT_REF.equals(item.getNodeName())){
			return "";
		}
		String displayName = item.getAttributes().getNamedItem(DISPLAY_NAME).getNodeValue();
		if(displayName.endsWith(" : Timing Element")){
			displayName = displayName.substring(0,displayName.indexOf(" : Timing Element"));
		}
		return "\"" + displayName + "\" ";
	}

	private static org.jsoup.nodes.Document createBaseHumanReadableDocument() {
		org.jsoup.nodes.Document htmlDocument = org.jsoup.nodes.Document.createShell("");
		Element head = htmlDocument.head();
		appendStyleNode(head);
		return htmlDocument;
	}

	private static void appendStyleNode(Element head) {
		String styleTagString = MATCssUtil.getCSS();
		head.append(styleTagString);
	}

}