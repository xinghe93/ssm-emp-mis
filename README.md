# ssm-emp-mis

《JAVA 框架技术（一）》实验二：AI 赋能 SSM 框架整合与 EMP 员工管理 MIS 系统开发。

使用原生 Spring Framework、Spring MVC 和 MyBatis-Plus，采用 Java Config、Maven WAR 打包，部署到外部 Apache Tomcat。仅开发后端 RESTful API，不引入 Spring Boot。

## 当前进度

已完成阶段 1：项目初始化与 Maven 依赖管理，等待开始阶段 2。

本阶段配置依赖、编译、WAR 打包、Git 忽略规则及项目说明；尚未实现 Servlet 初始化、数据库连接、员工业务接口或测试用例。WAR 构建成功不代表 SSM 已整合完成，也不代表业务接口可以访问。

## 环境与版本

| 组件 | 项目版本/要求 |
| --- | --- |
| JDK | 25，编译、Maven 和 Tomcat 使用相同主版本，不使用预览特性 |
| Maven | 当前开发环境为 3.10.0 |
| Spring Framework / Spring MVC | 7.0.11 |
| 外部 Tomcat | 11.0.x，Servlet 6.1；本机已确认安装 11.0.22 |
| MyBatis-Plus | 3.5.18.1 |
| MyBatis-Spring | 4.1.0 |
| MySQL 服务端 | 计划使用 8.4.x LTS，实例与连接信息尚未配置 |
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
- MP 分页单独引入 `mybatis-plus-jsqlparser`，后续通过 `MybatisSqlSessionFactoryBean` 显式注册插件。
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

后续测试命令：

```sh
mvn test
mvn verify -Pintegration
```

Surefire 执行 `*Test` 单元测试；启用 integration profile 后，Failsafe 执行 `*IT` 集成测试。当前尚无测试用例，Maven 输出 `No tests to run` 不能视为业务测试通过。后续数据库集成测试只允许使用独立的 `ssm_emp_test` 库。

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

- `src/main/java/com/xinghe/emp/`：后续按 config、controller、service、mapper、entity、dto、common、interceptor、aspect 分包。
- `src/main/resources/`：后续放置非敏感配置与日志配置。
- `src/test/java/`：后续放置单元测试、MVC 测试及集成测试。
- `database/`：阶段 2 新增数据库结构与示例数据脚本。
- `docs/`：后续记录接口、部署、事务验证及 ApiFox 测试。

现阶段不创建没有实际内容的业务类或空文档目录。

## 配置与数据约束

后续数据源配置从 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 等环境变量读取；测试库使用独立的测试环境变量。当前仅约定配置方式，尚未实现配置读取。

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
