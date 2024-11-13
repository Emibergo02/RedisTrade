create table if not exists `archived`
(
    trade_uuid  varchar(36) not null primary key,
    trader_uuid varchar(36) not null,
    target_uuid varchar(36) not null,
    timestamp   timestamp default CURRENT_TIMESTAMP not null,
    serialized  text        not null
);

create table if not exists `backup`
(
    trade_uuid  varchar(36)                         not null primary key,
    serialized  text                                not null
);

create table if not exists `ignored_players`
(
    ignorer varchar(16) not null,
    ignored varchar(16) not null,
    constraint ignored_players_pk unique (ignorer, ignored)
);

create table if not exists `player_list`
(
    player_name varchar(16) not null primary key,
    player_uuid varchar(36) not null
)