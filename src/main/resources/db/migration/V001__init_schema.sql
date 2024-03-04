create table if not exists chain_event
(
    id            bigserial primary key,
    tx_id         varchar(255) not null unique,
    type          varchar(64)  not null,
    event_data    jsonb,
    date_time_txn timestamp,
    block_number  bigint,
    imported_at   timestamp,
    created_at    timestamp,
    updated_at    timestamp
);
create index if not exists chain_event_event_tx_id_index ON chain_event (tx_id);
create index if not exists chain_event_event_type_index ON chain_event (type);