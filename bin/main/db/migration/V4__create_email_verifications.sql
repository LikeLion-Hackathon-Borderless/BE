create table email_verifications (
    id uuid primary key,
    email varchar(320) not null,
    code_hash varchar(100) not null,
    expires_at timestamp with time zone not null,
    sent_at timestamp with time zone not null,
    verified_at timestamp with time zone,
    verification_token uuid unique,
    consumed_at timestamp with time zone,
    failed_attempts integer not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_email_verifications_email_created
    on email_verifications(email, created_at desc);
