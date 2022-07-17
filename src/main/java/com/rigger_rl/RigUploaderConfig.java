package com.rigger_rl;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("rig")
public interface RigUploaderConfig extends Config
{
	@ConfigItem(
		keyName = "uploadUrl",
		name = "Upload URL",
		description = "URL to upload messages and screenshots to for RigBot"
	)
	default String uploadURL()
	{
		return "https://example.com/upload";
	}

	@ConfigItem(
			keyName = "setImage",
			name = "Upload Images",
			description = "Enable/Disable sending a screenshot to the server. "
	)
	default boolean setImage() {return true;}
}
