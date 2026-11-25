# 事务提交与回滚验证

Spring 根容器启用 `@EnableTransactionManagement`，`JdbcTransactionManager` 与 MyBatis-Plus 共用 Druid 数据源。`EmpService.update` 将更新和回查放在同一事务，`EmpService.deleteBatch` 则将 ID 检查、删除和影响行数校验放在同一事务。删除后发现行数不一致时抛出业务异常，已执行的删除随事务回滚。

## 实验用例

用例由 `TransactionBehaviorIT` 通过真实 MySQL 测试库执行。测试方法本身不启动外层事务，每次调用完成后由测试重新查询数据库，再清理该用例创建的记录。故障演示类放在测试源码中。

| 场景 | 实验方式 | 调用结束后查库结果 |
| --- | --- | --- |
| 批量删除成功 | 两条记录通过 Service 删除 | 两条记录均已删除 |
| 批量删除后报告行数不一致 | 测试拦截器先执行真实 `DELETE`，再向 Service 返回较小的行数，触发异常 | 两条记录都恢复 |
| 运行时异常退出事务方法 | 经 Spring 代理调用，写入后抛出 `IllegalStateException` | 无新增记录 |
| 受检异常的默认行为 | 写入后抛出测试专用受检异常 | 新增记录已提交 |
| 为受检异常指定 `rollbackFor` | 写入后抛出同一种受检异常 | 无新增记录 |
| 同类自调用 | 无事务方法直接调用本类的事务方法，内部写入后抛出异常 | 新增记录已提交 |
| 在事务方法内部捕获异常 | 写入后捕获运行时异常，方法正常返回 | 新增记录已提交 |

行数失配用例验证了生产批删方法在写入之后的回滚分支。受检异常、自调用和内部捕获用例说明，回滚行为由代理边界和实际传播到代理的异常决定。[Spring 声明式事务](https://docs.spring.io/spring-framework/reference/7.0/data-access/transaction/declarative/annotations.html)

## 执行

按 [部署说明](deployment.md) 配置 `TEST_DB_URL`、`TEST_DB_USERNAME` 和 `TEST_DB_PASSWORD`。测试库必须为 `ssm_emp_test`。

```sh
mvn verify -Pintegration -Dit.test=TransactionBehaviorIT
```

在 MySQL 8.4.11 上验证了上表全部 7 项用例。测试同时核对了事务配置接入前的行为，真实删除异常、运行时异常以及指定 `rollbackFor` 的三个回滚用例均在接入事务前失败，启用事务后通过。
