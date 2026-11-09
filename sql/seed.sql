-- ============================================================
-- 基础数据：默认分类（可重复执行）
-- ============================================================
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('教材书籍', '课本、考研资料、课外读物', 10);
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('数码配件', '耳机、充电器、键鼠、数据线', 20);
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('文体用品', '球类、球拍、文具、乐器', 30);
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('生活物资', '台灯、衣架、收纳、小家电', 40);
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('代步工具', '自行车、滑板、电动车配件', 50);
INSERT OR IGNORE INTO categories (name, description, sort_order) VALUES ('其他闲置', '未归类的闲置物品', 99);
