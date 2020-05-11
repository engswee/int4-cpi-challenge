import com.sap.gateway.ip.core.customdev.util.Message
import java.util.HashMap
import java.net.URLDecoder;
import groovy.xml.*
import org.apache.olingo.odata2.*
import org.apache.olingo.odata2.api.edm.Edm
import org.apache.olingo.odata2.api.edm.EdmEntitySet
import org.apache.olingo.odata2.api.edm.EdmEntityType
import org.apache.olingo.odata2.api.edm.EdmProperty
import org.apache.olingo.odata2.api.edm.EdmTyped
import org.apache.olingo.odata2.api.ep.EntityProvider
import org.apache.olingo.odata2.api.exception.ODataMessageException
import org.apache.olingo.odata2.api.uri.expression.ExpressionParserException
import org.apache.olingo.odata2.api.uri.expression.FilterExpression
import org.apache.olingo.odata2.core.uri.UriParserImpl

def Message processData(Message message) {

	def messageLog = messageLogFactory.getMessageLog(message)
	
	Map map = message.getHeaders()
	String CamelHttpQuery = map.get("CamelHttpQuery")
	messageLog.setStringProperty("CamelHttpQuery", CamelHttpQuery)
	//default to "" if no query parameters are present
	String filter = ""
	
	//only generate filter criteria from CamelHttpQuery if it exists
	if(CamelHttpQuery != null)
	{
		CamelHttpQuery = URLDecoder.decode(CamelHttpQuery,"UTF-8")
		messageLog.setStringProperty("CamelHttpQuery2", CamelHttpQuery)
		
		//Read EDM file from iFlow resources
		String edmx = this.getClass().getResource("/edmx/services_odata_org_V2_Northwind_Northwind_svc.edmx").getText("UTF-8")
		
		//Create EDM object
		ByteArrayInputStream inputStream = new ByteArrayInputStream(edmx.getBytes())
		Edm edm = EntityProvider.readMetadata(inputStream, false);
		
		//Orders entity set
		EdmEntitySet entitySet = edm.getDefaultEntityContainer().getEntitySet("Orders");
		//Orders entity type
		EdmEntityType entityType = entitySet.getEntityType();
			
		//generete filter
		filter = generateFilter(CamelHttpQuery,entityType);		
		
		//Parse the filter for correctness using Apache Olingo against the entity model
		if(!checkFilter(edm,entityType,filter,messageLog))
			throw new Exception(filter + " is not a valid filter")

	}
	
	message.setProperty("filterCrit", filter)	
	
	return message
}


private static String generateFilter(String CamelHttpQuery,EdmEntityType entityType) throws Exception
{
	HashMap<String,String> operatorMap = new HashMap<String,String>();
	operatorMap.put("=", " eq ");
	
	StringBuilder filter = new StringBuilder();
	
	//get different query parts i.e OrderID=10248 & ShipCountry=France"
	String[] camelHttpQueryArray = CamelHttpQuery.split("&");
	ArrayList<String> result = new ArrayList<String>();
	
	//replace operator with OData operator
	for(int i=0;i< camelHttpQueryArray.length;i++)
	{
		String expression = camelHttpQueryArray[i];
		
		Iterator<String> keyIterator = operatorMap.keySet().iterator();
		while(keyIterator.hasNext())
		{
			String key = keyIterator.next();
			
			//check for =
			if(expression.indexOf(key) > 0)
			{
				int index = expression.indexOf(key);
				int length = operatorMap.get(key).length();
				
				//replace = with eq
				expression = expression.replaceAll(key,operatorMap.get(key));
				
				//get the entity property i.e OrderID
				String propertyName = expression.substring(0, index);
				
				//get the property from the EDM entity type
				EdmTyped edmProperty = entityType.getProperty(propertyName);
				//if Property is not found throw an exception
				if(edmProperty==null) 
					throw new Exception(propertyName + " Is not a valid property on Orders Enity type!")
				
				//get type of property i.e String,Int32
				String edmPropertyType = edmProperty.getType().getName();
				//get value of property. secion on right of operator
				String value = expression.substring(index + length, expression.length());
				String newValue;
				
				//if property is not found set type to "" and it will be handled as default
				edmPropertyType = (edmPropertyType == null) ? "" : edmPropertyType;
				
				//only catering for Order types String,DateTime,Int32,Decimal
				switch(edmPropertyType)
				{
					//String value enclosed by '<value>' and escape ' with ''
					  case "String":
						  newValue = "'"+expression.substring(index + length, expression.length()).replaceAll("'", "''") +"'";
						  break;
					  //DateTime value encloses by datetime'<value>'
					  case "DateTime":
						  newValue = "datetime'"+expression.substring(index + length, expression.length()) +"'";
						  break;
					  //Rest as is
					  default:
						  newValue = expression.substring(index + length, expression.length());
				}
				
				//use the new formatted value according to enity type
				expression = expression.replaceAll(value,newValue);
			}
		}
		
		result.add(expression);
	}
	
	//Combine expressions into 1 filter
	for(int i=0; i<result.size();i++)
	{
		String expression = result.get(i);
		filter.append(expression);
		if(i < result.size() -1)
			filter.append(" and ");
	}
	
	return filter.toString();
}

private static boolean checkFilter(Edm edm,EdmEntityType entityType,String filter,def messageLog)
{
	boolean isValid = true
	//Parse the filter for correctness using Apache Olingo against the entity model
	UriParserImpl parseFilter = new UriParserImpl(edm);
	try
	{
		FilterExpression filterExpression = parseFilter.parseFilterString(entityType,filter);
	} 
	catch (ExpressionParserException e)
	{
		messageLog.setStringProperty("ExpressionParserException", e.toString())
		isValid = false
	} 
	catch (ODataMessageException e)
	{
		messageLog.setStringProperty("ExpressionParserException", e.toString())
		isValid = false
	}	

	return isValid
}

