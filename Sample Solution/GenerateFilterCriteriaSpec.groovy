import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
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
    MessageLog log

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
        log = Mock(MessageLog)
        mlf.getMessageLog(msg) >> log
    }

    def setHeader(name, value) {
        exchange.getIn().setHeader(name, value)
        msg.setHeader(name, exchange.getIn().getHeader(name))
    }

    def 'Multiple string values'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'CustomerID=TOMSP&ShipCountry=Germany')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "CustomerID eq 'TOMSP' and ShipCountry eq 'Germany'"
    }

    def 'Numeric EmployeeID as first parameter'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'EmployeeID=6&ShipCountry=Sweden')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 6 and ShipCountry eq 'Sweden'"
    }

    def 'Numeric EmployeeID as second parameter'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'ShipCountry=Sweden&EmployeeID=6')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipCountry eq 'Sweden' and EmployeeID eq 6"
    }
    def 'Encoded space characters'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'EmployeeID=4&ShipCity=Rio%20de%20Janeiro')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 4 and ShipCity eq 'Rio de Janeiro'"
    }

    def 'ShipPostalCode with alpha numeric'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'ShipPostalCode=WX3%206FW')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq 'WX3 6FW'"
    }

    def 'ShipPostalCode with number'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'ShipPostalCode=8010')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq '8010'"
    }

    def 'ShipPostalCode with number with leading zeros'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'ShipPostalCode=05023')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipPostalCode eq '05023'"
    }

    def 'Encoded European characters in ShipCity'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'ShipCity=K%C3%B6ln')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "ShipCity eq 'Köln'"
    }

    def 'Date OrderDate as first parameter'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'OrderDate=1996-11-27T00:00:00')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "OrderDate eq datetime'1996-11-27T00:00:00'"
    }

    def 'Date OrderDate as second parameter'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'EmployeeID=6&OrderDate=1996-11-27T00:00:00')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == "EmployeeID eq 6 and OrderDate eq datetime'1996-11-27T00:00:00'"
    }
    def 'Ampersand & using %26'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'EmployeeID=6%26$expand=Customer')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == 'EmployeeID eq 6 and $expand eq \'Customer\''
    }

    def 'Repeated parameter'() {
        given:
        //--------------------------------------------------------------
        // Initialize message with body, header and property
        def body = new String('dummy')
        setHeader('CamelHttpQuery', 'EmployeeID=6&ShipCountry=Sweden&EmployeeID=7')
        //--------------------------------------------------------------

        // Set exchange body in case automatic Type Conversion is required
        exchange.getIn().setBody(body)
        msg.setBody(exchange.getIn().getBody())

        when:
        // Execute script
        script.processData(msg)

        then:
        msg.getProperty('filterCrit') == 'EmployeeID eq 6 and ShipCountry eq \'Sweden\''
    }
}