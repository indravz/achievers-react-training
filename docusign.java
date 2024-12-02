String configFileName = "sifConfig" + (env != null ? "$" + env : "") + ".yml";

///////////////////////////////////////////////////
package com.gs.gsas.accounts.docusign;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

import static com.gs.gsas.configs.Environment.*;

@Slf4j
@Service
public class DocusignAccessTokenService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Instant tokenExpirationTime;
    private DocusignAccessToken accessToken;
    private final CloseableHttpClient sifHttpClient;

    public DocusignAccessTokenService(@Qualifier("sifHttpClient") CloseableHttpClient sifHttpClient) {
        this.sifHttpClient = sifHttpClient;
    }

    private static RSAPrivateKey buildPrivateKey() {
        if (DOCUSIGN_PRIVATE_KEY.isBlank()) {
            throw new RuntimeException("DOCUSIGN_PRIVATE_KEY environment variable cannot be blank.");
        }
        String privateKeyContent = "-----BEGIN RSA PRIVATE KEY-----\n"
                + DOCUSIGN_PRIVATE_KEY.replace("", "\n")
                + "\n-----END RSA PRIVATE KEY-----\n";

        try (PEMParser pemParser = new PEMParser(new StringReader(privateKeyContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof org.bouncycastle.asn1.pkcs.PEMKeyPair pemKeyPair) {
                return (RSAPrivateKey) converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo) {
                return (RSAPrivateKey) converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + object.getClass());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error building private key", e);
        }
    }

    private String generateAssertion() {
        Algorithm algorithm = Algorithm.RSA256(null, buildPrivateKey());
        long now = System.currentTimeMillis();
        return JWT.create()
                .withIssuer(DOCUSIGN_CLIENT_ID)
                .withAudience(DOCUSIGN_OAUTH_BASE_PATH)
                .withIssuedAt(new Date(now))
                .withClaim("scope", "signature impersonation")
                .withSubject(DOCUSIGN_USER_ID)
                .withExpiresAt(new Date(now + 3600 * 1000))
                .sign(algorithm);
    }

    private DocusignAccessToken requestAccessToken() throws IOException, ParseException {
        HttpPost request = new HttpPost("https://" + DOCUSIGN_OAUTH_BASE_PATH + "/oauth/token");
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");

        String requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + generateAssertion();
        request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED));

        try (CloseableHttpResponse response = sifHttpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode >= 200 && statusCode < 300) {
                return objectMapper.readValue(responseBody, DocusignAccessToken.class);
            } else {
                log.error("Error obtaining access token. Status: {}, Response: {}", statusCode, responseBody);
                throw new IOException("Failed to obtain access token.");
            }
        }
    }

    public synchronized String getAccessToken() {
        Instant now = Instant.now().minusSeconds(10);
        if (accessToken == null || tokenExpirationTime.isBefore(now)) {
            try {
                accessToken = requestAccessToken();
                tokenExpirationTime = Instant.now().plusSeconds(accessToken.getExpiresIn());
                log.info("Obtained new Docusign access token.");
            } catch (IOException | ParseException e) {
                throw new RuntimeException("Failed to retrieve access token.", e);
            }
        }
        return accessToken.getAccessToken();
    }
}



////////////////////////////////////////////////////////////////////////////////####################!!!!!!!!!!!!!!!!!!!
@Bean
@Qualifier("sifSSLContext")
public SSLContext createSIFSSLContext() {
    try {
        // Load SIF configuration when available
        log.info("Loading SIF configuration from /sifConfig.yml");
        SIFConfig sifConfig = SIFConfigLoader.loadConfig("sifConfig.yml");
        log.info(sifConfig.toString());
        SSLContext sslContext = SIFCAClient.createSSLContext(sifConfig);
        log.info("event=SIF_certificate_loaded");
        return sslContext;
    } catch (Exception ex) {
        // Log the error and fallback to default SSL context
        log.warn("Failed to load SIF configuration. Falling back to default SSLContext. Error: " + ex.getMessage());
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to load default SSLContext", e);
        }
    }
}


///////////////////////////////////////////////////////////

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.EntityUtils;
import org.apache.http.impl.client.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class OUslgiservice {

    private static final Logger log = LoggerFactory.getLogger(OUslgiservice.class);
    private static final String DOCUSIGN_ENDPOINT = "https://demo.docusign.net/restapi/";
    private static final String DOCUSIGN_ACCOUNT_ID = "YOUR_ACCOUNT_ID"; // Replace with actual account ID
    private final DocusignAccessTokenService docusignAccessTokenService;
    private final CloseableHttpClient docusignHttpClient;
    private final ObjectMapper objectMapper;

    public OUslgiservice(DocusignAccessTokenService docusignAccessTokenService, ObjectMapper objectMapper) {
        this.docusignAccessTokenService = docusignAccessTokenService;
        this.docusignHttpClient = HttpClients.createDefault(); // Default CloseableHttpClient
        this.objectMapper = objectMapper;
    }

    public byte[] getDocument(String envelopeId, String documentId) {
        String url = DOCUSIGN_ENDPOINT + "v2.1/accounts/" + DOCUSIGN_ACCOUNT_ID + "/envelopes/" + envelopeId + "/documents/" + documentId;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + docusignAccessTokenService.getAccessToken());

        try (CloseableHttpResponse response = docusignHttpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toByteArray(response.getEntity());
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Unable to call Docusign getDocument API. code={}, responseBody={}", response.getStatusLine().getStatusCode(), responseBody);
                throw new RuntimeException("Unable to call Docusign getDocument API. code=" + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute request", e);
        }
    }
}



///////////////////////////////////////////////////////////////////////////////////


import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OUslgiservice {

    private static final Logger Log = LoggerFactory.getLogger(OUslgiservice.class);
    private static final String DOCUSIGN_ENDPOINT = "https://demo.docusign.net/restapi/";
    private static final String DOCUSIGN_ACCOUNT_ID = "YOUR_ACCOUNT_ID"; // Replace with actual account ID
    private final DocusignAccessTokenService docusignAccessTokenService;
    private final CloseableHttpClient sifHttpClient;
    private final ObjectMapper objectMapper;

    public OUslgiservice(DocusignAccessTokenService docusignAccessTokenService, CloseableHttpClient sifHttpClient, ObjectMapper objectMapper) {
        this.docusignAccessTokenService = docusignAccessTokenService;
        this.sifHttpClient = sifHttpClient;
        this.objectMapper = objectMapper;
    }

    public Envelope getEnvelope(String envelopeId) {
        String url = DOCUSIGN_ENDPOINT + "v2.1/accounts/" + DOCUSIGN_ACCOUNT_ID + "/envelopes/" + envelopeId + "?include=recipients";
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + docusignAccessTokenService.getAccessToken());

        Envelope envelope;
        try (CloseableHttpResponse response = sifHttpClient.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode >= 200 && statusCode < 300) {
                String responseBody = EntityUtils.toString(response.getEntity());
                envelope = objectMapper.readValue(responseBody, Envelope.class);
            } else {
                String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "null";
                Log.info("Unable to call Docusign getEnvelope API. code={}, responseBody={}", statusCode, responseBody);
                throw new RuntimeException("Unable to call Docusign getEnvelope API. code=" + statusCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute request", e);
        }

        return envelope;
    }
}

//////////////////////////////////////////////////////////////////////////
import okhttp3.*;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OUslgiservice {

    private static final Logger Log = LoggerFactory.getLogger(OUslgiservice.class);
    private static final String DOCUSIGN_ENDPOINT = "https://demo.docusign.net/restapi/";
    private static final String DOCUSIGN_ACCOUNT_ID = "YOUR_ACCOUNT_ID"; // Replace with actual account ID
    private final DocusignAccessTokenService docusignAccessTokenService;
    private final OkHttpClient docusignHttpClient;
    private final ObjectMapper objectMapper;

    public OUslgiservice(DocusignAccessTokenService docusignAccessTokenService, OkHttpClient docusignHttpClient, ObjectMapper objectMapper) {
        this.docusignAccessTokenService = docusignAccessTokenService;
        this.docusignHttpClient = docusignHttpClient;
        this.objectMapper = objectMapper;
    }

    public Envelope getEnvelope(String envelopeId) {
        String url = DOCUSIGN_ENDPOINT + "v2.1/accounts/" + DOCUSIGN_ACCOUNT_ID + "/envelopes/" + envelopeId + "?include=recipients";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + docusignAccessTokenService.getAccessToken())
                .build();

        Envelope envelope;
        try {
            Response response = docusignHttpClient.newCall(request).execute();
            ResponseBody body = response.body();

            if (response.isSuccessful()) {
                assert body != null;
                envelope = objectMapper.readValue(body.string(), Envelope.class);
            } else {
                Log.info("Unable to call Docusign getEnvelope API. code={}, responseBody={}", response.code(), body != null ? body.string() : "null");
                throw new RuntimeException("Unable to call Docusign getEnvelope API. code=" + response.code());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute request", e);
        }

        return envelope;
    }
}




/////////////////////////////////////////////////////////////////////////
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class DocusignService {
    private static final String DOCUSIGN_ENDPOINT = "https://demo.docusign.net/restapi/v2.1/accounts/";
    private static final String DOCUSIGN_ACCOUNT_ID = "YOUR_ACCOUNT_ID";  // replace with actual account ID

    private final ObjectMapper objectMapper;
    private final OkHttpClient docusignHttpClient;
    private final DocusignAccessTokenService docusignAccessTokenService;

    public DocusignService(
        ObjectMapper objectMapper,
        @Qualifier("docusignHttpClient") OkHttpClient docusignHttpClient,
        DocusignAccessTokenService docusignAccessTokenService
    ) {
        this.objectMapper = objectMapper;
        this.docusignHttpClient = docusignHttpClient;
        this.docusignAccessTokenService = docusignAccessTokenService;
    }

    public EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition) {
        try {
            String jsonBody = objectMapper.writeValueAsString(envelopeDefinition);
            RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            // Build the request
            Request request = new Request.Builder()
                .url(DOCUSIGN_ENDPOINT + DOCUSIGN_ACCOUNT_ID + "/envelopes")
                .post(requestBody)
                .header("Authorization", "Bearer " + docusignAccessTokenService.getAccessToken())
                .build();

            // Execute the request
            Response response = docusignHttpClient.newCall(request).execute();

            // Process the response
            return processResponse(response);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to create envelope", e);
        }
    }

    private EnvelopeSummary processResponse(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() && responseBody != null) {
            // Parse the envelope summary from the response
            EnvelopeSummary envelopeSummary = objectMapper.readValue(responseBody.byteStream(), EnvelopeSummary.class);
            log.info("Call to Docusign createEnvelope succeeded for envelopeId={}", envelopeSummary.getEnvelopeId());
            return envelopeSummary;
        } else {
            throw new RuntimeException("Call to Docusign createEnvelope API unsuccessful, code: " + response.code());
        }
    }
}


/////////////////////////////////////////////////////////////

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class DocusignService {
    private static final String DOCUSIGN_ENDPOINT = "https://demo.docusign.net/restapi/v2.1/accounts/";
    private static final String DOCUSIGN_ACCOUNT_ID = "YOUR_ACCOUNT_ID";  // replace with actual account ID

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient sifHttpClient;
    private final DocusignAccessTokenService docusignAccessTokenService;

    public DocusignService(
        ObjectMapper objectMapper,
        @Qualifier("sifHttpClient") CloseableHttpClient sifHttpClient,
        DocusignAccessTokenService docusignAccessTokenService
    ) {
        this.objectMapper = objectMapper;
        this.sifHttpClient = sifHttpClient;
        this.docusignAccessTokenService = docusignAccessTokenService;
    }

    public EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition) {
        try {
            // Convert envelope definition to JSON
            String jsonBody = objectMapper.writeValueAsString(envelopeDefinition);
            
            // Create the HTTP POST request
            HttpPost request = new HttpPost(DOCUSIGN_ENDPOINT + DOCUSIGN_ACCOUNT_ID + "/envelopes");
            request.setHeader("Authorization", "Bearer " + docusignAccessTokenService.getAccessToken());
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            // Execute the request
            try (CloseableHttpResponse response = sifHttpClient.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return objectMapper.readValue(responseBody, EnvelopeSummary.class);
                } else {
                    log.error("Failed to create envelope. Status: {}, Response: {}", statusCode, response.getEntity());
                    throw new IOException("Failed to create envelope with status: " + statusCode);
                }
            }
        } catch (Exception e) {
            log.error("Error creating envelope", e);
            throw new RuntimeException(e);
        }
    }
}
