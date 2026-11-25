# ssm-emp-mis

《JAVA 框架技术（一）》实验二：AI 赋能 SSM 框架整合与 EMP 员工管理 MIS 系统开发。

使用原生 Spring Framework、Spring MVC 和 MyBatis-Plus，采用 Java Config、Maven WAR 打包，部署到外部 Apache Tomcat。仅开发后端 RESTful API，不引入 Spring Boot。

## 当前进度

已完成阶段 1 至阶段 6。下一阶段是回归测试、ApiFox 验证和交付。

项目已具备 Maven WAR 构建、SSM 全注解配置、MySQL 初始化脚本和六个员工 HTTP 接口。员工接口需要实验登录，响应统一使用 `Result<T>`，完整更新和批量删除使用声明式事务。请求与响应见 [员工 REST API](docs/api.md)。

## 环境与版本

| 组件 | 项目版本/要求 |
| --- | --- |
| JDK | 25，编译、Maven 和 Tomcat 使用相同主版本，不使用预览特性 |
| Maven | 当前开发环境为 3.10.0 |
| Spring Framework / Spring MVC | 7.0.11 |
| 外部 Tomcat | 11.0.x，Servlet 6.1；本机已确认安装 11.0.22 |
| MyBatis-Plus | 3.5.18.1 |
| MyBatis-Spring | 4.1.0 |
| MySQL 服务端 | 已确认 MySQL 8.4.11，Docker 容器 mysql8，127.0.0.1:3306 |
| MySQL Connector/J | 9.6.0 |
| Druid | 1.2.28 |
| Jackson | 3.1.8 LTS |
| Hibernate Validator / Jakarta Validation | 9.1.6.Final / 3.1.1 |
| SLF4J / Log4j2 | 2.0.20 / 2.26.1 |
| AspectJ | 1.9.25.1，仅用于 Spring AOP 支持 |
| Lombok | 1.18.48 |
| JUnit Jupiter | 6.1.3 |

Spring、MyBatis-Plus、Jackson、Log4j2 和 JUnit 分别使用 BOM 统一模块版本，版本集中在 `pom.xml` 的 properties 中。

兼容性约束：

- Spring 7 使用 `jakarta.servlet` 和 Jakarta Validation；部署目标是 Tomcat 11，不是 Tomcat 9 或 10.1。
- 原生 Spring 项目显式引入 MyBatis-Spring 4.x，不使用 MyBatis-Plus 的 Spring Boot Starter。
- MP 分页依赖 `mybatis-plus-jsqlparser`，在 `MybatisSqlSessionFactoryBean` 中显式注册，使用 MySQL 方言，单页最多 100 条。
- Jackson 3 已包含 Java 时间类型支持，不额外添加 Jackson 2 的 `jackson-datatype-jsr310`。
- Servlet API 和 Lombok 使用 provided 作用域，不打入 WAR；JUnit、Spring Test 和 Expressly 仅用于测试。
- 日志使用 SLF4J 2 → Log4j2，以及 Commons Logging → Log4j2；不同时添加 Logback、slf4j-simple 或 log4j-to-slf4j。

官方参考：[Spring 7.0](https://docs.spring.io/spring/reference/7.0/index.html)、[Tomcat 版本对照](https://tomcat.apache.org/whichversion)、[MyBatis-Spring 兼容表](https://mybatis.org/spring/)、[MP 安装](https://baomidou.com/getting-started/install/)、[MP 分页](https://baomidou.com/plugins/pagination/)、[Hibernate Validator](https://hibernate.org/validator/releases/9.1/)。

## 构建与检查

先确认当前终端环境：

```sh
java -version
mvn -version
```

依赖解析与构建：

```sh
mvn validate
mvn dependency:tree
mvn help:effective-pom
mvn package
```

构建产物为 `target/ssm-emp-mis.war`。当前没有完整 Maven Wrapper，使用已安装的 `mvn`。

测试命令：

```sh
mvn test
mvn verify -Pintegration
```

Surefire 执行 `*Test`，启用 integration profile 后 Failsafe 执行 `*IT`。MySQL 集成测试使用独立的 `ssm_emp_test` 库。环境变量和部署步骤见 [Tomcat 11 本地部署](docs/deployment.md)。

### 阶段 1 实际验收记录（2026-11-24）

- 在 JDK 25.0.5.0.3、Maven 3.10.0 下执行 validate、dependency:tree、help:effective-pom、package，均成功。
- 本次构建通过 `-Dmaven.repo.local=/Users/xinghe/dev/tools/maven/maven-repository` 使用与 IDEA 一致的现有缓存，没有修改全局 settings。
- 依赖树和有效 POM 分别输出到 `target/dependency-tree.txt`、`target/effective-pom.xml`，随 target 一起被 Git 忽略。
- 核实 Spring 模块均为 7.0.11，MyBatis-Spring 为 4.1.0，MyBatis 为 3.5.19；未引入 Spring Boot、旧 javax Servlet/Validation API 或冲突的日志实现。
- 核实 Compiler Plugin 的 release/parameters 属性绑定和 Lombok 显式处理器配置。当前没有 Java 源码，未验证实际业务代码编译。
- 已检查 WAR 压缩包完整性及 38 个运行依赖，Servlet API、Lombok、JUnit、Spring Test、Expressly 和测试 EL API 未打入 WAR。
- Surefire 输出 `No tests to run`；未执行数据库集成测试、Tomcat 部署或 ApiFox 测试。

IDEA 中将项目 SDK、Maven 运行 JDK 及部署 Tomcat 的 JRE 都设置为 JDK 25，重新加载 Maven 项目。Lombok 注解处理器已在 Compiler Plugin 中显式配置。

项目不定义自有远程仓库，不修改用户全局 Maven settings。镜像和本地缓存沿用开发环境配置；依赖下载失败时先区分镜像、网络和坐标问题。

## 目录约定

- `src/main/java/com/xinghe/emp/config/`：Servlet 初始化、Spring MVC、Druid 和 MyBatis-Plus 配置。
- `src/main/java/com/xinghe/emp/controller/`：员工 HTTP 路由、参数绑定和响应。
- `src/main/java/com/xinghe/emp/` 下的 `entity`、`mapper`、`service`、`dto` 和 `common`：员工映射、数据访问、业务操作、请求模型和业务异常。
- `src/main/resources/`：非敏感连接池参数与 Log4j2 控制台日志配置。
- `src/test/java/`：MVC JSON、MySQL 框架和员工 Service 集成测试。
- `database/`：数据库结构、示例数据及测试库脚本。
- `docs/`：部署说明和员工接口约定，后续补充事务与 ApiFox 记录。

现阶段不创建没有实际内容的业务类或空文档目录。

## 数据库初始化（阶段 2）

使用现有 Docker 容器 mysql8 中的 MySQL 8.4.11，不重新安装或重建容器。业务库为 `ssm_emp`，测试库为 `ssm_emp_test`，字符集为 utf8mb4，排序规则为 utf8mb4_0900_ai_ci，表引擎为 InnoDB。

### 员工表

| 字段 | 类型 | 约束与含义 |
| --- | --- | --- |
| emp_id | BIGINT | 自增主键，对应 Long |
| emp_name | VARCHAR(50) | 非空；允许同名员工 |
| gender | TINYINT | 非空；0 未说明、1 男、2 女 |
| dept | VARCHAR(50) | 非空；部门索引 idx_emp_dept |
| post | VARCHAR(50) | 非空 |
| salary | DECIMAL(12,2) | 非空；范围 0.00～9999999999.99，对应 BigDecimal |
| hire_date | DATE | 非空；对应 LocalDate |
| status | TINYINT | 非空；默认 1；1 在职、0 离职 |

数据库使用 CHECK 约束限制性别、非负薪资和任职状态。`status` 不参与逻辑删除，离职记录仍正常查询。

姓名、部门、岗位的非空白校验、薪资最多两位小数、入职日期不得晚于当天，在阶段 6 的应用参数校验中实现。MySQL DECIMAL 可能对超出小数位数的数据进行舍入，不能依赖它替代入参精度校验；数据库需保持严格 SQL 模式以拒绝溢出、超长字符串和非法日期。

### 初始化顺序

先通过有建库权限的账号确认实际服务器版本、已有库表及表数据，不能仅凭本机 mysql 客户端版本判断服务端：

```sh
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password \
  --execute="SELECT VERSION(); SHOW DATABASES LIKE 'ssm_emp%';"
```

确认没有同名员工表或需要保留的现有结构后，再按顺序执行。`--password` 不携带参数，客户端会交互提示输入密码。

```sh
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password \
  --default-character-set=utf8mb4 < database/01-schema.sql
```

检查 `ssm_emp.emp` 为空后，执行一次示例数据脚本：

```sh
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password \
  --execute="SELECT COUNT(*) FROM ssm_emp.emp;"
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password \
  --default-character-set=utf8mb4 < database/02-sample-data.sql
mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --password \
  --default-character-set=utf8mb4 < database/03-test-schema.sql
```

- 01-schema.sql 创建业务库及 emp 表；已有表时明确失败，不使用 IF NOT EXISTS 掩盖表结构不一致。
- 02-sample-data.sql 插入 24 条虚构员工数据，覆盖 6 个部门、3 种性别编码及在职/离职。该脚本不是幂等脚本，重复执行会追加数据；不得把重跑脚本当作重置方式。
- 03-test-schema.sql 用 CREATE TABLE LIKE 复制业务表结构、索引及 CHECK 约束，测试表初始没有员工数据。后续如变更业务表，需要同步迁移测试表；该脚本不会自动同步已有表。
- 脚本不包含删库、删表、清空、替换员工数据或创建带密码账号的语句；遇到错误应停止处理，不加 `--force` 忽略错误。
- 初始化账号只用于建库和导入；后续应用和测试连接分别使用对应库的受限账号，不将 root 账号作为应用默认配置。

初始化后的检查可在 MySQL 客户端执行：

```sql
SHOW CREATE TABLE ssm_emp.emp;
SHOW CREATE TABLE ssm_emp_test.emp;
SELECT COUNT(*) AS employee_count FROM ssm_emp.emp;
SELECT dept, COUNT(*) AS employee_count FROM ssm_emp.emp GROUP BY dept;
SELECT COUNT(*) AS test_employee_count FROM ssm_emp_test.emp;
```

预期业务库有 24 条员工记录、每个部门 4 条，测试表有 0 条记录。负薪资、非法性别和非法状态的写入应由 CHECK 约束拒绝。约束及回滚实验只在测试库中执行，不在业务库中试写非法数据。

### 后续连接配置约定

| 环境变量 | 作用 |
| --- | --- |
| DB_URL | 业务 JDBC URL：jdbc:mysql://127.0.0.1:3306/ssm_emp |
| DB_USERNAME / DB_PASSWORD | 业务库受限账号及密码 |
| TEST_DB_URL | 测试 JDBC URL：jdbc:mysql://127.0.0.1:3306/ssm_emp_test |
| TEST_DB_USERNAME / TEST_DB_PASSWORD | 测试库独立账号及密码 |

凭据由终端或 IDEA 环境变量提供。应用的数据源配置读取 `DB_*`，集成测试单独用 `TEST_DB_*` 配置测试上下文中的数据源。

### 阶段 2 实际验收记录（2026-11-24）

- 通过服务端查询确认现有 mysql8 容器为 MySQL 8.4.11，开启 STRICT_TRANS_TABLES；初始化前两个实验库均不存在。
- 三个脚本已实际执行，创建 ssm_emp.emp、插入 24 条示例数据，并创建空的 ssm_emp_test.emp。
- 核对两个表的 8 个字段、字段类型、非空约束、自增主键、部门索引、InnoDB、utf8mb4 排序规则、3 个 CHECK 约束及默认在职状态。
- 核对业务表 24 条记录、6 个部门、3 种性别编码、2 种任职状态，以及金额和日期的精确读写。
- 只在测试库中验证中文和表情字符、自增 ID、默认状态、合法闰日、0.00 与 9999999999.99 的薪资边界。
- 负薪资、非法性别、非法状态被 CHECK 拒绝（3819）；空姓名、薪资溢出、超长姓名、非法日期分别被拒绝（1048、1264、1406、1292）。
- 验证正常插入后显式回滚，以及事务中先成功插入再发生约束异常并断开连接后回滚；最终测试表保持 0 条记录。自增序列可能已推进，回滚不保证 ID 连续。
- 重跑两个结构脚本均因表已存在而停止（1050）；业务表记录数、ID 合计及薪资合计未改变。未重跑一次性示例数据脚本。
- 本阶段未创建 Java 类或应用数据源配置，也未执行 JUnit、Tomcat 或 ApiFox 测试。

## 阶段 3 实际验收记录（2026-11-24）

- `mvn verify -Pintegration` 构建成功。Surefire 运行 1 项 MockMvc 测试，Failsafe 运行 1 项 MySQL 集成测试，均通过。
- MockMvc 验证了中文、ISO 日期和精确的小数金额。集成测试确认父子容器共用同一个 Druid 数据源，连接实际指向 MySQL 8.4.11 的 `ssm_emp_test`，`SELECT 1` 成功，测试 Mapper 的第 2 页返回 1 条记录并报告总数 3。
- `target/ssm-emp-mis.war` 已部署到独立的本机 Tomcat 11.0.22 实例。日志确认根容器、Druid 和 DispatcherServlet 启动，访问 `/ssm-emp-mis/emps` 进入 MVC 并返回预期的 HTTP 404。停止时确认连接池关闭，Tomcat 未报告 JDBC 驱动或清理线程残留。
- 验收使用临时只读数据库账号，结束后已清理。部署步骤见 [Tomcat 11 本地部署](docs/deployment.md)。

## 阶段 4 实际验收记录（2026-11-25）

- `EmpMapper` 使用 MyBatis-Plus `BaseMapper`。Service 支持新增、完整更新、单条与批量物理删除、按 ID 查询，以及按姓名、性别和部门筛选分页。新增默认在职，离职记录保留在查询结果中。
- 分页默认第 1 页、每页 10 条，按员工 ID 倒序，超出末页返回空记录。姓名按字面子串匹配，`%`、`_` 和 `!` 按普通字符处理。
- `mvn verify -Pintegration` 构建成功，1 项 MockMvc 测试与 5 项 MySQL 集成测试均通过。员工测试覆盖自增、金额和日期读写、同内容更新、批量删除中的无效或缺失 ID、组合筛选和越界分页。
- 本次 MySQL 实例中两个实验库起初均不存在。验收按原表结构初始化 `ssm_emp_test`，员工测试写入事务均回滚，最终测试表保持 0 条记录，临时账号已清理。当前实例部署前仍需初始化业务库 `ssm_emp`。
- 批量删除会在执行前检查所有 ID。并发删除场景的事务原子性在阶段 6 接入声明式事务后验收。

## 阶段 5 实际验收记录（2026-11-25）

- `mvn verify -Pintegration` 构建成功，9 项 MVC 与 JSON 测试和 5 项 MySQL 集成测试通过。确认请求绑定、`201` 和 `Location`、成功响应、删除数量与分页字段。
- 将 WAR 部署到本机 Tomcat 11.0.22，使用 MySQL 8.4.11 的独立测试库验证六个 HTTP 接口。新增、查询、更新、多条件分页及两种删除方式均返回预期结果。
- 从现有脚本恢复了缺失的业务库及 24 条样例。HTTP 验收的写入限定在 `ssm_emp_test`，验收结束后业务库仍有 24 条样例、测试表为空，临时账号已清理。
- 将 MyBatis-Plus 的 SQL 解析线程池纳入 Spring 生命周期，Tomcat 停止后未报告解析线程残留。

## 阶段 6 实际验收记录（2026-11-25）

- `mvn verify -Pintegration` 构建成功，13 项 MVC 与 JSON 测试和 12 项 MySQL 集成测试均通过。
- MockMvc 验证参数校验及 `400`、`401`、`404`、`405`、`409`、`415`、`500`、`503` 响应。外部 Tomcat 11.0.22 验证了真实 Cookie 登录、Session ID 更新、登出失效和六个员工接口。
- 检查控制台请求日志中的路由、处理耗时和状态，并核对实验密码、Session ID 和测试查询参数未写入日志。
- 事务集成测试在 MySQL 8.4.11 上验证了批删提交、真实 SQL 写入后的回滚及三种常见失效场景。细节见 [事务验证](docs/transaction-verification.md)。测试结束后业务表和测试表记录数与执行前一致。

## 配置与数据约束

数据源从 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 读取连接信息，测试库使用独立的测试环境变量。实验登录从 `AUTH_USERNAME` 和 `AUTH_PASSWORD` 读取账号与密码。

- 不在 POM、源码、README、初始化脚本或 ApiFox 导出中写入真实密码。
- 本地 `.env` 和 application-local.properties 等敏感配置已加入忽略规则；原生 Spring 不会自动加载 `.env`，环境变量需由终端或 IDEA 运行配置提供。
- `salary` 使用 BigDecimal；`hire_date` 使用 LocalDate。
- `status` 表示在职/离职，不作为 MP 逻辑删除字段。
- 初始化脚本不默认删除已有库表或清空数据。

## 后续阶段

1. 项目初始化与 Maven 依赖管理。
2. MySQL 8.x 数据库及初始化脚本。
3. SSM 全注解框架整合与外部 Tomcat 部署验证。
4. 员工 Entity、Mapper 和 Service。
5. RESTful Controller、统一响应与分页查询。
6. 参数校验、全局异常、Session 登录拦截、AOP 和事务管理。
7. JUnit、ApiFox 回归测试、优化与交付。

每次只执行一个阶段；完成后记录变更与实际验收结果。Git 仓库已初始化，origin 使用 `git@github.com:xinghe93/ssm-emp-mis.git`；未经明确指令不自动提交、推送或修改 Git 历史。
