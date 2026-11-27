package automation;

/**
 * Lists the browsers supported by the project and their display names.
 */
public enum BrowserType
{
    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari");

    private final String descriptor;

    /**
     * Stores the browser name returned by the enum’s display method.
     *
     * @param str the descriptive value to store for the enum constant
     */
    BrowserType(String str)
    {
        this.descriptor = str;
    }

    /**
     * Returns a readable representation of this value.
     *
     * @return the readable label of this browser type
     */
    public String toString()
    {
        return this.descriptor;
    }

}
