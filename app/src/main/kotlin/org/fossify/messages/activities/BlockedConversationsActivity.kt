package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.adapters.BlockedConversationsAdapter
import org.fossify.messages.databinding.ActivityBlockedConversationsBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.removeAllBlockedConversations
import org.fossify.messages.helpers.IS_BLOCKED_BIN
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.THREAD_TITLE
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class BlockedConversationsActivity : SimpleActivity() {
  private var bus: EventBus? = null
  private val binding by viewBinding(ActivityBlockedConversationsBinding::inflate)

  @SuppressLint("InlinedApi")
  override fun onCreate(savedInstanceState: Bundle?) {
    isMaterialActivity = true
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    setupOptionsMenu()

    updateMaterialActivityViews(
        mainCoordinatorLayout = binding.blockedCoordinator,
        nestedView = binding.conversationsList,
        useTransparentNavigation = true,
        useTopSearchMenu = false,
    )
    setupMaterialScrollListener(
        scrollingView = binding.conversationsList,
        toolbar = binding.blockedToolbar,
    )

    loadBlockedConversations()
  }

  override fun onResume() {
    super.onResume()
    setupToolbar(binding.blockedToolbar, NavigationIcon.Arrow)
    updateMenuColors()

    loadBlockedConversations()
  }

  override fun onDestroy() {
    super.onDestroy()
    bus?.unregister(this)
  }

  private fun setupOptionsMenu() {
    binding.blockedToolbar.inflateMenu(R.menu.blocked_menu)
    binding.blockedToolbar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.empty_blocked -> removeAll()
        else -> return@setOnMenuItemClickListener false
      }
      return@setOnMenuItemClickListener true
    }
  }

  private fun updateOptionsMenu(conversations: ArrayList<Conversation>) {
    binding.blockedToolbar.menu.apply {
      findItem(R.id.empty_blocked).isVisible = conversations.isNotEmpty()
    }
  }

  private fun updateMenuColors() {
    updateStatusbarColor(getProperBackgroundColor())
  }

  private fun loadBlockedConversations() {
    ensureBackgroundThread {
      val conversations =
          try {
            conversationsDB.getAllBlocked().toMutableList() as ArrayList<Conversation>
          } catch (e: Exception) {
            ArrayList()
          }

      runOnUiThread { setupConversations(conversations) }
    }

    bus = EventBus.getDefault()
    try {
      bus!!.register(this)
    } catch (ignored: Exception) {}
  }

  private fun removeAll() {
    ConfirmationDialog(
        activity = this,
        message = "",
        messageId = R.string.empty_blocked_confirmation,
        positive = org.fossify.commons.R.string.yes,
        negative = org.fossify.commons.R.string.no,
    ) {
      removeAllBlockedConversations { loadBlockedConversations() }
    }
  }

  private fun getOrCreateConversationsAdapter(): BlockedConversationsAdapter {
    var currAdapter = binding.conversationsList.adapter
    if (currAdapter == null) {
      hideKeyboard()
      currAdapter =
          BlockedConversationsAdapter(
              activity = this,
              recyclerView = binding.conversationsList,
              onRefresh = { notifyDatasetChanged() },
              itemClick = { handleConversationClick(it) },
          )

      binding.conversationsList.adapter = currAdapter
      if (areSystemAnimationsEnabled) {
        binding.conversationsList.scheduleLayoutAnimation()
      }
    }
    return currAdapter as BlockedConversationsAdapter
  }

  private fun setupConversations(conversations: ArrayList<Conversation>) {
    val sortedConversations =
        conversations
            .sortedWith(
                compareByDescending<Conversation> {
                      config.pinnedConversations.contains(it.threadId.toString())
                    }
                    .thenByDescending { it.date }
            )
            .toMutableList() as ArrayList<Conversation>

    showOrHidePlaceholder(conversations.isEmpty())
    updateOptionsMenu(conversations)

    try {
      getOrCreateConversationsAdapter().apply { updateConversations(sortedConversations) }
    } catch (ignored: Exception) {}
  }

  private fun showOrHidePlaceholder(show: Boolean) {
    binding.conversationsFastscroller.beGoneIf(show)
    binding.noConversationsPlaceholder.beVisibleIf(show)
    binding.noConversationsPlaceholder.setTextColor(getProperTextColor())
    binding.noConversationsPlaceholder.text = getString(R.string.no_blocked_conversations)
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun notifyDatasetChanged() {
    getOrCreateConversationsAdapter().notifyDataSetChanged()
  }

  private fun handleConversationClick(any: Any) {
    Intent(this, ThreadActivity::class.java).apply {
      val conversation = any as Conversation
      putExtra(THREAD_ID, conversation.threadId)
      putExtra(THREAD_TITLE, conversation.title)
      putExtra(IS_BLOCKED_BIN, true)
      startActivity(this)
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
    loadBlockedConversations()
  }
}
