import com.sap.gateway.ip.core.customdev.util.Message
import java.text.SimpleDateFormat
import org.osgi.framework.FrameworkUtil

Message processData(Message message) {
   
    //Build datatype map
    def iFlowName = message.exchange.getContext().getName()
    def dtMap = parseFieldDatatype(iFlowName, "Order")
    
    //Create filter query
    def input = message.getHeaders().get("CamelHttpQuery")
    def filter = buildFilter(dtMap, input)
    message.setProperty("filterCrit", filter)

    return message
}

def buildFilter(dtMap, query){
    def filter = ""
    //Only build filter, if query parameter was set and filled. Otherwise keep filter empty/select everything
    if (query != null && query != ""){
        //Transform all pairs
        def seenFields = []
        def preparedQuery = query.tokenize('&').collect { pair -> 
            
            def parts = pair.split("=")
            //if no value is provided, assume empty value
            if (parts.length == 1){
                parts = [parts[0], ""]
            }
            
            //Check provided fieldname for being valid
            if (!dtMap.containsKey(parts[0])){
            	throw new Exception("Invalid fieldname (${parts[0]}) provided")
            }
            
            //Avoid HTTP pollution
            if (seenFields.contains(parts[0])){
        		throw new Exception("Illegal query. Fieldname (${parts[0]}) provided more than once.")
        	}
        	seenFields << parts[0]
        	
        	//Build OData filter part
            pair = formatSelectorByDatatype(dtMap, parts[0], parts[1])
        }
        filter = preparedQuery.join(" and ")        
    }
    return filter
}

def formatSelectorByDatatype(dtMap, fieldName, fieldVal){
	if (dtMap[fieldName] == "Edm.Int32"){
		//Check if valid Int32
		if (!fieldVal.matches("\\d+")){
			throw new Exception("Invalid Int32 value provided for ${fieldName}")
		}
                //Remove leading zeros, if value =! 0
		return "${fieldName} eq ${fieldVal.replaceAll("^0+(?!\$)","")}"
	} else if (dtMap[fieldName] == "Edm.String"){	
		//Encapsulate and escape string values			
		return "${fieldName} eq '${fieldVal.replace("'","\\'")}'"
	} else if (dtMap[fieldName] == "Edm.Decimal"){				
		//Check if valid Decimal
		if (!fieldVal.matches("(\\d*[\\.,])?\\d+")){
			throw new Exception("Invalid Decimal value provided for ${fieldName}")
		}
                //Remove leading zeros, if value >= 1
		return "${fieldName} eq ${fieldVal.replaceAll("^0+(?!\\.|\$)","")}"
	} else if (dtMap[fieldName] == "Edm.DateTime"){	
 		//Check if logical datetime per syntax
                if (!fieldVal.matches("\\d{4}-(0\\d|1[12])-([0-2]\\d|3[01])(T([01]\\d|2[0-4])(:[0-5]\\d){2})?")){
			throw new Exception("Invalid date provided. Provide date either in yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd format!")
		}		
		if (fieldVal.matches("^\\d{4}(\\-\\d{2}){2}\$")){
			//Handle whole day via gt/lt filter
			//Parse date first, to check if it's logical consistent (regex pre-check was syntax only)
			def date = new SimpleDateFormat("yyyy-MM-dd").parse(fieldVal)
			return "(${fieldName} ge DateTime'${date.format("yyyy-MM-dd'T'HH:mm:ss")}' and ${fieldName} lt DateTime'${date.plus(1).format("yyyy-MM-dd'T'HH:mm:ss")}')"
		} else {			
			//Handle exact datetime
			//Parse date first, to check if it's logical consistent (regex pre-check was syntax only)
			def date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(fieldVal)
			return "${fieldName} eq DateTime'${fieldVal}'"
		}		
	} else {
               throw new Exception("Data type ${dtMap[fieldName]} not implemented yet")
        }
}

def parseFieldDatatype(iFlowName, entity){
    
    //Get list of EDMX files 
    def edmxFiles = FrameworkUtil.getBundle(Class.forName("com.sap.gateway.ip.core.customdev.util.Message")).getBundleContext().getBundles().find{ bndl -> bndl.getSymbolicName() == iFlowName }.findEntries("/edmx", "*.edmx", false)
    def result = [:]
    //Check EDMX files for fitting entity descriptions
    edmxFiles.each { file -> 
        def edmxStr = readTextFileToString(file)
        def xml = new XmlSlurper().parseText(edmxStr)
        if (xml.'**'.any{ node -> node.name() == "EntityType" && node.@Name == entity }){
            //Loop through all properties of the given EntityType
        	result = xml.'**'.findAll { node -> node.name() == "Property" && node.parent().name() == "EntityType" && node.parent().@Name == entity }.collectEntries{ 
        		[it.@Name.text(), it.@Type.text()]
        	}
        	return
        }
    }
    if (result.size() == 0){
        throw new Exception("IFlow doesn't contain a valid EDMX file")
    }
    return result
}

def readTextFileToString(URL file){
	BufferedReader br = new BufferedReader(new InputStreamReader(file.openConnection().getInputStream()))
	def fileContent = ""
    while(br.ready()){
        fileContent += br.readLine()
    }
    br.close()
    return fileContent
}
