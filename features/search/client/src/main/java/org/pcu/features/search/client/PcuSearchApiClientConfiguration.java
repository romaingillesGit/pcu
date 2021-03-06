package org.pcu.features.search.client;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxrs.client.Client;
import org.pcu.providers.file.api.PcuFileApi;
import org.pcu.providers.search.api.PcuSearchApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * In package BELOW others else (ex. in .web) doesn't scan them
 * @author mdutoo
 */
@Configuration
@ComponentScan(basePackageClasses={PcuSearchApiClientConfiguration.class, PcuPlatformRestClientConfiguration.class}) // i.e. ("org.pcu.features.search.client") ; not org.pcu else scans ex. ESSearchProviderApiImpl
// which can't find client, and in another project does not work (order ?)
public class PcuSearchApiClientConfiguration {

   @Autowired
   private PcuPlatformRestClientConfiguration clientConf;
   
   @Bean
   public Client pcuSearchApiRestClient(SpringBus bus,
         JacksonJsonProvider pcuApiJsonProvider/*,
         ESApiExceptionMapper exceptionMapper,
         ESApiResponseExceptionMapper responseExceptionMapper*/) {
      return clientConf.pcuApiRestClientForClass(PcuSearchApi.class, bus, pcuApiJsonProvider);
   }
   @Bean
   public Client pcuFileApiRestClient(SpringBus bus,
         JacksonJsonProvider pcuApiJsonProvider/*,
         ESApiExceptionMapper exceptionMapper,
         ESApiResponseExceptionMapper responseExceptionMapper*/) {
      return clientConf.pcuApiRestClientForClass(PcuFileApi.class, bus, pcuApiJsonProvider);
   }
    
}