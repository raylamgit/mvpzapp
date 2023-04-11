@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

/***
 *
 * Language script, which processors files to the defined target dataset
 * and reports the file as a build output file in the build report.
 *
 * Can be used for JCL, XML, Shared Copybooks and any other type of source code
 * which needs to be packaged and processed by the pipeline.
 *
 * Please note:
 *
 * * Verify the allocation options and adjust to your needs.
 *
 * * File names cannot exeed more than 8 characters, so they can be stored in
 *   the target dataset.
 *
 */

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
// Set to keep information about which datasets where already checked/created
@Field HashSet<String> verifiedBuildDatasets = new HashSet<String>()

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.processor_requiredBuildProperties)

List<String> buildList = argMap.buildList

// iterate through build list
buildList.each { buildFile ->
	println "*** processorring file $buildFile"

	// local variables and log file
	String member = CopyToPDS.createMemberName(buildFile)
	//Ray Lam 
	println "***** Ray Lam member is $member"

	// validate lenght of member name
	def memberLen = member.size()

	if (memberLen > 8) {
		errorMsg = "*! Warning. Member name (${member}) exceeds length of 8 characters. "
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg)
	} else {

		// evaluate the datasetmapping, which maps build files to targetDataset defintions
		PropertyMappings dsMapping = new PropertyMappings("processor_datasetMapping")

		//Ray Lam
		println "***** Ray Lam dsMapping  is $dsMapping"
		
		// obtain the target dataset based on the mapped dataset key
		String targetDataset = props.getProperty(dsMapping.getValue(buildFile))
		
		//Ray Lam
		println "***** Ray Lam targetDataset   is $targetDataset "
		
		//targetDataset = "RLAM.GENTEST.PRO"
		
		//println "***** Ray Lam Hard coded targetDataset   is $targetDataset"

		if (targetDataset != null) {

			// allocate target dataset
			if (!verifiedBuildDatasets.contains(targetDataset)) { // using a cache not to allocate all defined datasets
				verifiedBuildDatasets.add(targetDataset)
				buildUtils.createDatasets(targetDataset.split(), props.processor_srcOptions)
			}

			// copy the file to the target dataset
			String deployType = buildUtils.getDeployType("processor", buildFile, null)
			
			//Ray Lam
			println "***** Ray Lam deployType   is $deployType "
	

			try {
				int rc = new CopyToPDS().file(new File(buildUtils.getAbsolutePath(buildFile))).dataset(targetDataset).member(member).output(true).deployType(deployType).execute()
				// Ray Lam if (props.verbose) println "** Copyied $buildFile to $targetDataset with deployTyoe $deployType; rc = $rc"
				println "** Copyied $buildFile to $targetDataset with deployTyoe $deployType; rc = $rc"
				

				if (rc!=0){
					String errorMsg = "*! The CopyToPDS return code ($rc) for $buildFile exceeded the maximum return code allowed (0)."
					println(errorMsg)
					props.error = "true"
					buildUtils.updateBuildResult(errorMsg:errorMsg)
				}
			} catch (BuildException e) { // Catch potential exceptions like file truncation
				String errorMsg = "*! The CopyToPDS failed with an exception ${e.getMessage()}."
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg)
			}
		} else {
			String errorMsg =  "*! Target dataset for $buildFile could not be obtained from file properties. "
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg)
		}
	}
}
