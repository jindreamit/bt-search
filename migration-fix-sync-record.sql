-- 修复 torrent_sync_record 表结构
-- 问题：表没有主键，导致每次同步都插入新记录，累积了 90 万+ 条记录
-- 解决：添加 id 主键列，清理历史数据，只保留一条记录

-- Step 1: 备份最新的一条记录（用于恢复）
CREATE TABLE IF NOT EXISTS torrent_sync_record_backup AS
SELECT max_synced_id, last_sync_time
FROM torrent_sync_record
ORDER BY last_sync_time DESC
LIMIT 1;

-- Step 2: 清空原表
TRUNCATE TABLE torrent_sync_record;

-- Step 3: 添加主键列
ALTER TABLE torrent_sync_record ADD COLUMN id BIGINT NOT NULL FIRST;
ALTER TABLE torrent_sync_record ADD PRIMARY KEY (id);

-- Step 4: 恢复数据，使用 id=1 作为固定主键
INSERT INTO torrent_sync_record (id, max_synced_id, last_sync_time)
SELECT 1, max_synced_id, last_sync_time
FROM torrent_sync_record_backup;

-- Step 5: 删除备份表
DROP TABLE IF EXISTS torrent_sync_record_backup;

-- 验证
SELECT * FROM torrent_sync_record;
