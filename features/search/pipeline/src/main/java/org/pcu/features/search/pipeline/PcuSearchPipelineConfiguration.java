package org.pcu.features.search.pipeline;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxrs.client.Client;
import org.pcu.features.search.client.PcuPlatformRestClientConfiguration;
import org.pcu.features.search.client.PcuSearchApiClientConfiguration;
import org.pcu.platform.rest.server.PcuPlatformRestServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * In package BELOW others else (ex. in .web) doesn't scan them.
 * 
 * @author mdutoo
 */
@Configuration
@ComponentScan(basePackageClasses={PcuSearchPipelineConfiguration.class, PcuSearchApiClientConfiguration.class,
      PcuPlatformRestServerConfiguration.class})
public class PcuSearchPipelineConfiguration {
   
   @Autowired
   private PcuPlatformRestClientConfiguration clientConf;

   /** TODO test only */
   @Bean
   public Client pcuSearchIndexPipelineApiRestClient(SpringBus bus,
         JacksonJsonProvider pcuApiJsonProvider/*,
         ESApiExceptionMapper exceptionMapper,
         ESApiResponseExceptionMapper responseExceptionMapper*/) {
      return clientConf.pcuApiRestClientForClass(PcuSearchIndexPipelineApi.class, bus, pcuApiJsonProvider);
   }
   
}