CREATE TABLE player_stats (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    game_id UUID NOT NULL,
    total_matches INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    total_score INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,
    UNIQUE (user_id, game_id)
);

CREATE TABLE match_results (
    id UUID PRIMARY KEY,
    match_session_id UUID NOT NULL UNIQUE,
    game_id UUID NOT NULL,
    winner_user_id UUID,
    loser_user_id UUID,
    winner_score INTEGER NOT NULL DEFAULT 0,
    loser_score INTEGER NOT NULL DEFAULT 0,
    played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leaderboards (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0
);
