@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import groovy.transform.*
	

// sysPrint Method

def sysPrint(String prnPDS, String member, File logFile) {
	
	new CopyToPDS().file(new File("$logFile"))
    	.hfsEncoding("UTF-8")
    	.dataset(prnPDS)
    	.pdsEncoding("IBM-1047")
    	.member(member)
    	.execute()


   println("***** Ray Lam sysPrint copyToPADS finished")
   String printPDS = prnPDS + "($member)"
   return printPDS
		
}

def headLines(File logFile, String headLine, String headersPDS) {
	
	println "***** Ray Lam HEADLINES $headLine"

// All the following methods have been repplaced by the "headlines" method .

def copyDashes(File logFile) {
	
	println "***** Ray Lam DASHED $logFile"
	
	CopyToHFS copyCmd = new CopyToHFS();
	copyCmd.setDataset("RLAM.IDZAPP.HEADERS");
	copyCmd.setMember("DASHES");
	copyCmd.setFile(new File("$logFile"));
	copyCmd.append(true);
	copyCmd.copy();
	
		
	return logFile

}

