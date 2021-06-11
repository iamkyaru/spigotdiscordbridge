package com.github.iamkyaru.spigotdiscordbridge.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.configuration.ConfigurationSection
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

class ChannelMessagingService(
    private val redisConfig: ConfigurationSection,
    private val jedis: JedisPool,
    private val channel: String,
) : JedisPubSub() {
    private val subscribers: MutableList<(data: JsonObject) -> Unit> = mutableListOf()

    init {
        this.useResource { it.subscribe(this, this.channel) }
    }

    fun publish(data: JsonObject = JsonObject()) {
        val message = data.toString()
        this.useResource { jedis ->
            jedis.publish(this.channel, message)
        }
    }

    fun subscribe(subscription: (data: JsonObject) -> Unit) {
        this.subscribers += subscription
    }

    override fun onMessage(channel: String, message: String) {
        if (channel != this.channel)
            return
        val data = JsonParser().parse(message).asJsonObject
        this.subscribers.forEach { it.invoke(data) }
    }

    private fun useResource(block: (Jedis) -> Unit) = this.jedis.resource.use { resource ->
        if (this.redisConfig.contains("password"))
            resource.auth(this.redisConfig.getString("password"))
        block(resource)
    }
}