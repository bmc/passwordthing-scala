# --- !Ups
create table user(
    id integer primary key asc,
    username varchar(30) not null,
    password varchar(30) not null
);

create table site(
    id integer primary key asc,
    user_id integer not null,
    name varchar(255) not null,
    url text,
    email text,
    password text,
    notes text
);

create table site_tags(
    id integer primary key asc,
    site_id integer not null,
    tag varchar(255) not null
);

# --- !Downs

drop table site_tags;
drop table site;
drop table user;
