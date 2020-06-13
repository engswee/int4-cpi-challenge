import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLogFactory
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultExchange
import spock.lang.Shared
import spock.lang.Specification

class GenerateFilterCriteriaSpec extends Specification {
    @Shared Script script
    @Shared CamelContext context
    @Shared MessageLogFactory mlf
    Message msg
    Exchange exchange

    def setupSpec() {
        // Setup Spock stubs and mocks
        mlf = Stub(MessageLogFactory)
        // Setup binding to inject variable into script
        Binding binding = new Binding()
        GroovyShell shell = new GroovyShell(binding)
        binding.setProperty('messageLogFactory', mlf)
        script = shell.parse(new File('src/main/resources/script/GenerateFilterCriteria.groovy'))
        context = new DefaultCamelContext()
    }

    def setup() {
        exchange = new DefaultExchange(context)
        msg = new MessageImpl(exchange)
    }

    def setHeader(name, value) {
        exchange.getIn().setHeader(name, value)
        msg.setHeader(name, exchange.getIn().getHeader(name))
    }

    def 'Multiple string values'() {
        given:
        setHeader('CamelHttpQuery', 'CustomerID=TOMSP&ShipCountry=Germany')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "CustomerID eq 'TOMSP' and ShipCountry eq 'Germany'"
    }

    def 'Numeric EmployeeID as first parameter'() {
        given:
        setHeader('CamelHttpQuery', 'EmployeeID=6&ShipCountry=Sweden')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 6 and ShipCountry eq 'Sweden'"
    }

    def 'Numeric EmployeeID as second parameter'() {
        given:
        setHeader('CamelHttpQuery', 'ShipCountry=Sweden&EmployeeID=6')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipCountry eq 'Sweden' and EmployeeID eq 6"
    }
    def 'Encoded space characters'() {
        given:
        setHeader('CamelHttpQuery', 'EmployeeID=4&ShipCity=Rio%20de%20Janeiro')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 4 and ShipCity eq 'Rio de Janeiro'"
    }

    def 'ShipPostalCode with alpha numeric'() {
        given:
        setHeader('CamelHttpQuery', 'ShipPostalCode=WX3%206FW')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq 'WX3 6FW'"
    }

    def 'ShipPostalCode with number'() {
        given:
        setHeader('CamelHttpQuery', 'ShipPostalCode=8010')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq '8010'"
    }

    def 'ShipPostalCode with number with leading zeros'() {
        given:
        setHeader('CamelHttpQuery', 'ShipPostalCode=05023')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq '05023'"
    }

    def 'Encoded European characters in ShipCity'() {
        given:
        setHeader('CamelHttpQuery', 'ShipCity=K%C3%B6ln')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipCity eq 'Köln'"
    }

    def 'Date OrderDate as first parameter'() {
        given:
        setHeader('CamelHttpQuery', 'OrderDate=1996-11-27T00:00:00')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "OrderDate eq datetime'1996-11-27T00:00:00'"
    }

    def 'Date OrderDate as second parameter'() {
        given:
        setHeader('CamelHttpQuery', 'EmployeeID=6&OrderDate=1996-11-27T00:00:00')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 6 and OrderDate eq datetime'1996-11-27T00:00:00'"
    }
    def 'Ampersand & using %26'() {
        given:
        setHeader('CamelHttpQuery', 'EmployeeID=6%26$expand=Customer')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == 'EmployeeID eq 6 and $expand eq \'Customer\''
    }

    def 'Repeated parameter'() {
        given:
        setHeader('CamelHttpQuery', 'EmployeeID=6&ShipCountry=Sweden&EmployeeID=7')

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == 'EmployeeID eq 6 and ShipCountry eq \'Sweden\''
    }
}