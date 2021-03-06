package com.dicoding.finalsoccermatches.external.api

import com.dicoding.finalsoccermatches.domain.entity.*
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface SoccerService {
    @GET("api/v1/json/1/eventspastleague.php")
    fun getPastMatches(@Query("id") id: String): Deferred<MatchResponse>

    @GET("api/v1/json/1/eventsnextleague.php")
    fun getNextMatches(@Query("id") id: String): Deferred<MatchResponse>

    @GET("api/v1/json/1/lookupteam.php")
    fun getTeamDetails(@Query("id") id: String): Deferred<TeamResponse>

    @GET("api/v1/json/1/lookupevent.php")
    fun getMatchDetails(@Query("id") id: String): Deferred<MatchResponse>

    @GET("api/v1/json/1/all_leagues.php")
    fun getAllLeagues(): Deferred<LeagueResponse>

    @GET("api/v1/json/1/searchevents.php")
    fun getMatchByKeyword(@Query("e") keyword: String): Deferred<MatchSearchResponse>

    @GET("api/v1/json/1/lookup_all_teams.php")
    fun getTeams(@Query("id") leagueId: String): Deferred<TeamResponse>

    @GET("api/v1/json/1/searchteams.php")
    fun getTeamsByKeyword(@Query("t") keyword: String): Deferred<TeamResponse>

    @GET("api/v1/json/1/lookup_all_players.php")
    fun getPlayers(@Query("id") teamId: String): Deferred<PlayerResponse>

    @GET("api/v1/json/1/lookupplayer.php")
    fun getPlayerDetails(@Query("id") playerId: String): Deferred<PlayerDetailResponse>
}