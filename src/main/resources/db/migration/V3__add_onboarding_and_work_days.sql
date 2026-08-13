alter table users add column profile_image_url varchar(500);
alter table users add column email_verified boolean not null default true;
alter table users add column terms_agreed_at timestamp with time zone not null default current_timestamp;
alter table users add column terms_version varchar(20) not null default '2026-08-13';
alter table users add column onboarding_step varchar(30) not null default 'PROFILE';

create table user_work_days (
    user_id uuid not null references users(id) on delete cascade,
    day_of_week varchar(10) not null,
    primary key (user_id, day_of_week)
);

insert into user_work_days(user_id, day_of_week)
select id, day_name
from users
cross join (values ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) days(day_name);
