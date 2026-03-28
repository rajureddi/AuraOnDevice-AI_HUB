package com.aura_on_device_ai.mnnllm.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.annotation.IdRes
import com.aura_on_device_ai.mnnllm.android.R

class BottomTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : com.google.android.material.card.MaterialCardView(context, attrs, defStyle) {

    enum class Tab(@IdRes val viewId: Int) {
        CHAT(R.id.tab_chat),
        AI_STUDIO(R.id.tab_ai_studio),
        MODEL_MARKET(R.id.tab_model_market)
    }

    private var listener: ((Tab) -> Unit)? = null

    private val tabChat: LinearLayout
    private val tabAiStudio: LinearLayout
    private val tabModelMarket: LinearLayout

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_bottom_tab_bar, this, true)

        tabChat          = findViewById(R.id.tab_chat)
        tabAiStudio      = findViewById(R.id.tab_ai_studio)
        tabModelMarket  = findViewById(R.id.tab_model_market)

        tabChat.setOnClickListener { select(Tab.CHAT) }
        tabAiStudio.setOnClickListener { select(Tab.AI_STUDIO) }
        tabModelMarket.setOnClickListener { select(Tab.MODEL_MARKET) }
        
        select(Tab.CHAT)
    }

    fun setOnTabSelectedListener(block: (Tab) -> Unit) {
        listener = block
    }

    fun getSelectedTab(): Tab {
        return when {
            tabChat.isSelected -> Tab.CHAT
            tabAiStudio.isSelected -> Tab.AI_STUDIO
            tabModelMarket.isSelected -> Tab.MODEL_MARKET
            else -> Tab.CHAT
        }
    }

    fun select(tab: Tab) {
        tabChat.isSelected        = (tab == Tab.CHAT)
        tabAiStudio.isSelected    = (tab == Tab.AI_STUDIO)
        tabModelMarket.isSelected = (tab == Tab.MODEL_MARKET)

        // Find and update icon/text selected states
        findView<ImageView>(R.id.icon_chat)?.isSelected = tabChat.isSelected
        findView<TextView>(R.id.text_chat)?.isSelected = tabChat.isSelected

        findView<ImageView>(R.id.icon_ai_studio)?.isSelected = tabAiStudio.isSelected
        findView<TextView>(R.id.text_ai_studio)?.isSelected = tabAiStudio.isSelected
        
        findView<ImageView>(R.id.icon_model_market)?.isSelected = tabModelMarket.isSelected
        findView<TextView>(R.id.text_model_market)?.isSelected = tabModelMarket.isSelected

        listener?.invoke(tab)
    }

    private inline fun <reified T : View> findView(@IdRes id: Int): T? = findViewById(id)
}

