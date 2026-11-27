package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Groups file upload operations used by the Juice Shop lab tests and provides results for the related checks.
 */
public final class UploadActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    UploadActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Collects the upload-policy observation for the file-type validation challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> uploadUnexpectedFileType() throws IOException, InterruptedException {
        return api.uploadMultipart("/file-upload", "audit.txt", "text/plain",
                "Juice Shop unexpected file type check".getBytes(StandardCharsets.UTF_8), null);
    }

    /**
     * Collects the upload-policy observation for the file-size validation challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> uploadOversizedPdf() throws IOException, InterruptedException {
        byte[] bytes = new byte[120_000];
        Arrays.fill(bytes, (byte) 'A');
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(header, 0, bytes, 0, header.length);
        return api.uploadMultipart("/file-upload", "test.pdf", "application/pdf", bytes, null);
    }

    /**
     * Collects the file-handling observation for the deprecated-interface lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> useDeprecatedXmlUpload() throws IOException, InterruptedException {
        var session = api.login();
        return api.uploadMultipart("/file-upload", "invoice.xml", "application/xml",
                "<?xml version=\"1.0\"?><invoice><id>1</id></invoice>".getBytes(StandardCharsets.UTF_8), session.token());
    }
}
