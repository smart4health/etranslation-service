CREATE TABLE requests
(
    id uuid NOT NULL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ NULL,
    from_lang VARCHAR NOT NULL,
    to_lang VARCHAR NOT NULL,
    format VARCHAR NOT NULL,
    original_nonce bytea NOT NULL,
    to_translate_nonce bytea NOT NULL,
    original bytea NOT NULL,
    to_translate bytea NOT NULL,
    send_failure_at TIMESTAMPTZ NULL,
    send_failure_count INT NOT NULL,
    CHECK ((sent_at IS NOT NULL AND send_failure_at IS NULL)
        OR (sent_at IS NULL))
);

CREATE TABLE responses
(
    id uuid NOT NULL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    translated_text bytea,
    translated_text_nonce bytea,
    to_lang VARCHAR,
    extras jsonb,
    CHECK ((translated_text IS NOT NULL AND to_lang IS NOT NULL AND translated_text_nonce IS NOT NULL AND extras IS NULL)
        OR (translated_text IS NULL AND to_lang IS NULL AND translated_text_nonce IS NULL AND extras IS NOT NULL))
);