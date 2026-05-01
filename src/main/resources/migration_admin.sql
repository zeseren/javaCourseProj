-- 管理员功能数据库迁移脚本
-- 如果已有 campus_vote 数据库，运行此脚本添加新字段并创建管理员
-- 在 PostgreSQL 中执行：psql -U postgres -d campus_vote -f migration_admin.sql

-- 给 users 表添加 role 字段（默认 'user'，已有数据自动兼容）
alter table users add column if not exists role varchar(20) not null default 'user';

-- 给 vote_questions 表添加 status 字段（默认 'approved'，已有问卷自动变为已通过状态）
alter table vote_questions add column if not exists status varchar(20) not null default 'approved';

-- 创建默认管理员账号（密码 admin123，bcrypt 哈希值，仅在不存在时创建）
insert into users (username, password, role)
select 'admin', '$2a$10$w9D75CryqA5IbCI5cDe6bOhx17QVQmtv48bc6yB6CRrHyTgHIc2YC', 'admin'
where not exists (select 1 from users where username = 'admin');
