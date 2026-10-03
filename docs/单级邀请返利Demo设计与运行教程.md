# 从需求到 Demo：设计一个单级邀请返利系统

这篇教程面向已经接触过 Java、Spring Boot 和 Vue 的开发者。我们用一个小型邀请返利 Demo，走一遍从业务边界、数据表、接口到本地体验的设计过程。

教程重点是解释为什么这样设计。实现代码可以在本仓库的 `backend/` 和 `frontend/` 目录中对照阅读。

## 1. 要解决什么问题

用户 A 邀请用户 C 注册。C 每次模拟充值成功，系统按充值金额的 10% 给 A 记一笔即时可用返利；C 自己只看到自己的充值结果，A 在自己的推广页面看到返利余额和返利流水。

一次最小演示闭环包括：

1. A 登录并分享邀请码。
2. C 注册时填写 A 的邀请码，形成邀请关系。
3. C 模拟充值，系统更新 C 的站内余额、保存充值订单，并给 A 创建返利流水。
4. C 查看充值金额、充值结果、充值流水号和充值后余额；A 登录查看返利。

这里的“支付”只是本地模拟成功，不会连接支付平台，也不会真的扣款。

## 2. 先划定业务边界

为了让 Demo 聚焦在邀请返利闭环，本期采用以下规则：

- 每个用户最多有一个直接邀请人；邀请关系在注册时建立，之后不允许修改。
- 邀请码可选。填写有效邀请码才建立关系；未填写则正常注册，但没有邀请人。
- 每笔成功充值按固定 10% 计算返利，使用人民币两位小数和 `HALF_UP` 舍入。
- 充值金额增加充值用户的站内余额；返利归邀请人所有，立即可用。
- C 的充值接口和充值历史不返回返利金额。返利流水仅通过 A 自己的返利查询接口查看，管理员有只读后台查询权限。
- 不做真实支付、退款、返利冲正、提现、冻结、多级分销或余额消费。
- 使用 H2 内存数据库，服务每次重启后演示数据恢复初始状态。

明确“不做什么”可以避免 Demo 过早引入支付回调、资金状态机、退款逆向流水等额外复杂度。

## 3. 设计数据表

本 Demo 使用四张表：用户表、邀请关系表、充值订单表和返利流水表。

### 3.1 用户表 `app_user`

保存登录身份及站内账户信息。`phone` 唯一；密码只保存 BCrypt 哈希；普通用户有唯一邀请码，管理员没有邀请码；`balance` 保存用户自己的站内余额。

```text
app_user
├── id             主键
├── phone          登录手机号，唯一
├── password_hash  BCrypt 密码哈希
├── role           USER / ADMIN
├── balance        站内余额 DECIMAL(19,2)
├── invite_code    普通用户邀请码，唯一
├── created_at
└── updated_at
```

### 3.2 邀请关系表 `affiliate_relation`

每个受邀用户最多只能绑定一个邀请人，因此 `invitee_user_id` 同时作为主键。邀请人可以对应多条关系。

```text
affiliate_relation
├── invitee_user_id       被邀请人，也是主键
├── inviter_user_id       邀请人
├── invite_code_snapshot  注册时使用的邀请码
└── bound_at              关系建立时间
```

邀请码快照用于保留注册时的绑定依据。关系表不复制双方手机号，查询列表时再关联用户表。

### 3.3 充值订单表 `recharge_order`

只记录成功的模拟充值。`out_trade_no` 是展示给用户的充值流水号；`request_key` 配合用户 ID 防止同一次请求重试后重复充值。

```text
recharge_order
├── id
├── out_trade_no      充值流水号，唯一
├── user_id           充值用户
├── request_key       幂等键，与 user_id 组成唯一约束
├── amount            充值金额
├── rebate_amount     本单返利快照，供账务关联和管理员查看
├── balance_after     充值后的余额快照
├── paid_at
└── created_at
```

`rebate_amount` 是订单内部的计算快照，不代表充值用户有权查看返利。面向用户的接口只返回该用户充值所需的信息；字段是否可见由接口返回对象和权限边界决定，不能因为数据库里有关联数据就一并返回。

### 3.4 返利流水表 `affiliate_ledger`

每笔返利流水属于邀请人，并通过 `source_order_id` 指向触发返利的充值订单。该字段唯一，防止一笔订单重复发放返利。

```text
affiliate_ledger
├── id
├── user_id         返利归属邀请人
├── source_order_id 来源充值订单，唯一
├── amount          返利金额
└── created_at
```

返利总额由流水求和，不在用户表中再维护一个容易与流水不一致的冗余字段。

### 3.5 表之间的关系

```text
app_user (邀请人) 1 ── N affiliate_relation N ── 1 app_user (受邀人)
app_user (充值用户) 1 ── N recharge_order 1 ── 0..1 affiliate_ledger
app_user (返利归属人) 1 ── N affiliate_ledger
```

订单与返利流水分开保存：订单回答“谁充了多少钱”，流水回答“返利记给了谁”。这样 C 的充值查询和 A 的返利查询可以采用不同的授权边界。

完整 DDL 位于 `backend/src/main/resources/schema.sql`。

## 4. 设计接口

接口统一使用 `/api/v1` 前缀，成功响应包装为 `code`、`message` 和 `data`。登录态使用服务端 Session Cookie；前端不提交 `userId` 来决定查询谁的数据。

| 方法 | 路径 | 用途 | 可访问身份 |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | 注册，可选填写邀请码 | 未登录 |
| `POST` | `/api/v1/auth/login` | 手机号密码登录 | 未登录 |
| `POST` | `/api/v1/auth/logout` | 退出并销毁 Session | 已登录 |
| `GET` | `/api/v1/auth/me` | 获取当前会话用户 | 已登录 |
| `GET` | `/api/v1/users/me/affiliate` | 查询自己的推广概览 | 普通用户 |
| `GET` | `/api/v1/users/me/invitees` | 分页查询自己的受邀好友 | 普通用户 |
| `GET` | `/api/v1/users/me/rebates` | 分页查询归自己所有的返利流水 | 普通用户 |
| `GET` | `/api/v1/users/me/recharges` | 分页查询自己的充值记录 | 普通用户 |
| `POST` | `/api/v1/recharges/simulate` | 模拟一笔成功充值 | 普通用户 |
| `GET` | `/api/v1/admin/users` | 管理员查询用户 | 管理员 |
| `GET` | `/api/v1/admin/referral-relations` | 管理员查询邀请关系 | 管理员 |
| `GET` | `/api/v1/admin/recharge-orders` | 管理员查询充值订单 | 管理员 |
| `GET` | `/api/v1/admin/rebate-ledgers` | 管理员查询返利流水 | 管理员 |

用户侧“我的”接口从 Session 获取当前用户 ID，并在服务端按该 ID 过滤数据。管理员查询与普通用户查询使用不同接口，并在服务端验证角色。

### 4.1 注册时建立邀请关系

注册请求可以包含邀请码：

```json
{
  "phone": "13900000003",
  "password": "DemoPass123",
  "inviteCode": "A的实际邀请码"
}
```

后端在一个事务内校验邀请码、创建普通用户并写入邀请关系。邀请码不存在时拒绝注册，不能悄悄将用户当作无邀请关系注册。

### 4.2 模拟充值成功

```http
POST /api/v1/recharges/simulate
Idempotency-Key: 每次主动充值生成的新键
Content-Type: application/json
```

```json
{ "amount": "100.00" }
```

充值用户得到的成功响应只包含订单 ID、充值流水号、充值金额、充值后余额和成功时间，不包含返利金额：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "orderId": "5001",
    "outTradeNo": "R示例流水号",
    "amount": "100.00",
    "balanceAfter": "100.00",
    "paidAt": "服务端生成的时间"
  }
}
```

同一次请求因网络重试而重发时要复用幂等键；用户主动再充一笔时生成新键。相同用户、相同幂等键和相同金额返回原订单结果；同一幂等键却使用不同金额时返回冲突。

### 4.3 返利查询

A 调用 `GET /api/v1/users/me/rebates` 时，服务端只查询归当前 Session 用户所有的返利流水。C 查询 `GET /api/v1/users/me/recharges` 时只获得自己的充值记录，响应不会包含返利字段。页面隐藏字段不是安全边界，必须同时限制服务端的查询列和返回对象。

## 5. 充值事务如何保证一致

充值、余额和返利涉及多张表，需要放在一个事务里完成。当前实现的主要步骤如下：

1. 从登录 Session 得到充值用户 ID，并校验充值金额及幂等键。
2. 用 `SELECT ... FOR UPDATE` 锁定充值用户账户行，串行处理该用户的余额更新。
3. 查询该用户是否已有相同幂等键的订单；有则按金额判断能否重放原结果。
4. 查询该用户是否有邀请人；有邀请人时计算充值金额的 10%，四舍五入到分。
5. 更新充值用户余额，写入成功充值订单。
6. 若返利金额大于零，写入归邀请人所有的返利流水。
7. 任一步数据库写入失败，事务整体回滚，避免余额、订单和返利流水只有一部分成功。

返利比例和金额都由后端决定。前端不提交邀请人 ID、返利比例或返利金额。

对应实现可从 `RechargeController`、`RechargeService` 和 `AffiliateService` 开始阅读。

## 6. 本地启动并体验 Demo

本项目目前提供本地运行入口，没有配置线上演示站点。

### 6.1 环境要求

- JDK 17
- Maven 3.6.3 或更高版本
- Node.js 22.12 或更高版本

### 6.2 启动后端

在仓库根目录打开终端：

```powershell
mvn -f backend/pom.xml spring-boot:run
```

后端地址：<http://localhost:8080>

### 6.3 启动前端

再打开一个终端：

```powershell
Set-Location frontend
npm install
npm run dev
```

浏览器访问：<http://localhost:5173>

Vite 会把 `/api` 请求代理到本地后端。后端和前端需要分别保持运行。

### 6.4 走通邀请返利闭环

1. 用演示用户 A 登录：手机号 `13800000001`，密码 `DemoPass123`。
2. 在推广概览复制 A 的邀请码或邀请链接。
3. 退出 A，打开注册页面，用一个尚未注册的手机号创建 C，并填写 A 的邀请码。
4. C 登录后进行模拟充值。充值成功提示显示充值金额、流水号和充值后余额，不显示返利金额。
5. 退出 C 并重新登录 A，在返利余额和返利明细中查看 C 充值所产生的返利。
6. 可用管理员账号 `13800000003` / `AdminPass123` 查看后台只读数据。

普通演示用户 B 的账号为 `13800000002` / `DemoPass123`。初始演示数据中 A、B 没有邀请关系；新注册用户填写邀请码后才会建立关系。

H2 使用内存数据库，停止并重启后端会重置业务数据。要重复体验时重新注册 C 或重启后端后重新走流程。

## 7. 阅读代码的入口

```text
backend/
├── src/main/resources/schema.sql                 表结构与约束
└── src/main/java/com/techplant/affiliate/
    ├── controller/                               HTTP 接口
    ├── service/                                   注册、返利和充值业务
    ├── common/                                    Session、响应和异常处理
    └── config/                                    Web 配置与演示数据初始化

frontend/src/
├── services/api.js                                前端 API 调用
└── views/
    ├── LoginView.vue                              登录
    ├── RegisterView.vue                           注册与填写邀请码
    ├── UserDashboard.vue                          普通用户推广与充值
    └── AdminDashboard.vue                         管理员只读查询
```

更完整的字段定义、约束和接口响应约定见仓库根目录的 `单级邀请返利Demo详细设计.md`。
