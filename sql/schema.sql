-- ============================================================
-- 低碳校园资源循环流转桌面系统 数据库初始化脚本（SQLite 3）
-- 迭代二范围：users / categories / items / favorites 全量落地；
--             流转、评价、日志表随迭代一设计一并建表，供迭代三使用。
-- 说明：所有 DDL 均带 IF NOT EXISTS，可重复执行。
-- ============================================================

PRAGMA foreign_keys = ON;

-- 用户表：密码仅保存 PBKDF2 派生值，不保存明文
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    nickname      TEXT    NOT NULL,
    contact       TEXT,
    role          TEXT    NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('STUDENT', 'ADMIN')),
    status        TEXT    NOT NULL DEFAULT 'ACTIVE'  CHECK (status IN ('ACTIVE', 'BANNED')),
    credit_score  INTEGER NOT NULL DEFAULT 100       CHECK (credit_score >= 0 AND credit_score <= 150),
    created_at    TEXT    NOT NULL,
    last_login_at TEXT
);

-- 物品分类表：管理员维护，迭代二提供只读查询
CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

-- 闲置物品表
CREATE TABLE IF NOT EXISTS items (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id    INTEGER NOT NULL REFERENCES categories(id),
    title          TEXT    NOT NULL,
    description    TEXT,
    item_type      TEXT    NOT NULL CHECK (item_type IN ('SELL', 'EXCHANGE', 'FREE')),
    price          REAL    NOT NULL DEFAULT 0 CHECK (price >= 0),
    item_condition TEXT    NOT NULL DEFAULT 'GOOD' CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    status         TEXT    NOT NULL DEFAULT 'ONLINE' CHECK (status IN ('ONLINE', 'OFF_SHELF', 'LOCKED', 'SOLD', 'REMOVED')),
    tags           TEXT,
    image_path     TEXT,
    view_count     INTEGER NOT NULL DEFAULT 0,
    created_at     TEXT    NOT NULL,
    updated_at     TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_owner    ON items(owner_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_status   ON items(status);

-- 收藏表：复合主键天然防止重复收藏
CREATE TABLE IF NOT EXISTS favorites (
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id    INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    created_at TEXT    NOT NULL,
    PRIMARY KEY (user_id, item_id)
);

-- ---------- 以下表属于迭代一总体设计，迭代三接入业务 ----------

-- 意向沟通记录
CREATE TABLE IF NOT EXISTS trade_requests (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id      INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    requester_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message      TEXT,
    status       TEXT    NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    created_at   TEXT    NOT NULL
);

-- 成交/交换流转记录
CREATE TABLE IF NOT EXISTS trade_records (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id       INTEGER NOT NULL REFERENCES items(id),
    request_id    INTEGER REFERENCES trade_requests(id),
    seller_id     INTEGER NOT NULL REFERENCES users(id),
    buyer_id      INTEGER NOT NULL REFERENCES users(id),
    final_price   REAL    NOT NULL DEFAULT 0,
    carbon_saved  REAL    NOT NULL DEFAULT 0,
    finished_at   TEXT    NOT NULL
);

-- 双方互评
CREATE TABLE IF NOT EXISTS reviews (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    trade_id     INTEGER NOT NULL REFERENCES trade_records(id) ON DELETE CASCADE,
    reviewer_id  INTEGER NOT NULL REFERENCES users(id),
    reviewee_id  INTEGER NOT NULL REFERENCES users(id),
    score        INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   TEXT    NOT NULL
);

-- 关键操作日志（审计）
CREATE TABLE IF NOT EXISTS operation_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    action     TEXT NOT NULL,
    detail     TEXT,
    created_at TEXT NOT NULL
);
