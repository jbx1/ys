CREATE TABLE "user" (
    id serial, -- the internal ID of the user, useful for compact foreign key references
    user_id varchar NOT NULL, -- the external unique id of the user, generated from outside the system
    created_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX user_id_idx ON "user"(user_id);

COMMENT ON COLUMN "user".user_id IS 'The external unique ID of the user, known to the outside world.';
COMMENT ON COLUMN "user".id IS 'The unique internal ID of the user.';

CREATE TYPE accreditation_type AS ENUM ('BY_INCOME','BY_NET_WORTH');
CREATE TYPE accreditation_status AS ENUM('PENDING', 'CONFIRMED', 'EXPIRED', 'FAILED');

CREATE TABLE accreditation(
    id UUID NOT NULL default gen_random_uuid(),
    user_id int NOT NULL REFERENCES "user"(id),
    type accreditation_type NOT NULL,
    document_name varchar NOT NULL,
    document_mime_type varchar NOT NULL default 'application/pdf',
    document_content varchar NOT NULL,
    status accreditation_status NOT NULL,
    created_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_ts timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX accreditation_user_id_idx ON accreditation(user_id);
CREATE INDEX accreditation_type_idx ON accreditation(type);
CREATE INDEX accreditation_status_idx ON accreditation(status);
