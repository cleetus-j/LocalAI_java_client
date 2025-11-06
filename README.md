# LocalAI_java_client
An ugly but functioning java client for local network LocalAI client, and Deepseek\ChatGPT\Gemini client with convo saving and loading for resource limited machines. No need for web browser to chat or anything. Batteries not included.

This works with Java Swing, and really just vibed together in an afternoon out of curiosity. For linux, you need to have the API keys in your ./profile file as below:
  
export DEEPSEEK_API_KEY=""
export GEMINI_API_KEY=""
export CHATGPT_API_KEY=""

Then give a source ~/.profile to apply it. Otherwise you can use your LocalAI instance to chat without a browser open. The window is fixed at 1280x1024, but you can modify it in the code if you want.
You can save and load back conversations, they can be compressed as well in zip format.
The program has java 11 level, and Alibaba's Dragonwell 11 SDK was used for development and testing.

Deepseek and ChatGPT uses the paid tier API stuff, Gemini has a generous free tier to be used. The code was cobbled together from Deepseek and Gemini. The base UI was done with my LocalAI's qwen2.5-coder-7b-3x-instruct-ties-v1.2-i1 model as a test. I don't like writing GUI code, and I don't care about looks for tools that mostly I would use.

The project has some parts that are not used, as I did not found a way to unload models from the target instance's GPU VRAM. I was tired and it's easier to restart the docker container anyway.
Keyboard shortcuts are there as well for easier use, and less fiddling with the mouse:

F9- Send message (As enter is used for newlines)
F10- Load Conversation (from txt or zip)
F11-  Save Conversation (again, to txt or zip)
F12-  Refresh model list (if you install new ones, or restart the instance)

What should work, but not (fix it, future me!):
F1-  switch to local network mode
F2-  switch to online model mode
