package com.mcplab.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * SPRING MVC vs SPRING BOOT — Startup Comparison
 * ================================================
 *
 * Spring MVC (your EFFORT-Web setup):
 *   web.xml
 *     └── declares DispatcherServlet
 *           └── points to servlet-context.xml
 *                 └── component-scan, view resolvers, interceptors …
 *     └── declares ContextLoaderListener
 *           └── points to root-context.xml
 *                 └── DataSource, JMS, Security, Scheduling beans …
 *   → You managed Tomcat separately (webapps/ deploy, server.xml, catalina.sh)
 *
 * Spring Boot (this project):
 *   This single class does ALL of the above.
 *   - No web.xml
 *   - No servlet-context.xml / root-context.xml
 *   - No external Tomcat install
 *   SpringApplication.run() bootstraps everything from application.properties
 *   and the annotations on this class.
 */

/*
 * @SpringBootApplication is a convenience meta-annotation that combines three annotations:
 *
 *   1. @Configuration
 *      Marks this class as a source of @Bean definitions (same role as your XML config files).
 *      You can add @Bean methods directly here, though dedicated @Configuration classes
 *      (in com.mcplab.config) are preferred for larger projects.
 *
 *   2. @EnableAutoConfiguration
 *      Tells Spring Boot to inspect the classpath and application.properties, then
 *      automatically wire sensible default beans — DataSource, JdbcTemplate, HikariCP,
 *      DispatcherServlet, Jackson, etc.
 *      In EFFORT-Web you declared every one of those beans manually in XML.
 *      Auto-configuration replaces that entirely.
 *
 *   3. @ComponentScan
 *      Scans the package of this class (com.mcplab.server) and all sub-packages for
 *      @Component, @Service, @Repository, @Controller, @RestController, and @Configuration.
 *      Equivalent to:
 *          <context:component-scan base-package="com.mcplab"/> in root-context.xml
 *      Because this class lives in com.mcplab.server, Spring scans com.mcplab.* —
 *      which covers com.mcplab.tools and com.mcplab.config automatically.
 */
@SpringBootApplication
public class AiMcpLabApplication {

    /*
     * LEARNING NOTE — Embedded vs External Tomcat
     * ============================================
     * In EFFORT-Web / Spring MVC:
     *   1. You built a WAR:          mvn package  → target/effort.war
     *   2. Copied it to Tomcat:      cp effort.war $TOMCAT_HOME/webapps/
     *   3. Started Tomcat manually:  $TOMCAT_HOME/bin/startup.sh
     *   Tomcat was a separate process you installed and managed on the server.
     *
     * In Spring Boot:
     *   Tomcat is a dependency inside the JAR (spring-boot-starter-web pulls it in).
     *   SpringApplication.run() below:
     *     a) Creates the Spring ApplicationContext (replaces ContextLoaderListener)
     *     b) Registers DispatcherServlet programmatically (replaces web.xml <servlet>)
     *     c) Starts embedded Tomcat on server.port (default 8080)
     *     d) Deploys the DispatcherServlet into that Tomcat instance
     *   Everything in one process, one command:
     *       mvn spring-boot:run
     *       — or —
     *       java -jar target/ai-mcp-lab-0.0.1-SNAPSHOT.jar
     */
    public static void main(String[] args) {
        SpringApplication.run(AiMcpLabApplication.class, args);
    }
}
