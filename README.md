# R3CT Daily Quests & Rewards 🎯

A powerful, highly configurable Daily Quests and Login Rewards mod for Minecraft.
Keep your players engaged with dynamic tasks, login streaks, and a beautifully integrated GUI. Built for both Fabric and NeoForge!

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.11-success)
![Fabric](https://img.shields.io/badge/Loader-Fabric-orange)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## ✨ Features

* **⚔️ Daily Quests System:** * Generate random daily tasks from customizable pools.
    * Maintain your Quest Streak by completing tasks daily.
    * **Freeze/Shield System:** Earn shields to protect your streak even if you miss a day!

* **🎁 Daily Rewards System:** * Claim escalating rewards for consecutive daily logins.
    * Build up your Reward Streak to earn bonus multipliers.
    * Includes its own separate Shield System to save your login streak.

* **🖥️ Beautiful GUI:** * Fully interactive, clean, and modern menus built directly into Minecraft.
    * Track your progress easily (Default keys: `G` for Quests, `H` for Rewards).
    * On-screen HUD to track your active quest progress in real-time. Easily toggle it on or off by pressing the `.` (period) key!

* **🔄 Cross-Platform:** * Fully native support and identical features for both **Fabric** and **NeoForge**.

## 🔌 Dependencies & Requirements

To run this mod, you will need to install a few library mods depending on your loader:

**For Fabric:**
* [Fabric API](https://modrinth.com/mod/fabric-api) (Required)
* [Cloth Config API](https://modrinth.com/mod/cloth-config) (Required)
* [Mod Menu](https://modrinth.com/mod/modmenu) (Recommended - to access in-game settings)

**For NeoForge:**
* [Cloth Config API](https://modrinth.com/mod/cloth-config) (Required)

## ⚙️ Configuration & Customization

The mod is highly customizable! There are two ways to configure the mod:

### 1. In-Game Settings (Client-side)
Players can access the mod settings via **Mod Menu** (on Fabric) or the **Mods tab** (on NeoForge). Here, users can:
* Toggle the on-screen Quest HUD on or off.
* Adjust the X and Y coordinates of the HUD to fit their screen.

### 2. File Configuration (Server-side / Modpack Creators)
All core mechanics, quests, and rewards can be completely rewritten to fit your server or modpack. After running the mod once, navigate to the `config/r3ct/` folder. You will find 4 JSON files:

* **`quests.json`** - Add, remove, or modify the pool of daily quests. Customize their difficulty, objective, name, description, required amount, points, and specific item rewards.
* **`daily_quest_rewards.json`** - Configure the randomized bonus reward given to players for completing 3 daily quests. Adjust the item types, min/max quantities, and weight (drop chance).
* **`daily_rewards.json`** - Customize login rewards. Adjust the loot pools for specific day tiers (days 1-4, 5-6, and day 7), including items, amounts, and drop chances.
* **`r3ctdailyquests.json`** - Tweak the core mechanics and technical rules. Configure quest rerolling costs, XP rewards, streak shield requirements (e.g., how many perfect days give a shield), quest refresh hours, and server optimization limits.

## 📥 Installation

1. Download the latest release from the releases tab.
2. Download the required dependencies listed above for your specific mod loader.
3. Place all `.jar` files into your Minecraft `mods` folder.
4. Launch the game and enjoy!

## 📄 License
This project is available under the [MIT License](LICENSE). Feel free to learn from the code and include it in your modpacks!