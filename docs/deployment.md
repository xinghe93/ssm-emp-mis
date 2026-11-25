# Tomcat 11 本地部署

使用 JDK 25、Maven 和外部 Tomcat 11.0.x。数据库初始化步骤见 [README](../README.md#数据库初始化阶段-2)。

## 配置数据库连接

在将运行 Tomcat 的终端或 IDEA 的 Tomcat 运行配置中提供以下环境变量。

| 变量 | 示例或用途 |
| --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/ssm_emp` |
| `DB_USERNAME` | 业务库账号，使用独立的受限账号 |
| `DB_PASSWORD` | 该账号的密码，通过环境变量提供 |
| `AUTH_USERNAME` | 自设的实验登录账号 |
| `AUTH_PASSWORD` | 实验登录密码，通过环境变量提供 |

业务账号需要 `ssm_emp` 的 `SELECT`、`INSERT`、`UPDATE` 和 `DELETE` 权限以支持员工接口。JDBC URL 填写连接地址及所需选项，账号和密码单独提供。

`application.properties` 保存连接池参数，默认最大 8 个连接、获取连接最多等待 3 秒、建立连接超时 5 秒。应用启动时创建一个连接，缺少环境变量或数据库连接失败时可直接从启动日志定位。停止或重新部署应用时，Spring 关闭连接池，然后 Servlet 监听器清理本应用加载的 JDBC 驱动和 Connector/J 线程。

## 构建并部署

在项目根目录执行 `mvn package`，将生成的 `target/ssm-emp-mis.war` 部署到 Tomcat 的 `webapps`。用当前设置了上述环境变量的终端运行 Tomcat 的 `bin/catalina.sh run`，或在 IDEA 的 Tomcat 运行配置里部署该 WAR，将应用上下文设为 `/ssm-emp-mis`。

Tomcat 会发现 `WebAppInitializer` 并创建根容器与 MVC 子容器。成功启动后，日志应包含根容器初始化完成、Druid 初始化完成及 `dispatcher` Servlet 初始化完成。

未登录时，以下请求返回 HTTP `401` 和统一 JSON。Tomcat 若使用其他端口，请替换 `8080`。

```sh
curl -i 'http://127.0.0.1:8080/ssm-emp-mis/emps?page=1&size=2'
```

向 `POST /auth/login` 提交实验账号的 JSON，之后携带返回的 `JSESSIONID` Cookie 再请求列表，应返回 HTTP `200` 和分页 JSON。登录、退出和员工请求示例见 [员工 REST API](api.md)。

请求日志切面记录已进入 Controller 的请求方法、路由模板、状态或异常类型和处理耗时。拦截器提前拒绝或参数绑定失败的请求没有进入 Controller。切面不记录用户名密码、请求体、查询参数或 Session ID。

## 运行框架验收测试

`mvn test` 运行员工 HTTP 映射、请求绑定、参数校验、错误响应、登录和 JSON 转换测试。

在终端设置 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD` 后执行 `mvn verify -Pintegration`。测试 JDBC URL 指向 `ssm_emp_test`，使用仅有该库 `SELECT`、`INSERT`、`UPDATE`、`DELETE` 权限的独立账号。集成测试验证框架启动、分页和员工 CRUD。员工用例核对实际连接的库名，测试写入在事务中回滚。

事务验收另外使用独立的员工样例验证提交和回滚，每次重新查库比对后清理该用例。用例与命令见 [事务提交与回滚验证](transaction-verification.md)。
