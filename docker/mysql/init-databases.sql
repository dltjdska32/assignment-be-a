-- ===================================================================
-- 같은 MySQL 인스턴스 안에 테스트 전용 스키마만 추가
-- (앱용 assignment 는 docker-compose 의 MYSQL_DATABASE 로 이미 생성됨)
-- ===================================================================
CREATE DATABASE IF NOT EXISTS assignment_test
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- ===================================================================
-- 앱 DB(assignment) 카테고리 마스터 테이블 + 시드 데이터
-- Hibernate ddl-auto=update 와 충돌하지 않도록
-- 컬럼 타입/길이를 엔티티 매핑(ProductCategory + BaseEntity) 과 일치시킴.
-- ===================================================================
USE assignment;

CREATE TABLE IF NOT EXISTS product_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NULL,
    level       BIGINT       NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    is_deleted  BIT(1)       NOT NULL DEFAULT b'0',
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_category UNIQUE (category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- INSERT IGNORE 로 멱등성 확보 (이미 있으면 건너뜀)
INSERT IGNORE INTO product_category
    (id, parent_id, level, category, is_deleted, created_at, updated_at)
VALUES
    -- ────────── 최상위 카테고리 (level = 1) ──────────
    ( 1, NULL, 1, 'DEVELOPMENT', 0, NOW(6), NOW(6)),
    ( 2, NULL, 1, 'LANGUAGE',    0, NOW(6), NOW(6)),
    ( 3, NULL, 1, 'DESIGN',      0, NOW(6), NOW(6)),
    ( 4, NULL, 1, 'CERTIFICATE', 0, NOW(6), NOW(6)),
    ( 5, NULL, 1, 'UNIVERSITY',  0, NOW(6), NOW(6)),
    ( 6, NULL, 1, 'ETC',         0, NOW(6), NOW(6)),

    -- ────────── 개발 하위 (parent_id = 1) ──────────
    ( 7, 1, 2, 'WEB_DEVELOPMENT', 0, NOW(6), NOW(6)),
    ( 8, 1, 2, 'FRONTEND',        0, NOW(6), NOW(6)),
    ( 9, 1, 2, 'BACKEND',         0, NOW(6), NOW(6)),
    (10, 1, 2, 'FULLSTACK',       0, NOW(6), NOW(6)),
    (11, 1, 2, 'APP_DEVELOPMENT', 0, NOW(6), NOW(6)),
    (12, 1, 2, 'DATABASE',        0, NOW(6), NOW(6)),

    -- ────────── 외국어 하위 (parent_id = 2) ──────────
    (13, 2, 2, 'KOREAN',   0, NOW(6), NOW(6)),
    (14, 2, 2, 'POLISH',   0, NOW(6), NOW(6)),
    (15, 2, 2, 'ENGLISH',  0, NOW(6), NOW(6)),
    (16, 2, 2, 'JAPANESE', 0, NOW(6), NOW(6)),
    (17, 2, 2, 'GERMAN',   0, NOW(6), NOW(6)),
    (18, 2, 2, 'SPANISH',  0, NOW(6), NOW(6)),
    (19, 2, 2, 'CHINESE',  0, NOW(6), NOW(6)),

    -- ────────── 디자인 하위 (parent_id = 3) ──────────
    (20, 3, 2, 'CAD',            0, NOW(6), NOW(6)),
    (21, 3, 2, 'GRAPHIC_DESIGN', 0, NOW(6), NOW(6)),
    (22, 3, 2, 'PHOTO',          0, NOW(6), NOW(6)),
    (23, 3, 2, 'VIDEO',          0, NOW(6), NOW(6)),

    -- ────────── 자격증 하위 (parent_id = 4) ──────────
    (24, 4, 2, 'INFO_PROCESSING_ENGINEER', 0, NOW(6), NOW(6)),
    (25, 4, 2, 'INFO_SECURITY_ENGINEER',   0, NOW(6), NOW(6)),
    (26, 4, 2, 'SQLD',                     0, NOW(6), NOW(6)),
    (27, 4, 2, 'COMPUTER_LITERACY_LV1',    0, NOW(6), NOW(6)),
    (28, 4, 2, 'COMPUTER_LITERACY_LV2',    0, NOW(6), NOW(6)),
    (29, 4, 2, 'COMPUTER_LITERACY_LV3',    0, NOW(6), NOW(6)),

    -- ────────── 대학 교육 하위 (parent_id = 5) ──────────
    (30, 5, 2, 'MATH',            0, NOW(6), NOW(6)),
    (31, 5, 2, 'ENGINEERING',     0, NOW(6), NOW(6)),
    (32, 5, 2, 'NATURAL_SCIENCE', 0, NOW(6), NOW(6)),
    (33, 5, 2, 'EDUCATION',       0, NOW(6), NOW(6));
