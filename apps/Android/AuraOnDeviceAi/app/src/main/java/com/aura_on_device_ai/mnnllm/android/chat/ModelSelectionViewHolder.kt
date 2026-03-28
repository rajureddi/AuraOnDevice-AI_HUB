package com.aura_on_device_ai.mnnllm.android.chat

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aura_on_device_ai.mls.api.ModelItem
import com.aura_on_device_ai.mnnllm.android.R
import com.aura_on_device_ai.mnnllm.android.modelist.ModelItemWrapper
import com.aura_on_device_ai.mnnllm.android.widgets.ModelAvatarView
import com.aura_on_device_ai.mnnllm.android.widgets.TagsLayout

class ModelSelectionViewHolder(
    itemView: View,
    private val onModelClicked: (ModelItemWrapper) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val modelAvatar: ModelAvatarView = itemView.findViewById(R.id.model_avatar)
    private val modelNameTextView: TextView = itemView.findViewById(R.id.tv_model_name)
    private val tagsLayout: TagsLayout = itemView.findViewById(R.id.tags_layout)
    private val checkImageView: ImageView = itemView.findViewById(R.id.iv_check)

    private var currentModelWrapper: ModelItemWrapper? = null

    init {
        itemView.setOnClickListener {
            currentModelWrapper?.let { onModelClicked(it) }
        }
    }

    fun bind(modelWrapper: ModelItemWrapper, isSelected: Boolean) {
        currentModelWrapper = modelWrapper
        val modelItem = modelWrapper.modelItem
        
        // Set model name
        modelNameTextView.text = modelItem.modelName ?: modelItem.modelId
        
        // Set model avatar
        modelAvatar.setModelName(modelItem.modelName ?: modelItem.modelId ?: "")
        
        // Set tags (simplified version from ModelItemHolder)
        val tags = getDisplayTags(modelItem)
        tagsLayout.setTags(tags)
        
        // Set selection state
        checkImageView.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
    }

    private fun getDisplayTags(modelItem: ModelItem): List<String> {
        return com.aura_on_device_ai.mnnllm.android.modelmarket.TagMapper.getDisplayTagList(modelItem.tags).take(3)
    }
} 

