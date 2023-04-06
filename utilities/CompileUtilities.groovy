@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.json.*
//import groovy.json.JsonParserType
//import groovy.json.JsonBuilder
//import groovy.json.JsonSlurper
import groovy.transform.*
import com.ibm.jzos.ZFile
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import com.ibm.jzos.FileAttribute
import groovy.ant.*

//@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))
	
println("** Building files mapped to ${this.class.getName()}.groovy script")

println("***** Ray Lam testing Cobol.groovy")

def compileParms(String processorMember, String Member, String Option1, String Option2) {


/* Testing Processor Ray lam
	String ProcessorParm = props.getFileProperty('processorSearchPath', buildFile)
	println "***** Ray Lam Processor parms for $buildFile = $ProcessorParm"

	 println "***** Ray Lam call parseProcessor"
	String processor = parseProcessor(buildFile, logicalFile, member, logFile)
 

// Ray Lam Test Processor 
	String processorFile = props.getFileProperty('processorSearchPath', buildFile)

	println "***** Ray Lam Processor file for $buildFile = $processorFile"	
	String processorMember = "${processorFile}/${member}.pro"
	
	println "***** Ray Lam Processor member for $buildFile = $processorMember"	

*/
// Parse the JSON file

	println "***** Ray Lam Parsing JSON file using JsonSlurper"	
	println "***** Ray Lam Processor member for processorMember = $processorMember"
	println "***** Ray Lam Processor member for Member = $Member"
	String[] allowedEncodings = ["UTF-8", "IBM-1047"]
	//depFilePath = getAbsolutePath(processorMember)
	// Load dependency file and verify existance
	 
	File depFile = new File(processorMember)
	assert depFile.exists() : "*! Dependency file not found: ${depFile}"

    
    String encoding = retrieveHFSFileEncoding(depFile) // Determine the encoding from filetag
	JsonSlurper slurper = new JsonSlurper().setType(JsonParserType.INDEX_OVERLAY) // Use INDEX_OVERLAY, fastest parser
	def depFileData
	if (encoding) {
		println "Parsing dependency file as ${encoding}: "
		assert allowedEncodings.contains(encoding) : "*! Dependency file must be encoded and tagged as either UTF-8 or IBM-1047 but was ${encoding}"
		depFileData = slurper.parse(depFile, encoding) // Parse dependency file with encoding
	}
	else {
		println "[WARNING] Dependency file is untagged. \nParsing dependency file with default system encoding: "
		depFileData = slurper.parse(depFile) // Assume default encoding for system
	}
	 println new JsonBuilder(depFileData).toPrettyString() // Pretty print if verbose
	 
     println "***** Parsing Json name and value pair "	

	 //def jsonSlurper = new JsonSlurper()

     //def config = jsonSlurper.parse(new File(processorMember))
  
	
   
	 //println "***** Parsing PROCESSOR  = ${depFileData.LGICDB01.PROCESSOR}"
	 println "***** Ray Lam Parsing PROCESSOR        = ${depFileData[Member].PROCESSOR}"
	 println "***** Ray Lam Parsing COBOOL VERSION   = ${depFileData[Member].COBOL.VERSION}"
	 println "***** Ray Lam Parsing COBOOL OPTIONS   = ${depFileData[Member].COBOL.OPTIONS}" 
	 println "***** Ray Lam Parsing LINKPARM AMODE    = ${depFileData[Member].LINKPARM.AMODE}"
	 println "***** Ray Lam Parsing LINKPARM RMODE    = ${depFileData[Member].LINKPARM.RMODE}"
	 println "***** Ray Lam Parsing LINKEDIT INCLUDE = ${depFileData[Member].LINKEDIT.INCLUDE}"
	 println "***** Ray Lam Parsing LINKEDIT INCLUDE = ${depFileData[Member].LINKEDIT.ENTRY}"
	 println "***** Ray Lam Parsing DB2 SSID         = ${depFileData[Member].DB2.SSID}"
	 // Ray Lam parsed a specfic item to a variable
	 String cobolOptions = depFileData[Member][Option1][Option2]
	 println "***** Ray Lam Parsing specific $Member $Option1 $Option2 = $cobolOptions"
 
 
	return cobolOptions

}
	
	def retrieveHFSFileEncoding(File file) {
	FileAttribute.Stat stat = FileAttribute.getStat(file.getAbsolutePath())
    FileAttribute.Tag tag = stat.getTag()
	int i = 0
	if (tag != null)
	{
  		char x = tag.getCodeCharacterSetID()
  		i = (int) x
	}

	switch(i) {
		case 0: return null // Return null if file is untagged
		case 1208: return "UTF-8"
		default: return "IBM-${i}"
	}
	
}