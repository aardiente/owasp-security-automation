package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Application-specific uploads actions relocated from the original challenge helper. */
public final class UploadActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON serializer.
     * @param api configured application client
     * @param json shared JSON serializer
     */
    UploadActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Performs the upload unexpected file type action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
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
     * Performs the upload oversized PDF action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
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
     * Performs the use deprecated XML upload action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
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
