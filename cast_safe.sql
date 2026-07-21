CREATE OR REPLACE FUNCTION varchar_to_uuid(varchar) RETURNS uuid AS $$
    SELECT $1::text::uuid;
$$ LANGUAGE SQL IMMUTABLE STRICT;

CREATE CAST (varchar AS uuid) WITH FUNCTION varchar_to_uuid(varchar) AS IMPLICIT;
