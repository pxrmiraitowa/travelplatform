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

public class SqlRunner {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("Usage: SqlRunner <url> <username> <password> <schema.sql> <data.sql>");
        }

        String url = args[0];
        String username = args[1];
        String password = args[2];
        Path schema = Path.of(args[3]);
        Path data = Path.of(args[4]);

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            execute(statement, "CREATE DATABASE IF NOT EXISTS travel_user DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
            execute(statement, "CREATE DATABASE IF NOT EXISTS travel_product DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
            execute(statement, "CREATE DATABASE IF NOT EXISTS travel_order DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");
            execute(statement, "CREATE DATABASE IF NOT EXISTS travel_content_trip DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci");

            runMonolithSeed(statement, "travel_user", schema, data);
            runMonolithSeed(statement, "travel_product", schema, data);
            runMonolithSeed(statement, "travel_content_trip", schema, data);
            runOrderSchema(statement);
        }
    }

    private static void runMonolithSeed(Statement statement, String database, Path schema, Path data) throws IOException, SQLException {
        execute(statement, "USE " + database);
        runScript(statement, Files.readString(schema, StandardCharsets.UTF_8));
        runScript(statement, Files.readString(data, StandardCharsets.UTF_8));
        normalizeDemoCredentials(statement);
        System.out.println("Initialized " + database);
    }

    private static void normalizeDemoCredentials(Statement statement) throws SQLException {
        execute(statement, """
                UPDATE `user`
                SET `password` = '$2a$10$ujhAXWqWhkHyzQIC5ywpjuBNnShqqvIj4b3hWe3BShQHWvJyrfPvu'
                WHERE `username` IN ('demo_user', 'admin')
                """);
    }

    private static void runOrderSchema(Statement statement) throws SQLException {
        execute(statement, "USE travel_order");
        execute(statement, """
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_no VARCHAR(40) NOT NULL UNIQUE,
                    user_id BIGINT NOT NULL,
                    biz_type VARCHAR(20) NOT NULL,
                    biz_id BIGINT NOT NULL,
                    product_name VARCHAR(160) NOT NULL,
                    product_summary VARCHAR(500),
                    unit_price DECIMAL(12, 2) NOT NULL,
                    quantity INT NOT NULL DEFAULT 1,
                    original_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
                    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
                    total_amount DECIMAL(12, 2) NOT NULL,
                    coupon_id BIGINT,
                    coupon_name VARCHAR(100),
                    order_status INT NOT NULL,
                    travel_date DATE,
                    contact_name VARCHAR(60) NOT NULL,
                    contact_phone VARCHAR(30) NOT NULL,
                    paid_at DATETIME,
                    refund_reason VARCHAR(255),
                    refunded_at DATETIME,
                    remark VARCHAR(255),
                    create_time DATETIME NOT NULL,
                    update_time DATETIME NOT NULL,
                    INDEX idx_orders_user_status (user_id, order_status),
                    INDEX idx_orders_biz (biz_type, biz_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='order table with product snapshot'
                """);
        System.out.println("Initialized travel_order");
    }

    private static void runScript(Statement statement, String sql) throws SQLException {
        for (String command : splitSql(sql)) {
            execute(statement, command);
        }
    }

    private static void execute(Statement statement, String sql) throws SQLException {
        String trimmed = sql.trim();
        if (!trimmed.isEmpty()) {
            statement.execute(trimmed);
        }
    }

    private static List<String> splitSql(String sql) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        String[] lines = sql.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }

            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                char prev = i > 0 ? line.charAt(i - 1) : '\0';
                if (ch == '\'' && !inDoubleQuote && prev != '\\') {
                    inSingleQuote = !inSingleQuote;
                } else if (ch == '"' && !inSingleQuote && prev != '\\') {
                    inDoubleQuote = !inDoubleQuote;
                }

                if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                    commands.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
            current.append('\n');
        }

        if (!current.toString().trim().isEmpty()) {
            commands.add(current.toString());
        }
        return commands;
    }
}
