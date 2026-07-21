CREATE OR REPLACE FUNCTION varchar_to_uuid(varchar) RETURNS uuid AS $$
    SELECT CAST($1 AS uuid);
$$ LANGUAGE SQL STRICT;

CREATE CAST (varchar AS uuid) WITH FUNCTION varchar_to_uuid(varchar) AS IMPLICIT;
