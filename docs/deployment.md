# Tomcat 11 本地部署

使用 JDK 25、Maven 和外部 Tomcat 11.0.x。数据库初始化步骤见 [README](../README.md#数据库初始化阶段-2)。

## 配置数据库连接

在将运行 Tomcat 的终端或 IDEA 的 Tomcat 运行配置中提供以下环境变量。

| 变量 | 示例或用途 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/ssm_emp` |
| `DB_USERNAME` | 业务库账号，使用独立的受限账号 |
| `DB_PASSWORD` | 该账号的密码，通过环境变量提供 |

本阶段启动只需账号能连接数据库并执行读取。后续 CRUD 可按需为业务账号添加 `INSERT`、`UPDATE` 和 `DELETE` 权限。JDBC URL 仅填写连接地址及所需选项，账号和密码单独提供。

`application.properties` 保存连接池参数，默认最大 8 个连接、获取连接最多等待 3 秒、建立连接超时 5 秒。应用启动时创建一个连接，缺少环境变量或数据库连接失败时可直接从启动日志定位。停止或重新部署应用时，Spring 关闭连接池，然后 Servlet 监听器清理本应用加载的 JDBC 驱动和 Connector/J 线程。

## 构建并部署

在项目根目录执行 `mvn package`，将生成的 `target/ssm-emp-mis.war` 部署到 Tomcat 的 `webapps`。用当前设置了上述环境变量的终端运行 Tomcat 的 `bin/catalina.sh run`，或在 IDEA 的 Tomcat 运行配置里部署该 WAR，将应用上下文设为 `/ssm-emp-mis`。

Tomcat 会发现 `WebAppInitializer` 并创建根容器与 MVC 子容器。成功启动后，日志应包含根容器初始化完成、Druid 初始化完成及 `dispatcher` Servlet 初始化完成。

阶段 3 可用以下请求确认已进入 MVC。Tomcat 若使用其他端口，请替换 `8080`。

```sh
curl -i http://127.0.0.1:8080/ssm-emp-mis/emps
```

这时 `/emps` 应返回 HTTP 404，应用日志出现 `No mapping for GET /ssm-emp-mis/emps`。目前 WAR 尚无业务 Mapper，启动时扫描器会提示未发现 Mapper。员工 Mapper 和接口分别在阶段 4 和阶段 5 加入。

## 运行框架验收测试

`mvn test` 运行 MockMvc 测试，验证 JSON 输出中的中文、ISO 日期和金额精度。

在终端设置 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD` 后执行 `mvn verify -Pintegration`。测试 JDBC URL 指向 `ssm_emp_test`，使用该库的独立账号。集成测试使用真实 MySQL 连接检查当前库和 `SELECT 1`，并通过测试 Mapper 执行分页查询。测试查询只读取固定值，测试 Mapper 不会打入 WAR。
