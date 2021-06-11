package com.github.iamkyaru.spigotdiscordbridge.services

import com.google.gson.JsonObject
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.text.SimpleDateFormat

class ChatService(
    private val messagesConfig: ConfigurationSection,
    private val channelMessagingService: ChannelMessagingService
) : Listener {
    init {
        this.channelMessagingService.subscribe { data ->
            val username = data.get("username").asString
            val message = data.get("message").asString
            val timestamp = data.get("timestamp").asLong
            val broadcastMessage = this.buildBroadcastMessage(username, message, timestamp)
            Bukkit.broadcast(*TextComponent.fromLegacyText(broadcastMessage))
        }
    }

    private fun buildBroadcastMessage(username: String, message: String, timestamp: Long): String =
        this.messagesConfig.getString("broadcast-message")
            .replace("%username%", username)
            .replace("%message%", ChatColor.translateAlternateColorCodes('&', message))
            .replace("%timestamp%", DATE_FORMATTER.format(timestamp))

    @EventHandler
    @Suppress("unused")
    fun handlePlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        this.channelMessagingService.publish(JsonObject().apply {
            add("player", JsonObject().apply {
                addProperty("name", player.name)
                addProperty("uuid", player.uniqueId.toString())
            })
            addProperty("message", event.message)
            addProperty("timestamp", System.currentTimeMillis())
        })
    }

    companion object {
        private val DATE_FORMATTER = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    }
}