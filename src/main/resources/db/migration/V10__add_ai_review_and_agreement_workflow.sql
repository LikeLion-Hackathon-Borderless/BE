create table ai_reviews (
    id uuid primary key,
    conversation_id uuid not null references conversations(id),
    creator_id uuid not null references users(id),
    status varchar(20) not null,
    original_content varchar(4000) not null,
    source_language varchar(10),
    recipient_language varchar(10),
    translated_content varchar(4000),
    ai_task varchar(1000),
    ai_task_confidence varchar(20) not null,
    ai_deadline timestamp with time zone,
    ai_deadline_confidence varchar(20) not null,
    ai_expected_outcome varchar(1000),
    ai_expected_outcome_confidence varchar(20) not null,
    final_task varchar(1000),
    final_assignee_user_id uuid references users(id),
    final_deadline timestamp with time zone,
    final_expected_outcome varchar(1000),
    provider varchar(30) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table ai_review_attachments (
    ai_review_id uuid not null references ai_reviews(id) on delete cascade,
    attachment_id uuid not null references attachments(id),
    primary key (ai_review_id, attachment_id)
);

create table ai_review_evidence (
    id uuid primary key,
    ai_review_id uuid not null references ai_reviews(id) on delete cascade,
    attachment_id uuid not null references attachments(id),
    locator varchar(255),
    excerpt varchar(1000),
    confidence varchar(20) not null,
    confirmed boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table messages add column ai_review_id uuid references ai_reviews(id);

create table understanding_cards (
    id uuid primary key,
    message_id uuid not null unique references messages(id),
    conversation_id uuid not null references conversations(id),
    ai_review_id uuid references ai_reviews(id),
    sender_id uuid not null references users(id),
    recipient_id uuid not null references users(id),
    card_state varchar(20) not null,
    revision_number integer not null,
    task varchar(1000),
    assignee_user_id uuid references users(id),
    deadline timestamp with time zone,
    expected_outcome varchar(1000),
    original_content varchar(4000) not null,
    translated_content varchar(4000),
    needs_clarification boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table understanding_card_revisions (
    id uuid primary key,
    card_id uuid not null references understanding_cards(id),
    revision_number integer not null,
    task varchar(1000),
    assignee_user_id uuid references users(id),
    deadline timestamp with time zone,
    expected_outcome varchar(1000),
    change_note varchar(1000),
    created_by uuid not null references users(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_card_revision unique (card_id, revision_number)
);

create table understanding_card_responses (
    id uuid primary key,
    card_id uuid not null references understanding_cards(id),
    revision_number integer not null,
    responder_id uuid not null references users(id),
    response_type varchar(40) not null,
    comment varchar(1000),
    proposed_deadline timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_card_response_revision unique (card_id, revision_number)
);

create table agreement_logs (
    id uuid primary key,
    conversation_id uuid not null references conversations(id),
    card_id uuid not null references understanding_cards(id),
    revision_number integer not null,
    agreement_status varchar(20) not null,
    task varchar(1000),
    deadline timestamp with time zone,
    expected_outcome varchar(1000),
    original_content varchar(4000) not null,
    agreed_by uuid references users(id),
    agreed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table agreement_log_file_references (
    id uuid primary key,
    agreement_log_id uuid not null references agreement_logs(id) on delete cascade,
    attachment_id uuid not null,
    file_name varchar(255) not null,
    locator varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_ai_reviews_conversation on ai_reviews(conversation_id, created_at desc);
create index idx_cards_conversation on understanding_cards(conversation_id, updated_at desc);
create index idx_agreement_logs_conversation on agreement_logs(conversation_id, created_at desc);
