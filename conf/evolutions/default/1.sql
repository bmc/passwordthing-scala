# --- !Ups

create table appusers(
    id integer primary key asc,
    username varchar(30) not null unique,
    encrypted_password varchar(255) not null,
    is_admin boolean not null default false
);

create table sites(
    id integer primary key,
    user_id integer not null,
    name varchar(255) not null,
    username varchar(255),
    email varchar(255),
    url varchar(255),
    password varchar(255),
    notes varchar(4096)
);

create table site_tags(
    id integer primary key,
    site_id integer not null,
    tag varchar(255) not null
);

# --- !Downs

drop table site_tags;
drop table sites;
drop table users;
