--liquibase formatted sql

--changeset fhirest-hashchain:hashchain_config_init
insert into store.hashchain_config(algorithm, version) select 'sha256', 'v1';
--

