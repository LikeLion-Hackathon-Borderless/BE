alter table ai_reviews add column agent_thread_id varchar(100);
alter table ai_reviews add column agent_session_status varchar(20);
alter table ai_reviews add column agent_interrupt_json varchar(4000);
alter table ai_reviews add column agent_card_json varchar(8000);

create unique index uk_ai_reviews_agent_thread on ai_reviews(agent_thread_id);
