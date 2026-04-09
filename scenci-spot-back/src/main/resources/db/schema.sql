SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS scenic_area (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(255) NULL,
  open_time_desc VARCHAR(255) NULL,
  contact_phone VARCHAR(32) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scenic_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role VARCHAR(16) NOT NULL,
  scenic_id BIGINT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  login_type VARCHAR(16) NOT NULL DEFAULT 'ACCOUNT',
  phone VARCHAR(32) NULL,
  username VARCHAR(64) NULL,
  password_hash VARCHAR(255) NULL,
  nickname VARCHAR(64) NULL,
  avatar_url VARCHAR(255) NULL,
  full_name VARCHAR(64) NULL,
  id_card_no VARCHAR(32) NULL,
  oauth_provider VARCHAR(32) NULL,
  oauth_open_id VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_oauth (oauth_provider, oauth_open_id),
  KEY idx_user_role (role),
  KEY idx_user_scenic (scenic_id),
  CONSTRAINT fk_user_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @user_full_name_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND COLUMN_NAME = 'full_name'
);
SET @user_full_name_alter_sql := IF(
  @user_full_name_col_exists = 0,
  'ALTER TABLE user_account ADD COLUMN full_name VARCHAR(64) NULL AFTER nickname',
  'SELECT 1'
);
PREPARE user_full_name_stmt FROM @user_full_name_alter_sql;
EXECUTE user_full_name_stmt;
DEALLOCATE PREPARE user_full_name_stmt;

SET @user_avatar_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND COLUMN_NAME = 'avatar_url'
);
SET @user_avatar_alter_sql := IF(
  @user_avatar_col_exists = 0,
  'ALTER TABLE user_account ADD COLUMN avatar_url VARCHAR(255) NULL AFTER nickname',
  'SELECT 1'
);
PREPARE user_avatar_stmt FROM @user_avatar_alter_sql;
EXECUTE user_avatar_stmt;
DEALLOCATE PREPARE user_avatar_stmt;

SET @user_id_card_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND COLUMN_NAME = 'id_card_no'
);
SET @user_id_card_alter_sql := IF(
  @user_id_card_col_exists = 0,
  'ALTER TABLE user_account ADD COLUMN id_card_no VARCHAR(32) NULL AFTER full_name',
  'SELECT 1'
);
PREPARE user_id_card_stmt FROM @user_id_card_alter_sql;
EXECUTE user_id_card_stmt;
DEALLOCATE PREPARE user_id_card_stmt;

SET @user_oauth_provider_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND COLUMN_NAME = 'oauth_provider'
);
SET @user_oauth_provider_alter_sql := IF(
  @user_oauth_provider_col_exists = 0,
  'ALTER TABLE user_account ADD COLUMN oauth_provider VARCHAR(32) NULL AFTER id_card_no',
  'SELECT 1'
);
PREPARE user_oauth_provider_stmt FROM @user_oauth_provider_alter_sql;
EXECUTE user_oauth_provider_stmt;
DEALLOCATE PREPARE user_oauth_provider_stmt;

SET @user_oauth_openid_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND COLUMN_NAME = 'oauth_open_id'
);
SET @user_oauth_openid_alter_sql := IF(
  @user_oauth_openid_col_exists = 0,
  'ALTER TABLE user_account ADD COLUMN oauth_open_id VARCHAR(128) NULL AFTER oauth_provider',
  'SELECT 1'
);
PREPARE user_oauth_openid_stmt FROM @user_oauth_openid_alter_sql;
EXECUTE user_oauth_openid_stmt;
DEALLOCATE PREPARE user_oauth_openid_stmt;

SET @user_oauth_key_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_account'
    AND INDEX_NAME = 'uk_oauth'
);
SET @user_oauth_key_alter_sql := IF(
  @user_oauth_key_exists = 0,
  'ALTER TABLE user_account ADD UNIQUE KEY uk_oauth (oauth_provider, oauth_open_id)',
  'SELECT 1'
);
PREPARE user_oauth_key_stmt FROM @user_oauth_key_alter_sql;
EXECUTE user_oauth_key_stmt;
DEALLOCATE PREPARE user_oauth_key_stmt;

CREATE TABLE IF NOT EXISTS timeslot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_slot_scenic (scenic_id),
  CONSTRAINT fk_slot_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS refund_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  free_refund_hours INT NOT NULL DEFAULT 24,
  allow_reschedule TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_rule_scenic (scenic_id),
  CONSTRAINT fk_rule_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  image_url VARCHAR(512) NULL,
  ticket_type VARCHAR(32) NOT NULL,
  price_cent INT NOT NULL,
  stock_qty INT NOT NULL DEFAULT 0,
  morning_enabled TINYINT NOT NULL DEFAULT 1,
  afternoon_enabled TINYINT NOT NULL DEFAULT 1,
  valid_from DATE NULL,
  valid_to DATE NULL,
  refund_rule_id BIGINT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ticket_scenic (scenic_id),
  KEY idx_ticket_status (status),
  CONSTRAINT fk_ticket_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id),
  CONSTRAINT fk_ticket_rule FOREIGN KEY (refund_rule_id) REFERENCES refund_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ticket_project (ticket_id, project_id),
  KEY idx_tp_ticket (ticket_id),
  KEY idx_tp_project (project_id),
  CONSTRAINT fk_tp_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
  CONSTRAINT fk_tp_project FOREIGN KEY (project_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ticket_image_col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket'
    AND COLUMN_NAME = 'image_url'
);
SET @ticket_image_alter_sql := IF(
  @ticket_image_col_exists = 0,
  'ALTER TABLE ticket ADD COLUMN image_url VARCHAR(512) NULL AFTER name',
  'SELECT 1'
);
PREPARE ticket_stmt FROM @ticket_image_alter_sql;
EXECUTE ticket_stmt;
DEALLOCATE PREPARE ticket_stmt;

SET @col_exists_stock_qty = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket'
    AND COLUMN_NAME = 'stock_qty'
);
SET @ddl_add_stock_qty = IF(@col_exists_stock_qty = 0,
  'ALTER TABLE ticket ADD COLUMN stock_qty INT NOT NULL DEFAULT 0 AFTER price_cent',
  'SELECT 1');
PREPARE stmt_add_stock_qty FROM @ddl_add_stock_qty;
EXECUTE stmt_add_stock_qty;
DEALLOCATE PREPARE stmt_add_stock_qty;

SET @col_exists_morning_enabled = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket'
    AND COLUMN_NAME = 'morning_enabled'
);
SET @ddl_add_morning_enabled = IF(@col_exists_morning_enabled = 0,
  'ALTER TABLE ticket ADD COLUMN morning_enabled TINYINT NOT NULL DEFAULT 1 AFTER stock_qty',
  'SELECT 1');
PREPARE stmt_add_morning_enabled FROM @ddl_add_morning_enabled;
EXECUTE stmt_add_morning_enabled;
DEALLOCATE PREPARE stmt_add_morning_enabled;

SET @col_exists_afternoon_enabled = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket'
    AND COLUMN_NAME = 'afternoon_enabled'
);
SET @ddl_add_afternoon_enabled = IF(@col_exists_afternoon_enabled = 0,
  'ALTER TABLE ticket ADD COLUMN afternoon_enabled TINYINT NOT NULL DEFAULT 1 AFTER morning_enabled',
  'SELECT 1');
PREPARE stmt_add_afternoon_enabled FROM @ddl_add_afternoon_enabled;
EXECUTE stmt_add_afternoon_enabled;
DEALLOCATE PREPARE stmt_add_afternoon_enabled;
CREATE TABLE IF NOT EXISTS ticket_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  visit_date DATE NOT NULL,
  timeslot_id BIGINT NOT NULL,
  total_qty INT NOT NULL,
  sold_qty INT NOT NULL DEFAULT 0,
  locked_qty INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inv (ticket_id, visit_date, timeslot_id),
  CONSTRAINT fk_inv_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
  CONSTRAINT fk_inv_slot FOREIGN KEY (timeslot_id) REFERENCES timeslot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  scenic_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  visit_date DATE NOT NULL,
  timeslot_id BIGINT NOT NULL,
  total_amount_cent INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  close_reason VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user (user_id, created_at),
  KEY idx_order_scenic (scenic_id, created_at),
  CONSTRAINT fk_order_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_order_slot FOREIGN KEY (timeslot_id) REFERENCES timeslot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  ticket_name VARCHAR(128) NOT NULL,
  unit_price_cent INT NOT NULL,
  qty INT NOT NULL,
  amount_cent INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_item_order (order_id),
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
  CONSTRAINT fk_item_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pay_no VARCHAR(64) NOT NULL,
  channel VARCHAR(16) NOT NULL DEFAULT 'VIRTUAL',
  status VARCHAR(16) NOT NULL,
  amount_cent INT NOT NULL,
  gateway_trade_no VARCHAR(128) NULL,
  mock_payload JSON NULL,
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_pay_order (order_id),
  CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES ticket_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  qr_code VARCHAR(255) NOT NULL,
  verify_code VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'UNUSED',
  used_at DATETIME NULL,
  verify_method VARCHAR(16) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_qr (qr_code),
  UNIQUE KEY uk_verify_code (verify_code),
  KEY idx_ot_order (order_id),
  CONSTRAINT fk_ot_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
  CONSTRAINT fk_ot_item FOREIGN KEY (order_item_id) REFERENCES ticket_order_item(id),
  CONSTRAINT fk_ot_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aftersale_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  req_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  req_type VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NULL,
  status VARCHAR(16) NOT NULL,
  auditor_id BIGINT NULL,
  audit_comment VARCHAR(255) NULL,
  target_visit_date DATE NULL,
  target_timeslot_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_req_no (req_no),
  KEY idx_ar_order (order_id),
  CONSTRAINT fk_ar_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
  CONSTRAINT fk_ar_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_ar_auditor FOREIGN KEY (auditor_id) REFERENCES user_account(id),
  CONSTRAINT fk_ar_target_slot FOREIGN KEY (target_timeslot_id) REFERENCES timeslot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS refund_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  amount_cent INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  finished_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_refund_order (order_id),
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES ticket_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_minute (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  stat_minute DATETIME NOT NULL,
  in_count INT NOT NULL DEFAULT 0,
  out_count INT NOT NULL DEFAULT 0,
  in_park_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flow_minute (scenic_id, stat_minute),
  KEY idx_flow_time (stat_minute),
  CONSTRAINT fk_flow_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_area_minute (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  area_code VARCHAR(64) NOT NULL,
  stat_minute DATETIME NOT NULL,
  crowd_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flow_area (scenic_id, area_code, stat_minute),
  KEY idx_fam_time (stat_minute),
  CONSTRAINT fk_fam_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flow_threshold (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  threshold_type VARCHAR(16) NOT NULL,
  area_code VARCHAR(64) NULL,
  value INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ft_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS video_analysis_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scenic_id BIGINT NOT NULL,
  video_path VARCHAR(512) NOT NULL,
  area_code VARCHAR(64) NULL,
  direction VARCHAR(8) NOT NULL DEFAULT 'ENTER',
  sample_ms INT NOT NULL DEFAULT 1000,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  error_msg VARCHAR(512) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_vjob_scenic_status (scenic_id, status),
  CONSTRAINT fk_vjob_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id),
  CONSTRAINT fk_vjob_user FOREIGN KEY (created_by) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS video_people_count (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  scenic_id BIGINT NOT NULL,
  area_code VARCHAR(64) NULL,
  stat_time DATETIME NOT NULL,
  people_count INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_vpc_job_time (job_id, stat_time),
  KEY idx_vpc_scenic_time (scenic_id, stat_time),
  CONSTRAINT fk_vpc_job FOREIGN KEY (job_id) REFERENCES video_analysis_job(id),
  CONSTRAINT fk_vpc_scenic FOREIGN KEY (scenic_id) REFERENCES scenic_area(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(16) NULL,
  to_status VARCHAR(16) NOT NULL,
  operator_type VARCHAR(16) NOT NULL,
  operator_id BIGINT NULL,
  detail_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_osl_order_time (order_id, created_at),
  CONSTRAINT fk_osl_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
  CONSTRAINT fk_osl_operator FOREIGN KEY (operator_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== 补全字段：门票描述/入园须知 =====================
SET @col_desc_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'description'
);
SET @ddl_add_desc = IF(@col_desc_exists = 0,
  'ALTER TABLE ticket ADD COLUMN description TEXT NULL AFTER image_url',
  'SELECT 1');
PREPARE stmt_add_desc FROM @ddl_add_desc;
EXECUTE stmt_add_desc;
DEALLOCATE PREPARE stmt_add_desc;

-- ===================== 操作审计日志表 =====================
CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NULL,
  operator_role VARCHAR(16) NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NULL,
  target_id VARCHAR(128) NULL,
  detail TEXT NULL,
  ip VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_op (operator_id, created_at),
  KEY idx_audit_module (module, action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== 站内通知表 =====================
CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receiver_id BIGINT NULL COMMENT 'NULL 表示系统广播',
  title VARCHAR(128) NOT NULL,
  content TEXT NULL,
  ntype VARCHAR(32) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARNING/SUCCESS',
  is_read TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notif_receiver (receiver_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== 补全字段：视频任务方向 =====================
SET @vjob_dir_col_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'video_analysis_job' AND COLUMN_NAME = 'direction'
);
SET @vjob_dir_alter_sql := IF(
  @vjob_dir_col_exists = 0,
  'ALTER TABLE video_analysis_job ADD COLUMN direction VARCHAR(8) NOT NULL DEFAULT ''ENTER'' AFTER area_code',
  'SELECT 1'
);
PREPARE vjob_dir_stmt FROM @vjob_dir_alter_sql;
EXECUTE vjob_dir_stmt;
DEALLOCATE PREPARE vjob_dir_stmt;

SET FOREIGN_KEY_CHECKS = 1;
