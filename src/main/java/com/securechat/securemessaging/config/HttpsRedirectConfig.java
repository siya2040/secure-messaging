package com.securechat.securemessaging.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * In production with direct SSL (no Nginx): adds a plain HTTP connector on port 8080
 * that redirects all traffic to HTTPS on port 8443.
 *
 * Not active in dev (HTTP only) or prod (Nginx handles redirect).
 * Activate with profile "ssl-direct" if running Spring Boot SSL without Nginx.
 */
@Configuration
@Profile("ssl-direct")
public class HttpsRedirectConfig {

    @Value("${server.port:8443}")
    private int httpsPort;

    private static final int HTTP_PORT = 8080;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirect() {
        return factory -> {
            Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            httpConnector.setScheme("http");
            httpConnector.setPort(HTTP_PORT);
            httpConnector.setSecure(false);
            httpConnector.setRedirectPort(httpsPort);
            factory.addAdditionalTomcatConnectors(httpConnector);
        };
    }
}
