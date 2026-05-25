package com.bitpub.tournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;

    @Column(nullable = false)
    private String status; // DRAFT, REGISTRATION, ONGOING, FINISHED

    @Column(name = "max_participants")
    private int maxParticipants;

    @Column(name = "team_size")
    private int teamSize; // 1 for individuals, >1 for teams

    @ElementCollection
    @CollectionTable(name = "tournament_locations", joinColumns = @JoinColumn(name = "tournament_id"))
    @Column(name = "location_id")
    private List<String> locationIds = new ArrayList<>();

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentMatch> matches = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaderboardEntry> leaderboard = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
