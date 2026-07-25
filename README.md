# R3CT Daily Quests & Rewards 🎯

<div align="center">

[![Modrinth](https://img.shields.io/modrinth/dt/4NTCjyNQ?style=for-the-badge&label=Modrinth&logo=modrinth&logoColor=white&color=2EA043)](https://modrinth.com/mod/r3ct-daily-quests-rewards)
[![CurseForge](https://img.shields.io/curseforge/dt/1508573?style=for-the-badge&label=CurseForge&logo=curseforge&logoColor=white&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/r3ct-daily-quests-rewards)
[![Wiki](https://img.shields.io/badge/Documentation-Wiki-6E40C9?style=for-the-badge&logo=readthedocs&logoColor=white)](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki)
[![License: MIT](https://img.shields.io/badge/License-MIT-0576BA?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](https://opensource.org/licenses/MIT)

</div>

A powerful, highly configurable Daily Quests and Login Rewards mod for Minecraft.
Keep your players engaged with dynamic tasks, login streaks, competitive leaderboards, and a beautifully integrated GUI. Built natively for both Fabric and NeoForge!

<div align="center">
  <img src="./images/daily_banner.png" width="100%" alt="R3CT Daily Quests & Rewards">
</div>

---

## ✨ Features

* **⚔️ Daily Quests System:**
  * Generate 5 random daily tasks from highly customizable pools.
  * Earn Points to unlock massive **Milestone Rewards** (50, 100, 150, 200 points).
  * **Quest Streak:** Maintain your daily quest streak to earn powerful XP multipliers for every mission you complete!
  * **Reroll System:** Spend points to reroll quests you don't like and swap them for new, random tasks.
  * **Freeze/Shield System:** Earn shields to protect your streak even if you miss a day.

<div align="center">
  <a href="./images/daily_quests.png" target="_blank">
    <img src="./images/daily_quests.png" width="100%" alt="Quests (Click to enlarge)">
  </a>
</div>

* **🎁 Daily Rewards System:**
  * Claim rewards every day you log in. Missed a day? Don't worry, your total days progress is saved and you pick up right where you left off!
  * Build up your **Reward Streak** by logging in consecutively to unlock an additional daily reward from a special, exclusive loot pool.
  * Unlock powerful **Bonus Cycle Rewards** for claiming daily rewards for 7, 14, and 21 days in total.
  * Includes its own separate Shield System to save your login streak.

<div align="center">
  <a href="./images/daily_rewards.png" target="_blank">
    <img src="./images/daily_rewards.png" width="100%" alt="Rewards (Click to enlarge)">
  </a>
</div>

* **🏆 Competitive Leaderboards:**
  * Compete with your friends or server community!
  * Track the Top 10 players globally for: Total Quests Completed, Max Quest Streak, Total Rewards Collected, and Max Reward Streak.

* **🖥️ Beautiful GUI & Integration:**
  * Fully interactive, clean, and modern menus built directly into Minecraft.
  * Available in **English** and **Polish** (with native game translations for item names).
  * On-screen HUD to track your active quest progress in real-time. Easily toggle it on or off by pressing the `.` (period) key!

<div align="center">
  <a href="./images/daily_hud.png" target="_blank">
    <img src="./images/daily_hud.png" width="100%" alt="HUD (Click to enlarge)">
  </a>
</div>

* **🔄 Cross-Platform:**
  * Fully native support and identical features for both **Fabric** and **NeoForge**.

---

## 🔌 Dependencies & Requirements

To run this mod, you will need to install a few library mods depending on your loader:

**For Fabric:**
* [Fabric API](https://modrinth.com/mod/fabric-api) (Required)
* [Mod Menu](https://modrinth.com/mod/modmenu) (Optional - to access in-game settings)

**For NeoForge:**
* Nothing!

---

## 📖 Documentation

For detailed guides on how to set up quests, rewards, and technical mechanics, visit our official Wiki:
👉 **[View the Wiki](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki)**

<details>
<summary><b>Click to see popular topics 💡</b></summary>

* [📥 Getting Started](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki/Getting-Started)
* [⚔️ Customizing Quests](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki/Quests-Setup)
* [📅 Setting up Login Rewards](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki/Daily-Rewards)
* [🖥️ Admin Commands](https://github.com/R3CTrc/R3CT-Daily-Quests-and-Rewards/wiki/Commands-&-Permissions)

</details>

---

## ⚙️ Configuration & Customization

The mod is highly customizable! The configuration is split to give you the ultimate control. After running the mod once, navigate to the `config/r3ct_daily/` folder:

### 1. Client-Side (`r3ct_daily_client.json`)
Players can access the client settings via **Mod Menu** (on Fabric) or the **Mods tab** (on NeoForge). Here, users can:
* Toggle the on-screen Quest HUD on or off.
* Adjust the X and Y coordinates of the HUD to fit their screen.
* Scale the size of the HUD, Quests, Rewards, and Leaderboard Screens independently.

### 2. Server-Side / Modpack Creators
You can open all server config files directly from the in-game config menu! The system features an automatic backup mechanic, ensuring your data is safe during updates.

* **`r3ct_daily_quests.json`** - Manage the pool of daily tasks across different dimensions.
* **`r3ct_daily_rewards.json`** - Customize daily login reward pools, milestone rewards, and bonuses for completing daily quests.
* **`r3ct_daily_server.json`** - The core brain! Tweak mechanics, reroll costs, shield limits, and network optimization rules.

---

## 📥 Installation

1. Download the latest release from the **Versions** tab.
2. Download the required dependencies listed above for your specific mod loader.
3. Place all `.jar` files into your Minecraft `mods` folder.
4. Launch the game and enjoy!

---

## 📦 Check out my other mods!

<div align="center">
  <a href="https://modrinth.com/mod/r3ct-collection" target="_blank">
    <img src="./images/collection_banner.png" width="100%" alt="Check out R3CT Collection!">
  </a>
</div>

---

## 💖 Support the Development

I'm a computer science student, and I develop game mods and software in my free time. If my work has improved your server or modpack, consider supporting my coding journey! Every coffee helps me survive late-night debugging sessions. ☕💻

[![Ko-Fi](https://img.shields.io/badge/Support_me_on_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/r3ct_)

### 🌟 Memberships & Perks
Want to get more involved? Check out my Ko-fi memberships for exclusive perks:
* 🥇 **Diamond Supporter:** Name in the Hall of Fame and custom feature requests!

[Join a Tier and support the mod!](https://ko-fi.com/r3ct_/tiers)

---

## 📄 License
This project is available under the [MIT License](LICENSE). Feel free to learn from the code and include it in your modpacks!