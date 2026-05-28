CREATE TABLE tournaments (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    game_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tournament_participants (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    user_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    seed INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'REGISTERED'
);

CREATE TABLE tournament_matches (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    round INTEGER NOT NULL,
    match_index INTEGER NOT NULL,
    player_a_id UUID,
    player_b_id UUID,
    winner_id UUID,
    score_a INTEGER NOT NULL DEFAULT 0,
    score_b INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED'
);
