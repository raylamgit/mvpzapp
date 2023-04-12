@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import groovy.transform.*
import groovy.cli.commons.*
import com.ibm.jzos.ZFile	
	
// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))
//@Field def concatUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ConcatUtilities.groovy"))	
	


/*
 * createCompileCommand - creates a MVSExec command for compiling the COBOL program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = createCobolParms(buildFile, logicalFile)
	String compiler = props.getFileProperty('cobol_compiler', buildFile)

	// define the MVSExec command to compile the program
	MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)
	
	isZUnitTestCase = (props.getFileProperty('cobol_testcase', buildFile).equals('true')) ? true : false


	// add DD statements to the compile command
 
	if (isZUnitTestCase){
	compile.dd(new DDStatement().name("SYSIN").dsn("${props.cobol_testcase_srcPDS}($member)").options('shr').report(true))
	}
	else
	{
		compile.dd(new DDStatement().name("SYSIN").dsn("${props.cobol_srcPDS}($member)").options('shr').report(true))
	}
	 
	compile.dd(new DDStatement().name("SYSPRINT").options(props.cobol_printTempOptions))
	compile.dd(new DDStatement().name("SYSMDECK").options(props.cobol_tempOptions))
	(1..17).toList().each { num ->
		compile.dd(new DDStatement().name("SYSUT$num").options(props.cobol_tempOptions))
	}

	// define object dataset allocation
	compile.dd(new DDStatement().name("SYSLIN").dsn("${props.cobol_objPDS}($member)").options('shr').output(true))

	// add a syslib to the compile command with optional bms output copybook and CICS concatenation
	compile.dd(new DDStatement().name("SYSLIB").dsn(props.cobol_cpyPDS).options("shr"))
	// adding bms copybook libraries only when it exists
	 
	if (props.bms_cpyPDS && ZFile.dsExists("'${props.bms_cpyPDS}'"))
		compile.dd(new DDStatement().dsn(props.bms_cpyPDS).options("shr"))
	if(props.team)
		compile.dd(new DDStatement().dsn(props.cobol_BMS_PDS).options("shr"))
		
	
	// add additional datasets with dependencies based on the dependenciesDatasetMapping
	PropertyMappings dsMapping = new PropertyMappings('cobol_dependenciesDatasetMapping')
	dsMapping.getValues().each { targetDataset ->
		// exclude the defaults cobol_cpyPDS and any overwrite in the alternativeLibraryNameMap
		if (targetDataset != 'cobol_cpyPDS')
			compile.dd(new DDStatement().dsn(props.getProperty(targetDataset)).options("shr"))
	}

	// add custom concatenation
	def compileSyslibConcatenation = props.getFileProperty('cobol_compileSyslibConcatenation', buildFile) ?: ""
	if (compileSyslibConcatenation) {
		def concatDatasets = concatExpand(compileSyslibConcatenation, buildFile)
		def String[] syslibDatasets = concatDatasets.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
		// Ray Lam testing concat
		println "***** Ray Lam syslibDatasets -> $syslibDatasets" 
	}
	
	// add subsystem libraries
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHCOB).options("shr"))

	if (buildUtils.isMQ(logicalFile))
		compile.dd(new DDStatement().dsn(props.SCSQCOBC).options("shr"))
		
	/* Ray Lam Skip
	// add additional zunit libraries
	if (isZUnitTestCase)
	compile.dd(new DDStatement().dsn(props.SBZUSAMP).options("shr"))
	*/

	// adding alternate library definitions
	if (props.cobol_dependenciesAlternativeLibraryNameMapping) {
		alternateLibraryNameAllocations = buildUtils.parseJSONStringToMap(props.cobol_dependenciesAlternativeLibraryNameMapping)
		alternateLibraryNameAllocations.each { libraryName, datasetDefinition ->
			datasetName = props.getProperty(datasetDefinition)
			if (datasetName) {
				compile.dd(new DDStatement().name(libraryName).dsn(datasetName).options("shr"))
			}
			else {
				String errorMsg = "*! Cobol.groovy. The dataset definition $datasetDefinition could not be resolved from the DBB Build properties."
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg)
			}
		}
	}
	
	// add a tasklib to the compile command with optional CICS, DB2, and IDz concatenations
	String compilerVer = props.getFileProperty('cobol_compilerVersion', buildFile)
	compile.dd(new DDStatement().name("TASKLIB").dsn(props."SIGYCOMP_$compilerVer").options("shr"))
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))
	
	if (props.SFELLOAD)
		compile.dd(new DDStatement().dsn(props.SFELLOAD).options("shr"))

	// add optional DBRMLIB if build file contains DB2 code
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().name("DBRMLIB").dsn("$props.cobol_dbrmPDS($member)").options('shr').output(true).deployType('DBRM'))

	// add IDz User Build Error Feedback DDs
	if (props.errPrefix) {
		compile.dd(new DDStatement().name("SYSADATA").options("DUMMY"))
		// SYSXMLSD.XML suffix is mandatory for IDZ/ZOD to populate remote error list
		compile.dd(new DDStatement().name("SYSXMLSD").dsn("${props.hlq}.${props.errPrefix}.SYSXMLSD.XML").options(props.cobol_compileErrorFeedbackXmlOptions))
	}

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	compile.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))

	return compile
}


/*
 * createCobolParms - Builds up the COBOL compiler parameter list from build and file properties
 */
def createCobolParms(String buildFile, LogicalFile logicalFile) {
	def parms = props.getFileProperty('cobol_compileParms', buildFile) ?: ""
	def cics = props.getFileProperty('cobol_compileCICSParms', buildFile) ?: ""
	def sql = props.getFileProperty('cobol_compileSQLParms', buildFile) ?: ""
	def errPrefixOptions = props.getFileProperty('cobol_compileErrorPrefixParms', buildFile) ?: ""
	def compileDebugParms = props.getFileProperty('cobol_compileDebugParms', buildFile)

	if (buildUtils.isCICS(logicalFile))
		parms = "$parms,$cics"

	if (buildUtils.isSQL(logicalFile))
		parms = "$parms,$sql"

	if (props.errPrefix)
		parms = "$parms,$errPrefixOptions"

	// add debug options
	if (props.debug)  {
		parms = "$parms,$compileDebugParms"
	}

	if (parms.startsWith(','))
		parms = parms.drop(1)

	if (props.verbose) println "Cobol compiler parms for $buildFile = $parms"
	return parms
}


def concatExpand(String concatEnv, String buildFile) {
	
	println "***** Ray Lam concatExpand -> $concatEnv"
	
	
	def concatEnvDatasets = props.getFileProperty(concatEnv, buildFile) ?: ""
	println "***** Ray Lam concatExpand -> $concatEnvDatasets"
	
	
	return concatEnvDatasets

}
