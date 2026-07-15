-- V4: refresh tokens for the JWT auth flow

CREATE TABLE refresh_tokens
(
    id          UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    token       VARCHAR(512)             NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_on_user FOREIGN KEY (user_id) REFERENCES users (id);
