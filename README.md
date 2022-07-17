# RigUploader

---


The RigUploader plugin was created since I really don't like webhooks and wanted some extra tracking of the number of deaths etc.
from friends who used the plugin. It's designed to be used with a separate [Discord bot](https://github.com/yorgei/rig-bot) that handles more of the settings allowing for
more customization per server and posting to multiple servers at once. I am by no means a 
Java developer so this won't have the cleanest code, but if it works it works.

RigUploader will send a POST request to the URL entered in the config section of the app which contains a Base64 Encoded string of
a screenshot of the game window if screenshots are enabled in the config. With other information such as level, location,
item name/id etc... 


### Planned features

- [x] Track time played since last death (maybe change this to use varbits of game time instead of ticks?)
- [x] Upload death message 
- [x] Upload death screenshot
- [ ] Upload death location
- [ ] Upload death enemy
- [ ] Upload quest completion message
- [ ] Upload quest completion screenshot
- [ ] Upload level up message
- [ ] Upload level up screenshot

---

### Credits

- [cepawiel](https://github.com/cepawiel/RuneLite-Discord-Notifications) I used a bit from their repo 
- cowys
- Big Dummy
- killing guy3 (rip)