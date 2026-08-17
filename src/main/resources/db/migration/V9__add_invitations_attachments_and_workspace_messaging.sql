create table workspace_invitations (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    inviter_id uuid not null references users(id),
    invited_email varchar(320),
    invitation_type varchar(20) not null,
    token_hash varchar(64) not null unique,
    status varchar(20) not null,
    expires_at timestamp with time zone not null,
    accepted_by uuid references users(id),
    accepted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_workspace_invitation_type check (invitation_type in ('EMAIL', 'LINK')),
    constraint ck_workspace_invitation_status check (status in ('PENDING', 'ACCEPTED', 'REVOKED'))
);

create index idx_workspace_invitations_workspace on workspace_invitations(workspace_id, status);
create index idx_workspace_invitations_email on workspace_invitations(workspace_id, invited_email, status);

alter table conversations add column workspace_id uuid references workspaces(id);
alter table conversations alter column direct_key type varchar(110);
create index idx_conversations_workspace on conversations(workspace_id, last_message_at desc);

alter table messages add column delivery_mode varchar(30) not null default 'AS_IS';
alter table messages add column delivery_status varchar(20) not null default 'SENT';
alter table messages add column confirmation_status varchar(20) not null default 'UNCONFIRMED';
alter table messages add column scheduled_for timestamp with time zone;

create table attachments (
    id uuid primary key,
    conversation_id uuid not null references conversations(id),
    uploader_id uuid not null references users(id),
    message_id uuid references messages(id),
    storage_key varchar(255) not null unique,
    original_file_name varchar(255) not null,
    content_type varchar(100) not null,
    size_bytes bigint not null,
    processing_status varchar(30) not null,
    extraction_error_code varchar(50),
    extracted_text text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_attachments_conversation on attachments(conversation_id, created_at);
create index idx_attachments_message on attachments(message_id);
