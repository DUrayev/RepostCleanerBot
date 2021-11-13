# RepostCleanerBot
Telegram bot to clean reposts in chats.

RepostCleanerBot can help you to analyze and clean reposts from any of your chats.
Telegram Bot API doesn't have access to user messages history. However, Telegram has Telegram Database Library (or simply TDLib), a tool that allow building your own Telegram clients.
Therefore, RepostCleanerBot has embedded Telegram client, which has access to your history.
As an additional Telegram client should be connected to user account, they need to pass an authorization to connect the RepostCleanerBot to your account as a new Device.
User will need to scan provided QR code by mobile phone, so it's supposed that you chat with RepostCleanerBot on another device (e.g. laptop).

To remove RepostCleanerBot from account user can send /logout or /stop command. In addition, it's possible to go to the Settings > Devices > Find "RepostCleaner" bot session and terminate it.

# Authorization in bot by QR code
As you need to scan QR code by mobile phone, so it's supposed that you chat with the bot on another device (e.g. laptop).

# Authorization without another device (using mobile phone only)
RepostCleanerBot supports authorization using phone number that is enabled for bot admins only.

After you provide phone number to bot, Telegram will send you verification code via Telegram Service Notification. For security reason Telegram controls if you re-send this verification code in any other chat and immediately expires it.

To be able to login by verification code, you should add an extra digit at the beginning of your original code and send it to bot to bypass Telegram security (e.g. if you received verification code '37627' you should send '037627' code to bot)

# Analyzing of all chats history
RepostCleanerBot supports the flow to analyze all chats history that is enabled for bot admins only, because it's time and resources consuming process


# Building from sources
### Prerequisites
[Java 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) installed

[Maven 3](https://maven.apache.org/download.cgi) installed

### Register your bot in Telegram BotFather
Register your bot by the following [instruction](https://core.telegram.org/bots#6-botfather)

Save obtained **bot name** and **bot token** *(do not disclosure it somewhere)*

### Register your Telegram Client Application
Register your Telegram Client Application by the following [instruction](https://core.telegram.org/api/obtaining_api_id)

Save obtained **api_id** and **api_hash** *(do not disclosure it somewhere)*

### Obtain your Telegram user id
You can obtain your Telegram User ID in different ways:

- using existing Telegram bots, e.g.: [@userinfobot](https://t.me/userinfobot) or [@my_id_bot](https://t.me/my_id_bot)
- send **/myid** command to [@RepostCleanerBot](https://t.me/RepostCleanerBot)
- find a different bot that provides your user id

Save obtained **user id**

### Add system environment variables
| Environment variable name   | Value         |
| --------------------------- |:-------------:|
| REPOST_CLEANER_BOT_NAME     | **bot name**  |
| REPOST_CLEANER_BOT_TOKEN    | **bot token** |
| REPOST_CLEANER_BOT_API_ID   | **api_id**    |
| REPOST_CLEANER_BOT_API_HASH | **api_hash**  |
| REPOST_CLEANER_BOT_ADMIN_ID | **user id**   |


### Build project
Build project by maven
```
mvn clean install
```
Run main class with all dependencies 
```
java -Dfile.encoding=UTF-8 -cp target/repost-cleaner-bot.jar;target/dependency/* org.telegram.repostcleanerbot.RepostCleanerApplication
```

# Add user to admins
Send **/promote @user_name** command to bot to grant user admin permissions

Send **/demote @user_name** command to demote user

# Additional admin bot commands
- **/backup** - returns a backup of the bot database
- **/recover** - recovers the database
- **/ban @username** - bans the user from accessing your bot commands and features
- **/unban @username** - lifts the ban from the user

