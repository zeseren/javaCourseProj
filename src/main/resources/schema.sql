create database campus_vote;

\c campus_vote;

create table if not exists users (
    id serial primary key,
    username varchar(50) not null unique,
    password varchar(255) not null,
    created_at timestamp not null default current_timestamp
);

create table if not exists vote_questions (
    id serial primary key,
    title varchar(200) not null,
    user_id integer not null references users(id),
    created_at timestamp not null default current_timestamp
);

create table if not exists vote_options (
    id serial primary key,
    question_id integer not null references vote_questions(id) on delete cascade,
    content varchar(200) not null,
    vote_count integer not null default 0
);
