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
