create table if not exists `trader`
(
    uuid                    binary(16)  not null
        primary key,
    name                    varchar(16) null,
    receipt_serialized_item text        null
);
create table if not exists `order`
(
    id               int auto_increment
        primary key,
    seller           varchar(36)                         not null
        constraint order_trader_uuid_fk
            references trader,
    buyer            varchar(36)                         not null
        constraint order_trader_uuid_fk2
            references trader,
    timestamp        timestamp default CURRENT_TIMESTAMP not null,
    serialized_items text                                not null,
    offer            double                              not null,
    completed        tinyint   default 0                 not null,
    collected        tinyint   default 0                 not null,
    review           tinyint   default 0
);