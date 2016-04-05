CREATE TABLE IF NOT EXISTS web_apache_log (
                message string,
                host       string,
                clientip   string,
                ident       string,
                auth       string,
                timestamp       string,
                datetime timestamp,
                verb     string,
                request string,
                httpversion string,
                response string,
                bytes     string,
                referer    string,
                agent string 
)
WITH (
    number_of_replicas = '0-all',
    refresh_interval = 1000
);

CREATE TABLE IF NOT EXISTS web_apache_client_info (
                clientip   string,
                ipinfo_hostname string,
                ipinfo_city string,
                ipinfo_country string,
                ipinfo_region string,
                ipinfo_loc geo_point,
                geolocation geo_point,
                primary key (clientip)
)clustered by (clientip) into 6 shards
WITH (
    number_of_replicas = '0-all',
    refresh_interval = 1000
)
