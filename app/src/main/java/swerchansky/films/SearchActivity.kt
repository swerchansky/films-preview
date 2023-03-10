package swerchansky.films

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import swerchansky.films.ConstantValues.FAVOURITE_FILM_WAS_DELETED
import swerchansky.films.ConstantValues.FAVOURITE_SEARCH_TYPE
import swerchansky.films.ConstantValues.FILM_FAVOURITE_CHANGED
import swerchansky.films.ConstantValues.POPULAR_SEARCH_TYPE
import swerchansky.films.recyclers.FavouriteFilmsAdapter
import swerchansky.films.recyclers.TopFilmsAdapter
import swerchansky.service.FilmService
import swerchansky.service.entity.FilmEntity


class SearchActivity : AppCompatActivity() {
   companion object {
      const val TAG = "SearchActivity"
      const val FILM_SERVICE_TAG = "FilmService"
   }

   private lateinit var searchText: EditText
   private lateinit var recycler: RecyclerView
   private lateinit var shimmer: ShimmerFrameLayout
   private lateinit var backArrow: ImageButton
   private lateinit var notFoundButton: AppCompatButton
   private lateinit var filmServiceIntent: Intent

   private var isBound = false
   private var fullFilmList = mutableListOf<FilmEntity>()
   private var filteredFilmList = mutableListOf<FilmEntity>()
   var filmService: FilmService? = null

   private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
      @SuppressLint("NotifyDataSetChanged")
      override fun onReceive(context: Context?, intent: Intent) {
         when (intent.getIntExtra("type", -1)) {
            FILM_FAVOURITE_CHANGED -> {
               recycler.adapter?.notifyItemChanged(
                  (recycler.adapter as TopFilmsAdapter).films.indexOfFirst {
                     it.filmId == intent.getStringExtra("text")?.toInt()
                  }
               )
            }
            FAVOURITE_FILM_WAS_DELETED -> {
               val filmId = intent.getStringExtra("text")!!.toInt()
               val position = filteredFilmList.indexOfFirst { it.filmId == filmId }
               filteredFilmList.removeAt(position)
               recycler.adapter?.notifyItemRemoved(position)
               Handler(Looper.getMainLooper()).postDelayed({
                  recycler.adapter?.notifyDataSetChanged()
               }, 300)
               fullFilmList.removeAt(fullFilmList.indexOfFirst { it.filmId == filmId })
            }
         }
      }
   }

   private val boundServiceConnection: ServiceConnection = object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, service: IBinder) {
         val binderBridge = service as FilmService.MyBinder
         filmService = binderBridge.getService()
         when (intent.getIntExtra("type", -1)) {
            POPULAR_SEARCH_TYPE -> {
               fullFilmList = filmService!!.topFilms.toMutableList()
            }
            FAVOURITE_SEARCH_TYPE -> {
               filmService!!.getFavouritesFilms(wait = true)
               fullFilmList = filmService!!.favouritesFilms.toMutableList()
            }
         }
         isBound = true
      }

      override fun onServiceDisconnected(name: ComponentName) {
         isBound = false
         filmService = null
      }
   }


   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      window.setFlags(
         WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
         WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
      )
      setContentView(R.layout.activity_search)

      searchText = findViewById(R.id.searchText)
      recycler = findViewById(R.id.searchList)
      shimmer = findViewById(R.id.shimmerLayout)
      backArrow = findViewById(R.id.backArrow)
      notFoundButton = findViewById(R.id.notFoundButton)

      shimmer.stopShimmer()
      shimmer.visibility = View.GONE

      backArrow.setOnClickListener {
         finish()
      }

      searchText.addTextChangedListener {
         if (fullFilmList.isNotEmpty()) {
            shimmer.startShimmer()
            shimmer.visibility = View.VISIBLE
            filteredFilmList = fullFilmList.filter { film ->
               film.nameRu?.contains(searchText.text.toString(), true) ?: false
            }.toMutableList()
            recycler.apply {
               layoutManager = LinearLayoutManager(this@SearchActivity)
               adapter = when (intent.getIntExtra("type", -1)) {
                  POPULAR_SEARCH_TYPE -> TopFilmsAdapter(this@SearchActivity, filteredFilmList)
                  FAVOURITE_SEARCH_TYPE -> FavouriteFilmsAdapter(
                     this@SearchActivity,
                     filteredFilmList
                  )
                  else -> null
               }
            }
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
         }
         if (filteredFilmList.isEmpty() or searchText.text.isEmpty()) {
            notFoundButton.visibility = View.VISIBLE
            recycler.adapter = null
         } else {
            notFoundButton.visibility = View.GONE
         }
      }

      filmServiceIntent = Intent(this, FilmService::class.java)
      startService(filmServiceIntent)
      bindService(filmServiceIntent, boundServiceConnection, BIND_AUTO_CREATE)
      LocalBroadcastManager.getInstance(this)
         .registerReceiver(messageReceiver, IntentFilter(FILM_SERVICE_TAG))
   }

   override fun onDestroy() {
      super.onDestroy()
      if (isBound) {
         unbindService(boundServiceConnection)
      }
      LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
   }
}