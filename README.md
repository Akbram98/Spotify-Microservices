# Spotify-Microservices

## Description

 Developed a backend java API by leveraging Spring Boot to utilize two microservices for
managing user profiles and favorite playlists. To achieve this, two databases were used for
the backend: mongoDB and neo4j. The neo4j database was used to store user profile data and
interactions/relationships, while mongoDB was used to store a collection of songs. After setting up the
backend, the microservices were implemented to include a number of REST API endpoints
which were then used to enable communication between the two backend servers via CRUD operations. Maven
was used to compile and run these microservices, where it was then be possible to use these
services by utilizing http requests via the port number specified. For more info on specific features see [here](https://github.com/Akbram98/Spotify-Microservices/blob/main/project_phase1.pdf).

## Getting Started

  - [Download neo4j](https://neo4j.com/download/)
  - [Download MongoDB](https://www.mongodb.com/try/download/community-edition/releases)
  - [Download Postman](https://www.postman.com/downloads/)
  - [Spring Boot Guide](https://spring.io/guides/gs/spring-boot)
  - [Running an application with Spring Boot](https://docs.spring.io/spring-boot/maven-plugin/run.html)
  - [Basic guide for HTTP requests](https://apidog.com/blog/rest-api-endpoints/)

### Prerequisites
   - JAVA environment version: JAVA SE 1.8([How to change JAVA environment variable for Windows OS](https://www.codejava.net/java-core/how-to-set-java-home-environment-variable-on-windows-10))
   - Understanding of the MVC Patten for java development([Info on MVC Pattern](https://www.geeksforgeeks.org/mvc-design-pattern/))
## Project Demo
  If your interested in watching an over 10 minute video for this project demo, then here is the [link](https://drive.google.com/file/d/1-0xkSOoiiYA0XI4seIAxa_OfJNArmAik/view?usp=sharing).
## Learning Outcomes
 - Explored NoSQL (MongoDB) and Graph Database (Neo4j)
 - Practiced the implementation of background business logic to support REST API endpoints that provide access to read and modify data in a variety of databases
 - Practiced Software Architecture, in particular Server/Client model
 - Practiced using the Model-View-Controller(MVC) pattern for java development
 - Practiced using a build automation tool such as Maven

