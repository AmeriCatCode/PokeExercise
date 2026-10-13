-- Imported Pokémon (main entity, US-03 / US-04).
-- The PokeAPI data is copied once, at import, and never changed afterwards; only the proprietary
-- fields (local_name, local_region and the tags) are edited by users.
CREATE TABLE pokemon (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- The Pokémon's number in PokeAPI: unique, so a Pokémon can't be imported twice, even by two
    -- users at the same moment.
    pokeapi_id   INTEGER          NOT NULL,
    -- From PokeAPI (read-only)
    name         VARCHAR(100)     NOT NULL,
    sprite_url   VARCHAR(500),
    image_url    VARCHAR(500),
    category     VARCHAR(100)     NOT NULL,
    height_m     DOUBLE PRECISION NOT NULL,
    weight_kg    DOUBLE PRECISION NOT NULL,
    description  TEXT             NOT NULL,
    -- Proprietary (editable)
    local_name   VARCHAR(100),
    local_region VARCHAR(100),
    -- Control: version detects concurrent edits (optimistic locking)
    version      BIGINT           NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT uk_pokemon_pokeapi_id UNIQUE (pokeapi_id)
);

-- Lists copied from PokeAPI, in PokeAPI order (sort_order).
CREATE TABLE pokemon_type (
    pokemon_id BIGINT      NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    sort_order INTEGER     NOT NULL,
    type       VARCHAR(30) NOT NULL,
    PRIMARY KEY (pokemon_id, sort_order)
);

CREATE TABLE pokemon_ability (
    pokemon_id BIGINT       NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    sort_order INTEGER      NOT NULL,
    ability    VARCHAR(100) NOT NULL,
    PRIMARY KEY (pokemon_id, sort_order)
);

CREATE TABLE pokemon_stat (
    pokemon_id BIGINT      NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    sort_order INTEGER     NOT NULL,
    name       VARCHAR(50) NOT NULL,
    base_value INTEGER     NOT NULL,
    PRIMARY KEY (pokemon_id, sort_order)
);

CREATE TABLE pokemon_evolution_stage (
    pokemon_id   BIGINT       NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    sort_order   INTEGER      NOT NULL,
    stage        INTEGER      NOT NULL,
    species_id   INTEGER      NOT NULL,
    name         VARCHAR(100) NOT NULL,
    evolves_from VARCHAR(100),
    PRIMARY KEY (pokemon_id, sort_order)
);

-- Proprietary internal classification tags: several per Pokémon, each one only once.
CREATE TABLE pokemon_tag (
    pokemon_id BIGINT      NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    tag        VARCHAR(30) NOT NULL,
    PRIMARY KEY (pokemon_id, tag)
);
