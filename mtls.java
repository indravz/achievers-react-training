



authenticationResponse.getHeaders().add("X-Kong-Authorization", "Bearer " + tokenResponse.getAccessToken());











/////////////////////////////////////////////////////////////////////////////////////

base64 truststore.p12 > truststore.p12.base64


keytool -import -trustcacerts -file ca-cert.pem -alias ca-cert -keystore truststore.p12 -storepass your-password

  keytool -import -trustcacerts -file another-client-cert.pem -alias another-client -keystore truststore.p12 -storepass your-password




FROM registry.aws.site.gs.com:443/dx/javaeng/java-maven/jdk21-ubi-rhel8:3.9.5-current

WORKDIR /app

COPY target/panther-service-0.0.1-SNAPSHOT.jar /app/panther-service-0.0.1-SNAPSHOT.jar
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]


  ///////////////////////////////////////////////////////

#!/bin/bash
echo "Fetching Truststore from Secrets Manager..."

# Fetch the truststore from Secrets Manager
TRUSTSTORE_BASE64=$(echo "$TRUSTSTORE_BASE64" | base64 -d)

# Write the truststore to the correct location
echo "$TRUSTSTORE_BASE64" > /path/to/truststore.p12

# Run the Spring Boot application with the truststore
exec java -Djavax.net.ssl.trustStore=/path/to/truststore.p12 \
          -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWORD" \
          -jar /app/panther-service-0.0.1-SNAPSHOT.jar
  
//////////////////////////////////////////////////


  ///////////////////////
  server.ssl.key-store=classpath:server.p12
server.ssl.key-store-password=your-server-key-password
server.ssl.key-store-type=PKCS12
server.ssl.trust-store=/path/to/truststore.p12
server.ssl.trust-store-password=your-truststore-password
server.ssl.trust-store-type=PKCS12
server.ssl.client-auth=need
/////////////////////////////

#!/bin/bash
echo "Fetching Truststore from Secrets Manager..."

# Fetch the truststore from Secrets Manager
TRUSTSTORE_BASE64=$(aws secretsmanager get-secret-value \
  --secret-id my-truststore --query 'SecretString' --output text | jq -r '.["truststore.p12"]')

# Decode and save the truststore file
echo "$TRUSTSTORE_BASE64" | base64 -d > /path/to/truststore.p12

# Run the Spring Boot application
exec java -Djavax.net.ssl.trustStore=/path/to/truststore.p12 \
          -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWORD" \
          -jar your-app.jar



COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]


    server:
  ssl:
    enabled: true
    trust-store: /path/to/truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
    client-auth: need







/////////////////////////////////////////////////////////////////////////////////////////////

server.ssl.enabled=false  # Disable server-side SSL in Spring Boot as it's handled by the load balancer

# Enable client certificate validation
server.ssl.client-auth=need   # 'need' means client certificate is required for validation

# Truststore location after it's written from Secrets Manager
server.ssl.trust-store=/tmp/truststore.p12   # Path to the dynamically fetched and saved truststore
server.ssl.trust-store-password=${TRUSTSTORE_PASSWORD}   # The password for the truststore
server.ssl.trust-store-type=PKCS12   # Type of the truststore




import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

@Component
public class ClientCertificateFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Retrieve the client certificate from the request (depending on how the load balancer forwards it)
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs != null && certs.length > 0) {
            // You can inspect and validate the certificate here
            X509Certificate clientCert = certs[0];
            // Custom logic for validating the client certificate
            System.out.println("Client Certificate: " + clientCert.getSubjectDN());
        }

        filterChain.doFilter(request, response); // Proceed with the filter chain
    }
}
