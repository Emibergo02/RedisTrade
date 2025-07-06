create table if not exists archived
(
    trade_uuid  varchar(36) not null primary key,
    trade_timestamp   timestamp default CURRENT_TIMESTAMP not null,

    trader_uuid varchar(36) not null,
    trader_name varchar(16) not null,
    trader_rating tinyint default 0 not null,
    trader_price text not null,

    customer_uuid varchar(36) not null,
    customer_name varchar(16) not null,
    customer_rating tinyint default 0 not null,
    customer_price text not null,

    trader_items text not null,
    customer_items text not null
);

create table if not exists backup
(
    trade_uuid  varchar(36)                         not null primary key,
    server_id    int                                 not null,
    timestamp   timestamp default CURRENT_TIMESTAMP not null,
    serialized  text                                not null
);

create table if not exists ignored_players
(
    ignorer varchar(16) not null,
    ignored varchar(16) not null,
    constraint ignored_players_pk unique (ignorer, ignored)
);

create table if not exists player_list
(
    player_name varchar(16) not null primary key,
    player_uuid varchar(36) not null
)