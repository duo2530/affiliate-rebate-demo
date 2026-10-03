# 青禾计划：邀请返利 Demo

使用 Java 17、Spring Boot 4.1.1、Vue 3、Vite 和 H2 内存数据库实现的单级邀请返利演示。

项目设计与本地体验教程：[从需求到 Demo：设计一个单级邀请返利系统](docs/单级邀请返利Demo设计与运行教程.md)

知乎文章风格 V2：[如何写一个简单的分销功能](docs/如何写一个简单的分销功能-v2.md)

## 本地运行要求

- JDK 17
- Maven 3.6.3+
- Node.js 22.12+（当前 Vite 7 需要现代 Node.js）

## 启动后端

在一个终端中运行。可以留在项目根目录执行：

```powershell
mvn -f backend/pom.xml spring-boot:run
```

也可以先进入 `backend` 目录，再运行 `mvn spring-boot:run`。

后端地址：<http://localhost:8080>

## 启动前端

在另一个终端中运行：

``` powershell
Set-Location frontend
npm install
npm run dev
```

打开 <http://localhost:5173>

## 演示账号

| 身份 | 手机号 | 密码 |
|---|---|---|
| 演示用户 A | 13800000001 | DemoPass123 |
| 演示用户 B | 13800000002 | DemoPass123 |
| 管理员 | 13800000003 | AdminPass123 |

也可以在登录页点击账号卡片自动填入手机号和密码。普通用户间初始没有邀请关系；用户 A 可复制邀请码链接，新注册的用户通过链接或手动填写邀请码加入。

## 演示流程

1. 用演示用户 A 登录，复制邀请码或邀请链接。
2. 退出后打开注册页，输入新的手机号、密码和邀请码。
3. 新用户模拟充值成功，充值用户余额增加。
4. 切换回邀请人，查看返利余额和返利明细。
5. 使用管理员账号查看用户、邀请关系、充值订单和返利流水。

返利比例固定为充值金额的 10%，按人民币分四舍五入。充值接口支持幂等重试。H2 为内存数据库，后端每次启动都会恢复初始演示账号及空的业务数据。

## 构建

后端编译打包（跳过测试）：

``` powershell
Set-Location backend
mvn -DskipTests package
```

前端生产构建：

``` powershell
Set-Location frontend
npm run build
```

本 Demo 不包含真实支付、退款、返利提现、冻结解冻和余额消费。
