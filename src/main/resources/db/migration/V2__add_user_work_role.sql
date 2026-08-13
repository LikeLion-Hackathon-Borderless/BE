alter table users add column role varchar(30);
alter table users add column custom_role varchar(50);

alter table users add constraint ck_users_custom_role
    check (
        (role = 'OTHER' and custom_role is not null and length(trim(custom_role)) > 0)
        or (role <> 'OTHER' and custom_role is null)
        or role is null
    );
