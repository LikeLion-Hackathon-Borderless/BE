create table oauth_login_codes (
    id uuid primary key,
    code uuid not null unique,
    user_id uuid not null references users(id) on delete cascade,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_oauth_login_codes_expiry on oauth_login_codes(expires_at);
