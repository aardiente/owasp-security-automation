package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;

/** Composes the existing Juice Shop action domains around one per-test HTTP client. */
public final class JuiceShopChallengeActions {
    private final JuiceShopApi api;
    private final AccountActions account;
    private final BasketActions basket;
    private final FeedbackActions feedback;
    private final ResourceActions resources;
    private final CatalogActions catalog;
    private final UploadActions uploads;
    private final BrowserActions browser;

    public record BasketAttempt(int ownBasketId, int targetBasketId, HttpResponse<String> response) {}
    public record FeedbackAttempt(int authenticatedUserId, int forgedUserId, HttpResponse<String> response) {}
    public record FeedbackRemoval(int deletedCount, HttpResponse<String> response, boolean fiveStarsRemain) {}

    /**
     * Creates the action domains for a test invocation, sharing one client and serializer.
     * @param api configured application client
     */
    public JuiceShopChallengeActions(JuiceShopApi api) {
        this.api = api;
        Json json = new Json();
        this.account = new AccountActions(api, json);
        this.basket = new BasketActions(api, json);
        this.feedback = new FeedbackActions(api, json);
        this.resources = new ResourceActions(api, json);
        this.catalog = new CatalogActions(api, json);
        this.uploads = new UploadActions(api, json);
        this.browser = new BrowserActions(api, json);
    }

    /** Returns the account action domain for this test invocation. */
    public AccountActions account() { return account; }

    /** Returns the basket action domain for this test invocation. */
    public BasketActions basket() { return basket; }

    /** Returns the feedback action domain for this test invocation. */
    public FeedbackActions feedback() { return feedback; }

    /** Returns the resources action domain for this test invocation. */
    public ResourceActions resources() { return resources; }

    /** Returns the catalog action domain for this test invocation. */
    public CatalogActions catalog() { return catalog; }

    /** Returns the uploads action domain for this test invocation. */
    public UploadActions uploads() { return uploads; }

    /** Returns the browser action domain for this test invocation. */
    public BrowserActions browser() { return browser; }

    /**
     * Polls challenge progress until the named Juice Shop challenge is recorded as solved.
     *
     * <p>Polls the challenge status before failing with an error if completion is not observed within the fixed retry count.</p>
     *
     * @param challengeName the Score Board name of the Juice Shop challenge to inspect
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public void waitForChallenge(String challengeName) throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            if (api.isChallengeSolved(challengeName)) return;
            Thread.sleep(200);
        }
        throw new IllegalStateException("Challenge not completed: " + challengeName);
    }
}
