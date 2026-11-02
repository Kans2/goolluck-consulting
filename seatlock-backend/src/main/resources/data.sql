-- ============================================================================
-- SeatLock — Seed Data: 20 seats
-- ============================================================================
-- Uses MERGE (H2) / INSERT … ON CONFLICT (PostgreSQL-compatible via H2 MODE=PostgreSQL).
-- This keeps the script idempotent across restarts.
-- ============================================================================

INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'A1', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'A1');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'A2', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'A2');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'A3', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'A3');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'A4', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'A4');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'A5', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'A5');

INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'B1', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'B1');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'B2', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'B2');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'B3', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'B3');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'B4', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'B4');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'B5', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'B5');

INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'C1', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'C1');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'C2', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'C2');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'C3', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'C3');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'C4', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'C4');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'C5', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'C5');

INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'D1', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'D1');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'D2', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'D2');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'D3', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'D3');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'D4', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'D4');
INSERT INTO seats (seat_number, status, version, updated_at)
SELECT 'D5', 'AVAILABLE', 0, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM seats WHERE seat_number = 'D5');
