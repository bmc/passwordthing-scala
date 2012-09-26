# --- !Ups

alter table appusers add column first_name varchar(255);
alter table appusers add column last_name varchar(255);
alter table appusers add column email varchar(255);

# --- !Downs

create table temp(
    id integer primary key asc,
    username varchar(30) not null unique,
    encrypted_password varchar(255) not null,
    is_admin boolean not null default false
);
insert into temp select id, username, encrypted_password, is_admin from app_users;
drop table appusers;
create table appusers(
    id integer primary key asc,
    username varchar(30) not null unique,
    encrypted_password varchar(255) not null,
    is_admin boolean not null default false
);
insert into appusers select * from temp;
drop table temp;