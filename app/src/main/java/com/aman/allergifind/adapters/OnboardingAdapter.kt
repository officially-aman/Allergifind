package com.aman.allergifind.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.aman.allergifind.HomeActivity
import com.aman.allergifind.R

data class OnboardingItem(val title: String, val description: String, val imageRes: Int)

class OnboardingAdapter(private val items: List<OnboardingItem>, private val context: Context) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_item_layout, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.onboardingImage)
        private val titleTextView: TextView = itemView.findViewById(R.id.onboardingTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.onboardingDescription)
        private val startButton: Button = itemView.findViewById(R.id.startButton)

        fun bind(item: OnboardingItem, position: Int) {
            imageView.setImageResource(item.imageRes)
            titleTextView.text = item.title
            descriptionTextView.text = item.description

            // Show the "Start" button only on the second item (position 1)
            if (position == 1) {
                startButton.visibility = View.VISIBLE
                startButton.setOnClickListener {
                    // Navigate to HomeActivity
                    val intent = Intent(context, HomeActivity::class.java)
                    context.startActivity(intent)
                    (context as? AppCompatActivity)?.finish()  // Close onboarding after navigating
                }
            } else {
                startButton.visibility = View.GONE
            }
        }
    }
}
