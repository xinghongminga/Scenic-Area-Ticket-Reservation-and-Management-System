INSERT INTO scenic_area (id, name, address, open_time_desc, contact_phone, status)
VALUES (1, '示例景区', '示例市示例区示例路1号', '08:30-18:00', '010-88886666', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO scenic_area (id, name, address, open_time_desc, contact_phone, status)
VALUES
  (2, '游乐园', '示例景区-游乐园区', '09:00-20:00', '010-88886666', 1),
  (3, '动物园', '示例景区-动物园区', '08:30-18:00', '010-88886666', 1),
  (4, '水族馆', '示例景区-水族馆区', '09:30-21:00', '010-88886666', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO timeslot (id, scenic_id, name, start_time, end_time, status)
VALUES
  (1, 1, '上午场', '08:30:00', '12:00:00', 1),
  (2, 1, '下午场', '13:00:00', '18:00:00', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO refund_rule (id, scenic_id, name, free_refund_hours, allow_reschedule)
VALUES (1, 1, '默认24小时可退', 24, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO user_account (id, role, scenic_id, status, login_type, phone, username, password_hash, nickname)
VALUES
  (1, 'TOURIST', NULL, 1, 'PHONE', '13800000001', NULL, NULL, '游客A'),
  (2, 'ADMIN', 1, 1, 'ACCOUNT', NULL, 'admin', 'admin123', '景区管理员'),
  (3, 'ANALYST', 1, 1, 'ACCOUNT', NULL, 'analyst', 'analyst123', '运营分析师'),
  (4, 'AUDITOR', 1, 1, 'ACCOUNT', NULL, 'auditor', 'auditor123', '票务审核员')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO ticket (id, scenic_id, name, ticket_type, price_cent, valid_from, valid_to, refund_rule_id, status)
VALUES
  (1, 1, '成人票', 'SINGLE', 12000, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 1, 1),
  (2, 1, '学生票', 'DISCOUNT', 8000, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 1, 1),
  (3, 1, '双人票', 'DOUBLE', 22000, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 1, 1),
  (4, 1, '团体票', 'GROUP', 50000, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 1, 2 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 1 AND project_id = 2);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 2, 3 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 2 AND project_id = 3);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 3, 2 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 3 AND project_id = 2);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 3, 4 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 3 AND project_id = 4);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 4, 2 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 4 AND project_id = 2);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 4, 3 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 4 AND project_id = 3);

INSERT INTO ticket_project (ticket_id, project_id)
SELECT 4, 4 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ticket_project WHERE ticket_id = 4 AND project_id = 4);

INSERT INTO flow_threshold (scenic_id, threshold_type, area_code, value, enabled)
SELECT 1, 'DAILY_MAX', NULL, 20000, 1 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM flow_threshold WHERE scenic_id = 1 AND threshold_type = 'DAILY_MAX' AND area_code IS NULL);

INSERT INTO flow_threshold (scenic_id, threshold_type, area_code, value, enabled)
SELECT 1, 'INSTANT_MAX', NULL, 3500, 1 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM flow_threshold WHERE scenic_id = 1 AND threshold_type = 'INSTANT_MAX' AND area_code IS NULL);

INSERT INTO flow_threshold (scenic_id, threshold_type, area_code, value, enabled)
SELECT 1, 'AREA_MAX', 'A1', 800, 1 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM flow_threshold WHERE scenic_id = 1 AND threshold_type = 'AREA_MAX' AND area_code = 'A1');

INSERT INTO ticket_inventory (ticket_id, visit_date, timeslot_id, total_qty, sold_qty, locked_qty, status)
SELECT 1, DATE_ADD(CURRENT_DATE, INTERVAL n DAY), 1, 500, 0, 0, 1 FROM (
  SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
) t
WHERE NOT EXISTS (
  SELECT 1 FROM ticket_inventory i
  WHERE i.ticket_id = 1 AND i.visit_date = DATE_ADD(CURRENT_DATE, INTERVAL n DAY) AND i.timeslot_id = 1
);

INSERT INTO ticket_inventory (ticket_id, visit_date, timeslot_id, total_qty, sold_qty, locked_qty, status)
SELECT 1, DATE_ADD(CURRENT_DATE, INTERVAL n DAY), 2, 500, 0, 0, 1 FROM (
  SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
) t
WHERE NOT EXISTS (
  SELECT 1 FROM ticket_inventory i
  WHERE i.ticket_id = 1 AND i.visit_date = DATE_ADD(CURRENT_DATE, INTERVAL n DAY) AND i.timeslot_id = 2
);
