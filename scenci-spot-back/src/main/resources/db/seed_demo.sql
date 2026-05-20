-- ============================================================
-- 景区预约管理系统 - 演示数据
-- 模型：1个主景区，下设6个子项目，门票按项目组合售卖
-- 可直接重复执行，ON DUPLICATE KEY UPDATE 保证幂等
-- ============================================================

-- ===================== 1. 景区/项目 =====================
-- ID=1 是主景区，ID=2~7 是园内子项目
INSERT INTO scenic_area (id, name, address, open_time_desc, contact_phone, status) VALUES
(1, '东方山水度假区', '江苏省南京市江宁区汤山街道1号', '09:00-21:00', '025-52881234', 1),
(2, '水上乐园',       '江苏省南京市江宁区汤山街道1号', '10:00-19:00', '025-52881234', 1),
(3, '欢乐世界',       '江苏省南京市江宁区汤山街道1号', '09:00-21:00', '025-52881234', 1),
(4, '冰雪王国',       '江苏省南京市江宁区汤山街道1号', '10:00-20:00', '025-52881234', 1),
(5, '萌宠动物园',     '江苏省南京市江宁区汤山街道1号', '09:30-18:00', '025-52881234', 1),
(6, '极限挑战区',     '江苏省南京市江宁区汤山街道1号', '10:00-20:00', '025-52881234', 1),
(7, '儿童梦幻城堡',   '江苏省南京市江宁区汤山街道1号', '09:00-18:00', '025-52881234', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), address=VALUES(address), open_time_desc=VALUES(open_time_desc);

-- ===================== 2. 时段（仅主景区） =====================
INSERT INTO timeslot (id, scenic_id, name, start_time, end_time, status) VALUES
(1, 1, '上午场', '09:00:00', '14:00:00', 1),
(2, 1, '下午场', '14:00:00', '21:00:00', 1),
(3, 1, '夜场',   '17:00:00', '21:00:00', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), start_time=VALUES(start_time), end_time=VALUES(end_time);

-- ===================== 3. 退改规则 =====================
INSERT INTO refund_rule (id, scenic_id, name, free_refund_hours, allow_reschedule) VALUES
(1, 1, '游玩日前24小时免费退', 24, 1),
(2, 1, '游玩日前48小时免费退', 48, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ===================== 4. 门票（主景区不同票种） =====================
-- 通票→全部项目 / 单项票→指定项目 / 亲子票→儿童友好项目
INSERT INTO ticket (id, scenic_id, name, image_url, description, ticket_type, price_cent, stock_qty, morning_enabled, afternoon_enabled, valid_date, refund_rule_id, status) VALUES
-- 通票类
( 1, 1, '成人通票',   NULL, '含全部7个项目，当日不限次数。',                     'SINGLE',  29800, 5000, 1, 1, NULL, 1, 1),
( 2, 1, '学生通票',   NULL, '全日制在校学生，含全部项目，凭学生证入园。',         'STUDENT', 19800, 2000, 1, 1, NULL, 1, 1),
( 3, 1, '老人通票',   NULL, '60岁以上老人，含全部项目，凭身份证入园。',           'SENIOR',  14800, 1000, 1, 1, NULL, 1, 1),
( 4, 1, '家庭套票A',  NULL, '2成人+1儿童，含全部项目，儿童限1.5m以下。',          'FAMILY',  69800,  500, 1, 1, NULL, 1, 1),
-- 单项/组合票
( 5, 1, '水上乐园票', NULL, '仅含水上乐园1个项目。',                             'SINGLE',  12800, 2000, 1, 1, NULL, 2, 1),
( 6, 1, '冰雪王国票', NULL, '仅含冰雪王国1个项目（含防寒服租赁）。',              'SINGLE',  15800, 1500, 1, 1, NULL, 2, 1),
( 7, 1, '极限挑战票', NULL, '仅含极限挑战区1个项目。',                           'SINGLE',  16800, 1000, 1, 1, NULL, 2, 1),
( 8, 1, '亲子畅玩票', NULL, '1成人+1儿童，含萌宠动物园+儿童梦幻城堡+旋转木马。',  'FAMILY',  25800,  600, 1, 1, NULL, 2, 1),
-- 儿童/夜场
( 9, 1, '儿童票',     NULL, '1.2m以下儿童，含萌宠动物园+儿童梦幻城堡+旋转木马。',  'CHILD',   9800, 1500, 1, 1, NULL, 2, 1),
(10, 1, '夜场票',     NULL, '仅17:00后入园，含欢乐世界+摩天轮。',                'SINGLE',  12800, 2500, 0, 1, NULL, 2, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), price_cent=VALUES(price_cent), stock_qty=VALUES(stock_qty), description=VALUES(description);

-- ===================== 5. 门票-项目关联 =====================
-- 门票 1~4（通票→全部项目 2~7）
-- 门票 5→仅水上乐园(2)
-- 门票 6→仅冰雪王国(4)
-- 门票 7→仅极限挑战区(6)
-- 门票 8→萌宠动物园(5)+儿童梦幻城堡(7)+欢乐世界(3)
-- 门票 9→萌宠动物园(5)+儿童梦幻城堡(7)+欢乐世界(3)
-- 门票10→欢乐世界(3)   （夜场仅此一个项目）
INSERT INTO ticket_project (ticket_id, project_id) VALUES
(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),
(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),
(4,2),(4,3),(4,4),(4,5),(4,6),(4,7),
(5,2),
(6,4),
(7,6),
(8,3),(8,5),(8,7),
(9,3),(9,5),(9,7),
(10,3)
ON DUPLICATE KEY UPDATE ticket_id=VALUES(ticket_id);

-- ===================== 6. 库存（未来7天，仅主景区的上午/下午场） =====================
INSERT INTO ticket_inventory (ticket_id, visit_date, timeslot_id, total_qty, sold_qty, locked_qty, status)
SELECT t.id,
       DATE_ADD(CURRENT_DATE, INTERVAL d.n DAY),
       s.id,
       FLOOR(t.stock_qty * 0.5),
       0, 0, 1
FROM ticket t
JOIN timeslot s ON s.scenic_id = t.scenic_id AND s.name != '夜场'
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) d
WHERE t.status = 1
  AND NOT EXISTS (
    SELECT 1 FROM ticket_inventory i
    WHERE i.ticket_id = t.id AND i.visit_date = DATE_ADD(CURRENT_DATE, INTERVAL d.n DAY) AND i.timeslot_id = s.id
  );

-- 夜场票(10)只补充夜场时段库存
INSERT INTO ticket_inventory (ticket_id, visit_date, timeslot_id, total_qty, sold_qty, locked_qty, status)
SELECT 10,
       DATE_ADD(CURRENT_DATE, INTERVAL d.n DAY),
       3,
       FLOOR(2500 * 0.5),
       0, 0, 1
FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) d
WHERE NOT EXISTS (
  SELECT 1 FROM ticket_inventory i
  WHERE i.ticket_id = 10 AND i.visit_date = DATE_ADD(CURRENT_DATE, INTERVAL d.n DAY) AND i.timeslot_id = 3
);

-- ===================== 7. 用户 =====================
INSERT INTO user_account (id, role, scenic_id, status, login_type, phone, username, password_hash, nickname, full_name, id_card_no) VALUES
-- 管理员（主景区）
( 1, 'ADMIN',    1, 1, 'ACCOUNT', NULL,        'admin',    '123456', '赵管理', '赵建国', '320102198003154578'),
-- 审核员
( 2, 'AUDITOR',  1, 1, 'ACCOUNT', NULL,        'auditor',  '123456', '孙审核', '孙明辉', '320103198507224319'),
-- 分析员
( 3, 'ANALYST',  1, 1, 'ACCOUNT', NULL,        'analyst',  '123456', '周分析', '周博文', '320105199212017831'),
-- 游客（手机号注册，无密码，靠验证码登录）
( 4, 'TOURIST', NULL, 1, 'PHONE', '13812340001', NULL, NULL, '张游客', '张伟',   '320101199505064312'),
( 5, 'TOURIST', NULL, 1, 'PHONE', '13812340002', NULL, NULL, '李游客', '李娜',   '320102199612087825'),
( 6, 'TOURIST', NULL, 1, 'PHONE', '13812340003', NULL, NULL, '王游客', '王磊',   '320103199107256713'),
( 7, 'TOURIST', NULL, 1, 'PHONE', '13956780001', NULL, NULL, '赵游客', '赵芳',   '320104199803174321'),
( 8, 'TOURIST', NULL, 1, 'PHONE', '13956780002', NULL, NULL, '陈游客', '陈强',   '320105197510266511'),
( 9, 'TOURIST', NULL, 1, 'PHONE', '13956780003', NULL, NULL, '刘游客', '刘梅',   '320106199208289021'),
(10, 'TOURIST', NULL, 1, 'PHONE', '13698760001', NULL, NULL, '黄游客', '黄磊',   '320107200003153311'),
(11, 'TOURIST', NULL, 1, 'PHONE', '13698760002', NULL, NULL, '周游客', '周华',   '320108199707016234'),
(12, 'TOURIST', NULL, 1, 'PHONE', '13698760003', NULL, NULL, '吴游客', '吴敏',   '320109199410123456')
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname), full_name=VALUES(full_name), phone=VALUES(phone);

-- ===================== 8. 客流阈值（主景区+部分热门项目） =====================
INSERT INTO flow_threshold (scenic_id, threshold_type, area_code, value, enabled) VALUES
(1, 'DAILY_MAX',   NULL, 30000, 1),
(1, 'INSTANT_MAX', NULL,  8000, 1),
(2, 'INSTANT_MAX', NULL,  2000, 1),
(3, 'INSTANT_MAX', NULL,  3000, 1),
(6, 'INSTANT_MAX', NULL,  1000, 1)
ON DUPLICATE KEY UPDATE value=VALUES(value);
