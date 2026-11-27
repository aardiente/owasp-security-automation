package tests.Dvwa;

import automation.AppConfig;
import pom.Dvwa.SecurityPage.SecurityLevel;
import utilities.ExcelFileReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Prepares DVWA test cases from Excel spreadsheets. Organizes the cases by security level before passing them to the tests.
 */
final class DvwaCases {
    /**
     * Prevents creation of this utility.
     */
    private DvwaCases() { }

    /**
     * Reads a configured spreadsheet from the test project resources or an external override.
     *
     * @param key spreadsheet configuration key
     * @param sheet worksheet name, or null for the first worksheet
     * @return rows excluding the spreadsheet header
     */
    static Object[][] load(String key, String sheet) {
        return ExcelFileReader.extractData(AppConfig.testWorkbook(key), sheet);
    }

    /**
     * Converts a cell to trimmed text, treating missing optional columns as empty.
     *
     * @param row spreadsheet row
     * @param column zero-based column index
     * @return standardized cell text
     */
    static String cell(Object[] row, int column) {
        return column >= row.length || row[column] == null ? "" : row[column].toString().trim();
    }

    /**
     * Parses the existing ALL or pipe-separated security-level spreadsheet syntax.
     *
     * @param value spreadsheet security-level field
     * @param row one-based Excel row for validation diagnostics
     * @return selected security modes in enum order
     */
    static EnumSet<SecurityLevel> levels(String value, int row) {
        if (value.equalsIgnoreCase("ALL")) return EnumSet.allOf(SecurityLevel.class);
        EnumSet<SecurityLevel> selected = EnumSet.noneOf(SecurityLevel.class);
        try {
            for (String part : value.split("\\|"))
                selected.add(SecurityLevel.valueOf(part.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("Unknown security level in workbook row " + row, failure);
        }
        if (selected.isEmpty()) throw new IllegalStateException("Missing security levels in workbook row " + row);
        return selected;
    }

    /**
     * Expands cases level-first, preserving the ordering used by upload and stored-XSS tests.
     *
     * @param cases validated spreadsheet cases
     * @param levels extracts each case's selected modes
     * @param label case family for an empty-spreadsheet error
     * @return TestNG level/case parameter pairs
     */
    static <T> Object[][] levelFirst(List<T> cases, Function<T, ? extends Set<SecurityLevel>> levels, String label) {
        List<Object[]> invocations = new ArrayList<>();
        for (SecurityLevel level : SecurityLevel.values())
            for (T probe : cases)
                if (levels.apply(probe).contains(level)) invocations.add(new Object[]{level, probe});
        if (invocations.isEmpty()) throw new IllegalStateException(label + " workbook produced no test cases");
        return invocations.toArray(Object[][]::new);
    }

    /**
     * Adds the selected modes for one case, preserving SQLi's original case-first order.
     *
     * @param invocations destination parameter rows
     * @param probe case object
     * @param levels selected security modes
     */
    static <T> void appendCase(List<Object[]> invocations, T probe, Set<SecurityLevel> levels) {
        for (SecurityLevel level : SecurityLevel.values())
            if (levels.contains(level)) invocations.add(new Object[]{level, probe});
    }
}
