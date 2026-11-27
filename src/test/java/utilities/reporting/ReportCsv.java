package utilities.reporting;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides shared tools for reading and writing CSV reports so both report views use the same format.
 */
final class ReportCsv {
    /**
     * Prevents creation of the CSV reader and writer.
     */
    private ReportCsv() {}

    /**
     * Quotes one value; standardizes line breaks to preserve one physical row per result.
     */
    static String quote(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    /**
     * Parses quoted fields, escaped quotes, and CRLF or LF record separators.
     */
    static List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean fieldPresent = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"') {
                fieldPresent = true;
                if (quoted && i + 1 < content.length() &&
                        content.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                row.add(field.toString());
                field.setLength(0);
                fieldPresent = false;
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < content.length() &&
                        content.charAt(i + 1) == '\n') {
                    i++;
                }

                row.add(field.toString());
                field.setLength(0);

                if (fieldPresent || !(row.size() == 1 && row.get(0).isEmpty()))
                    rows.add(row);

                row = new ArrayList<>();
                fieldPresent = false;
            } else {
                field.append(c);
                fieldPresent = true;
            }
        }

        if (!row.isEmpty() || fieldPresent) {
            row.add(field.toString());
            rows.add(row);
        }

        return rows;
    }
}
