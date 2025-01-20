package creeperdev.dmlog;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessengerLog extends JavaPlugin implements Listener {
    private String webhookUrl;
    private String embedColor;
    private String embedTitle;
    private String embedFooter;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("DMLog has been enabled!");
    }

    private void loadConfig() {
        webhookUrl = getConfig().getString("webhook-url");
        embedColor = getConfig().getString("embed-color", "#00ff00");
        embedTitle = getConfig().getString("embed-title", "Private Message Log");
        embedFooter = getConfig().getString("embed-footer", "DMLog Plugin");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        if (message.startsWith("/tell ") || message.startsWith("/msg ") || message.startsWith("/w ")) {
            handlePrivateMessage(event);
        }
    }

    private void handlePrivateMessage(PlayerCommandPreprocessEvent event) {
        String fullCommand = event.getMessage();
        Pattern pattern = Pattern.compile("^/(tell|msg|w)\\s+(\\S+)\\s+(.+)$");
        Matcher matcher = pattern.matcher(fullCommand);

        if (matcher.find()) {
            String sender = event.getPlayer().getName();
            String receiver = matcher.group(2);
            String message = matcher.group(3);

            sendToDiscord(sender, receiver, message);
        }
    }

    private void sendToDiscord(String sender, String receiver, String message) {
        JSONObject json = new JSONObject();
        JSONObject embed = new JSONObject();

        // Convert hex color to decimal
        String colorHex = embedColor.replace("#", "");
        int colorDec = Integer.parseInt(colorHex, 16);

        embed.put("title", embedTitle);
        embed.put("color", colorDec);
        embed.put("description", "**From:** " + sender + "\n**To:** " + receiver + "\n**Message:** " + message);

        JSONObject footer = new JSONObject();
        footer.put("text", embedFooter);
        embed.put("footer", footer);

        json.put("embeds", new JSONObject[]{embed});

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getLogger().warning("Failed to send message to Discord: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
            }
        });
    }
}