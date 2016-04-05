CREATE TABLE IF NOT EXISTS workshop_weather (
    city            string,
    temp_low        integer,
    temp_high         integer,
    prcp            double,
    datetime        timestamp,
    primary key (city, datetime)
)clustered by (city) into 6 shards
WITH (
    number_of_replicas = '0-all',
    refresh_interval = 1000
);

CREATE TABLE IF NOT EXISTS workshop_cities (
    name            string,
    country         string, 
    location        geo_point,
    primary key (name, country)
)clustered by (name) into 6 shards
WITH (
    number_of_replicas = '0-all',
    refresh_interval = 1000
);