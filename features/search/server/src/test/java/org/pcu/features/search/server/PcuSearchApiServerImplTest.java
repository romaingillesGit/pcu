package org.pcu.features.search.server;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pcu.features.search.server.meta.PcuField;
import org.pcu.features.search.server.meta.PcuIndex;
import org.pcu.features.search.server.meta.PcuIndexField;
import org.pcu.features.search.server.meta.PcuType;
import org.pcu.providers.file.api.PcuFileApi;
import org.pcu.providers.file.api.PcuFileResult;
import org.pcu.providers.search.api.PcuDocument;
import org.pcu.providers.search.api.PcuIndexResult;
import org.pcu.providers.search.api.PcuSearchApi;
import org.pcu.providers.search.elasticsearch.spi.ESSearchProviderConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * Test of PCU search API and features
 * WARNING requires ElasticSearch 5.5 to have been started independently.
 * 
 * TODO do this test with AND without the proxy client 
 * @author mardut
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={PcuSearchServerConfiguration.class,ESSearchProviderConfiguration.class},
   initializers = ConfigFileApplicationContextInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties="server.port=45665")
//rather than @SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="server_port=45665")
//which would require listening to an ApplicationEvent and therefore using a Provider pattern
//see https://docs.spring.io/spring-boot/docs/current/reference/html/howto-embedded-servlet-containers.html https://stackoverflow.com/questions/30312058/spring-boot-how-to-get-the-running-port
//or with autoconf redefine cxf.jaxrs.client.address
@ActiveProfiles("test")
public class PcuSearchApiServerImplTest /*extends PcuSearchApiClientTest */{
   @LocalServerPort
   protected int serverPort;
   
   @Autowired @Qualifier("pcuSearchApiRestClient") //@Qualifier("pcuSearchApiImpl")
   private PcuSearchApi searchApi;
   @Autowired @Qualifier("pcuFileApiRestClient")
   private PcuFileApi fileApi;

   @Autowired @Qualifier("pcuApiMapper")
   private ObjectMapper pcuApiMapper;
   @Autowired @Qualifier("pcuApiPrettyMapper")
   private ObjectMapper pcuApiPrettyMapper;
   
   @Test
   public void testIndex() throws Exception {
      String index = "files";
      PcuDocument pcuDoc = new PcuDocument();
      pcuDoc.setType("file");
      pcuDoc.setId("myid"); // TODO gen
      // testing very long id in HTTP URL :
      //pcuDoc.setId("mdutoo-laptop/home/mardut/dev/pcu/workspace/pcu/features/search/server/src/main/java/org/pcu/features/search/server/deeper/pcu/features/search/server/src/main/java/org/pcu/features/search/server/PcuSearchApiServerImpl.java");
      // NOO ES can't handle slash in HTTP url, must be encoded : No handler found for uri [/files/file/mydir/myfile.doc] and method [PUT] https://stackoverflow.com/questions/35025401/no-handler-found-for-uri-index-type-and-method-put
      pcuDoc.setId(URLEncoder.encode("mdutoo-laptop/home/mardut/dev/pcu/workspace/pcu/features/search/server/src/main/java/org/pcu/features/search/server/deeper/pcu/features/search/server/src/main/java/org/pcu/features/search/server/PcuSearchApiServerImpl.java", "UTF-8"));
      // TODO Q or only encode in ESSearchProviderImpl ??
      pcuDoc.setProperties(new LinkedHashMap<>());
      pcuDoc.getProperties().put("name", "a.doc");
      PcuIndexResult res = searchApi.index(index, pcuDoc);
      System.out.println("indexed\n" + pcuApiPrettyMapper.writeValueAsString(pcuDoc));
      System.out.println("found\n" + pcuApiPrettyMapper.writeValueAsString(searchApi.get(index, pcuDoc.getId())));
      // TODO check res
      
      // TODO more, from ES client API test
   }

   
   /**
    * The client connector's crawler should work this way :
    * @throws Exception
    */
   @Test
   public void testSimulateCrawl() throws Exception {
      // prepare file to crawl :
      String testContent = "My test content";
      File testFile = File.createTempFile("pcu_test_", ".doc");
      testFile.deleteOnExit();
      try (FileOutputStream testFileOut = new FileOutputStream(testFile)) {
         IOUtils.write(testContent, testFileOut, (Charset) null);
      }
      
      // init crawler :
      String store = "fileCrawlerStore"; // TODO manage
      String index = "files";
      //String fileType = "file";
      // server id : MAC address (else system info using OS-specific commands like dmesg or /proc & /sys) ; TODO Q or readable host ??
      String connectorComputerName = InetAddress.getLocalHost().getHostName(); // or IP by getCanonicalHostName(), or both ?
      String connectorComputerId = Base64.getEncoder().encodeToString(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress());
      Collections.list(NetworkInterface.getNetworkInterfaces()).stream().forEach(itf -> System.out.println(itf.toString()));
      for (NetworkInterface itf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
         try {
            System.out.println(itf + " " + itf.isVirtual() + " " + itf.isLoopback() + " " + itf.getHardwareAddress() + " " + itf.getMTU());
         } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      };
      
      // init crawl :
      String host = /*rootUrl != null ? rootUrl.getHost() : */connectorComputerName;
      
      // 1. upload crawled content :
      PcuFileResult fileRes;
      try (FileInputStream testFileIn = new FileInputStream(testFile)) {
         fileRes = fileApi.storeContent(store,testFileIn);
      }
      // and check :
      InputStream testFileInRes = fileApi.getContent(store, fileRes.getPath());
      assertEquals(testContent, IOUtils.toString(testFileInRes, (Charset) null));
      
      // 2. index crawled metadata :
      
      PcuDocument pcuDoc = new PcuDocument();
      pcuDoc.setProperties(new LinkedHashMap<>());
      pcuDoc.setType("file");
      
      // TODO Q date as long timestamp (pcu) or formatted (fscrawler) ?
      
      // id :
      pcuDoc.setId(new UUID().toString()); // id - best lucene id http://blog.mikemccandless.com/2014/05/choosing-fast-unique-identifier-uuid.html
      // or use it to dedup identity resolution ? then with configurable policy : using or not connectorComputerId, url protocol / host / port, even scan root dir
      //String id = "file://server1/a/b/file.doc"; // NOO ES doesn't support slash, and not a good Lucene id anyway
      String globalFilePath = connectorComputerId + "/" + /*url != null ? host/protocol/ : file/*/ testFile.getAbsolutePath();
      pcuDoc.setId(globalFilePath); // id - (connector) app's - (url or) server + path (fscrawler : local path only), allows dedup / identity resolution NOO no slash in ES id
      pcuDoc.setId(md5(globalFilePath)); // id - (connector) app's - (url or) server + path MD5 (fscrawler : local path only), allows dedup / identity resolution
      //pcuDoc.setId(ecmDoc.getId()); // id - (ECM) app's, allows dedup / identity resolution
      
      // version & ordering :
      //pcuDoc.getProperties().put("version", esApi.getDocument("file", pcuDoc.getId())).getVersion(); // version if optimistic locking (not if pipeline)
      pcuDoc.getProperties().put(/*local_*/"version", nextLocalOrder()); // local ordering as version (else local_version)
      pcuDoc.getProperties().put("global_version", nextLamport(/*impactingExternalProcessLamports*/)); // global ordering (lamport timestamp) as version (else global_version)
      
      pcuDoc.getProperties().put("store_path", store + "/" + fileRes.getPath()); // or 2 props ? or in content.store_path ? or store_id, id_in_store ?
      
      // crawl :
      pcuDoc.getProperties().put("synced", LocalDateTime.now()); // fscrawler : file.indexing_date
      // TODO Q scan (connectorComputerId (binary), host, root path, start/end date, crawl job id, crawl conf...) ?
      // or as a mixin only on root dir ?
      
      // file :
      LinkedHashMap<String, Object> file = new LinkedHashMap<>();
      pcuDoc.getProperties().put("file", file); // TODO of type "file"
      file.put("length", testFile.length()); // fscrawler : filesize
      file.put("last_modified", testFile.lastModified()); // TODO Q or globally ?! or annotated as global prop @modified ?
      file.put("name", testFile.getName());
      file.put("path", testFile.getAbsolutePath()); // TODO analyze as a tree structure ?
      // TODO Q also file url, "virtual" path, extension, indexed_chars ? content_type (or in content) ??
      // TODO group, owner (fscrawler : in attributes/)
      
      // http : (gotten from HTTP request if any)
      LinkedHashMap<String, Object> http = new LinkedHashMap<>();
      pcuDoc.getProperties().put("http", http); // TODO of type "http"
      http.put("url", "http://myserver/myfile.doc"); // if any ; else file:/// ??
      http.put("mimetype", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); // fscrawler : rather in file ?!?
      // TODO Q also other headers : size, expire... ??
      
      // meta : (or tika, pdfbox... ?)
      LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
      pcuDoc.getProperties().put("meta", meta); // TODO of type "meta"
      meta.put("author", "Jane Dee");
      meta.put("title", "CV of John Doe");
      meta.put("date", "20161001T15:29:45.042");
      meta.put("keywords", Arrays.asList(new Object[] { "cv" }));
      meta.put("language", "en"); // TODO Q meta or detected ??
      // TODO format, identifier, contributor, coverage, modifier, creator_tool, publisher, relation, rights, source, type,
      // description, created, print_date, metadata_date, latitude, longitude, altitude, rating, comments ?!
      
      // content :
      LinkedHashMap<String, Object> content = new LinkedHashMap<>();
      pcuDoc.getProperties().put("content", content); // TODO of type "content"
      content.put("hash", fileRes.getPath()); // why not ; or rather in store/, file/ ? or md5, digest (nuxeo), checksum (fscrawler) ?
      content.put("fulltext", testContent); // parsed client-side by tika in connector crawler
      // TODO Q also detected language ??
      // OPT properties : alternatively parsed JSON (& XML) as nested complex objects ?
      
      // file treeS in ES :
      pcuDoc.getProperties().put("path", globalFilePath); // most exact tree
      pcuDoc.getProperties().put("readable_path", host + testFile.getAbsolutePath()); // readable tree
      
      PcuIndexResult indexRes = searchApi.index(index, pcuDoc);
      
      // find : (TODO by path...)
      PcuDocument res = searchApi.get(index, pcuDoc.getId());
      assertEquals(pcuDoc.getId(), res.getId());
      //assertEquals(pcuDoc.getVersion(), res.getVersion());
      // get content :
      String[] storePath = ((String) pcuDoc.getProperties().get("store_path")).split("/", 2);
      InputStream foundContent = fileApi.getContent(storePath[0], storePath[1]);
      assertEquals(testContent, IOUtils.toString(foundContent, (Charset) null));
      
      // TODO test versionS & optimistic locking
      
      // X. simulate tailing a file :
      String testAppendContent = "\nand another test content";
      // create = first append (no random access)
      try (FileInputStream testFileIn = new FileInputStream(testFile)) {
         fileRes = fileApi.appendContent(store, globalFilePath, null, testFileIn);
      }
      assertEquals(testContent, IOUtils.toString(fileApi.getContent(store, fileRes.getPath()), (Charset) null));
      // and index meta :
      content.put("path", fileRes.getPath());
      indexRes = searchApi.index(index, pcuDoc);
      // then, actual append (using random access) :
      fileRes = fileApi.appendContent(store, fileRes.getPath(), testFile.length(), new ByteArrayInputStream(testAppendContent.getBytes()));
      assertEquals(testContent + testAppendContent, IOUtils.toString(fileApi.getContent(store, fileRes.getPath()), (Charset) null));
      // NB. don't index metas anymore, or ONLY WHAT DOESN'T CHANGE, besides that at most length or modified MIGHT be updated using (even batch ?) event system
      
      // Y. simulate indexing an app's business entities :
      // TODO
   /*}

   @Test
   public void testSimulateDocumentMetamodelCheck() throws Exception {*/
      // build meta :
      Schema schemas = new Schema.Parser().parse(resourceLoader // TODO getResources("classpath*:"avro/*.avsc")
            .getResource("classpath:" + "file.avsc").getInputStream()); // TODO close()
      assertEquals(1, schemas.getTypes().size());
      Schema schema = schemas.getTypes().get(0);
      
      // convert instance to JSON : (TODO skip this step...)
      String pcuDocumentJson = pcuApiMapper.writeValueAsString(pcuDoc);
      // validate instance :
      //GenericRecord testFileAvroRec = new GenericData.Record(fileSchema);
      DatumReader<GenericRecord> genericDatumReader = new GenericDatumReader<GenericRecord>(schema);
      System.out.println("validating\n" + pcuApiPrettyMapper.writeValueAsString(pcuDoc));
      Decoder decoder = DecoderFactory.get().jsonDecoder(schema, new ByteArrayInputStream(pcuDocumentJson.getBytes()));
      GenericRecord pcuDocRec = genericDatumReader.read(null, decoder); // AvroTypeException
      assertEquals(pcuDocRec.get("name"), pcuDoc.getProperties().get("name"));
      
      /////////////////////////////////////
      
      //build meta :
      PcuType fileType = new PcuType("file");
      PcuField nameField = new PcuField("name", "string");
      fileType.addField(nameField);
      pcuTypes.put(fileType.getName(), fileType);
      PcuIndex fileIndex = new PcuIndex("files");
      PcuIndexField nameIndexField = new PcuIndexField(nameField);
      fileIndex.addField(nameIndexField);
      indexConfs.put(fileIndex.getName(), fileIndex);
      // check meta :
      checkMetamodel(fileIndex);

      // check that type in index :
      PcuType type = getPcuType(fileType.getName());
      PcuIndex indexConf = getPcuIndex(fileType.getName());
      if (!indexConf.getTypes().contains(type.getName())) {
         throw new RuntimeException("Can't find type " + type.getName() + " in index " + indexConf.getName());
      }

      // check pcuDoc against type :
      for (Entry<String, Object> entry : pcuDoc.getProperties().entrySet()) {
         String name = entry.getKey();
         PcuField pcuField = type.getFields().get(name);
         if (pcuField == null) {
            throw new RuntimeException("Can't find field " + name + " in type " + type.getName());
         }
         Object value = entry.getValue();
         switch(pcuField.getType()) {
         case "int":
            if (!(value instanceof Integer)) {
               throw new RuntimeException("Value for field " + name + " in type " + type.getName() + " should be of type " + pcuField.getType() + " but is " + value);
            }
            // TODO try parsing from string ?
         }
      }
      // TODO also check missing required fields
   }

   private String md5(String s) {
      try {
         return new BigInteger(1, MessageDigest.getInstance("MD5").digest(s.getBytes())).toString(16);
      } catch (NoSuchAlgorithmException nsaex) {
         throw new RuntimeException("Can't compute md5, error initing hash / digest", nsaex); // TODO
      }
   }

   @Autowired
   private ResourceLoader resourceLoader; // TODO PathMatchingResourcePatternResolver
   

   private HashMap<String,PcuIndex> indexConfs = new HashMap<String,PcuIndex>();
   private HashMap<String,PcuType> pcuTypes = new HashMap<String,PcuType>();
   public PcuType getPcuType(String type) {
      PcuType pcuType = pcuTypes.get(type);
      if (pcuType == null) {
         throw new RuntimeException("Can't find type " + type);
      }
      return pcuType;
   }
   public PcuIndex getPcuIndex(String name) {
      PcuIndex index = indexConfs.get(name);
      if (index == null) {
         throw new RuntimeException("Can't find index " + name);
      }
      return index;
   }
   public PcuIndex getPcuIndex(PcuIndexField indexField) {
      PcuIndex index = indexConfs.get(indexField.getIndexType());
      if (index == null) {
         throw new RuntimeException("Can't find index " + indexField.getIndexType() + " of index field " + indexField.getName());
      }
      return index;
   }
   public PcuField getPcuField(PcuIndexField indexField) {
      PcuIndex index = getPcuIndex(indexField);
      for (String type : index.getTypes()) {
         PcuType pcuType = getPcuType(type);
         PcuField pcuField = pcuType.getField(indexField.getName());
         if (pcuField != null) {
            return pcuField;
         }
      }
      return null;
   }
   public List<PcuField> getDuplicatePcuFields(PcuIndex index, PcuIndexField indexField) {
      List<PcuField> duplicatePcuFields = new ArrayList<PcuField>(3);
      for (String type : index.getTypes()) {
         PcuType pcuType = pcuTypes.get(type);
         if (pcuType == null) {
            throw new RuntimeException("Can't find type " + type + " of index " + index.getName());
         }
         PcuField pcuField = pcuType.getField(indexField.getName());
         if (pcuField != null) {
            duplicatePcuFields.add(pcuField);
         }
      }
      return duplicatePcuFields;
   }
   private void checkMetamodel(PcuIndex index) {
      for (PcuIndexField indexField : index.getFields().values()) {
         List<PcuField> duplicatePcuFields = getDuplicatePcuFields(index, indexField);
         if (duplicatePcuFields.isEmpty()) {
            throw new RuntimeException("Can't find PCU field for " + indexField.getName() + " of index " + index.getName()
               + " among PCU types " + index.getTypes());
         }
         if (duplicatePcuFields.size() > 1) {
            throw new RuntimeException("More than one PCU field for " + indexField.getName() + " of index " + index.getName()
               + " among PCU types " + index.getTypes() + " : exists in PCU types "
                  + duplicatePcuFields.stream().map(f -> f.getPcuType()).collect(Collectors.toList()));
         }
      }
   }

   private long currentLocalOrder = 0; // or Math.BigInteger ?
   private long currentTime = 0; // or Math.BigInteger ? TODO rather load from fs ??
   /** version / order based on crawl local time
    * TODO threaded (to avoid synchronized)
    * TODO or readable using date string ?? */
   private long nextLocalOrder() {
      long time = System.nanoTime();
      if (time == currentTime) {
         currentLocalOrder = currentLocalOrder + 1;
      } else {
         currentTime = time;
         currentLocalOrder = currentTime * 5000;
      }
      return currentLocalOrder;
   }
   private long currentLamport = 0; // or Math.BigInteger ? TODO rather load from fs ??
   /** version / order based on "modified" meta ???
    * TODO threaded (to avoid synchronized) */
   private Object nextLamport(long ... lamports) {
      // TODO ? if (lamports.length == 0) {
      for (long lamport : lamports) {
         if  (lamport > currentLamport) {
            currentLamport = lamport;
         }
      }
      currentLamport++;
      return currentLamport;
   }
   
}
