package io.pivotal.reactiveclient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;


import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class ReactiveclientApplication implements CommandLineRunner {

    @Value("${server.ssl.trust-store}")
    String trustStorePath;
    @Value("${server.ssl.trust-store-password}")
    String trustStorePass;
    @Value("${server.ssl.alias}")
    String keyAlias;
    @Value("${server.ssl.key-store}")
    String keyStorePath;
    @Value("${server.ssl.key-store-password}")
    String keyStorePass;

    private WebClient createWebClientWithServerURLAndDefaultValues() {

        SslContext sslContext;

        try {
            // Load the trusted CA certificate
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(new FileInputStream(ResourceUtils.getFile(trustStorePath)), trustStorePass.toCharArray());

            List<Certificate> certificateCollection;
            certificateCollection = Collections.list(trustStore.aliases()).stream().filter(t -> {
                try {
                    return trustStore.isCertificateEntry(t);
                } catch (KeyStoreException e1) {
                    throw new RuntimeException("Error reading truststore", e1);
                }
            }).map(t -> {
                try {
                    return trustStore.getCertificate(t);
                } catch (KeyStoreException e2) {
                    throw new RuntimeException("Error reading truststore", e2);
                }
            }).collect(Collectors.toList());

            // Load the client key
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStorePath)), keyStorePass.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(trustStore, trustStorePass.toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePass.toCharArray());

            sslContext = SslContextBuilder.forClient()
                    //.keyManager(privateKey)
                    .trustManager((X509Certificate[]) certificateCollection.toArray(new X509Certificate[certificateCollection.size()]))
                    .protocols("TLSv1.2")
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
            return WebClient.builder()
                    .baseUrl("https://localhost:8443")
                    .clientConnector(connector)
                    .build();

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveclientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        WebClient webClient = createWebClientWithServerURLAndDefaultValues();

        Mono<ClientResponse> result = webClient.get()
                .uri("/restexamples/hello")
                .accept(MediaType.TEXT_PLAIN)
                .exchange();

        String response = result.block().toString();
        System.out.println(response);

        System.exit(0);
    }
}
