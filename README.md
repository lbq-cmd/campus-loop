# 低碳校园资源循环流转桌面系统（迭代二）

《软件构造》课程分组实验项目 · 3 人小组 · 8 周迭代 · Java + Swing + SQLite

本仓库当前完成到 **迭代二（第 3-4 周：核心业务模块开发 + 单元测试）**，同时保留了迭代一的
需求、UML 与数据库设计文档，作为后续迭代的输入。

---

## 一、迭代二交付内容

| 文档要求 | 本工程对应产物 | 状态 |
| --- | --- | --- |
| 用户、物品实体业务模块 | `domain` + `service`（UserService / ItemService） | ✅ |
| 数据库访问层 | `dao` 接口 + `dao.jdbc` SQLite 实现 + `DaoFactory` | ✅ |
| JUnit 单元测试用例 | `src/test/java` 下 10 个测试类、86 个用例 | ✅ 全部通过 |
| 基础数据存取 | `sql/schema.sql` + `sql/seed.sql`，程序首次启动自动建库 | ✅ |
| 面向对象编码、模块化分解 | 四层结构：domain / dao / service / app | ✅ |
| JDBC 数据库开发 | PreparedStatement、事务模板、外键、索引 | ✅ |
| 迭代一文档（需求、UML、数据库设计、分工） | `docs/01~04` | ✅ |
| 迭代二测试报告 | `docs/05-迭代二测试报告.md` | ✅ |

> 迭代三（Swing GUI、模块集成、集成测试）与迭代四（重构、打包、完整报告）尚未开始，
> 当前 `ConsoleApp` 只是验证服务层可用性的临时控制台入口，迭代三会被 GUI 替换。

## 二、环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 11 及以上（开发环境使用 JDK 19） |
| Maven | 3.6 及以上 |
| 数据库 | SQLite 3（由 `sqlite-jdbc` 驱动内嵌，无需安装） |

## 三、构建与测试

```bash
# 编译 + 运行全部单元测试（86 个用例）
mvn clean test

# 打包，产物：target/campus-loop.jar + target/lib/
mvn clean package
```

## 四、运行控制台演示

```bash
# 方式一：打包后直接运行（Windows 建议先执行 chcp 65001 以正常显示中文）
java -jar target/campus-loop.jar

# 方式二：从源码运行
mvn -q compile
java -cp "target/classes;target/lib/*" com.campusloop.app.ConsoleApp
```

- 数据库默认位于 `data/campusloop.db`，首次运行自动建表并写入 6 个默认分类；
- 首次运行会创建演示管理员：`admin / Admin123456`（正式部署请立即修改）；
- 可用 `-Dcampusloop.db.url=jdbc:sqlite:<路径>` 指定其他数据库文件。

控制台菜单覆盖：注册、登录、发布物品、检索（关键词 + 4 种排序）、我的发布与下架、收藏、个人资料。

## 五、工程结构

```text
campus-loop/
├── pom.xml                       Maven 构建（Java 11、JUnit 5、sqlite-jdbc）
├── sql/
│   ├── schema.sql                数据库初始化脚本（8 张表、索引、约束）
│   └── seed.sql                  默认分类数据
├── docs/                         课程设计文档（需求 / 设计 / 数据库 / 分工 / 测试报告）
│   └── uml/                      PlantUML 类图与时序图
├── src/main/java/com/campusloop/
│   ├── app/                      程序入口（迭代二为控制台演示）
│   ├── common/                   数据库连接、配置、异常、校验、密码、日志
│   ├── domain/                   实体与值对象（User、Item、Category、SearchCriteria…）
│   ├── dao/                      数据访问接口 + JDBC 实现 + DAO 工厂
│   └── service/                  业务服务（用户、物品）与排序策略
├── src/main/resources/app.properties
└── src/test/java/com/campusloop/  单元测试与测试夹具
```

## 六、设计要点速览

- **分层**：`app → service → dao → domain`，上层只依赖接口，下层不反向依赖；
- **设计模式**：单例（Database、DAO 工厂）、工厂方法（DaoFactory）、建造者（Item、SearchCriteria、
  UserCommand）、策略（ItemSorter 排序）、模板方法（Database.inTransaction 事务模板）；
- **防御性编程**：服务层统一校验（用户名规则、密码强度、价格规则、标签数量/长度、归属与状态校验），
  自定义异常体系 + 日志，界面层只负责提示；
- **安全**：PBKDF2 加盐哈希存密码、常量时间比较；全部 SQL 预编译，LIKE 通配符转义；
- **数据库**：外键级联、状态 CHECK 约束、信用分区间约束、必要索引；事务失败自动回滚。

## 七、已知限制（后续迭代处理）

1. 尚无图形界面，控制台仅用于验证业务链路（迭代三补 Swing）；
2. 流转（意向沟通 / 成交）、评价、统计报表、CSV 导出、图片存储尚未接入业务（表结构已就绪）；
3. 未接入代码覆盖率工具，测试覆盖以用例清单说明（迭代三/四补 JaCoCo 与集成测试）；
4. 迭代四需要产出可独立运行的单文件 Jar（shade/assembly）与部署说明书。
