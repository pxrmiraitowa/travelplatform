import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes a standalone SQL patch file against the local demo database.
 * This keeps demo-data maintenance independent from Spring Boot startup.
 */
public class ApplyDemoDataPatch {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/travel_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_SQL_FILE = "scripts/demo-data-patch-20260601.sql";

    public static void main(String[] args) throws Exception {
        String sqlFile = args.length > 0 ? args[0] : DEFAULT_SQL_FILE;
        String url = args.length > 1 ? args[1] : DEFAULT_URL;
        String username = args.length > 2 ? args[2] : DEFAULT_USERNAME;
        String password = args.length > 3 ? args[3] : DEFAULT_PASSWORD;

        Path sqlPath = Path.of(sqlFile);
        if (!Files.exists(sqlPath)) {
            throw new IllegalArgumentException("SQL patch file not found: " + sqlPath.toAbsolutePath());
        }

        String script = Files.readString(sqlPath, StandardCharsets.UTF_8);
        if (!script.isEmpty() && script.charAt(0) == '\uFEFF') {
            script = script.substring(1);
        }

        List<String> statements = splitStatements(script);
        if (statements.isEmpty()) {
            System.out.println("No SQL statements found in " + sqlPath.toAbsolutePath());
            return;
        }

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            int affectedTotal = 0;
            for (int i = 0; i < statements.size(); i++) {
                String sql = statements.get(i);
                try (Statement statement = connection.createStatement()) {
                    int affected = statement.executeUpdate(sql);
                    affectedTotal += Math.max(affected, 0);
                    System.out.printf("[%d/%d] affected=%d%n", i + 1, statements.size(), affected);
                }
            }
            connection.commit();
            System.out.printf("Patch applied successfully. statements=%d, affected=%d%n", statements.size(), affectedTotal);
        } catch (SQLException ex) {
            throw new SQLException("Failed to apply SQL patch " + sqlPath.toAbsolutePath() + ": " + ex.getMessage(), ex);
        }
    }

    private static List<String> splitStatements(String script) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && ch == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (!inSingleQuote && ch == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (ch == '\'' && !isEscaped(current)) {
                inSingleQuote = !inSingleQuote;
            }

            if (ch == ';' && !inSingleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        addStatement(statements, current);
        return statements;
    }

    private static boolean isEscaped(StringBuilder current) {
        int slashCount = 0;
        for (int i = current.length() - 1; i >= 0 && current.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }
}
