# 数据库脚本说明

数据库脚本只维护一份，Docker Compose 直接挂载这些源码文件，避免部署目录与后端资源目录产生两套不同版本。

| 执行顺序 | 用途 | 源文件 | 容器内路径 |
| --- | --- | --- | --- |
| 01 | 建表 | `travel-platform-server/src/main/resources/sql/schema.sql` | `/docker-entrypoint-initdb.d/01-schema.sql` |
| 02 | 演示数据 | `travel-platform-server/src/main/resources/sql/data-demo.sql` | `/docker-entrypoint-initdb.d/02-demo-data.sql` |
| 03 | 演示数据迁移 | `travel-platform-server/scripts/demo-data-patch-20260601.sql` | `/docker-entrypoint-initdb.d/03-demo-data-migration-20260601.sql` |
| 手动 | 集成测试数据 | `travel-platform-server/src/test/resources/sql/data-test.sql` | `/opt/travel-platform/sql/data-test.sql` |

MySQL 官方镜像只会在数据目录为空时自动执行 `/docker-entrypoint-initdb.d`。因此，修改初始化脚本后需要在确认不再需要旧数据的前提下删除 Compose 数据卷，再重新启动：

```powershell
docker compose down -v
docker compose up -d --build
```

`docker compose down -v` 会永久删除本地 Compose 数据库数据和上传卷，不要对需要保留的数据执行该命令。

测试数据不会在普通演示环境中自动导入。如需向当前 Compose 数据库补充测试数据，可执行：

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /opt/travel-platform/sql/data-test.sql'
```

迁移补丁需要应用到已经存在的数据卷时，可手动执行：

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/03-demo-data-migration-20260601.sql'
```

所有初始化与测试数据语句应保持幂等，确保重复执行不会产生重复数据或破坏已有业务记录。
