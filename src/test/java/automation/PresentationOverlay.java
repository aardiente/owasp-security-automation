
package automation;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders the optional in-browser code/action overlay used during slow-motion presentations.
 */
public final class PresentationOverlay {

    private static final Map<Path, List<String>> SOURCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> RESOURCE_CACHE = new ConcurrentHashMap<>();

    private static final String JS_PATH = "presentation/overlay.js";
    private static final String CSS_PATH = "presentation/overlay.css";

    /**
     * Prevents direct instantiation of this utility class.
     */
    private PresentationOverlay() {}

    /**
     * Displays the presentation overlay describing the current browser action and related source context.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param action the user-facing title for the current action
     * @param fallbackCode the code excerpt shown when source extraction is unavailable
     * @param detail the contextual explanation shown beside the presentation overlay action
     */
    public static void show(WebDriver driver, String action, String fallbackCode, String detail) {
        if (!AppConfig.isSlowMoEnabled() || AppConfig.isHeadless()) return;

        try {
            SourceFrame test = null;
            SourceFrame page = null;

            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                if (test == null && frame.getClassName().startsWith("tests.")) test = sourceFrame(frame);
                if (page == null && frame.getClassName().startsWith("pom.")) page = sourceFrame(frame);
                if (test != null && page != null) break;
            }

            String heading = test != null ? test.className() + " · " + readable(test.method()) : "Selenium browser automation";
            String testLabel = test != null ? test.className() + ".java" : "SELENIUM ACTION";
            String testCode = test != null && !test.code().isBlank() ? test.code() : fallbackCode;
            String pageLabel = page != null ? page.className() + ".java" : "";
            String pageCode = page != null ? page.code() : "";

            if (pageCode.equals(testCode)) pageCode = "";

            String script = RESOURCE_CACHE.computeIfAbsent(JS_PATH, PresentationOverlay::loadResource);
            String stylesheet = RESOURCE_CACHE.computeIfAbsent(CSS_PATH, PresentationOverlay::loadResource);

            ((JavascriptExecutor) driver).executeScript(script, stylesheet, action, heading,
                    testLabel, testCode, pageLabel, pageCode, detail == null ? "" : detail);

        } catch (RuntimeException ignored) {
            // Presentation-only feature. Never interrupt test execution.
        }
    }

    /**
     * Reads a bundled presentation asset into a UTF-8 string.
     *
     * @param resource the classpath resource to read
     * @return the UTF-8 text of the requested bundled presentation asset
     */
    private static String loadResource(String resource) {
        try (InputStream stream = PresentationOverlay.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing presentation resource: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load presentation resource: " + resource, e);
        }
    }

    /**
     * Resolves source context for a stack-trace frame shown in the presentation overlay.
     *
     * @param frame the stack-trace frame used to locate source context
     * @return the captured source-code frame, when one can be resolved
     */
    private static SourceFrame sourceFrame(StackTraceElement frame) {
        String classPath = frame.getClassName().split("\\$", 2)[0];
        String className = classPath.substring(classPath.lastIndexOf('.') + 1);
        Path file = Path.of("src", "test", "java", classPath.replace('.', '/') + ".java");
        List<String> lines = SOURCE_CACHE.computeIfAbsent(file, PresentationOverlay::readSource);
        int position = frame.getLineNumber() - 1;

        if (position < 0 || position >= lines.size()) {
            return new SourceFrame(className, frame.getMethodName(), "");
        }

        StringBuilder statement = new StringBuilder();

        for (int i = position; i < lines.size() && i < position + 6; i++) {
            String line = lines.get(i).strip();

            if (line.isBlank() || line.startsWith("//")) continue;
            if (!statement.isEmpty()) statement.append('\n');

            statement.append(line);
            if (line.endsWith(";") || line.endsWith("{") || line.endsWith("}")) break;
        }

        return new SourceFrame(className, frame.getMethodName(), statement.toString());
    }

    /**
     * Reads source lines for an available Java file.
     *
     * @param file the file being inspected or written
     * @return the UTF-8 lines read from the selected source file
     */
    private static List<String> readSource(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Converts a Java method identifier into readable words for the overlay.
     *
     * @param method the HTTP request method or reflected Java method being processed
     * @return a human-readable label derived from a Java method name
     */
    private static String readable(String method) {
        return method.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
    }

    private record SourceFrame(String className, String method, String code) {}
}
