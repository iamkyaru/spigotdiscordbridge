package com.github.iamkyaru.spigotdiscordbridge

import com.github.iamkyaru.spigotdiscordbridge.services.ChannelMessagingService
import com.github.iamkyaru.spigotdiscordbridge.services.ChatService
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.plugin.java.JavaPlugin
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.io.File
import java.nio.file.Files

@Suppress("unused")
class SpigotDiscordBridgePlugin : JavaPlugin() {
    private lateinit var jedisPool: JedisPool

    override fun onEnable() {
        val config = this.createOrGetConfig()
        val redisConfig = config.getConfigurationSection("redis")
        val hostname = redisConfig.getString("hostname", "localhost")
        val port = redisConfig.getInt("port", 6379)
        this.jedisPool = JedisPool(JedisPoolConfig(), hostname, port)
        val channelMessagingService = ChannelMessagingService(redisConfig, this.jedisPool, "player-chat")
        val messagesConfig = config.getConfigurationSection("messages")
        super.getServer().pluginManager.registerEvents(ChatService(messagesConfig, channelMessagingService), this)
    }

    private fun createOrGetConfig(): FileConfiguration {
        val dataFolder = super.getDataFolder()
        if (!dataFolder.exists())
            dataFolder.mkdir()
        val file = File(dataFolder, "config.yml")
        if (!file.exists()) {
            val inputStream = this::class.java.getResourceAsStream("config.yml")
                ?: error("Unable to get resource config.yml.")
            Files.copy(inputStream, file.toPath())
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    override fun onDisable() {
        if (this::jedisPool.isInitialized) {
            this.jedisPool.close()
        }
    }
}