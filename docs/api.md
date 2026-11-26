# 员工 REST API

假设 Tomcat 的 HTTP 端口为 `8080`，WAR 上下文为 `/ssm-emp-mis`，基础地址为 `http://127.0.0.1:8080/ssm-emp-mis`。下表路径相对该地址。

## 初始化与运行

1. 新库执行 [建表脚本](../database/01-schema.sql)。空表需要演示数据时，再执行 [样本脚本](../database/02-sample-data.sql)。已有 `emp` 表时，用 `SHOW INDEX FROM ssm_emp.emp WHERE Key_name = 'idx_emp_name'` 检查姓名索引，缺少时执行 `ALTER TABLE ssm_emp.emp ADD INDEX idx_emp_name (emp_name)`。
2. 在 Tomcat 的运行环境中设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`AUTH_USERNAME` 和 `AUTH_PASSWORD`。数据库连接地址指向 `ssm_emp`。
3. 在项目根目录执行 `mvn clean verify`，把生成的 `target/ssm-emp-mis.war` 部署到 Tomcat。可以导入 [ApiFox 接口定义](apifox/ssm-emp-mis.openapi.json) 进行联调。

`mvn -Dtest=TransactionBehaviorTest test` 单独运行事务实验。它使用 H2 内存数据库，复用系统的 Service、Mapper 和事务配置。

## 实验登录

发送 `POST /auth/login`，请求体为包含 `username` 和 `password` 的 JSON 对象。使用 Tomcat 环境变量 `AUTH_USERNAME` 和 `AUTH_PASSWORD` 配置的实验账号。成功时返回 `200` 和 `JSESSIONID` Cookie，响应中的 `data` 为 `null`。

客户端随后携带该 Cookie 访问 `/emps`。重新登录会更换已有 Session 的 ID，连续空闲 30 分钟后 Session 失效。发送 `POST /auth/logout` 会使当前 Session 失效，退出请求无需请求体。

## 浏览器联调与静态资源

员工和登录接口默认允许来自 `http://localhost:5173` 和 `http://127.0.0.1:5173` 的浏览器请求。前端地址不同，可在 Tomcat 的运行环境中设置 `CORS_ALLOWED_ORIGINS`，用逗号分隔允许的完整来源（协议、主机和端口）。浏览器通过 `fetch` 登录和调用接口时均设置 `credentials: "include"`。本地联调时，前端和 API 使用同一种主机名，例如都用 `localhost`，以便浏览器在不同端口之间携带会话 Cookie。

将 HTML、CSS、JavaScript 或图片放在 `src/main/resources/static/`，部署后可通过 `/ssm-emp-mis/static/文件名` 访问，无需登录。

## 员工接口

| 方法与路径 | 输入 | 成功响应 |
| --- | --- | --- |
| `POST /emps` | 员工 JSON，`status` 可省略 | `201 Created`，返回新增员工及 `Location` |
| `PUT /emps` | `empId` 和全部可编辑字段 | `200 OK`，返回更新后的员工 |
| `DELETE /emps/{id}` | 路径中的员工 ID | `200 OK`，`data` 为删除条数 |
| `DELETE /emps` | JSON 请求体 `[1,2,3]` | `200 OK`，`data` 为删除条数 |
| `GET /emps/{id}` | 路径中的员工 ID | `200 OK`，返回员工 |
| `GET /emps` | 可选查询参数 `name`、`gender`、`dept`、`page`、`size` | `200 OK`，返回分页结果 |

## 新增与更新

POST 和 PUT 使用 `Content-Type: application/json`。新增请求示例见下方。

```json
{
  "empName": "张三",
  "gender": 1,
  "dept": "研发部",
  "post": "工程师",
  "salary": 12345.67,
  "hireDate": "2024-02-29"
}
```

员工 ID 由数据库生成。创建成功后，`Location` 形如 `/ssm-emp-mis/emps/42`。PUT 使用相同字段，需另外提供 `empId` 和 `status`，并提交全部可编辑字段。`gender` 为 `0` 未说明、`1` 男、`2` 女，`status` 为 `1` 在职、`0` 离职。新增省略状态时默认 `1`。薪资使用 JSON 数字，最多两位小数。入职日期格式为 `yyyy-MM-dd`，不能晚于服务器当天日期。

## 查询与删除

| 参数 | 说明 |
| --- | --- |
| `name` | 姓名包含式匹配。`%`、`_` 和 `!` 按普通字符匹配 |
| `gender` | 性别精确匹配，取值 `0`、`1`、`2` |
| `dept` | 部门精确匹配 |
| `page` | 页码从 `1` 开始，默认 `1` |
| `size` | 每页 `1` 至 `100` 条，默认 `10` |

多个筛选条件同时生效，记录按 `empId` 倒序。`total` 是筛选后的总记录数，`pages` 是总页数。超过末页时保留请求页码，`records` 返回空数组。

单条和批量接口均执行物理删除。办理离职时使用 PUT 将 `status` 改为 `0`。批量删除使用 `Content-Type: application/json`，请求体为含 1 至 100 个互不重复的正整数 ID 的数组。Service 在事务中核对 ID 和删除行数，发生不一致时回滚本次删除。

## 成功响应

所有成功响应使用 `code`、`message`、`data` 三个字段，业务码 `0` 表示成功。下面是筛选后无匹配记录的分页响应。

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10,
    "pages": 0
  }
}
```

## 错误响应

错误响应沿用 `code`、`message`、`data` 三个字段，`code` 与 HTTP 状态码相同，`data` 为 `null`。

| 状态 | 常见原因 |
| --- | --- |
| `400` | 参数缺失、类型或 JSON 错误、校验不通过 |
| `401` | 未登录、Session 已失效或登录凭据错误 |
| `404` | 员工或接口不存在 |
| `405` | 请求方法不受支持 |
| `409` | 数据库约束冲突 |
| `415` | 请求内容类型不受支持 |
| `500` | 未预期的处理错误 |
| `503` | 实验账号尚未配置，登录暂时不可用 |
