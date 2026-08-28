import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlRunner {
    private static final Set<String> USER_TABLES = Set.of("role", "user", "user_role", "user_contact");
    private static final Set<String> PRODUCT_TABLES = Set.of(
            "flight", "train_ticket", "hotel", "hotel_room", "tour_package", "coupon", "attraction");
    private static final Set<String> CONTENT_TABLES = Set.of(
            "trip_plan", "trip_plan_item", "price_alert", "share_post", "share_image", "review");
    private static final Set<String> ORDER_TABLES = Set.of("orders");
    private static final Set<String> ALL_TABLES = union(USER_TABLES, PRODUCT_TABLES, CONTENT_TABLES, ORDER_TABLES);
    private static final Pattern TABLE_COMMAND = Pattern.compile(
            "(?is)^(?:CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?|INSERT\\s+INTO\\s+|UPDATE\\s+|DELETE\\s+FROM\\s+)[`]?([a-zA-Z0-9_]+)[`]?.*");

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

            runOwnedSeed(statement, "travel_user", schema, data, USER_TABLES);
            normalizeDemoCredentials(statement);
            runOwnedSeed(statement, "travel_product", schema, data, PRODUCT_TABLES);
            runOwnedSeed(statement, "travel_content_trip", schema, data, CONTENT_TABLES);
            runOrderSchema(statement);
        }
    }

    private static void runOwnedSeed(Statement statement, String database, Path schema, Path data,
                                     Set<String> ownedTables) throws IOException, SQLException {
        execute(statement, "USE " + database);
        removeForeignTables(statement, ownedTables);
        runScript(statement, Files.readString(schema, StandardCharsets.UTF_8), ownedTables);
        runScript(statement, Files.readString(data, StandardCharsets.UTF_8), ownedTables);
        System.out.println("Initialized " + database);
    }

    private static void removeForeignTables(Statement statement, Set<String> ownedTables) throws SQLException {
        execute(statement, "SET FOREIGN_KEY_CHECKS = 0");
        for (String table : ALL_TABLES) {
            if (!ownedTables.contains(table)) {
                execute(statement, "DROP TABLE IF EXISTS `" + table + "`");
            }
        }
        execute(statement, "SET FOREIGN_KEY_CHECKS = 1");
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
        removeForeignTables(statement, ORDER_TABLES);
        execute(statement, """
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_no VARCHAR(40) NOT NULL UNIQUE,
                    user_id BIGINT NOT NULL,
                    biz_type VARCHAR(20) NOT NULL,
                    biz_id BIGINT NOT NULL,
                    variant_id BIGINT,
                    variant_name VARCHAR(100),
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
        execute(statement, "ALTER TABLE orders ADD COLUMN IF NOT EXISTS variant_id BIGINT AFTER biz_id");
        execute(statement, "ALTER TABLE orders ADD COLUMN IF NOT EXISTS variant_name VARCHAR(100) AFTER variant_id");
        System.out.println("Initialized travel_order");
    }

    private static void runScript(Statement statement, String sql, Set<String> ownedTables) throws SQLException {
        for (String command : splitSql(sql)) {
            Matcher matcher = TABLE_COMMAND.matcher(command.trim());
            if (matcher.matches() && ownedTables.contains(matcher.group(1).toLowerCase())) {
                execute(statement, command);
            }
        }
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> result = new HashSet<>();
        for (Set<String> set : sets) {
            result.addAll(set);
        }
        return Set.copyOf(result);
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
