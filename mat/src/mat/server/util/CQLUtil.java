package mat.server.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import mat.dao.clause.CQLLibraryDAO;
import mat.model.clause.CQLLibrary;
import mat.model.cql.CQLIncludeLibrary;
import mat.model.cql.CQLModel;
import mat.model.cql.parser.CQLBaseStatementInterface;
import mat.model.cql.parser.CQLCodeModelObject;
import mat.model.cql.parser.CQLDefinitionModelObject;
import mat.model.cql.parser.CQLFileObject;
import mat.model.cql.parser.CQLFunctionModelObject;
import mat.model.cql.parser.CQLParameterModelObject;
import mat.model.cql.parser.CQLValueSetModelObject;
import mat.server.CQLUtilityClass;
import mat.server.cqlparser.CQLFilter;
import mat.shared.CQLErrors;
import mat.shared.CQLObject;
import mat.shared.GetUsedCQLArtifactsResult;
import mat.shared.LibHolderObject;
import mat.shared.SaveUpdateCQLResult;
import mat.shared.UUIDUtilClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cqframework.cql.cql2elm.CQLtoELM;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// TODO: Auto-generated Javadoc
/**
 * The Class CQLUtil.
 */
public class CQLUtil {

	/** The Constant xPath. */
	static final javax.xml.xpath.XPath xPath = XPathFactory.newInstance().newXPath();
	
	/** The Constant logger. */
	private static final Log logger = LogFactory.getLog(CQLUtil.class);

	/**
	 * Gets the used CQL artifacts.
	 *
	 * @param originalDoc the original doc
	 * @param cqlFileObject the cql file object
	 * @return the used CQL artifacts
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static CQLArtifactHolder getUsedCQLArtifacts(Document originalDoc, CQLFileObject cqlFileObject)
			throws XPathExpressionException {

		CQLArtifactHolder cqlArtifactHolder = CQLUtil.getCQLArtifactsReferredByPoplns(originalDoc);
		CQLUtil cqlUtil = new CQLUtil();
		CQLArtifactHolder usedCQLArtifactHolder = cqlUtil.new CQLArtifactHolder();

		usedCQLArtifactHolder.getCqlDefinitionUUIDSet().addAll(cqlArtifactHolder.getCqlDefinitionUUIDSet());
		usedCQLArtifactHolder.getCqlFunctionUUIDSet().addAll(cqlArtifactHolder.getCqlFunctionUUIDSet());

		for (String cqlDefnUUID : cqlArtifactHolder.getCqlDefinitionUUIDSet()) {
			String xPathCQLDef = "//cqlLookUp/definitions/definition[@id='" + cqlDefnUUID + "']";
			Node cqlDefinition = (Node) xPath.evaluate(xPathCQLDef, originalDoc.getDocumentElement(),
					XPathConstants.NODE);

			String cqlDefnName = "\"" + cqlDefinition.getAttributes().getNamedItem("name").getNodeValue() + "\"";
			CQLDefinitionModelObject cqlDefinitionModelObject = cqlFileObject.getDefinitionsMap().get(cqlDefnName);

			CQLUtil.collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlDefinitionModelObject,
					usedCQLArtifactHolder);
		}

		for (String cqlFuncUUID : cqlArtifactHolder.getCqlFunctionUUIDSet()) {
			String xPathCQLDef = "//cqlLookUp/functions/function[@id='" + cqlFuncUUID + "']";
			Node cqlFunction = (Node) xPath.evaluate(xPathCQLDef, originalDoc.getDocumentElement(),
					XPathConstants.NODE);

			String cqlFuncName = "\"" + cqlFunction.getAttributes().getNamedItem("name").getNodeValue() + "\"";
			CQLFunctionModelObject cqlFunctionModelObject = cqlFileObject.getFunctionsMap().get(cqlFuncName);

			CQLUtil.collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlFunctionModelObject, usedCQLArtifactHolder);
		}

		return usedCQLArtifactHolder;
	}

	/**
	 * Gets the view CQL used CQL artifacts.
	 *
	 * @param originalDoc the original doc
	 * @param cqlFileObject the cql file object
	 * @return the view CQL used CQL artifacts
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static CQLArtifactHolder getViewCQLUsedCQLArtifacts(Document originalDoc, CQLFileObject cqlFileObject)
			throws XPathExpressionException {

		CQLArtifactHolder cqlArtifactHolder = CQLUtil.getCQLArtifactsReferredByViewCQL(originalDoc, cqlFileObject);
		CQLUtil cqlUtil = new CQLUtil();
		CQLArtifactHolder usedCQLArtifactHolder = cqlUtil.new CQLArtifactHolder();

		usedCQLArtifactHolder.getCqlParameterIdentifierSet().addAll(cqlArtifactHolder.getCqlParameterIdentifierSet());
		usedCQLArtifactHolder.getCqlDefinitionUUIDSet().addAll(cqlArtifactHolder.getCqlDefinitionUUIDSet());
		usedCQLArtifactHolder.getCqlFunctionUUIDSet().addAll(cqlArtifactHolder.getCqlFunctionUUIDSet());

		for (String cqlParamUUID : cqlArtifactHolder.getCqlParameterIdentifierSet()) {
			String xPathCQLParam = "//cqlLookUp/definitions/parameter[@id='" + cqlParamUUID + "']";
			Node cqlParam = (Node) xPath.evaluate(xPathCQLParam, originalDoc.getDocumentElement(), XPathConstants.NODE);

			String cqlParamName = "\"" + cqlParam.getAttributes().getNamedItem("name").getNodeValue() + "\"";
			CQLParameterModelObject cqlParameterModelObject = cqlFileObject.getParametersMap().get(cqlParamName);

			CQLUtil.collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlParameterModelObject, usedCQLArtifactHolder);
		}

		for (String cqlDefnUUID : cqlArtifactHolder.getCqlDefinitionUUIDSet()) {
			String xPathCQLDef = "//cqlLookUp/definitions/definition[@id='" + cqlDefnUUID + "']";
			Node cqlDefinition = (Node) xPath.evaluate(xPathCQLDef, originalDoc.getDocumentElement(),
					XPathConstants.NODE);

			String cqlDefnName = "\"" + cqlDefinition.getAttributes().getNamedItem("name").getNodeValue() + "\"";
			CQLDefinitionModelObject cqlDefinitionModelObject = cqlFileObject.getDefinitionsMap().get(cqlDefnName);

			CQLUtil.collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlDefinitionModelObject,
					usedCQLArtifactHolder);
		}

		for (String cqlFuncUUID : cqlArtifactHolder.getCqlFunctionUUIDSet()) {
			String xPathCQLDef = "//cqlLookUp/functions/function[@id='" + cqlFuncUUID + "']";
			Node cqlFunction = (Node) xPath.evaluate(xPathCQLDef, originalDoc.getDocumentElement(),
					XPathConstants.NODE);

			String cqlFuncName = "\"" + cqlFunction.getAttributes().getNamedItem("name").getNodeValue() + "\"";
			CQLFunctionModelObject cqlFunctionModelObject = cqlFileObject.getFunctionsMap().get(cqlFuncName);

			CQLUtil.collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlFunctionModelObject, usedCQLArtifactHolder);
		}

		return usedCQLArtifactHolder;
	}

	/**
	 * Loop through all of the cqlDefinitionsModels in the list and get all
	 * referred to definitions and functions. On each definition and function,
	 * recursively find all of the referred to definitions and functions for
	 * them.
	 *
	 * @param originalDoc the original doc
	 * @param cqlFileObject the cql file object
	 * @param cqlBaseStatementInterface the cql base statement interface
	 * @param usedCQLArtifactHolder the used CQL artifact holder
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void collectUsedCQLArtifacts(Document originalDoc, CQLFileObject cqlFileObject,
			CQLBaseStatementInterface cqlBaseStatementInterface, CQLArtifactHolder usedCQLArtifactHolder)
			throws XPathExpressionException {

		// get valuesets used by current artifact
		List<CQLValueSetModelObject> cqlValueSetModelObjects = cqlBaseStatementInterface.getReferredToValueSets();
		for (CQLValueSetModelObject cqlValueSetModelObject : cqlValueSetModelObjects) {
			usedCQLArtifactHolder.addValuesetIdentifier(cqlValueSetModelObject.getIdentifier().replace("\"", ""));
		}

		// get codes used by currect artifact
		List<CQLCodeModelObject> cqlCodeModelObjects = cqlBaseStatementInterface.getReferredToCodes();
		for (CQLCodeModelObject cqlCodeModelObject : cqlCodeModelObjects) {
			usedCQLArtifactHolder.addCQLCode(cqlCodeModelObject.getCodeIdentifier().replace("\"", ""));
		}

		// get parameters used by current artifact
		List<CQLParameterModelObject> cqlParameterModelObjects = cqlBaseStatementInterface.getReferredToParameters();
		for (CQLParameterModelObject cqlParameterModelObject : cqlParameterModelObjects) {
			usedCQLArtifactHolder.addParameterIdentifier(cqlParameterModelObject.getIdentifier().replace("\"", ""));
		}

		// get all definitions which current artifact refer to, recursively find
		// all artifacts which could be nested.
		List<CQLDefinitionModelObject> cqlDefinitionModelObjectList = cqlBaseStatementInterface
				.getReferredToDefinitions();
		for (CQLDefinitionModelObject cqlDefinitionModelObject : cqlDefinitionModelObjectList) {
			usedCQLArtifactHolder
					.addDefinitionUUID(CQLUtil.getCQLDefinitionUUID(originalDoc, cqlDefinitionModelObject, true));

			collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlDefinitionModelObject, usedCQLArtifactHolder);

		}

		// get all functions which current artifact refer to, recursively find
		// all artifacts which could be nested.
		List<CQLFunctionModelObject> cqlFunctionModelObjectList = cqlBaseStatementInterface.getReferredToFunctions();
		for (CQLFunctionModelObject cqlFunctionModelObject : cqlFunctionModelObjectList) {
			usedCQLArtifactHolder
					.addFunctionUUID(CQLUtil.getCQLDefinitionUUID(originalDoc, cqlFunctionModelObject, false));

			collectUsedCQLArtifacts(originalDoc, cqlFileObject, cqlFunctionModelObject, usedCQLArtifactHolder);
		}
	}

	/**
	 * Given a CqlDefinitionModelObject, find out the <definition> node and
	 * return its "uuid" attribute value.
	 *
	 * @param originalDoc the original doc
	 * @param cqlModelObject the cql model object
	 * @param isDefinition the is definition
	 * @return the CQL definition UUID
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static String getCQLDefinitionUUID(Document originalDoc, CQLBaseStatementInterface cqlModelObject,
			boolean isDefinition) throws XPathExpressionException {

		//System.out.println("cql defn name bfor:" + cqlModelObject.getIdentifier());
		String defnName = cqlModelObject.getIdentifier().replaceAll("\"", "");
		//System.out.println("cql defn name aftr:" + defnName);

		String cqlArtifactName = "definitions//definition";
		if (!isDefinition) {
			cqlArtifactName = "functions//function";
		}

		String xPathForDefinitions = "//cqlLookUp/" + cqlArtifactName + "[@name='" + defnName + "']/@id";
		//System.out.println(xPathForDefinitions);
		Node cqlDefinitionUUID = (Node) xPath.evaluate(xPathForDefinitions, originalDoc.getDocumentElement(),
				XPathConstants.NODE);
		return cqlDefinitionUUID.getNodeValue();
	}

	/**
	 * This method will find out all the CQLDefinitions referred to by
	 * populations defined in a measure.
	 *
	 * @param originalDoc the original doc
	 * @return the CQL artifacts referred by poplns
	 */
	public static CQLArtifactHolder getCQLArtifactsReferredByPoplns(Document originalDoc) {
		CQLUtil cqlUtil = new CQLUtil();
		CQLUtil.CQLArtifactHolder cqlArtifactHolder = cqlUtil.new CQLArtifactHolder();

		String xPathForDefinitions = "//cqldefinition";
		String xPathForFunctions = "//cqlfunction";

		try {
			NodeList cqlDefinitions = (NodeList) xPath.evaluate(xPathForDefinitions, originalDoc.getDocumentElement(),
					XPathConstants.NODESET);

			for (int i = 0; i < cqlDefinitions.getLength(); i++) {
				String uuid = cqlDefinitions.item(i).getAttributes().getNamedItem("uuid").getNodeValue();
				String name = cqlDefinitions.item(i).getAttributes().getNamedItem("displayName").getNodeValue();

				cqlArtifactHolder.addDefinitionUUID(uuid);
				cqlArtifactHolder.addDefinitionIdentifier(name.replaceAll("\"", ""));
			}

			NodeList cqlFunctions = (NodeList) xPath.evaluate(xPathForFunctions, originalDoc.getDocumentElement(),
					XPathConstants.NODESET);

			for (int i = 0; i < cqlFunctions.getLength(); i++) {
				String uuid = cqlFunctions.item(i).getAttributes().getNamedItem("uuid").getNodeValue();
				String name = cqlFunctions.item(i).getAttributes().getNamedItem("displayName").getNodeValue();

				cqlArtifactHolder.addFunctionUUID(uuid);
				cqlArtifactHolder.addFunctionIdentifier(name.replaceAll("\"", ""));
			}

		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cqlArtifactHolder;
	}

	/**
	 * This method will find out all the CQLDefinitions/Functions/Parameters
	 * referred in viewCQL.
	 *
	 * @param originalDoc the original doc
	 * @param cqlFileObject the cql file object
	 * @return the CQL artifacts referred by view CQL
	 */
	private static CQLArtifactHolder getCQLArtifactsReferredByViewCQL(Document originalDoc,
			CQLFileObject cqlFileObject) {
		CQLUtil cqlUtil = new CQLUtil();
		CQLUtil.CQLArtifactHolder cqlArtifactHolder = cqlUtil.new CQLArtifactHolder();

		return cqlArtifactHolder;
	}

	/**
	 * Gets the used CQL valuesets.
	 *
	 * @param cqlFileObject the cql file object
	 * @return the used CQL valuesets
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static CQLArtifactHolder getUsedCQLValuesets(CQLFileObject cqlFileObject) throws XPathExpressionException {

		CQLUtil cqlUtil = new CQLUtil();
		CQLArtifactHolder usedCQLArtifactHolder = cqlUtil.new CQLArtifactHolder();

		Map<String, CQLDefinitionModelObject> defnMap = cqlFileObject.getDefinitionsMap();
		Collection<CQLDefinitionModelObject> cqlDefinitionModelObjects = defnMap.values();

		for (CQLDefinitionModelObject cqlDefinitionModelObject : cqlDefinitionModelObjects) {

			// get valuesets used by current artifact
			List<CQLValueSetModelObject> cqlValueSetModelObjects = cqlDefinitionModelObject.getReferredToValueSets();
			for (CQLValueSetModelObject cqlValueSetModelObject : cqlValueSetModelObjects) {
				usedCQLArtifactHolder.addValuesetIdentifier(cqlValueSetModelObject.getIdentifier().replace("\"", ""));
			}
		}

		Map<String, CQLFunctionModelObject> funcMap = cqlFileObject.getFunctionsMap();
		Collection<CQLFunctionModelObject> cqlFunctionModelObjects = funcMap.values();

		for (CQLFunctionModelObject cqlFunctionModelObject : cqlFunctionModelObjects) {
			// get valuesets used by current artifact
			List<CQLValueSetModelObject> cqlValueSetModelObjects = cqlFunctionModelObject.getReferredToValueSets();
			for (CQLValueSetModelObject cqlValueSetModelObject : cqlValueSetModelObjects) {
				usedCQLArtifactHolder.addValuesetIdentifier(cqlValueSetModelObject.getIdentifier().replace("\"", ""));
			}
		}

		Map<String, CQLParameterModelObject> paramMap = cqlFileObject.getParametersMap();
		Collection<CQLParameterModelObject> cqlParameterModelObjects = paramMap.values();

		for (CQLParameterModelObject cqlParameterModelObject : cqlParameterModelObjects) {
			// get valuesets used by current artifact
			List<CQLValueSetModelObject> cqlValueSetModelObjects = cqlParameterModelObject.getReferredToValueSets();
			for (CQLValueSetModelObject cqlValueSetModelObject : cqlValueSetModelObjects) {
				usedCQLArtifactHolder.addValuesetIdentifier(cqlValueSetModelObject.getIdentifier().replace("\"", ""));
			}
		}

		return usedCQLArtifactHolder;
	}

	/**
	 * Removes all unused cql definitions from the simple xml file. Iterates
	 * through the usedcqldefinitions set, adds them to the xpath string, and
	 * then removes all nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param usedDefinitionsList            the usedcqldefinitions
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedCQLDefinitions(Document originalDoc, List<String> usedDefinitionsList)
			throws XPathExpressionException {

		String idXPathString = "";
		for (String name : usedDefinitionsList) {
			idXPathString += "[@name !='" + name + "']";
		}

		String xPathForUnusedDefinitions = "//cqlLookUp//definition" + idXPathString;
		//System.out.println(xPathForUnusedDefinitions);

		NodeList unusedCQLDefNodeList = (NodeList) xPath.evaluate(xPathForUnusedDefinitions,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);

		for (int i = 0; i < unusedCQLDefNodeList.getLength(); i++) {
			Node current = unusedCQLDefNodeList.item(i);

			Node parent = current.getParentNode();
			parent.removeChild(current);
		}

	}

	/**
	 * Removes all unused cql functions from the simple xml file. Iterates
	 * through the usedcqlfunctions set, adds them to the xpath string, and then
	 * removes all nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param usedFunctionsList the used functions list
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedCQLFunctions(Document originalDoc, List<String> usedFunctionsList)
			throws XPathExpressionException {
		String idXPathString = "";
		for (String name : usedFunctionsList) {
			idXPathString += "[@name !='" + name + "']";
		}

		String xPathForUnusedFunctions = "//cqlLookUp//function" + idXPathString;
		//System.out.println(xPathForUnusedFunctions);

		NodeList unusedCqlFuncNodeList = (NodeList) xPath.evaluate(xPathForUnusedFunctions,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < unusedCqlFuncNodeList.getLength(); i++) {
			Node current = unusedCqlFuncNodeList.item(i);

			Node parent = current.getParentNode();
			parent.removeChild(current);
		}
	}

	/**
	 * Removes all unused cql valuesets from the simple xml file. Iterates
	 * through the usedcqlvaluesets set, adds them to the xpath string, and then
	 * removes all nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param cqlValuesetIdentifierSet            the usevaluesets
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedValuesets(Document originalDoc, Set<String> cqlValuesetIdentifierSet)
			throws XPathExpressionException {
		String nameXPathString = "";
		Set<String> cqlCodes = new HashSet<String>();

		for (String name : cqlValuesetIdentifierSet) {
			if (name.equals("Birthdate") || name.equals("Dead")) {
				cqlCodes.add(name);
			} else {
				nameXPathString += "[@name !='" + name + "']";
			}
		}	
			
		String xPathForUnusedValuesets = "//cqlLookUp//valueset" + nameXPathString;
		//System.out.println(xPathForUnusedValuesets);

		NodeList unusedCqlValuesetNodeList = (NodeList) xPath.evaluate(xPathForUnusedValuesets,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);
		
		for (int i = 0; i < unusedCqlValuesetNodeList.getLength(); i++) {
			Node current = unusedCqlValuesetNodeList.item(i);
			Node parent = current.getParentNode();
			parent.removeChild(current);
		}
		
		removeUnusedCodes(originalDoc, cqlCodes);
		removeUnsedCodeSystems(originalDoc);
	}

	/**
	 * Removes all unused cql codes from the simple xml file. Iterates through
	 * the usedcodes set, adds them to the xpath string, and then removes all
	 * nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param cqlCodesIdentifierSet            the usevaluesets
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedCodes(Document originalDoc, Set<String> cqlCodesIdentifierSet)
			throws XPathExpressionException {
		String nameXPathString = "";
		for (String codeName : cqlCodesIdentifierSet) {
			nameXPathString += "[@codeName !='" + codeName + "']";
		}

		String xPathForUnusedCodes = "//cqlLookUp//code" + nameXPathString;
		//System.out.println(xPathForUnusedCodes);

		NodeList unusedCqlCodesNodeList = (NodeList) xPath.evaluate(xPathForUnusedCodes,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < unusedCqlCodesNodeList.getLength(); i++) {
			Node current = unusedCqlCodesNodeList.item(i);
			Node parent = current.getParentNode();
			parent.removeChild(current);
		}
	}

	/**
	 * Removes the unsed code systems.
	 *
	 * @param originalDoc            the original doc
	 * @throws XPathExpressionException the x path expression exception
	 */
	private static void removeUnsedCodeSystems(Document originalDoc) throws XPathExpressionException {

		// find all used codeSystemNames
		String xPathForCodesystemNames = "//cqlLookUp/codes/code/@codeSystemName";
		NodeList codeSystemNameList = (NodeList) xPath.evaluate(xPathForCodesystemNames,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);

		String nameXPathString = "";
		for (int i = 0; i < codeSystemNameList.getLength(); i++) {
			String name = codeSystemNameList.item(i).getNodeValue();
			nameXPathString += "[@codeSystemName !='" + name + "']";
		}
		String xPathForUnusedCodeSystems = "//cqlLookUp/codeSystems/codeSystem" + nameXPathString;

		NodeList unusedCqlCodeSystemNodeList = (NodeList) xPath.evaluate(xPathForUnusedCodeSystems,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);

		for (int i = 0; i < unusedCqlCodeSystemNodeList.getLength(); i++) {
			Node current = unusedCqlCodeSystemNodeList.item(i);
			Node parent = current.getParentNode();
			parent.removeChild(current);
		}

	}

	/**
	 * Removes all unused cql parameters from the simple xml file. Iterates
	 * through the usedcqlparameters set, adds them to the xpath string, and
	 * then removes all nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param usedParameterList            the used parameters
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedParameters(Document originalDoc, List<String> usedParameterList)
			throws XPathExpressionException {
		String nameXPathString = "";
		for (String name : usedParameterList) {
			nameXPathString += "[@name !='" + name + "']";
		}

		String xPathForUnusedParameters = "//cqlLookUp//parameter" + nameXPathString;
		//System.out.println(xPathForUnusedParameters);

		NodeList unusedCqlParameterNodeList = (NodeList) xPath.evaluate(xPathForUnusedParameters,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < unusedCqlParameterNodeList.getLength(); i++) {
			Node current = unusedCqlParameterNodeList.item(i);

			Node parent = current.getParentNode();
			parent.removeChild(current);
		}
	}

	/**
	 * Removes all unused cql includes from the simple xml file. Iterates
	 * through the usedCQLLibraries set, adds them to the xpath string, and then
	 * removes all nodes that are not a part of the xpath string.
	 *
	 * @param originalDoc            the simple xml document
	 * @param usedLibList            the used includes
	 * @param cqlModel the cql model
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void removeUnusedIncludes(Document originalDoc, List<String> usedLibList, CQLModel cqlModel)
			throws XPathExpressionException {

		String nameXPathString = "";
		for (String libName : usedLibList) {
			String[] libArr = libName.split(Pattern.quote("|"));
			String libAliasName = libArr[1];

			String libPathAndVersion = libArr[0];
			String[] libPathArr = libPathAndVersion.split(Pattern.quote("-"));
			String libPath = libPathArr[0];
			String libVer = libPathArr[1];

			nameXPathString += "[@name != '" + libAliasName + "' or @cqlVersion != '" + libVer
					+ "' or @cqlLibRefName != '" + libPath + "']";
		}

		String xPathForUnusedIncludes = "//cqlLookUp//includeLibrarys/includeLibrary" + nameXPathString;
		//System.out.println("xPathForUnusedIncludes");
		//System.out.println(xPathForUnusedIncludes);
		NodeList unusedCqlIncludeNodeList = (NodeList) xPath.evaluate(xPathForUnusedIncludes,
				originalDoc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < unusedCqlIncludeNodeList.getLength(); i++) {
			Node current = unusedCqlIncludeNodeList.item(i);
			Node parent = current.getParentNode();
			parent.removeChild(current);
		}
	}

	/**
	 * Generate ELM.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibraryDAO the cql library DAO
	 * @return the save update CQL result
	 */
	public static SaveUpdateCQLResult generateELM(CQLModel cqlModel, CQLLibraryDAO cqlLibraryDAO) {
		return parseCQLLibraryForErrors(cqlModel, cqlLibraryDAO, null, true);
	}
	
	/**
	 * Parses the CQL library for errors.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibraryDAO the cql library DAO
	 * @param exprList the expr list
	 * @return the save update CQL result
	 */
	public static SaveUpdateCQLResult parseCQLLibraryForErrors(CQLModel cqlModel, CQLLibraryDAO cqlLibraryDAO,
			List<String> exprList) {
		return parseCQLLibraryForErrors(cqlModel, cqlLibraryDAO, exprList, false);
	}

	/**
	 * Parses the CQL library for errors.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibraryDAO the cql library DAO
	 * @param exprList the expr list
	 * @param generateELM the generate ELM
	 * @return the save update CQL result
	 */
	private static SaveUpdateCQLResult parseCQLLibraryForErrors(CQLModel cqlModel, CQLLibraryDAO cqlLibraryDAO,
			List<String> exprList, boolean generateELM) {

		SaveUpdateCQLResult parsedCQL = new SaveUpdateCQLResult();

		Map<String, LibHolderObject> cqlLibNameMap = new HashMap<String, LibHolderObject>();

		getCQLIncludeLibMap(cqlModel, cqlLibNameMap, cqlLibraryDAO);

		cqlModel.setIncludedCQLLibXMLMap(cqlLibNameMap);
		
		setIncludedCQLExpressions(cqlModel);

		validateCQLWithIncludes(cqlModel, cqlLibNameMap, parsedCQL, exprList, generateELM);

		return parsedCQL;
	}

	
	/**
	 * Gets the CQL include lib map.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibNameMap the cql lib name map
	 * @param cqlLibraryDAO the cql library DAO
	 * @return the CQL include lib map
	 */
	private static void getCQLIncludeLibMap(CQLModel cqlModel, Map<String, LibHolderObject> cqlLibNameMap,
			CQLLibraryDAO cqlLibraryDAO) {

		List<CQLIncludeLibrary> cqlIncludeLibraries = cqlModel.getCqlIncludeLibrarys();
		if (cqlIncludeLibraries == null) {
			return;
		}

		for (CQLIncludeLibrary cqlIncludeLibrary : cqlIncludeLibraries) {
			CQLLibrary cqlLibrary = cqlLibraryDAO.find(cqlIncludeLibrary.getCqlLibraryId());

			if (cqlLibrary == null) {
				logger.info("Could not find included library:" + cqlIncludeLibrary.getAliasName());
				continue;
			}

			String includeCqlXMLString = new String(cqlLibrary.getCQLByteArray());

			CQLModel includeCqlModel = CQLUtilityClass.getCQLStringFromXML(includeCqlXMLString);
			System.out.println("Include lib version for " + cqlIncludeLibrary.getCqlLibraryName() + " is:"
					+ cqlIncludeLibrary.getVersion());
			cqlLibNameMap.put(cqlIncludeLibrary.getCqlLibraryName() + "-" + cqlIncludeLibrary.getVersion() + "|" + cqlIncludeLibrary.getAliasName(),
					new LibHolderObject(includeCqlXMLString, cqlIncludeLibrary));
			getCQLIncludeLibMap(includeCqlModel, cqlLibNameMap, cqlLibraryDAO);
		}
	}

	/**
	 * Validate CQL with includes.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibNameMap the cql lib name map
	 * @param parsedCQL the parsed CQL
	 * @param exprList the expr list
	 * @param generateELM the generate ELM
	 */
	private static void validateCQLWithIncludes(CQLModel cqlModel, Map<String, LibHolderObject> cqlLibNameMap,
			SaveUpdateCQLResult parsedCQL, List<String> exprList, boolean generateELM) {

		List<File> fileList = new ArrayList<File>();
		List<CqlTranslatorException> cqlTranslatorExceptions = new ArrayList<CqlTranslatorException>();
		String cqlFileString = CQLUtilityClass.getCqlString(cqlModel, "").toString();

		try {
			File test = File.createTempFile(UUIDUtilClient.uuid(), null);
			File tempDir = test.getParentFile();

			File folder = new File(tempDir.getAbsolutePath() + File.separator + UUIDUtilClient.uuid());
			folder.mkdir();
			File mainCQLFile = createCQLTempFile(cqlFileString, UUIDUtilClient.uuid(), folder);
			fileList.add(mainCQLFile);

			for (String cqlLibName : cqlLibNameMap.keySet()) {
				
				CQLModel includeCqlModel = CQLUtilityClass
						.getCQLStringFromXML(cqlLibNameMap.get(cqlLibName).getMeasureXML());
				
				LibHolderObject libHolderObject = cqlLibNameMap.get(cqlLibName);
				
				String cqlString = CQLUtilityClass.getCqlString(includeCqlModel, "").toString();
				System.out.println("Creating file:"+libHolderObject.getCqlLibrary().getCqlLibraryName() + "-" + libHolderObject.getCqlLibrary().getVersion());
				File cqlIncludedFile = createCQLTempFile(cqlString, libHolderObject.getCqlLibrary().getCqlLibraryName() + "-" + libHolderObject.getCqlLibrary().getVersion(), folder);
				fileList.add(cqlIncludedFile);
				
			}

			CQLtoELM cqlToElm = new CQLtoELM(mainCQLFile);
			cqlToElm.doTranslation(!generateELM, false, generateELM);

			if (generateELM) {
				String elmString = cqlToElm.getElmString();
				parsedCQL.setElmString(elmString);
			}

			cqlTranslatorExceptions = cqlToElm.getErrors();

			fileList.add(test);
			fileList.add(folder);
			
			if(exprList != null){
								
				filterCQLArtifacts(cqlModel, parsedCQL, folder, cqlToElm, exprList);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			for (File file : fileList) {
				file.delete();
			}
		}

		List<CQLErrors> errors = new ArrayList<CQLErrors>();

		for (CqlTranslatorException cte : cqlTranslatorExceptions) {
			CQLErrors cqlErrors = new CQLErrors();

			cqlErrors.setStartErrorInLine(cte.getLocator().getStartLine());

			cqlErrors.setErrorInLine(cte.getLocator().getStartLine());
			cqlErrors.setErrorAtOffeset(cte.getLocator().getStartChar());

			cqlErrors.setEndErrorInLine(cte.getLocator().getEndLine());
			cqlErrors.setEndErrorAtOffset(cte.getLocator().getEndChar());

			cqlErrors.setErrorMessage(cte.getMessage());
			errors.add(cqlErrors);
		}

		parsedCQL.setCqlErrors(errors);
	}

	/**
	 * Filter CQL artifacts.
	 *
	 * @param cqlModel the cql model
	 * @param parsedCQL the parsed CQL
	 * @param folder the folder
	 * @param cqlToElm the cql to elm
	 * @param exprList the expr list
	 */
	
	private static void filterCQLArtifacts(CQLModel cqlModel, SaveUpdateCQLResult parsedCQL, File folder,
			CQLtoELM cqlToElm, List<String> exprList) {
		if (cqlToElm != null) {

			CQLFilter cqlFilter = new CQLFilter(cqlToElm.getLibrary(), exprList, folder.getAbsolutePath(), cqlModel);
			cqlFilter.findUsedExpressions();
			CQLObject cqlObject = cqlFilter.getCqlObject();
			GetUsedCQLArtifactsResult usedArtifacts = new GetUsedCQLArtifactsResult();
			usedArtifacts.setUsedCQLcodes(cqlFilter.getUsedCodes());
			usedArtifacts.setUsedCQLcodeSystems(cqlFilter.getUsedCodeSystems());
			usedArtifacts.setUsedCQLDefinitions(cqlFilter.getUsedExpressions());
			usedArtifacts.setUsedCQLFunctions(cqlFilter.getUsedFunctions());
			usedArtifacts.setUsedCQLParameters(cqlFilter.getUsedParameters());
			usedArtifacts.setUsedCQLValueSets(cqlFilter.getUsedValuesets());
			usedArtifacts.setUsedCQLLibraries(cqlFilter.getUsedLibraries());
			usedArtifacts.setValueSetDataTypeMap(cqlFilter.getValueSetDataTypeMap());
			usedArtifacts.setIncludeLibMap(cqlFilter.getUsedLibrariesMap());
			parsedCQL.setUsedCQLArtifacts(usedArtifacts);
			parsedCQL.setCqlObject(cqlObject);
			
			//System.out.println("Def to Def:"+cqlFilter.getDefinitionToDefinitionMap());
			usedArtifacts.setDefinitionToDefinitionMap(cqlFilter.getDefinitionToDefinitionMap());
						
			//System.out.println("Def to Func:"+cqlFilter.getDefinitionToFunctionMap());
			usedArtifacts.setDefinitionToFunctionMap(cqlFilter.getDefinitionToFunctionMap());
			
			//System.out.println("Func to Def:"+cqlFilter.getFunctionToDefinitionMap());
			usedArtifacts.setFunctionToDefinitionMap(cqlFilter.getFunctionToDefinitionMap());
			
			//System.out.println("Func to Func:"+cqlFilter.getFunctionToFunctionMap());
			usedArtifacts.setFunctionToFunctionMap(cqlFilter.getFunctionToFunctionMap());
		}
	}

	/**
	 * Creates the CQL temp file.
	 *
	 * @param cqlFileString the cql file string
	 * @param name the name
	 * @param parentFolder the parent folder
	 * @return the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static File createCQLTempFile(String cqlFileString, String name, File parentFolder) throws IOException {
		File cqlFile = new File(parentFolder, name + ".cql");
		FileWriter fw = new FileWriter(cqlFile);
		fw.write(cqlFileString);
		fw.close();
		return cqlFile;
	}
	
	/**
	 * This method will extract all the CQL expressions (Definitions, Functions, ValueSets, Parameters) from included 
	 * child (only child, not grand child) CQL Libs and set them inside CQLModel object.
	 * 
	 * This extracted list will then be used by UI CQL Workspace.
	 *
	 * @param cqlModel the new included CQL expressions
	 */
	private static void setIncludedCQLExpressions(CQLModel cqlModel) {
		
		List<CQLIncludeLibrary> cqlIncludeLibraries = cqlModel.getCqlIncludeLibrarys();
		if (cqlIncludeLibraries == null) {
			return;
		}
		
		for (CQLIncludeLibrary cqlIncludeLibrary : cqlIncludeLibraries) {
			
			LibHolderObject libHolderObject = cqlModel.getIncludedCQLLibXMLMap().get(cqlIncludeLibrary.getCqlLibraryName() + "-" + cqlIncludeLibrary.getVersion() + "|" + cqlIncludeLibrary.getAliasName());
			String alias = cqlIncludeLibrary.getAliasName();
			
			String xml = libHolderObject.getMeasureXML();
			XmlProcessor xmlProcessor = new XmlProcessor(xml);
			
			addNamesToList(alias, xmlProcessor, "//cqlLookUp/definitions/definition/@name", cqlModel.getIncludedDefNames());
			addNamesToList(alias, xmlProcessor, "//cqlLookUp/functions/function/@name", cqlModel.getIncludedFuncNames());
			addNamesToList(alias, xmlProcessor, "//cqlLookUp/valuesets/valueset/@name", cqlModel.getIncludedValueSetNames());
			addNamesToList(alias, xmlProcessor, "//cqlLookUp/parameters/parameter/@name", cqlModel.getIncludedParamNames());
			addNamesToList(alias, xmlProcessor, "//cqlLookUp/codes/code/@codeSystemName", cqlModel.getIncludedCodeNames());
			
		}
		
		/*System.out.println("Included Definition names:" + cqlModel.getIncludedDefNames());
		System.out.println("Included Function names:" + cqlModel.getIncludedFuncNames());
		System.out.println("Included Value-Set names:" + cqlModel.getIncludedValueSetNames());
		System.out.println("Included Parameter names:" + cqlModel.getIncludedParamNames());
		System.out.println("Included Code names:" + cqlModel.getIncludedCodeNames());*/
	}
	
	/**
	 * Adds the names to list.
	 *
	 * @param alias the alias
	 * @param xmlProcessor the xml processor
	 * @param xPathForFetch the x path for fetch
	 * @param listToAddTo the list to add to
	 */
	private static void addNamesToList(String alias,
			XmlProcessor xmlProcessor, String xPathForFetch, List<String> listToAddTo) {
		try {
			
			NodeList exprList = (NodeList) xmlProcessor.findNodeList(xmlProcessor.getOriginalDoc(), xPathForFetch);
			
			if(exprList != null){
				
				List<String> exprNameList = new ArrayList<String>();
				for(int i=0; i < exprList.getLength(); i++){
					exprNameList.add(alias + "." + exprList.item(i).getNodeValue());
				}
				listToAddTo.addAll(exprNameList);
			}			
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * The Class CQLArtifactHolder.
	 */
	public class CQLArtifactHolder {

		/** The cql definition UUID set. */
		private Set<String> cqlDefinitionUUIDSet = new HashSet<String>();
		
		/** The cql function UUID set. */
		private Set<String> cqlFunctionUUIDSet = new HashSet<String>();
		
		/** The cql valueset identifier set. */
		private Set<String> cqlValuesetIdentifierSet = new HashSet<String>();
		
		/** The cql parameter identifier set. */
		private Set<String> cqlParameterIdentifierSet = new HashSet<String>();
		
		/** The cql codes set. */
		private Set<String> cqlCodesSet = new HashSet<String>();

		/** The cql def from pop set. */
		private Set<String> cqlDefFromPopSet = new HashSet<String>();
		
		/** The cql func from pop set. */
		private Set<String> cqlFuncFromPopSet = new HashSet<String>();

		/**
		 * Gets the cql definition UUID set.
		 *
		 * @return the cql definition UUID set
		 */
		public Set<String> getCqlDefinitionUUIDSet() {
			return cqlDefinitionUUIDSet;
		}

		/**
		 * Gets the cql function UUID set.
		 *
		 * @return the cql function UUID set
		 */
		public Set<String> getCqlFunctionUUIDSet() {
			return cqlFunctionUUIDSet;
		}

		/**
		 * Gets the cql valueset identifier set.
		 *
		 * @return the cql valueset identifier set
		 */
		public Set<String> getCqlValuesetIdentifierSet() {
			return cqlValuesetIdentifierSet;
		}

		/**
		 * Gets the cql parameter identifier set.
		 *
		 * @return the cql parameter identifier set
		 */
		public Set<String> getCqlParameterIdentifierSet() {
			return cqlParameterIdentifierSet;
		}

		/**
		 * Adds the definition UUID.
		 *
		 * @param uuid the uuid
		 */
		public void addDefinitionUUID(String uuid) {
			cqlDefinitionUUIDSet.add(uuid);
		}

		/**
		 * Adds the function UUID.
		 *
		 * @param uuid the uuid
		 */
		public void addFunctionUUID(String uuid) {
			cqlFunctionUUIDSet.add(uuid);
		}

		/**
		 * Adds the valueset identifier.
		 *
		 * @param identifier the identifier
		 */
		public void addValuesetIdentifier(String identifier) {
			cqlValuesetIdentifierSet.add(identifier);
		}

		/**
		 * Adds the parameter identifier.
		 *
		 * @param identifier the identifier
		 */
		public void addParameterIdentifier(String identifier) {
			cqlParameterIdentifierSet.add(identifier);
		}

		/**
		 * Adds the definition identifier.
		 *
		 * @param identifier the identifier
		 */
		public void addDefinitionIdentifier(String identifier) {
			cqlDefFromPopSet.add(identifier);
		}

		/**
		 * Adds the function identifier.
		 *
		 * @param identifier the identifier
		 */
		public void addFunctionIdentifier(String identifier) {
			cqlFuncFromPopSet.add(identifier);
		}

		/**
		 * Sets the cql definition UUID set.
		 *
		 * @param cqlDefinitionUUIDSet the new cql definition UUID set
		 */
		public void setCqlDefinitionUUIDSet(Set<String> cqlDefinitionUUIDSet) {
			this.cqlDefinitionUUIDSet = cqlDefinitionUUIDSet;
		}

		/**
		 * Sets the cql function UUID set.
		 *
		 * @param cqlFunctionUUIDSet the new cql function UUID set
		 */
		public void setCqlFunctionUUIDSet(Set<String> cqlFunctionUUIDSet) {
			this.cqlFunctionUUIDSet = cqlFunctionUUIDSet;
		}

		/**
		 * Sets the cql valueset identifier set.
		 *
		 * @param cqlValuesetIdentifierSet the new cql valueset identifier set
		 */
		public void setCqlValuesetIdentifierSet(Set<String> cqlValuesetIdentifierSet) {
			this.cqlValuesetIdentifierSet = cqlValuesetIdentifierSet;
		}

		/**
		 * Sets the cql parameter identifier set.
		 *
		 * @param cqlParameterIdentifierSet the new cql parameter identifier set
		 */
		public void setCqlParameterIdentifierSet(Set<String> cqlParameterIdentifierSet) {
			this.cqlParameterIdentifierSet = cqlParameterIdentifierSet;
		}

		/**
		 * Gets the cql codes set.
		 *
		 * @return the cql codes set
		 */
		public Set<String> getCqlCodesSet() {
			return cqlCodesSet;
		}

		/**
		 * Sets the cql codes set.
		 *
		 * @param cqlCodesSet the new cql codes set
		 */
		public void setCqlCodesSet(Set<String> cqlCodesSet) {
			this.cqlCodesSet = cqlCodesSet;
		}

		/**
		 * Adds the CQL code.
		 *
		 * @param identifier the identifier
		 */
		public void addCQLCode(String identifier) {
			this.cqlCodesSet.add(identifier);
		}

		/**
		 * Gets the cql def from pop set.
		 *
		 * @return the cql def from pop set
		 */
		public Set<String> getCqlDefFromPopSet() {
			return cqlDefFromPopSet;
		}

		/**
		 * Sets the cql def from pop set.
		 *
		 * @param cqlDefFromPopSet the new cql def from pop set
		 */
		public void setCqlDefFromPopSet(Set<String> cqlDefFromPopSet) {
			this.cqlDefFromPopSet = cqlDefFromPopSet;
		}

		/**
		 * Gets the cql func from pop set.
		 *
		 * @return the cql func from pop set
		 */
		public Set<String> getCqlFuncFromPopSet() {
			return cqlFuncFromPopSet;
		}

		/**
		 * Sets the cql func from pop set.
		 *
		 * @param cqlFuncFromPopSet the new cql func from pop set
		 */
		public void setCqlFuncFromPopSet(Set<String> cqlFuncFromPopSet) {
			this.cqlFuncFromPopSet = cqlFuncFromPopSet;
		}
	}

	/**
	 * Adds the used CQL libsto simple XML.
	 *
	 * @param originalDoc the original doc
	 * @param includeLibMap the include lib map
	 */
	public static void addUsedCQLLibstoSimpleXML(Document originalDoc, Map<String, CQLIncludeLibrary> includeLibMap) {

		Node allUsedLibsNode = originalDoc.createElement("allUsedCQLLibs");
		originalDoc.getFirstChild().appendChild(allUsedLibsNode);

		for (String libName : includeLibMap.keySet()) {
			CQLIncludeLibrary cqlLibrary = includeLibMap.get(libName);

			Element libNode = originalDoc.createElement("lib");
			libNode.setAttribute("id", cqlLibrary.getCqlLibraryId());
			libNode.setAttribute("alias", cqlLibrary.getAliasName());
			libNode.setAttribute("name", cqlLibrary.getCqlLibraryName());
			libNode.setAttribute("version", cqlLibrary.getVersion());
			libNode.setAttribute("isUnUsedGrandChild", "false");

			allUsedLibsNode.appendChild(libNode);
		}

	}
	
	/**
	 * Adds the un used grand childrento simple XML.
	 *
	 * @param originalDoc the original doc
	 * @param result the result
	 * @param cqlModel the cql model
	 * @throws XPathExpressionException the x path expression exception
	 */
	public static void addUnUsedGrandChildrentoSimpleXML(Document originalDoc, SaveUpdateCQLResult result, CQLModel cqlModel) throws XPathExpressionException {

		String allUsedCQLLibsXPath = "//allUsedCQLLibs";
		
		XmlProcessor xmlProcessor = new XmlProcessor("<test></test>");
		xmlProcessor.setOriginalDoc(originalDoc);
		
		Node allUsedLibsNode = xmlProcessor.findNode(originalDoc, allUsedCQLLibsXPath);
		
		if(allUsedLibsNode != null){
			
			List<CQLIncludeLibrary> cqlChildLibs = cqlModel.getCqlIncludeLibrarys();
			
			for(CQLIncludeLibrary library:cqlChildLibs){
				String libId = library.getCqlLibraryId();
				String libAlias = library.getAliasName();
				String libName = library.getCqlLibraryName();
				String libVersion = library.getVersion();				
				
				Collection<CQLIncludeLibrary> childs = result.getUsedCQLArtifacts().getIncludeLibMap().values();
				boolean found = childs.contains(library);
				
				if(found){
					LibHolderObject libHolderObject = cqlModel.getIncludedCQLLibXMLMap().
							get(libName + "-" + libVersion + "|" + libAlias);
					
					if(libHolderObject != null){
						String xml = libHolderObject.getMeasureXML();
						CQLModel childCQLModel = CQLUtilityClass.getCQLStringFromXML(xml);
						List<CQLIncludeLibrary> cqlGrandChildLibs = childCQLModel.getCqlIncludeLibrarys();
						
						for(CQLIncludeLibrary grandChildLib : cqlGrandChildLibs){
							
							boolean gcFound = childs.contains(grandChildLib);// foundCQLLib(childs, grandChildLib);
							
							if(!gcFound){
								Element libNode = originalDoc.createElement("lib");
								libNode.setAttribute("id", grandChildLib.getCqlLibraryId());
								libNode.setAttribute("alias", grandChildLib.getAliasName());
								libNode.setAttribute("name", grandChildLib.getCqlLibraryName());
								libNode.setAttribute("version", grandChildLib.getVersion());
								libNode.setAttribute("isUnUsedGrandChild", "true");
								allUsedLibsNode.appendChild(libNode);
							}
							
						}
					}
					
				}
			}
		}
	}
	
	
	/**
	 * Gets the included CQL expressions.
	 *
	 * @param cqlModel the cql model
	 * @param cqlLibraryDAO the cql library DAO
	 * @return the included CQL expressions
	 */
	public static void getIncludedCQLExpressions(CQLModel cqlModel, CQLLibraryDAO cqlLibraryDAO){
		
		Map<String, LibHolderObject> cqlLibNameMap = new HashMap<String, LibHolderObject>();
		getCQLIncludeLibMap(cqlModel, cqlLibNameMap, cqlLibraryDAO);
		cqlModel.setIncludedCQLLibXMLMap(cqlLibNameMap);
		setIncludedCQLExpressions(cqlModel);
	}
	
//	private static boolean foundCQLLib(Collection<CQLIncludeLibrary> libs, CQLIncludeLibrary library){
//		boolean returnVal = false;
//		
//		String libString = library.getCqlLibraryName() + "-" + library.getVersion() + "|" + library.getAliasName();
//		System.out.println(libString);
//		
//		for(CQLIncludeLibrary lib : libs){
//			
//			if(lib.equals(library)){
//				returnVal = true;
//				break;
//			}
//			
//		}
//		
//		return returnVal;
//	}
}
