package swerchansky.films.recyclers


import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import swerchansky.database.FilmsDatabase
import swerchansky.films.ConstantValues.DELETE_FAVOURITE_FILM_REQUEST
import swerchansky.films.ConstantValues.FILM_FAVOURITE_GET_DETAILS
import swerchansky.films.FilmDetailsActivity
import swerchansky.films.MainActivity
import swerchansky.films.R
import swerchansky.service.entity.FilmEntity


class FavouriteFilmsAdapter(private val context: Context, val films: List<FilmEntity>) :
   RecyclerView.Adapter<RecyclerView.ViewHolder>() {
   private val scope = CoroutineScope(Dispatchers.IO)
   private val filmsDatabase by lazy {
      FilmsDatabase.getDatabase(context).filmsDAO()
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      return ViewHolder(
         LayoutInflater.from(context).inflate(R.layout.film_card, parent, false)
      )
   }

   override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      val film = films[position]
      val viewHolder = holder as ViewHolder
      viewHolder.filmName.text = film.nameRu
      viewHolder.filmAdditional.text = context.resources.getString(
         R.string.film_additional,
         film.genres.first().genre,
         film.year ?: "..."
      )
      viewHolder.filmPoster.setImageBitmap(film.posterImagePreview)
      viewHolder.filmCard.setOnClickListener {
         val intent = Intent(context, FilmDetailsActivity::class.java)
         intent.putExtra("filmId", film.filmId)
         intent.putExtra("type", FILM_FAVOURITE_GET_DETAILS)
         context.startActivity(intent)
      }
      viewHolder.filmCard.setOnLongClickListener {
         val intent = Intent(MainActivity.TAG)
         intent.putExtra("type", DELETE_FAVOURITE_FILM_REQUEST)
         intent.putExtra("filmId", film.filmId)
         LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
         return@setOnLongClickListener true
      }
      viewHolder.filmCard.setCardBackgroundColor(context.getColor(R.color.light_blue_white))

   }

   override fun getItemCount() = films.size

   class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val filmPoster: ShapeableImageView = view.findViewById(R.id.filmPoster)
      val filmName: TextView = view.findViewById(R.id.filmName)
      val filmAdditional: TextView = view.findViewById(R.id.filmAdditional)
      val filmCard: CardView = view.findViewById(R.id.filmCard)
   }

}