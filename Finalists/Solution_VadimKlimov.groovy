import com.sap.gateway.ip.core.customdev.util.Message

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

Message processData(Message message) {
    def httpQuery = message.headers['CamelHttpQuery'] as String

    final def ALLOWED_FIELD_NAME_PATTERN = /[\p{L}\p{Nl}_][\p{L}\p{Nl}\p{Nd}\p{Mn}\p{Mc}\p{Pc}\p{Cf}]{0,}/
    def filterParams = [:]

    httpQuery = (httpQuery) ? URLDecoder.decode(httpQuery, StandardCharsets.UTF_8.name()) : ''
    httpQuery.tokenize('&').findAll { it.trim().indexOf('=') > 0 }.each { httpQueryParam ->
        def queryParam = httpQueryParam.tokenize('=')*.trim().findAll { it }
        if (queryParam.size() > 0 && queryParam.size() <= 2 && queryParam[0] ==~ ALLOWED_FIELD_NAME_PATTERN) {
            filterParams.put(queryParam[0], determineFilterParamValue(queryParam[0], queryParam[1]))
        }
    }

    def filterQuery = filterParams.collect { "$it.key eq $it.value" }.join(' and ')
    message.setProperty('filterCrit', filterQuery)

    return message
}


static String determineFilterParamValue(String paramName, String paramValue) {
    // Parameters / fields that require forced typing
    final def FORCED_TYPE_FIELDS = [
            ShipPostalCode: 'Edm.String'
    ]

    // Allowed date/time formats
    final def ALLOWED_DATE_TIME_FORMATS = [
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    ]

    // Value transformers based on field type
    def getNumber = { it }
    def getBoolean = { it.toLowerCase() }
    def getDateTime = { "datetime'$it'" }
    def getString = { "'$it'" }

    // Check if string can be parsed to allowed date/time formats
    def isDateTime = { String value ->
        def result = ALLOWED_DATE_TIME_FORMATS.find { formatter ->
            try {
                return (formatter.parse(value)) ? true : false
            } catch (DateTimeParseException ignored) {
                return false
            }
        }
        return (result) ? true : false
    }

    // Determine parameter value dynamically
    def determineParamValueDynamic = { String value ->
        def result
        switch (value) {
            case { !it }:
                result = 'null'
                break
            case { it.isNumber() }:
                result = getNumber(value)
                break
            case { it.toLowerCase() in ['true', 'false'] }:
                result = getBoolean(value)
                break
            case { isDateTime(it) }:
                result = getDateTime(value)
                break
            default:
                result = getString(value)
                break
        }
        return result
    }

    // Determine parameter value using forced typing (for exceptional cases / fields)
    def determineParamValueForced = { String field, String value ->
        def result
        switch (FORCED_TYPE_FIELDS[field]) {
            case ('Edm.Int16' || 'Edm.Int32' || 'Edm.Int64' || 'Edm.Single' || 'Edm.Double' || 'Edm.Decimal'):
                result = getNumber(value)
                break
            case ('Edm.Boolean'):
                result = getBoolean(value)
                break
            case ('Edm.DateTime'):
                result = getDateTime(value)
                break
            case ('Edm.String'):
                result = getString(value)
                break
            default:
                result = determineParamValueDynamic(value)
                break
        }
        return result
    }

    // Determine parameter value
    if (FORCED_TYPE_FIELDS?.containsKey(paramName)) {
        return determineParamValueForced(paramName, paramValue)
    } else {
        return determineParamValueDynamic(paramValue)
    }
}