# --- !Ups

drop table site_tags;

# --- !Downs

create table site_tags(
    id integer primary key,
    site_id integer not null,
    tag varchar(255) not null
);
