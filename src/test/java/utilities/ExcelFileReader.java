package utilities;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Reads test data from an Excel worksheet and prepares it for use by TestNG. Can leave out the heading row.
 */
public class ExcelFileReader {

    // 1. Original Signature: Defaults to skipping the header (Keeps JuiceShop/Logins working)
    /**
     * Reads table-based test payloads from an Excel spreadsheet into a TestNG-compatible table of values.
     *
     * @param fileName the configured source or output filename
     * @param sheetName the worksheet name, or {@code null} for the default worksheet
     * @return the TestNG data-provider matrix; each row contains the parameters for one test run
     */
    public static Object[][] extractData(String fileName, String sheetName) {
        return extractData(fileName, sheetName, true);
    }

    // 2. New Signature: Allows you to explicitly say "false" to read row 0
    /**
     * Reads table-based test payloads from an Excel spreadsheet into a TestNG-compatible table of values.
     *
     * @param fileName the configured source or output filename
     * @param sheetName the worksheet name, or {@code null} for the default worksheet
     * @param skipHeader whether to omit the first worksheet row
     * @return the TestNG data-provider matrix; each row contains the parameters for one test run
     */
    public static Object[][] extractData(String fileName, String sheetName, boolean skipHeader) {
        Object[][] data = null;

        InputStream is = ExcelFileReader.class.getClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            try {
                File file = new File(fileName);
                if (file.exists()) {
                    is = new FileInputStream(file);
                }
            } catch (Exception e) {
                // Ignore, null check below will handle failure
            }
        }

        if (is == null) {
            throw new RuntimeException("Failed to locate Excel file: " + fileName + ". Ensure the path is correct or the file is in the resources directory.");
        }

        try (InputStream stream = is;
             Workbook book = new XSSFWorkbook(stream)) {

            Sheet sheet = (sheetName == null) ? book.getSheetAt(0) : book.getSheet(sheetName);

            if (sheet == null) {
                throw new RuntimeException("Sheet not found in " + fileName);
            }

            int lastRowIndex = sheet.getLastRowNum();
            int colCount = sheet.getRow(0).getLastCellNum();

            // Adjust array size and starting row based on whether a header exists
            int arraySize = skipHeader ? lastRowIndex : lastRowIndex + 1;
            int startRow = skipHeader ? 1 : 0;

            data = new Object[arraySize][colCount];
            DataFormatter formatter = new DataFormatter();

            for (int i = startRow; i <= lastRowIndex; i++) {
                Row row = sheet.getRow(i);
                int dataRowIndex = skipHeader ? i - 1 : i;

                if (row != null) {
                    for (int j = 0; j < colCount; j++) {
                        data[dataRowIndex][j] = formatter.formatCellValue(row.getCell(j));
                    }
                } else {
                    for (int j = 0; j < colCount; j++) {
                        data[dataRowIndex][j] = "";
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + fileName, e);
        }

        return data;
    }
}