package com.ernestoyaquello.dragdropswiperecyclerviewsample

//import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants.REDIRECT_URI
//import jdk.nashorn.internal.objects.NativeFunction.call
//import com.sun.corba.se.impl.activation.ServerMain.logError


import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import com.ernestoyaquello.dragdropswiperecyclerviewsample.config.local.ListFragmentType
import com.ernestoyaquello.dragdropswiperecyclerviewsample.config.local.currentListFragmentType
import com.ernestoyaquello.dragdropswiperecyclerviewsample.data.source.IceCreamRepository
import com.ernestoyaquello.dragdropswiperecyclerviewsample.data.source.SpotifyQueue
import com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelists.view.GridListFragment
import com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelists.view.HorizontalListFragment
import com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelists.view.VerticalListFragment
import com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelists.view.base.BaseListFragment
import com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelog.view.LogFragment
import com.ernestoyaquello.dragdropswiperecyclerviewsample.util.Logger
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;

import com.spotify.protocol.types.Item
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse

import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.SpotifyService
import kaaes.spotify.webapi.android.SpotifyCallback
import kaaes.spotify.webapi.android.SpotifyError
import kaaes.spotify.webapi.android.models.Album
import kaaes.spotify.webapi.android.models.Albums
import kaaes.spotify.webapi.android.models.SavedAlbum
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlayHistory
import kaaes.spotify.webapi.android.models.SavedTrack
import kaaes.spotify.webapi.android.models.TrackSimple
import kaaes.spotify.webapi.android.models.Track
import kaaes.spotify.webapi.android.models.Tracks
import retrofit.RetrofitError;
import retrofit.client.Response;

interface Echo {

    class Request : Item {
        val request: String?

        constructor(request: String) {
            this.request = request
        }

        /* For GSON */
        constructor() {
            request = null
        }
    }

    class Response : Item {
        val response: String?

        constructor(response: String) {
            this.response = response
        }

        /* For GSON */
        constructor() {
            response = null
        }
    }
}


/**
 * Main Activity of the app. Handles the navigation to the list sample screens and to the log screen.
 */
class MainActivity : AppCompatActivity() {

    private var logButtonTextView: TextView? = null
    private var logButtonLayout: FrameLayout? = null
    private var fab: FloatingActionButton? = null
    private var bottomNavigation: BottomNavigationView? = null

    private val CLIENT_ID = "57553798d4804700b5e86c71022fc507"
    private val REDIRECT_URI = "http://www.example.com/callback"
    private var mSpotifyAppRemote: SpotifyAppRemote? = null

    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private val REQUEST_CODE = 1337

    //private val mErrorCallback = { throwable -> logError(throwable, "Boom!") }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (tryNavigateToListFragment(item.itemId))
            return@OnNavigationItemSelectedListener true

        false
    }

    private val onLogButtonClickedListener = View.OnClickListener {
        navigateToLogFragment()
    }

    private val onLogUpdatedListener = object : Logger.OnLogUpdateListener {
        override fun onLogUpdated() = refreshLogButtonText()
    }

    private val onFabClickedListener = View.OnClickListener {
        // When in the log fragment, the FAB clears the log; when in a list fragment, it adds an item
        if (isLogFragmentOpen())
            Logger.reset()
/*
        else
            IceCreamRepository.getInstance().generateNewItem()
*/
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.elevation = 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            window.navigationBarColor = Color.BLACK

        setupLog()
        setupBottomNavigation()
        setupFab()
        refreshLogButtonText()
        navigateToListFragment()
        connectToSpotifyApp()
    }

    override fun onStart() {
        super.onStart()

        val builder = AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)

        builder.setScopes(arrayOf("streaming", "user-library-read", "user-read-recently-played"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            val response = AuthenticationClient.getResponse(resultCode, intent);

            when (response.getType()) {
                // Response was successful and contains auth token
                AuthenticationResponse.Type.TOKEN -> tryToken(response.accessToken)

                // Auth flow returned an error
                AuthenticationResponse.Type.ERROR -> Logger.log("got error")

                // Most likely auth flow was cancelled
                else -> Logger.log("got something else")
            }
        }
    }

    private fun tryToken(token: String) {
        val api = SpotifyApi()

        api.setAccessToken(token);

        val spotify = api.getService();

        val history = runBlocking(Dispatchers.IO) {
            spotify.getMyRecentlyPlayed(mapOf(SpotifyService.LIMIT to 50))
        }
        val queue = SpotifyQueue.getInstance()
        val trackIds = history.items.joinToString(separator = ",") { it.track.id }

        val trackList = runBlocking(Dispatchers.IO) {
            spotify.getTracks(trackIds)
        }

        val tracks = trackList.tracks
        val albumIdSet = mutableSetOf<String>()
        for (track in tracks) {
            albumIdSet.add(track.album.id)
        }
        val albumIds = albumIdSet.take(20).joinToString(separator = ",")
        val albumList = runBlocking(Dispatchers.IO) {
            spotify.getAlbums(albumIds)
        }

        val recentAlbum = albumList.albums[0]
        val albumTracks = runBlocking(Dispatchers.IO) {
            spotify.getAlbumTracks(recentAlbum.id)
        }
        for (albumTrack in albumTracks.items) {
            queue.addItem(albumTrack)
        }
/*
                spotify.getTracks(trackIds, object: SpotifyCallback<Tracks>() {
                    override fun success(trackList: Tracks, response: Response) {
                            override fun success(albumList: Albums, response: Response) {
                                val albums = albumList.albums
                                Logger.log("recent albums:")
                                for (album in albums) {
                                    Logger.log("  ${album.name}")
                                }
                            }

                            override fun failure(error: SpotifyError) {
                                Logger.log("getAlbums failure, ${error.toString()}")
                            }
                        })
                    }
                    override fun failure(error: SpotifyError) {
                        Logger.log("getTracks failure, ${error.toString()}")
                    }
                })
*/
    }
    private fun connected() {

    }

    private fun logError(throwable: Throwable, msg: String) {
        Toast.makeText(this, "Error: $msg", Toast.LENGTH_SHORT).show()
        //Log.e(FragmentActivity.TAG, msg, throwable)
        Logger.log("error: ${msg}")
    }

    private fun logMessage(msg: String) {
        logMessage(msg, Toast.LENGTH_SHORT)
    }

    private fun logMessage(msg: String, duration: Int) {
        Toast.makeText(this, msg, duration).show()
        Logger.log(msg)
    }

    private fun setupLog() {
        // Find log-related views
        logButtonLayout = findViewById(R.id.see_log_button)
        logButtonTextView = findViewById(R.id.see_log_button_text)

        // Initialise log and subscribe to log changes
        Logger.init(onLogUpdatedListener)

        // If the user clicks on the log button, we open the log fragment
        logButtonLayout?.setOnClickListener(onLogButtonClickedListener)
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.navigation)
        bottomNavigation?.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    private fun setupFab() {
        fab = findViewById(R.id.fab)
        fab?.setOnClickListener(onFabClickedListener)
    }

    private fun refreshLogButtonText() {
        val numItemsOnLog = Logger.instance?.messages?.size ?: 0
        logButtonTextView?.text = getString(R.string.seeLogMessagesTitle, numItemsOnLog)
    }

    private fun tryNavigateToListFragment(itemId: Int): Boolean {
        val listFragmentType: ListFragmentType? = when (itemId) {
            R.id.navigation_vertical_list -> ListFragmentType.VERTICAL
            R.id.navigation_horizontal_list -> ListFragmentType.HORIZONTAL
            R.id.navigation_grid_list -> ListFragmentType.GRID
            else -> null
        }

        if (listFragmentType != null && (listFragmentType != currentListFragmentType || isLogFragmentOpen())) {
            navigateToListFragment(listFragmentType)

            return true
        }

        return false
    }

    private fun navigateToListFragment(listFragmentType: ListFragmentType = currentListFragmentType) {
        currentListFragmentType = listFragmentType

        val fragment: BaseListFragment = when (listFragmentType) {
            ListFragmentType.VERTICAL -> VerticalListFragment.newInstance()
            ListFragmentType.HORIZONTAL -> HorizontalListFragment.newInstance()
            ListFragmentType.GRID -> GridListFragment.newInstance()
        }
        fragment.setSpotifyRemote(mSpotifyAppRemote!!)
        replaceFragment(fragment, listFragmentType.tag)
        onNavigatedToListFragment()
    }

    private fun navigateToLogFragment() {
        replaceFragment(LogFragment.newInstance(), LogFragment.TAG)
        onNavigatedToLogFragment()
    }

    private fun onNavigatedToListFragment() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        logButtonLayout?.visibility = View.VISIBLE
        fab?.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.ic_new_item))
    }

    private fun onNavigatedToLogFragment() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        logButtonLayout?.visibility = View.GONE
        fab?.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.ic_clear_items))
    }

    private fun isLogFragmentOpen() = supportFragmentManager.findFragmentByTag(LogFragment.TAG) != null

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.content_frame, fragment, tag)
        }.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                if (isLogFragmentOpen()) {
                    navigateToListFragment()

                    return true
                }

                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (isLogFragmentOpen())
            navigateToListFragment()
        else
            super.onBackPressed()
    }

    private fun connectToSpotifyApp() {
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build();
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {

            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Logger.log("failed connecting to Spotify App, ${throwable.message}")
            }
        })
    })

    }
}
