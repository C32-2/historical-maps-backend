ALTER TABLE map_catalog ADD COLUMN storage_key VARCHAR(60);
UPDATE map_catalog
SET storage_key = 'maps/' || id::text || '/tiles.pmtiles'
WHERE storage_key IS NULL;
ALTER TABLE map_catalog ALTER COLUMN storage_key SET NOT NULL;
ALTER TABLE map_catalog ALTER COLUMN slug TYPE VARCHAR(60);
ALTER TABLE map_catalog ALTER COLUMN description TYPE VARCHAR(250);
ALTER TABLE map_catalog ALTER COLUMN title TYPE VARCHAR(100);
