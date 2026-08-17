create table workspaces (
    id uuid primary key,
    name varchar(80) not null,
    organization_domain varchar(253),
    created_by uuid not null references users(id),
    deleted_at timestamp with time zone,
    deleted_by uuid references users(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table workspace_members (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    user_id uuid not null references users(id),
    membership_role varchar(20) not null,
    work_context_overridden boolean not null default false,
    time_zone_id varchar(35),
    work_start time,
    work_end time,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_workspace_member unique (workspace_id, user_id),
    constraint ck_workspace_member_role check (membership_role in ('OWNER', 'MEMBER')),
    constraint ck_workspace_member_work_context check (
        work_context_overridden = false
        or (time_zone_id is not null and work_start is not null and work_end is not null)
    )
);

create table workspace_member_work_days (
    workspace_member_id uuid not null references workspace_members(id) on delete cascade,
    day_of_week varchar(10) not null,
    primary key (workspace_member_id, day_of_week)
);

create index idx_workspace_members_user on workspace_members(user_id, workspace_id);
create index idx_workspace_members_workspace on workspace_members(workspace_id, membership_role);
create index idx_workspaces_active_updated on workspaces(deleted_at, updated_at desc);
