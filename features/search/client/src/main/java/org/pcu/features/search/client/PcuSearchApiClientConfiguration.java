package org.pcu.features.search.client;

import java.io.File;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.pcu.features.search.api.PcuSearchApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * In package BELOW others else (ex. in .web) doesn't scan them
 * @author mdutoo
 */
@Configuration
@ComponentScan // i.e. ("org.pcu.features.search.client") ; not org.pcu else scans ex. ESSearchProviderApiImpl
// which can't find client, and in another project does not work (order ?)
@Import(JaxRsConfig.class) // creates cxf bus (@EnableJaxRsProxyClient not conf'ble enough : address...)
public class PcuSearchApiClientConfiguration {

   ///public static final String PCU_ELASTICSEARCH_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
   ///public static final DateTimeFormatter PCU_ELASTICSEARCH_DATE_FORMATTER = DateTimeFormatter.ofPattern(PCU_ELASTICSEARCH_DATE_PATTERN);
   
   @Value("${pcu.rest.enableIndenting:false}")
   private boolean enableIndenting;
   
   /** for prop check purpose */
   @Autowired
   private Environment env;
   
   public PcuSearchApiClientConfiguration() {
      
   }
   
   /**
    * TODO move to PCU-wide common rest conf (but not common to spi clients)
    * TODO also mutualize with elasticSearchMapper, in rest helper ?!?
    * @return
    */
   @Bean
   @Primary // else NoUniqueBeanDefinitionException: No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available: expected single matching bean but found 2: pcuSearchApiMapper,elasticSearchMapper
   // see https://github.com/spring-projects/spring-boot/issues/6529
   public ObjectMapper pcuSearchApiMapper() {
      ObjectMapper mapper = new ObjectMapper();
      
      // date conf :
      // default 2016-09-30T16:53:40.255Z is ES'
      // see https://github.com/FasterXML/jackson-datatype-jsr310/issues/39
      JavaTimeModule javaTimeModule = new JavaTimeModule();
      ///javaTimeModule.addSerializer(ZonedDateTime.class, // else 2016-09-30T16:53:40.255Z
      ///      new ZonedDateTimeSerializer(PCU_ELASTICSEARCH_DATE_FORMATTER)); // else 2016-09-30T16:53:40.255
      mapper.registerModule(javaTimeModule);
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // else 1501857549.048000000 http://www.baeldung.com/jackson-serialize-dates
      
      // more lenient parsing that accepts single element as array :
      // NB. required on server-side only for ESQueryClause.must/...
      mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
      
      // don't output null fields (else there would be a throng of them) :
      // https://stackoverflow.com/questions/11757487/how-to-tell-jackson-to-ignore-a-field-during-serialization-if-its-value-is-null
      mapper.setSerializationInclusion(Include.NON_NULL);
      
      //mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      
      mapper.configure(SerializationFeature.INDENT_OUTPUT, enableIndenting);
      
      return mapper;
   }
   
   @Bean
   public JacksonJsonProvider pcuSearchApiJsonProvider(ObjectMapper pcuSearchApiMapper) {
      return new JacksonJsonProvider(pcuSearchApiMapper);
   }

   /** set it to empty or spaces to disable it */
   @Value("${pcu.search.api.restLogFile:es-rest-mock.log}")
   private String restLogFilePathProp;
   // inspired from 
   @Value("${pcu.search.api.client.address:http://localhost:${server.port:8080}/pcu}")
   private String address;
   protected int serverPort;
   @Value("${cxf.jaxrs.client.thread-safe:false}")
   private Boolean threadSafe;
   @Bean
   public Client pcuSearchApiRestClient(SpringBus bus,
         JacksonJsonProvider pcuSearchApiJsonProvider/*,
         ESApiExceptionMapper exceptionMapper,
         ESApiResponseExceptionMapper responseExceptionMapper*/) {
      JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
      bean.setBus(bus);        
      bean.setThreadSafe(threadSafe);
      
      bean.setAddress(address);
      bean.setServiceClass(PcuSearchApi.class);
      bean.setProvider(pcuSearchApiJsonProvider); // actually an addProvider
      /*bean.setProvider(exceptionMapper);
      bean.setProvider(responseExceptionMapper);*/
      
      if (restLogFilePathProp != null && !restLogFilePathProp.trim().isEmpty()) { // ex. not in prod
         //LOGGER.warn("Enabling logging of all REST exchanges including body to "
         //      + restLogFilePathProp + " (beware, hampers performance)");
         String restLogFilePath = new File(restLogFilePathProp).toURI().toString(); // if local in dev, is in /server
         LoggingFeature loggingFeature = new LoggingFeature(restLogFilePath, restLogFilePath, 500000); // in, out, limit (else 50kb)
         loggingFeature.setPrettyLogging(true);
         bean.getFeatures().add(loggingFeature);
      }
      
       return bean.create();
   }
    
}