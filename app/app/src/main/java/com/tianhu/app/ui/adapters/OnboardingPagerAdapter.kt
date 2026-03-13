package com.tianhu.app.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingPagerAdapter(private val context: Context) : RecyclerView.Adapter&lt;RecyclerView.ViewHolder&gt;() {

    companion object {
        private const val TYPE_FEATURE = 0
        private const val TYPE_TUTORIAL = 1
        private const val TYPE_PRIVACY = 2
    }

    private val pages = listOf(
        OnboardingPage(
            type = TYPE_FEATURE,
            imageRes = R.drawable.resource__apple,
            title = "智能果蔬识别",
            description = "使用 AI 技术，快速识别各种果蔬，轻松了解营养信息"
        ),
        OnboardingPage(
            type = TYPE_TUTORIAL,
            title = "使用教程",
            subtitle = "三步轻松识别果蔬"
        ),
        OnboardingPage(
            type = TYPE_PRIVACY,
            title = "隐私协议",
            subtitle = "请仔细阅读以下内容"
        )
    )

    override fun getItemViewType(position: Int): Int {
        return pages[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FEATURE -&gt; {
                val view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_feature, parent, false)
                FeatureViewHolder(view)
            }
            TYPE_TUTORIAL -&gt; {
                val view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_tutorial, parent, false)
                TutorialViewHolder(view)
            }
            TYPE_PRIVACY -&gt; {
                val view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_privacy, parent, false)
                PrivacyViewHolder(view)
            }
            else -&gt; throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val page = pages[position]
        when (holder) {
            is FeatureViewHolder -&gt; holder.bind(page)
            is TutorialViewHolder -&gt; holder.bind(page)
            is PrivacyViewHolder -&gt; holder.bind(page)
        }
    }

    override fun getItemCount(): Int = pages.size

    inner class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.pageImage)
        private val titleView: TextView = itemView.findViewById(R.id.pageTitle)
        private val descriptionView: TextView = itemView.findViewById(R.id.pageDescription)

        fun bind(page: OnboardingPage) {
            imageView.setImageResource(page.imageRes)
            titleView.text = page.title
            descriptionView.text = page.description
        }
    }

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.pageTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.pageSubtitle)

        fun bind(page: OnboardingPage) {
            titleView.text = page.title
            subtitleView.text = page.subtitle
        }
    }

    inner class PrivacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.pageTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.pageSubtitle)

        fun bind(page: OnboardingPage) {
            titleView.text = page.title
            subtitleView.text = page.subtitle
        }
    }

    data class OnboardingPage(
        val type: Int,
        val imageRes: Int = R.drawable.ic_home,
        val title: String = "",
        val description: String = "",
        val subtitle: String = ""
    )
}
