package com.dicoding.finalsoccermatches.presentation.search

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.dicoding.finalsoccermatches.BuildConfig
import com.dicoding.finalsoccermatches.R
import com.dicoding.finalsoccermatches.domain.data.MatchRepository
import com.dicoding.finalsoccermatches.domain.data.MatchRepositoryImpl
import com.dicoding.finalsoccermatches.external.api.MatchService
import com.dicoding.finalsoccermatches.presentation.detail.DetailActivity
import com.dicoding.finalsoccermatches.presentation.match.MatchAdapter
import com.dicoding.finalsoccermatches.presentation.match.MatchContract
import com.dicoding.finalsoccermatches.presentation.match.MatchPresenter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.android.synthetic.main.activity_match_search.*
import kotlinx.coroutines.experimental.*
import okhttp3.OkHttpClient
import org.jetbrains.anko.startActivity
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import kotlin.coroutines.experimental.CoroutineContext

class MatchSearchActivity : AppCompatActivity(), MatchContract.View,
    SwipeRefreshLayout.OnRefreshListener, CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private lateinit var adapter: MatchAdapter
    private lateinit var presenter: MatchContract.Presenter
    private lateinit var searchView: SearchView
    private var selectedItemId: Int = R.id.last_matches

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_search)
        initPresenter()
        initView()

        selectedItemId = intent.getIntExtra(getString(R.string.selected_item_id), R.id.last_matches)
    }

    private fun initPresenter() {
        if (this::presenter.isInitialized) return

        val objectMapper = ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerKotlinModule()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originRequest = chain.request()
                val originUrl = originRequest.url()

                val newUrl = originUrl.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TSDB_API_KEY)
                    .addQueryParameter("language", "en-US")
                    .build()
                val newRequest = originRequest.newBuilder()
                    .url(newUrl)
                    .build()

                chain.proceed(newRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

        val movieService = retrofit.create(MatchService::class.java)
        val repository: MatchRepository = MatchRepositoryImpl(movieService)

        presenter = MatchPresenter(repository, this)
    }

    private fun initView() {
        swipeRefresh.setOnRefreshListener(this)

        adapter = MatchAdapter { match ->
            startActivity<DetailActivity>(getString(R.string.event_id) to match.idEvent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        GlobalScope.launch {
            for (viewState in presenter.viewStates()) {
                runOnUiThread {
                    renderState(viewState)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun renderState(viewState: MatchContract.ViewState) {
        when (viewState) {
            is MatchContract.ViewState.LoadingState -> {
                swipeRefresh.isRefreshing = true
            }
            is MatchContract.ViewState.MatchResultState -> {
                swipeRefresh.isRefreshing = false
                adapter.submitList(viewState.matches)
            }
            is MatchContract.ViewState.ErrorState -> {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, viewState.error, Toast.LENGTH_SHORT).show()
                Log.e("error", viewState.error)
            }
        }
    }

    override fun onRefresh() {
        val query = searchView.query.toString().trim().replace(" ", "_")
        if (query.isNotEmpty()) {
            when (selectedItemId) {
                R.id.last_matches -> presenter.loadMatchesByKeyword(query)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.search_toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        if (searchItem != null) {
            searchView = searchItem.actionView as SearchView
            searchItem.expandActionView()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                onRefresh()
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        return super.onCreateOptionsMenu(menu)
    }
}