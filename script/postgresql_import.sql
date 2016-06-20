\set ON_ERROR_STOP on

-- create temporary table for json content

CREATE TABLE IF NOT EXISTS devsearch_features (
	feature varchar,
	file varchar,
	line int,
	repoRank decimal
);


-- IMPORT DATA

create temporary table temp_json (values text);
copy temp_json from '/home/devsearch/bucket-data/postgres-json-format.json';

INSERT INTO devsearch_features ("feature", "file", "line", "reporank")
SELECT values->>'feature' AS feature,
       values->>'file' AS file,
       (((values->>'line')::json)->>'$numberInt')::int AS line,
       log((values->>'repoRank')::float + 1)/log(25566) AS reporank
FROM   (
           SELECT values::json AS values
           FROM   temp_json
       ) a;

DROP TABLE temp_json;

-- INDEX DATA

CREATE INDEX feature_index ON devsearch_features (feature);
CREATE INDEX feature_n_file_index ON devsearch_features (feature, file);


-- CREATE CLAMP FUNCTION FOR SCORING

CREATE OR REPLACE FUNCTION clamp(x bigint, min bigint, max bigint) RETURNS bigint AS $$
BEGIN
    IF x > max THEN
        RETURN max;
    END IF;
    IF x < min THEN
        RETURN min;
    END IF;
    RETURN x;
END;
$$
LANGUAGE 'plpgsql' COST 10;


-- NORMALIZE DATA

CREATE TABLE IF NOT EXISTS features (
    feature varchar,
    ID SERIAL PRIMARY KEY
);

INSERT into features (SELECT DISTINCT feature FROM devsearch_features);

CREATE TABLE IF NOT EXISTS files (
	file varchar,
	ID SERIAL PRIMARY KEY
);

INSERT into files (SELECT DISTINCT file FROM devsearch_features);


CREATE TABLE IF NOT EXISTS data  (
	feature serial REFERENCES features (ID),
	file serial REFERENCES files (ID),
	line int,
	repoRank decimal
);

INSERT INTO data (
	SELECT
		features.ID as feature,
		files.ID as file,
		line,
		repoRank
	FROM
		features,
		files,
		devsearch_features
	WHERE
		features.feature = devsearch_features.feature AND
		files.file = devsearch_features.file
);

CREATE INDEX features_index ON features (feature);
CREATE INDEX data_feat_n_file_index ON data (feature, file);
CREATE INDEX data_feat_index ON data (feature);