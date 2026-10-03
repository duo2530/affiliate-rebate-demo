# 青禾计划：单级邀请返利 Demo

[English](README.en.md) | 简体中文

![青禾计划：单级邀请返利 Demo 封面](docs/assets/affiliate-rebate-cover.png)

一个用于学习和演示单级邀请返利业务的全栈 Demo。用户通过邀请码邀请新用户注册；新用户每次模拟充值成功，邀请人获得充值金额 10% 的返利。

项目重点是演示业务流程、表关系和接口边界，不连接真实支付渠道。

## 功能范围

- 手机号和密码注册、登录，支持切换演示用户。
- 注册时可填写邀请码，建立一层邀请关系。
- 模拟充值成功，更新充值用户余额并生成充值流水号。
- 邀请人查看自己的返利余额和返利明细。
- 管理员只读查看用户、邀请关系、充值订单和返利流水。
- 充值使用幂等键，避免重复请求造成重复入账。

充值用户只看到自己的充值金额、成功结果、流水号和充值后余额；返利只显示给返利归属人和管理员。

## 技术栈

- 后端：JDK 17、Spring Boot 4.1.1、Spring JDBC、H2
- 前端：Vue 3、Vite
- 数据库：H2 内存数据库，后端重启后演示数据重置

## 本地运行

### 环境要求

- JDK 17
- Maven 3.6.3 或更高版本
- Node.js 22.12 或更高版本

### 启动后端

在仓库根目录打开终端：

```powershell
mvn -f backend/pom.xml spring-boot:run
```

后端地址：<http://localhost:8080>

### 启动前端

再打开一个终端：

```powershell
Set-Location frontend
npm install
npm run dev
```

浏览器访问：<http://localhost:5173>

Vite 会将 `/api` 请求代理到本地后端。

## 演示账号

| 身份 | 手机号 | 密码 |
|---|---|---|
| 普通用户 A | `13800000001` | `DemoPass123` |
| 普通用户 B | `13800000002` | `DemoPass123` |
| 管理员 | `13800000003` | `AdminPass123` |

普通演示账号初始没有邀请关系。可以用 A 登录并复制邀请码，然后注册一个新用户 C 并填写该邀请码。C 充值成功后重新登录 A 查看返利记录。

## 项目文档

- [如何写一个简单的分销功能（知乎文章风格）](docs/如何写一个简单的分销功能-v2.md)
- [单级邀请返利 Demo 设计与运行教程](docs/单级邀请返利Demo设计与运行教程.md)
- [单级邀请返利 Demo 详细设计](单级邀请返利Demo详细设计.md)

## 构建

后端编译打包（跳过测试）：

```powershell
mvn -f backend/pom.xml -DskipTests package
```

前端生产构建：

```powershell
Set-Location frontend
npm run build
```

本项目不包含真实支付、退款、返利冲正、冻结解冻、提现、多级分销或余额消费。

## License

[MIT License](LICENSE)
