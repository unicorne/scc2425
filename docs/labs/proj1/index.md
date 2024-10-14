# Azure TuKano
## Project Assignment #1 - Azure PaaS

* [Assignment Text / Enunciado](scc2425-proj1-tukano)

* [Base Source Code](https://github.com/smduarte/scc2425/tree/main/scc2425-tukano)  - Tukano Maven Project;

## Local WebApp Deployment

In the initial stages of development, it is likely more convenient to deploy your web app to a local instance
of Tomcat10, while using remote Azure Resources (BlobStorage, CosmosDB and Redis Cache).

### Launch local Tomcat10 using Docker

`docker run -ti --net=host smduarte/tomcat10`

**Note:** Make sure nothing is already using port 8080 in your host machine.

Once Tomcat10 is running, you can access the Tomcat10 manager app via:

[http://127.0.0.1:8080/manager/html](http://tomcat:s3cret@127.0.0.1:8080/manager/html)

If prompted for a login/password, use: `tomcat` and `s3cret`

### Automate deployment with Maven

1. Update `pom.xml` to include the `tomcat7-maven-plugin`:

```xml
<plugin>
   <groupId>org.apache.tomcat.maven</groupId>
   <artifactId>tomcat7-maven-plugin</artifactId>
   <version>2.1</version>
       <configuration>
           <url>http://maven:s3cret@127.0.0.1:8080/manager/text</url>
           <server>tomcat-docker</server>
           <update>true</update>
           <path>/lab1</path>
       </configuration>
</plugin>
```
**Note:** The login is `maven` `s3cret`and the path must start with `/`
The plugin was originally form Tomcat7 but also works with Tomcat10.

2. To deploy the web application, execute:

   `mvn clean compile package tomcat7:redeploy`

**Note:** Confirm that the .war is being generated in the `target` folder. Make sure
that you have in the pom.xml
```xml<packaging>war</packaging>
```

### Code Example 

The project in Lab1 [scc2425-lab1-code.zip](../lab1/scc2425-lab1-code.zip) was updated
to include the local deployment option. Check it out.

## FAQ

+ **Can I convert the base code to Kotlin?**
  
  Yes. But should you?

+ **Can I convert to Spring and such?**
  
  No. Sorry.

+ **Can I work alone?**
  
  Depends. Explain why, sending an email stating your case.

+ **What is the maximum size of the group?**
  
  Up to 2 Humans. Maximum group grade 40, split among the Human and non-Human (AI) members, according to merit. 
