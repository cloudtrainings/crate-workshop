CREATE TABLE IF NOT EXISTS web_apache (
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
                agent string ,
                ipinfo_hostname string,
                ipinfo_city string,
                ipinfo_country string,
                ipinfo_region string,
                ipinfo_loc geo_point,
                geolocation geo_point
)
WITH (
    number_of_replicas = '0-all',
    refresh_interval = 1000
)
