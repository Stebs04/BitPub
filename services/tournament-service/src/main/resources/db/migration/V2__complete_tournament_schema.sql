ALTER TABLE tournaments
ADD COLUMN format VARCHAR(50) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
ADD COLUMN max_participants INTEGER NOT NULL DEFAULT 16,
ADD COLUMN team_size INTEGER NOT NULL DEFAULT 1;

CREATE TABLE tournament_locations (
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    location_id VARCHAR(255) NOT NULL
);

DROP TABLE tournament_participants;

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    seed INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'REGISTERED'
);

CREATE TABLE tournament_players (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL
);

ALTER TABLE tournament_matches
RENAME COLUMN player_a_id TO team_a_id;

ALTER TABLE tournament_matches
RENAME COLUMN player_b_id TO team_b_id;

ALTER TABLE tournament_matches
ADD COLUMN next_match_id UUID REFERENCES tournament_matches(id);

ALTER TABLE tournament_matches
ADD CONSTRAINT fk_match_team_a FOREIGN KEY (team_a_id) REFERENCES teams(id),
ADD CONSTRAINT fk_match_team_b FOREIGN KEY (team_b_id) REFERENCES teams(id),
ADD CONSTRAINT fk_match_winner FOREIGN KEY (winner_id) REFERENCES teams(id);

CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    points INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    goals_for INTEGER NOT NULL DEFAULT 0,
    goals_against INTEGER NOT NULL DEFAULT 0
);
