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
