package automation;

/**
 * Names the browser implementations accepted by the WebDriver factory.
 */
public enum BrowserType
{
    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari");

    private final String descriptor;

    /**
     * Creates the browser type component and initializes the dependencies required by its methods.
     *
     * <p>Stores the human-readable browser type label for the enum constant.</p>
     *
     * @param str the descriptive value to store for the enum constant
     */
    BrowserType(String str)
    {
        this.descriptor = str;
    }

    /**
     * Returns a human-readable representation of this value.
     *
     * @return the human-readable label of this browser type
     */
    public String toString()
    {
        return this.descriptor;
    }

}
