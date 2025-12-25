package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.extensions.config

object ReceiverUtils {

    fun isMessageFilteredOut(context: Context, body: String): Boolean {
        for (blockedKeyword in context.config.blockedKeywords) {
            if (body.contains(blockedKeyword, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

  fun doesSMSContainBlockedKeywords(
      config: Config,
      senderName: String,
      body: String,
  ): Boolean {
    for (blockedKeyword in config.blockedKeywords) {
      if (
          body.contains(blockedKeyword, ignoreCase = true) || senderName.contains(blockedKeyword, ignoreCase = true)
      ) {
        return true
      }
    }

    return false
  }
}
