资源包模板（尚未包含音频，补齐后才能播放）。
1. 将 Ogg Vorbis 音频保存为 assets/levelup/sounds/music/blood_moon.ogg。
2. 在 days.json 对应事件设置 "bgm": "levelup:music.blood_moon"。
   同时设置 "bgmDurationSeconds": 实际音频长度（秒），以便循环播放。
3. 客户端将此目录放入 resourcepacks 并启用，或压缩其内容作为服务器资源包。
   ZIP 根目录必须直接包含 pack.mcmeta 和 assets，不能额外套一层目录。
4. 若通过服务器下发，使用 server.properties 的 resource-pack URL 和对应 SHA-1。
5. 修改事件 JSON 后执行 /levelup reload。已有资源包的客户端需重载资源。
可以在 sounds.json 增加其他事件音乐，并在配置中填写对应声音事件 ID。
