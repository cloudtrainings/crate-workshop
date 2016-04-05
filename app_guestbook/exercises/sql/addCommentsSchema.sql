--comments table
DROP TABLE IF EXISTS guestbook.comments;
CREATE TABLE guestbook.comments (
    id STRING PRIMARY KEY,
    text STRING INDEX USING FULLTEXT WITH (analyzer = 'english'),
    created TIMESTAMP,
    post_id STRING
) WITH (number_of_replicas = '0-2');
