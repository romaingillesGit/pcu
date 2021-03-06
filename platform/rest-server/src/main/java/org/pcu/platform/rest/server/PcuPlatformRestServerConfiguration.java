package org.pcu.platform.rest.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.pcu.features.search.client.PcuPlatformRestClientConfiguration;
import org.pcu.platform.rest.server.swagger.PcuApiSwagger2Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * In package BELOW others else (ex. in .web) doesn't scan them.
 * 
 * Jackson XML provider is configured and set up by client module's configuration.
 * 
 * IMPORTANT : The final application-defaults.properties must contain the appropriate defaults for HTTP & CXF:
 * http.server.servlet-path (& default port), cxf.jaxrs.client.address (& default path), else with executable jar :
 * Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'cxf.jaxrs.client.address' in string value "${cxf.jaxrs.client.address}"
 * see application-defaults.template.properties (Spring Boot auto includes it, no need
 * for @ConfigurationProperties @PropertySource("classpath:application-defaults.properties")
 * 
 * @author mdutoo
 */
@Configuration
@ComponentScan(basePackageClasses={PcuPlatformRestServerConfiguration.class, PcuPlatformRestClientConfiguration.class})
@EnableAutoConfiguration // else Unable to start EmbeddedWebApplicationContext due to missing EmbeddedServletContainerFactory bean see https://stackoverflow.com/questions/21783391/spring-boot-unable-to-start-embeddedwebapplicationcontext-due-to-missing-embedd
public class PcuPlatformRestServerConfiguration {
   
    private static final Logger LOGGER = LoggerFactory.getLogger(PcuPlatformRestServerConfiguration.class);
   
    /** for prop check purpose */
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void postContruct () {
        /*if (!"/system".equals(env.getProperty("server.servlet-path"))) {
           throw new RuntimeException("missing server.servlet-path=/system "
                 + "in pcu-server(-defaults).properties");
        }*/ // TODO only check in prod
    }

    /** either this or @EnableAutoConfiguration else Unable to start EmbeddedWebApplicationContext due to missing EmbeddedServletContainerFactory bean
     * BUT KILLS RANDOM PORT */
    /*@Bean
    @ConditionalOnMissingBean(name = "servletContainer")
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        return factory;
     }*/

    /**
     * Registers CXF servlet. Otherwise, service is created but not available through HTTP.
     * Hardcoded, so removes need for props : cxf.path, cxf.servlet.init.service-list-path.
     * Inspired by CxfAutoConfiguration.java's code . */
    @Bean
    @ConditionalOnMissingBean(name = "cxfServletRegistration")
    public ServletRegistrationBean cxfServletRegistration() {
        String path = env.getProperty("cxf.path", "/pcu"); //  else default would be /services/pcu (NB. not / else blocks UI servlet)
        String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
        ServletRegistrationBean registration = new ServletRegistrationBean(
                new CXFServlet(), urlMapping); // cxf.path
        /*CxfProperties.Servlet servletProperties = this.properties.getServlet();
        registration.setLoadOnStartup(1);
        for (Map.Entry<String, String> entry : servletProperties.getInit().entrySet()) {
            registration.addInitParameter(entry.getKey(), entry.getValue());
        }*/
        registration.addInitParameter("service-list-path", "/info"); // cxf.servlet.init.service-list-path
        return registration;
    }

    /** Creates CXF bus */
    @Configuration
    @ConditionalOnMissingBean(SpringBus.class)
    @ImportResource("classpath:META-INF/cxf/cxf.xml")
    protected static class SpringBusConfiguration {

    }

    /** NB. reusing client-defined bus (but xxxApiImpl is happily not API client) */ 
    @Bean
    public Server pcuJaxrsServer(SpringBus bus, JacksonJsonProvider pcuSearchApiJsonProvider,
          List<PcuJaxrsServerBase> pcuJaxrsServers,/* PcuApiExceptionMapper pcuApiExceptionMapper,*/
          PcuApiSwagger2Feature pcuApiSwagger2Feature) {
       //pcuSearchApiSimpleImpls = pcuSearchApiSimpleImpls.stream()
       //      .filter(apiImpl -> !(apiImpl instanceof Proxy)).collect(Collectors.toList()); // else WARN  | OperationResourceInfoComparator:126 - Both org.pcu.features.search.simple.PcuSearchApiSimpleImpl#index and com.sun.proxy.$Proxy102#index are equal candidates for handling the current request which can lead to unpredictable results
        ArrayList<Object> providers = new ArrayList<Object>();
        providers.add(pcuSearchApiJsonProvider); // else web app ex Response.Status.UNSUPPORTED_MEDIA_TYPE
        ///providers.add(pcuApiExceptionMapper);
        
        JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setAddress("/");
        endpoint.setServiceBeanObjects(pcuJaxrsServers);
        endpoint.setProviders(providers);

        endpoint.getFeatures().add(pcuApiSwagger2Feature);
        
        String restLogFilePathProp = env.getProperty("pcu.search.es.restLogFile"); // es-rest-mock.log
        if (restLogFilePathProp != null && !restLogFilePathProp.trim().isEmpty()) { // ex. not in prod
           //LOGGER.warn("Enabling logging of all REST exchanges including body to "
           //      + restLogFilePathProp + " (beware, hampers performance)");
           String restLogFilePath = new File(restLogFilePathProp).toURI().toString(); // if local in dev, is in /server
           LoggingFeature loggingFeature = new LoggingFeature(restLogFilePath, restLogFilePath, 500000); // in, out, limit (else 50kb)
           loggingFeature.setPrettyLogging(true);
           endpoint.getFeatures().add(loggingFeature);
        }
        
        return endpoint.create();
    }
    
}