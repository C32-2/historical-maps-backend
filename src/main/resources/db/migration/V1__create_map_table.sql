CREATE TABLE IF NOT EXISTS map(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    pmtiles_path TEXT NOT NULL,
    preview_path TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_map_created_at ON map(created_at);