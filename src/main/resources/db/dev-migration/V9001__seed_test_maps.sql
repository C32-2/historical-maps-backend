INSERT INTO map (
    id,
    slug,
    description,
    pmtiles_path,
    preview_path,
    created_at,
    updated_at
) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'moscow-1860',
    'Test map for local development',
    '/data/maps/moscow-1860.pmtiles',
    '/data/previews/moscow-1860.jpg',
    now(),
    now()
),
(
    '22222222-2222-2222-2222-222222222222',
    'spb-1910',
    'Second test map for Postman checks',
    '/data/maps/spb-1910.pmtiles',
    '/data/previews/spb-1910.jpg',
    now(),
    now()
)
ON CONFLICT (id) DO NOTHING;
