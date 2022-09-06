--liquibase formatted sql

--changeset kefhir:drop-old-functions-2
drop function if exists extract_date;
drop function if exists extract_number;
drop function if exists extract_quantity;
drop function if exists extract_string;
drop function if exists extract_reference;
drop function if exists extract_uri;
drop function if exists merge_blindex;
drop function if exists save_resource;
drop function if exists delete_resource;
drop function if exists jsonpath1;
drop function if exists jsonpath2;
drop function if exists jsonpath3;
drop function if exists subpaths;
--
