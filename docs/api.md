# 员工 REST API

假设 Tomcat 的 HTTP 端口为 `8080`，WAR 上下文为 `/ssm-emp-mis`，基础地址为 `http://127.0.0.1:8080/ssm-emp-mis`。下表路径相对该地址。

| 方法与路径 | 输入 | 成功响应 |
| --- | --- | --- |
| `POST /emps` | 员工 JSON，`status` 可省略 | `201 Created`，返回新增员工及 `Location` |
| `PUT /emps` | `empId` 和全部可编辑字段 | `200 OK`，返回更新后的员工 |
| `DELETE /emps/{id}` | 路径中的员工 ID | `200 OK`，`data` 为删除条数 |
| `DELETE /emps?ids=1,2,3` | 逗号分隔的员工 ID，无请求体 | `200 OK`，`data` 为删除条数 |
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

员工 ID 由数据库生成。创建成功后，`Location` 形如 `/ssm-emp-mis/emps/42`。PUT 使用相同字段，需另外提供 `empId` 和 `status`，并提交全部可编辑字段。`gender` 为 `0` 未说明、`1` 男、`2` 女，`status` 为 `1` 在职、`0` 离职。新增省略状态时默认 `1`。薪资使用 JSON 数字，最多两位小数。日期格式为 `yyyy-MM-dd`。

## 查询与删除

| 参数 | 说明 |
| --- | --- |
| `name` | 姓名包含式匹配。`%`、`_` 和 `!` 按普通字符匹配 |
| `gender` | 性别精确匹配，取值 `0`、`1`、`2` |
| `dept` | 部门精确匹配 |
| `page` | 页码从 `1` 开始，默认 `1` |
| `size` | 每页 `1` 至 `100` 条，默认 `10` |

多个筛选条件同时生效，记录按 `empId` 倒序。`total` 是筛选后的总记录数，`pages` 是总页数。超过末页时保留请求页码，`records` 返回空数组。

单条和批量接口均执行物理删除。办理离职时使用 PUT 将 `status` 改为 `0`。批量删除接受 1 至 100 个互不重复的正整数 ID。Service 在删除前检查这些 ID 是否存在。并发场景的事务原子性在阶段 6 验收。

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

阶段 5 的响应结构覆盖成功请求。缺少 `ids` 或类型转换失败时，Spring MVC 返回 HTTP `400`。业务异常的 HTTP 状态映射、统一错误响应和登录拦截在阶段 6 接入。
