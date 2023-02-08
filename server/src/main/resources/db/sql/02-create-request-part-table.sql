CREATE TABLE request_parts (
    id,
    request_id,
    created_at,
    sent_at,
    format,
    content,
    nonce,
    send_failure_at,
    send_failure_count
) AS
    SELECT
        id,
        id,
        created_at,
        sent_at,
        format,
        to_translate,
        to_translate_nonce,
        send_failure_at,
        send_failure_count
    FROM requests;

ALTER TABLE requests
DROP COLUMN sent_at,
DROP COLUMN format,
DROP COLUMN to_translate_nonce,
DROP COLUMN to_translate,
DROP COLUMN send_failure_at,
DROP COLUMN send_failure_count;

ALTER TABLE responses
RENAME COLUMN id TO part_id;
