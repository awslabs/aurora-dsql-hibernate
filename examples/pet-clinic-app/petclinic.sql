drop table if exists owners cascade;
drop table if exists specialties cascade;
drop table if exists types cascade;
drop table if exists vet_specialties cascade;
drop table if exists vets cascade;
drop table if exists visits cascade;
drop table if exists pets cascade;
create table if not exists owners (id UUID DEFAULT gen_random_uuid() not null, address varchar(255) not null, city varchar(255) not null, first_name varchar(255) not null, last_name varchar(255) not null, telephone varchar(255) not null, primary key (id));
create table if not exists pets (birth_date date, id UUID DEFAULT gen_random_uuid() not null, owner_id UUID DEFAULT gen_random_uuid(), type_id UUID DEFAULT gen_random_uuid(), name varchar(255), primary key (id));
create table if not exists specialties (id UUID DEFAULT gen_random_uuid() not null, name varchar(255), primary key (id));
create table if not exists types (id UUID DEFAULT gen_random_uuid() not null, name varchar(255), primary key (id));
create table if not exists vet_specialties (specialty_id UUID DEFAULT gen_random_uuid() not null, vet_id UUID DEFAULT gen_random_uuid() not null, primary key (specialty_id, vet_id));
create table if not exists vets (id UUID DEFAULT gen_random_uuid() not null, first_name varchar(255) not null, last_name varchar(255) not null, primary key (id));
create table if not exists visits (visit_date date, id UUID DEFAULT gen_random_uuid() not null, pet_id UUID DEFAULT gen_random_uuid(), description varchar(255) not null, primary key (id));
