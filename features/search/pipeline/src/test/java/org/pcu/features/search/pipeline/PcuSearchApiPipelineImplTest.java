package org.pcu.features.search.pipeline;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pcu.providers.search.api.PcuDocument;
import org.pcu.providers.search.api.PcuIndexResult;
import org.pcu.providers.search.api.PcuSearchApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;


/**
 * Test of PCU search API and features
 * WARNING requires ElasticSearch 5.5 to have been started independently.
 * 
 * TODO do this test with AND without the proxy client 
 * @author mardut
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={PcuSearchPipelineConfiguration.class},
   initializers = ConfigFileApplicationContextInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties="server.port=45665")
//rather than @SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="server_port=45665")
//which would require listening to an ApplicationEvent and therefore using a Provider pattern
//see https://docs.spring.io/spring-boot/docs/current/reference/html/howto-embedded-servlet-containers.html https://stackoverflow.com/questions/30312058/spring-boot-how-to-get-the-running-port
//or with autoconf redefine cxf.jaxrs.client.address
@ActiveProfiles("test")
public class PcuSearchApiPipelineImplTest /*extends PcuSearchApiClientTest */{
   @LocalServerPort
   protected int serverPort;
   
   @Autowired @Qualifier("pcuSearchIndexPipelineApiRestClient") //@Qualifier("pcuSearchApiRestClient") //@Qualifier("pcuSearchApiPipelineImpl")
   private PcuSearchIndexPipelineApi searchApi;
   
   @Test
   public void test() {
      String index = "files";
      PcuDocument pcuDoc = new PcuDocument();
      pcuDoc.setType("file");
      pcuDoc.setId("myid"); // TODO gen
      pcuDoc.setProperties(new LinkedHashMap<>());
      pcuDoc.getProperties().put("name", "a.doc");
      PcuIndexResult res = searchApi.index(index, pcuDoc);
      assertNotNull(res);
      // TODO check res
      
      // TODO more, from ES client API test
   }
   
}
