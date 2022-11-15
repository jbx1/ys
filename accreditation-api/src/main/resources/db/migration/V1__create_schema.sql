CREATE TABLE users (
    id serial, -- the internal ID of the user, useful for compact foreign key references
    user_id varchar NOT NULL, -- the external unique id of the user, generated from outside the system
    created_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX users_user_id_idx ON users(user_id);

COMMENT ON COLUMN users.user_id IS 'The external unique ID of the user, known to the outside world.';
COMMENT ON COLUMN users.id IS 'The unique internal ID of the user.';

CREATE TABLE accreditations(
    id UUID NOT NULL default gen_random_uuid(),
    user_id int NOT NULL REFERENCES users(id),
    type varchar NOT NULL,
    document_name varchar NOT NULL,
    document_mime_type varchar NOT NULL default 'application/pdf',
    document_content varchar NOT NULL,
    status varchar NOT NULL,
    created_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX accreditations_user_id_idx ON accreditations(user_id);
CREATE INDEX accreditations_type_idx ON accreditations(type);
CREATE INDEX accreditations_status_idx ON accreditations(status);
