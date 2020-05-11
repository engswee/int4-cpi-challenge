import com.sap.gateway.ip.core.customdev.util.Message

import java.text.ParseException

Message processData(Message message) {
    def fullQuery = message.getHeader('CamelHttpQuery', String)
    URI uri = new URI("http://localhost?${fullQuery}")
    def decodedQuery = uri.getQuery()

    def uniqueParameters = new LinkedHashMap()
    decodedQuery.split('&').each { String parameterPair ->
        // Extract key and value from each pair of query parameter
        int index = parameterPair.indexOf('=')
        String key = parameterPair.substring(0, index)
        String value = parameterPair.substring(index + 1)
        if (!uniqueParameters.containsKey(key))
            uniqueParameters.put(key, value)
    }
    // Generate filter criteria
    def conditions = []
    uniqueParameters.each { key, String value ->
        if (value.isNumber() && key != 'ShipPostalCode')
            conditions << "${key} eq ${value}"
        else if (isDate("yyyy-MM-dd'T'HH:mm:ss", value))
            conditions << "${key} eq datetime'${value}'"
        else
            conditions << "${key} eq '${value}'"
    }
    def filterCrit = conditions.join(' and ')

    message.setProperty('filterCrit', filterCrit)
    return message
}

private boolean isDate(String pattern, String input) {
    try {
        def date = Date.parse(pattern, input)
        return true
    } catch (ParseException ex) {
        return false
    }
}