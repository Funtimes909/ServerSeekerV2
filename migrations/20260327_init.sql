create table if not exists favicons (
    hash bytea primary key,
    data text not null,
    first_seen timestamp without time zone not null default now (),
    last_seen timestamp without time zone not null default now ()
);

create table if not exists countries (
    network inet,
    country text not null,
    country_code text not null,
    asn text,
    asn_name text,
    primary key (network)
);

create table if not exists servers (
    address inet,
    port integer,
    first_seen timestamp without time zone not null default now (),
    last_seen timestamp without time zone not null,
    last_time_player_online timestamp without time zone,
    last_time_no_players_online timestamp without time zone,
    version_protocol integer not null,
    version_name text not null,
    enforces_secure_chat boolean not null,
    previews_chat boolean not null,
    is_online_mode boolean not null,
    is_whitelisted boolean,
    favicon_hash bytea references favicons (hash),
    max_players integer,
    online_players integer,
    description_formatted text,
    description_raw jsonb,

    -- neoforge
    neoforge_is_modded boolean,

    -- forge
    fml_network_version integer,

    -- nochatreports
    prevents_chat_reports boolean,

    -- better compatibility checker
    bcc_modpack_projectid integer,
    bcc_modpack_version text,
    bcc_modpack_name text,

    primary key (address, port)
);

create table if not exists players (
        address inet,
        port integer,
        uuid uuid,
        is_online_mode boolean not null,
        username text not null,
        first_seen timestamp without time zone not null default now (),
        last_seen timestamp without time zone not null,
        primary key (address, port, uuid),
        foreign key (address, port) references servers (address, port) on delete cascade
);

create table if not exists mods (
        address inet,
        port integer,
        id text,
        version text,
        primary key (address, port, id),
        foreign key (address, port) references servers (address, port) on delete cascade
);

create index if not exists countries_index on countries using gist (network inet_ops);