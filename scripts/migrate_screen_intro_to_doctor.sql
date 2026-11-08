-- =============================================================================
-- 将 screen_intro 数据合并至 doctor 表（按工号 asid = work_no 对齐）
-- 使用前请备份；验证通过后可手动 DROP TABLE screen_intro;
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 已存在 doctor：按工号补全展示字段（取 screen_intro 最新一条 create_date）
UPDATE doctor d
INNER JOIN (
    SELECT si.*
    FROM screen_intro si
    INNER JOIN (
        SELECT asid, MAX(COALESCE(create_date, '1970-01-01')) AS max_dt
        FROM screen_intro
        WHERE asid IS NOT NULL AND TRIM(asid) <> ''
        GROUP BY asid
    ) latest ON si.asid = latest.asid
        AND COALESCE(si.create_date, '1970-01-01') = latest.max_dt
) src ON d.work_no = src.asid
SET
    d.avatar = COALESCE(NULLIF(TRIM(src.intro1), ''), d.avatar),
    d.name = COALESCE(NULLIF(TRIM(src.intro2), ''), d.name),
    d.title = COALESCE(NULLIF(TRIM(src.intro3), ''), d.title),
    d.remarks = COALESCE(NULLIF(TRIM(src.remarks), ''), d.remarks),
    d.specialty = COALESCE(NULLIF(TRIM(src.specialty), ''), d.specialty),
    d.display_status = CASE WHEN src.status IN ('0', 0) THEN 0 ELSE 1 END;

-- 2) screen_intro 有工号但 doctor 不存在：插入新 doctor
INSERT INTO doctor (
    id, work_no, name, title, avatar, remarks, specialty,
    display_status, is_on_duty, create_date
)
SELECT
    REPLACE(UUID(), '-', '') AS id,
    TRIM(src.asid) AS work_no,
    NULLIF(TRIM(src.intro2), '') AS name,
    NULLIF(TRIM(src.intro3), '') AS title,
    NULLIF(TRIM(src.intro1), '') AS avatar,
    NULLIF(TRIM(src.remarks), '') AS remarks,
    NULLIF(TRIM(src.specialty), '') AS specialty,
    CASE WHEN src.status IN ('0', 0) THEN 0 ELSE 1 END AS display_status,
    1 AS is_on_duty,
    COALESCE(src.create_date, NOW()) AS create_date
FROM (
    SELECT si.*
    FROM screen_intro si
    INNER JOIN (
        SELECT asid, MAX(COALESCE(create_date, '1970-01-01')) AS max_dt
        FROM screen_intro
        WHERE asid IS NOT NULL AND TRIM(asid) <> ''
        GROUP BY asid
    ) latest ON si.asid = latest.asid
        AND COALESCE(si.create_date, '1970-01-01') = latest.max_dt
) src
LEFT JOIN doctor d ON d.work_no = TRIM(src.asid)
WHERE d.id IS NULL;

-- 3) 验证通过后可选执行：
-- DROP TABLE IF EXISTS screen_intro;
