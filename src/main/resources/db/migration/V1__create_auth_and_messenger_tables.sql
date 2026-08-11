create table users (
    id uuid primary key,
    email varchar(320) not null unique,
    password_hash varchar(100) not null,
    display_name varchar(50) not null,
    time_zone_id varchar(35) not null,
    preferred_language varchar(10) not null,
    work_start time not null,
    work_end time not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_users_work_time check (work_start < work_end)
);

create table conversations (
    id uuid primary key,
    type varchar(20) not null,
    direct_key varchar(73) unique,
    last_message_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_conversations_type check (type in ('DIRECT'))
);

create table conversation_members (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    last_read_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_conversation_member unique (conversation_id, user_id)
);

create table messages (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_id uuid not null references users(id),
    content varchar(4000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_members_user on conversation_members(user_id, conversation_id);
create index idx_messages_conversation_created on messages(conversation_id, created_at desc);
create index idx_messages_sender on messages(sender_id);
