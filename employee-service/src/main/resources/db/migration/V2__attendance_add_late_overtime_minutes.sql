-- ===================================================================
-- V2__attendance_add_late_overtime_minutes.sql
-- Добавление колонок опозданий и переработок для attendance-service (R2DBC)
-- ===================================================================

ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS late_minutes BIGINT;

ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS overtime_minutes BIGINT;
