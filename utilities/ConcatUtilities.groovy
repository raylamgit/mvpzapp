@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import groovy.transform.* 

	
def concatExpand(String concatEnv) {
	
	
	println "***** Ray Lam concatExpand -> $concatEnv"
	
	def String concatDatasets = ''
	
	switch (concatEnv) {
		
		 case "ENV1":
		         concatDatasets = "RLAM.RAYGENAP.BMS.COPY,\
		                           RLAM.RAYGENAP.COPY,\
		                           RLAM.RAYGENAP.OBJ"
		         println("concatExpand ENV1"); 
			 break;
				  
		 case "ENV2":
		         concatDatasets = "RLAM.GENAPP.BMS.COPY,\
		                           RLAM.GENAPP.COPY,\
		                           RLAM.GENAPP.OBJ"
		         println("concatExpand ENV2"); 
			 break;
		    default:
             println("no match");
		}
	
	println("concatDatasets -> $concatDatasets "); 
	return concatDatasets

}


