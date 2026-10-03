# Qinghe Plan: Single-Level Referral Rebate Demo

[简体中文](README.md) | English

A full-stack demo for learning and demonstrating a single-level referral rebate flow. A user invites a new user with an invitation code. Whenever the invitee completes a simulated recharge, the inviter receives a 10% rebate.

This project demonstrates the business flow, data relationships, and API boundaries. It does not connect to a real payment provider.

## Features

- Sign up and sign in with a phone number and password; switch between demo accounts.
- Optionally bind an inviter during registration with an invitation code.
- Simulate a successful recharge, update the recharging user's balance, and issue a recharge reference number.
- Let inviters view their own rebate balance and rebate records.
- Provide read-only admin views for users, referral relationships, recharge orders, and rebate ledgers.
- Use an idempotency key to prevent duplicate credits when a recharge request is retried.

Users see their own recharge amount, result, reference number, and updated balance. Rebate details are visible only to the rebate owner and administrators.

## Tech Stack

- Backend: JDK 17, Spring Boot 4.1.1, Spring JDBC, and H2
- Frontend: Vue 3 and Vite
- Database: in-memory H2; demo data resets whenever the backend restarts

## Run Locally

### Requirements

- JDK 17
- Maven 3.6.3 or later
- Node.js 22.12 or later

### Start the backend

Run this command from the repository root:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

Backend URL: <http://localhost:8080>

### Start the frontend

Open another terminal and run:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open <http://localhost:5173> in your browser. Vite proxies `/api` requests to the local backend.

## Demo Accounts

| Role | Phone | Password |
|---|---|---|
| User A | `13800000001` | `DemoPass123` |
| User B | `13800000002` | `DemoPass123` |
| Admin | `13800000003` | `AdminPass123` |

The demo users start without a referral relationship. Sign in as A and copy the invitation code, then register a new user C with that code. After C recharges, sign back in as A to view the rebate records.

## Documentation

- [How to Build a Simple Referral Rebate Feature (Chinese)](docs/如何写一个简单的分销功能-v2.md)
- [Design and Run Tutorial (Chinese)](docs/单级邀请返利Demo设计与运行教程.md)
- [Detailed Design (Chinese)](单级邀请返利Demo详细设计.md)

## Build

Build the backend package (tests are skipped):

```powershell
mvn -f backend/pom.xml -DskipTests package
```

Build the frontend for production:

```powershell
Set-Location frontend
npm run build
```

This demo does not include real payments, refunds, rebate reversals, frozen balances, withdrawals, multi-level referrals, or balance spending.

## License

[MIT License](LICENSE)
