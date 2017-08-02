=====================================================

PCU Consortium - PCU Platform

https://pcu-consortium.github.io

https://github.com/pcu-consortium/pcu

Copyright (c) 2017 The PCU Consortium

=====================================================


About PCU Platform
----------

The PCU Consortium aims at building an Open Source, unified Machine Learning (ML) platform targeted at business applications such as ecommerce,
that makes search a first-class citizen of Big Data.
It is bundled with a default application : the PCU Entreprise Search engine.


Features
   * ElasticSearch client API. It is built on built on Apache CXF, Jackson, Spring Boot. See https://github.com/pcu-consortium/pcu/blob/master/providers/search/elasticsearch & its README.
   * upcoming : search provider SPI, other providers (Spark compute, storage, Kafka messaging),
text mining / NLP, core Manager, search and ecommerce ML algorithms...

Tools
   * upcoming : Swagger online API developer documentation and further playground

Team
   * Specifications : the PCU Consortium
   * REST foundations & ElasticSearch API : Marc Dutoo

License : Apache License 2.0

Requirements : [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), [Maven 3](http://maven.apache.org/download.cgi)

Roadmap : TODO


# Quickstart

## Clone sources
git clone TODO

## Build
mvn clean install


# PCU project file tree :

pcu/

 (big_data_components/)

 (storage/ (stor ?))
 search/ (cpt-search ? cpt-st-search ?)
  spi (jaxrs, jackson, swagger), client (cxf, spring-boot) with test mock server
  elasticsearch/
   api (jaxrs, jackson, swagger), client (cxf, spring-boot), spi-impl with integration test
  solr/...
 file/
 (columnarfile/, columnardb/, documentdb/...)

 compute/ (comp ? cpt-comp ?)
  spi, client
  spark/
   api, client, spi-impl

 messaging/
  kafka/

 (platform/)

 core/ (or separately : metadata & configuration, its api developer & configuration backoffice, deployment or in other submodules...)

 pipeline/

 connector/ (& extractor...)

 (features/)
 
 search/ (feature-search ?)

 rec/ (recommendation ? feature-recommendation ?)

 (use cases/)

 entreprisesearch/ (app-search ? with simple built-in front-end)

 ecommerce/ (with Magento-specific extensions being in another project)

 B2B/, digitalinstore/ ...

