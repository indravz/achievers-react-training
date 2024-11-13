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
