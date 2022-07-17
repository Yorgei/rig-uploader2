package com.rigger_rl;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import javax.inject.Inject;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.ui.DrawManager;
import okhttp3.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
	name = "RigUploader"
)
public class RigUploaderPlugin extends Plugin
{
	private boolean shouldSendLevelMessage = false;
	private boolean shouldSendQuestMessage = false;
	private boolean shouldSendClueMessage = false;
	private int ticksWaited = 0;
	private int heartbeatTicks = 0;

	private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: [Qq]uest)?[!.]?$");
	private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: [Qq]uest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$");
	private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
	private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");
	private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of("You have a funny feeling like you're being followed",
			"You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed");

	@Inject
	private Client client;

	@Inject
	private RigUploaderConfig config;

	@Inject
	private DrawManager drawManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Override
	protected void startUp() throws Exception
	{
		log.info("RigUpload started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("RigUpload stopped");
	}

	private void resetState()
	{
		heartbeatTicks = 0;
		ticksWaited = 0;
		shouldSendQuestMessage = false;
		shouldSendClueMessage = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "RigUpload enabled", null);
		}
	}

	@Provides
	RigUploaderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RigUploaderConfig.class);
	}


	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer())
			{
				sendDeathMessage(true);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// Using this for time between death tracking
		heartbeatTicks++;
		if (heartbeatTicks >= 60){
			heartbeatTicks = 0;
			sendHeartbeat();
		}
	}


	private void sendHeartbeat()
	{
		HeartbeatBody heartbeatBody = new HeartbeatBody();
		heartbeatBody.setRsn(client.getLocalPlayer().getName());
		heartbeatBody.setType("heartbeat");
		String jsonData = GSON.toJson(heartbeatBody);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM);
		requestBodyBuilder.addFormDataPart("payload", jsonData);
		postMessage(requestBodyBuilder);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage().contains("Death"))
		{
			sendDeathMessage(config.setImage());
		}
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String chatMessage = event.getMessage();
		if (config.setImage() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			return;
		}
	}

	private void sendDeathMessage(boolean takeScreenshot)
	{
		String rsn = client.getLocalPlayer().getName();
		GenericBody genericBody = new GenericBody();
		genericBody.setRsn(rsn);
		genericBody.setAccountType(client.getAccountType());
		genericBody.setType("death");
		// TODO: add extras here
		if (takeScreenshot)
		{
			uploadWithScreenshot(genericBody);
			return;
		}

	}

	private void uploadWithoutScreenshot(GenericBody genericBody)
	{
		String uploadUrl = config.uploadURL();
		if (Strings.isNullOrEmpty(uploadUrl)) { client.addChatMessage(ChatMessageType.BROADCAST, "", "ERROR: Upload URL is empty while trying to upload a post.", "");}
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM);
		String jsonData = GSON.toJson(genericBody);
		requestBodyBuilder.addFormDataPart("payload", jsonData);
		postMessage(requestBodyBuilder);
	}

	private void uploadWithScreenshot(GenericBody genericBody)
	{
		String uploadUrl = config.uploadURL();
		if (Strings.isNullOrEmpty(uploadUrl)) { client.addChatMessage(ChatMessageType.BROADCAST, "", "ERROR: Upload URL is empty while trying to upload a post.", "");}
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM);
		takeScreenshot(requestBodyBuilder, genericBody);
	}

	private void takeScreenshot(MultipartBody.Builder requestBodyBuilder, GenericBody genericBody)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			try
			{
				log.info("trying to ss");
				byte[] imageBytes = convertImageToByteArray(bufferedImage);
				String imageB64 = new String(Base64.getEncoder().encode(imageBytes), "UTF-8");
				genericBody.setImage(imageB64);
				String jsonData = GSON.toJson(genericBody);
				requestBodyBuilder.addFormDataPart("payload", jsonData);
				postMessage(requestBodyBuilder);

			}
			catch (IOException e)
			{
				log.warn("Error converting image to byte array", e);
			}
		});
	}

	private void postMessage(MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		HttpUrl url = HttpUrl.parse(config.uploadURL());
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				client.addChatMessage(ChatMessageType.BROADCAST, "", "ERROR: Failed to send request", "");
				log.info("error sending request", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				response.close();
				log.info("sent request with no dramas");
			}
		});
	}


	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
			return byteArrayOutputStream.toByteArray();
		}


}
