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


    new CopyToHFS().file(new File("$logFile"))
    .hfsEncoding("UTF-8")
    .dataset("$headersPDS")
    .member("$headLine")
    .append(true)
    .execute()
		
     return

}

