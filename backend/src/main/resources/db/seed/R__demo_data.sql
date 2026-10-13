-- Demo data (Step 6): two demo users and a few imported Pokémon, so the app can be tried right away.
-- Credentials are documented in the README. This is mock data for local use and demos only:
-- set FLYWAY_LOCATIONS=classpath:db/migration to start without it.
--
-- Repeatable and safe to re-run: rows that already exist (same username, email or PokeAPI id) are
-- left untouched, so it never overwrites data created by users.
-- The Pokémon data is what the import stores for each of them (taken from this API's own detail).

-- Passwords: ash / Pikachu123, misty / Togepi123 (BCrypt hashes).
INSERT INTO app_user (username, email, password_hash, role)
VALUES ('ash', 'ash@example.com', '$2a$10$AWjLh3lIzz0DS0nJAyHeqOuZa.xKpt9y7Otij47w8ea8YhhpwUKwO', 'USER'),
       ('misty', 'misty@example.com', '$2a$10$5ibGk7gLqlF2.O80zK05i.atjuG0cQVBIAlHB2dfk5YqduxgxHeHS', 'USER')
ON CONFLICT DO NOTHING;

-- bulbasaur (#1)
WITH p AS (
    INSERT INTO pokemon (pokeapi_id, name, sprite_url, image_url, category, height_m, weight_kg, description,
                         local_name, local_region)
    VALUES (1, 'bulbasaur', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png', 'Seed Pokémon', 0.7, 6.9,
            'While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.',
            'Bulbizarre', 'Pallet Town')
    ON CONFLICT (pokeapi_id) DO NOTHING
    RETURNING id
),
types AS (
    INSERT INTO pokemon_type (pokemon_id, sort_order, type)
    SELECT p.id, v.* FROM p, (VALUES (0, 'grass'),
           (1, 'poison')) AS v(sort_order, type)
),
abilities AS (
    INSERT INTO pokemon_ability (pokemon_id, sort_order, ability)
    SELECT p.id, v.* FROM p, (VALUES (0, 'overgrow'),
           (1, 'chlorophyll')) AS v(sort_order, ability)
),
stats AS (
    INSERT INTO pokemon_stat (pokemon_id, sort_order, name, base_value)
    SELECT p.id, v.* FROM p, (VALUES (0, 'hp', 45),
           (1, 'attack', 49),
           (2, 'defense', 49),
           (3, 'special-attack', 65),
           (4, 'special-defense', 65),
           (5, 'speed', 45)) AS v(sort_order, name, base_value)
),
evolution AS (
    INSERT INTO pokemon_evolution_stage (pokemon_id, sort_order, stage, species_id, name, evolves_from)
    SELECT p.id, v.* FROM p, (VALUES (0, 1, 1, 'bulbasaur', NULL::varchar),
           (1, 2, 2, 'ivysaur', 'bulbasaur'),
           (2, 3, 3, 'venusaur', 'ivysaur')) AS v(sort_order, stage, species_id, name, evolves_from)
),
tags AS (
    INSERT INTO pokemon_tag (pokemon_id, tag)
    SELECT p.id, v.* FROM p, (VALUES ('grass-starter'),
           ('kanto-starter')) AS v(tag)
)
SELECT id FROM p;

-- charmander (#4)
WITH p AS (
    INSERT INTO pokemon (pokeapi_id, name, sprite_url, image_url, category, height_m, weight_kg, description,
                         local_name, local_region)
    VALUES (4, 'charmander', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/4.png', 'Lizard Pokémon', 0.6, 8.5,
            'From the time it is born, a flame burns at the tip of its tail. Its life would end if the flame were to go out.',
            'Salamèche', 'Pallet Town')
    ON CONFLICT (pokeapi_id) DO NOTHING
    RETURNING id
),
types AS (
    INSERT INTO pokemon_type (pokemon_id, sort_order, type)
    SELECT p.id, v.* FROM p, (VALUES (0, 'fire')) AS v(sort_order, type)
),
abilities AS (
    INSERT INTO pokemon_ability (pokemon_id, sort_order, ability)
    SELECT p.id, v.* FROM p, (VALUES (0, 'blaze'),
           (1, 'solar-power')) AS v(sort_order, ability)
),
stats AS (
    INSERT INTO pokemon_stat (pokemon_id, sort_order, name, base_value)
    SELECT p.id, v.* FROM p, (VALUES (0, 'hp', 39),
           (1, 'attack', 52),
           (2, 'defense', 43),
           (3, 'special-attack', 60),
           (4, 'special-defense', 50),
           (5, 'speed', 65)) AS v(sort_order, name, base_value)
),
evolution AS (
    INSERT INTO pokemon_evolution_stage (pokemon_id, sort_order, stage, species_id, name, evolves_from)
    SELECT p.id, v.* FROM p, (VALUES (0, 1, 4, 'charmander', NULL::varchar),
           (1, 2, 5, 'charmeleon', 'charmander'),
           (2, 3, 6, 'charizard', 'charmeleon')) AS v(sort_order, stage, species_id, name, evolves_from)
),
tags AS (
    INSERT INTO pokemon_tag (pokemon_id, tag)
    SELECT p.id, v.* FROM p, (VALUES ('fire-starter'),
           ('kanto-starter')) AS v(tag)
)
SELECT id FROM p;

-- squirtle (#7)
WITH p AS (
    INSERT INTO pokemon (pokeapi_id, name, sprite_url, image_url, category, height_m, weight_kg, description,
                         local_name, local_region)
    VALUES (7, 'squirtle', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/7.png', 'Tiny Turtle Pokémon', 0.5, 9,
            'When it feels threatened, it draws its limbs inside its shell and sprays water from its mouth.',
            NULL, NULL)
    ON CONFLICT (pokeapi_id) DO NOTHING
    RETURNING id
),
types AS (
    INSERT INTO pokemon_type (pokemon_id, sort_order, type)
    SELECT p.id, v.* FROM p, (VALUES (0, 'water')) AS v(sort_order, type)
),
abilities AS (
    INSERT INTO pokemon_ability (pokemon_id, sort_order, ability)
    SELECT p.id, v.* FROM p, (VALUES (0, 'torrent'),
           (1, 'rain-dish')) AS v(sort_order, ability)
),
stats AS (
    INSERT INTO pokemon_stat (pokemon_id, sort_order, name, base_value)
    SELECT p.id, v.* FROM p, (VALUES (0, 'hp', 44),
           (1, 'attack', 48),
           (2, 'defense', 65),
           (3, 'special-attack', 50),
           (4, 'special-defense', 64),
           (5, 'speed', 43)) AS v(sort_order, name, base_value)
),
evolution AS (
    INSERT INTO pokemon_evolution_stage (pokemon_id, sort_order, stage, species_id, name, evolves_from)
    SELECT p.id, v.* FROM p, (VALUES (0, 1, 7, 'squirtle', NULL::varchar),
           (1, 2, 8, 'wartortle', 'squirtle'),
           (2, 3, 9, 'blastoise', 'wartortle')) AS v(sort_order, stage, species_id, name, evolves_from)
)
SELECT id FROM p;

-- pikachu (#25)
WITH p AS (
    INSERT INTO pokemon (pokeapi_id, name, sprite_url, image_url, category, height_m, weight_kg, description,
                         local_name, local_region)
    VALUES (25, 'pikachu', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png', 'Mouse Pokémon', 0.4, 6,
            'Possesses cheek sacs in which it stores electricity. This clever forest-dweller roasts tough berries with an electric shock before consuming them.',
            'Pikachu', 'Viridian Forest')
    ON CONFLICT (pokeapi_id) DO NOTHING
    RETURNING id
),
types AS (
    INSERT INTO pokemon_type (pokemon_id, sort_order, type)
    SELECT p.id, v.* FROM p, (VALUES (0, 'electric')) AS v(sort_order, type)
),
abilities AS (
    INSERT INTO pokemon_ability (pokemon_id, sort_order, ability)
    SELECT p.id, v.* FROM p, (VALUES (0, 'static'),
           (1, 'lightning-rod')) AS v(sort_order, ability)
),
stats AS (
    INSERT INTO pokemon_stat (pokemon_id, sort_order, name, base_value)
    SELECT p.id, v.* FROM p, (VALUES (0, 'hp', 35),
           (1, 'attack', 55),
           (2, 'defense', 40),
           (3, 'special-attack', 50),
           (4, 'special-defense', 50),
           (5, 'speed', 90)) AS v(sort_order, name, base_value)
),
evolution AS (
    INSERT INTO pokemon_evolution_stage (pokemon_id, sort_order, stage, species_id, name, evolves_from)
    SELECT p.id, v.* FROM p, (VALUES (0, 1, 172, 'pichu', NULL::varchar),
           (1, 2, 25, 'pikachu', 'pichu'),
           (2, 3, 26, 'raichu', 'pikachu')) AS v(sort_order, stage, species_id, name, evolves_from)
),
tags AS (
    INSERT INTO pokemon_tag (pokemon_id, tag)
    SELECT p.id, v.* FROM p, (VALUES ('electric'),
           ('fan-favorite')) AS v(tag)
)
SELECT id FROM p;

-- eevee (#133)
WITH p AS (
    INSERT INTO pokemon (pokeapi_id, name, sprite_url, image_url, category, height_m, weight_kg, description,
                         local_name, local_region)
    VALUES (133, 'eevee', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/133.png', 'Evolution Pokémon', 0.3, 6.5,
            'Harbors the potential to evolve into manifold forms. Within Eevee lies the key to the mysteries of Pokémon evolution—I''m certain of it.',
            NULL, NULL)
    ON CONFLICT (pokeapi_id) DO NOTHING
    RETURNING id
),
types AS (
    INSERT INTO pokemon_type (pokemon_id, sort_order, type)
    SELECT p.id, v.* FROM p, (VALUES (0, 'normal')) AS v(sort_order, type)
),
abilities AS (
    INSERT INTO pokemon_ability (pokemon_id, sort_order, ability)
    SELECT p.id, v.* FROM p, (VALUES (0, 'run-away'),
           (1, 'adaptability'),
           (2, 'anticipation')) AS v(sort_order, ability)
),
stats AS (
    INSERT INTO pokemon_stat (pokemon_id, sort_order, name, base_value)
    SELECT p.id, v.* FROM p, (VALUES (0, 'hp', 55),
           (1, 'attack', 55),
           (2, 'defense', 50),
           (3, 'special-attack', 45),
           (4, 'special-defense', 65),
           (5, 'speed', 55)) AS v(sort_order, name, base_value)
),
evolution AS (
    INSERT INTO pokemon_evolution_stage (pokemon_id, sort_order, stage, species_id, name, evolves_from)
    SELECT p.id, v.* FROM p, (VALUES (0, 1, 133, 'eevee', NULL::varchar),
           (1, 2, 134, 'vaporeon', 'eevee'),
           (2, 2, 135, 'jolteon', 'eevee'),
           (3, 2, 136, 'flareon', 'eevee'),
           (4, 2, 196, 'espeon', 'eevee'),
           (5, 2, 197, 'umbreon', 'eevee'),
           (6, 2, 470, 'leafeon', 'eevee'),
           (7, 2, 471, 'glaceon', 'eevee'),
           (8, 2, 700, 'sylveon', 'eevee')) AS v(sort_order, stage, species_id, name, evolves_from)
)
SELECT id FROM p;
