create table google_calendar_connections (
    id uuid primary key,
    user_id uuid not null unique references users(id) on delete cascade,
    oauth_state uuid not null unique,
    state_expires_at timestamp with time zone not null,
    encrypted_access_token text,
    encrypted_refresh_token text,
    access_token_expires_at timestamp with time zone,
    granted_scopes varchar(500),
    selected_calendar_id varchar(1024),
    connected_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
