🛡️ FaZe LeKee's TANK WAR (2025 Ultimate Task)"Twist the game, Ace the war."这是一个基于 JavaFX 开发的复刻版《坦克大战》（Battle City），结合了经典玩法与现代化的无尽模式及双人对战功能。项目采用分层渲染架构，支持流畅的物理引擎和粒子特效。✨ 核心功能 (Features)🎮 多种游戏模式：闯关模式 (Stage Mode)：经典的关卡推进玩法，击败敌人保卫基地。无尽模式 (Endless Mode)：挑战生存极限，敌人源源不断，考验你的操作和耐力。双人对战 (PvP Mode)：本地双人同屏竞技，是时候和朋友一决高下了！⚡️ 现代化游戏引擎：分层渲染系统：地图、坦克、子弹、特效分层绘制，画面更清晰。道具系统：随机生成的增益道具（Buff），拾取后伴有金色粒子特效。音效管理：支持背景音乐（BGM）与战斗音效（开火、移动、爆炸）。⚙️ 完整游戏流程：包含主菜单、暂停功能、设置窗口以及平滑的场景切换。🛠️ 技术栈 (Tech Stack)开发语言: Java 17UI 框架: JavaFX 17.0.8构建工具: Apache Maven数据库: MySQL 8.0 (预留接口，用于排行榜/存档)核心库: javafx-controls, javafx-fxml, javafx-media🕹️ 操作说明 (Controls)游戏支持键盘操作，双人模式下可同时输入：玩家动作按键Player 1移动W / A / S / D开火JPlayer 2移动↑ / ↓ / ← / →开火Enter (回车)通用暂停/设置ESC🚀 快速开始 (Getting Started)环境要求JDK 17 或更高版本Maven 3.x运行方式克隆或下载项目使用 Maven 运行 (推荐):在项目根目录打开终端，输入以下命令：Bashmvn clean javafx:run
注意：pom.xml 中已配置主类为 game.AppLauncher在 IDE 中运行:导入项目为 Maven 项目。找到 src/game/AppLauncher.java。右键点击运行 main 方法。📂 项目结构 (Project Structure)Plaintextsrc/
├── controller/     # 输入控制 (InputHandler - 支持多键无冲)
├── game/           # 游戏入口 (AppLauncher, Main)
├── infra/          # 基础设施 (GameLoop, GameConfig, DBManager)
├── item/           # 道具与特效 (ItemSpawner, ParticleEffect)
├── map/            # 地图生成与管理 (MapModel, MapFactory)
├── model/          # 实体模型 (PlayerTank, EnemyTank, Bullet)
└── view/           # 视图与渲染 (BaseGameScene, SpritePainter)
    ├── StageGameScene.java      # 闯关模式场景
    ├── EndlessGameScene.java    # 无尽模式场景
    └── TwoPlayerGameScene.java  # 双人对战场景
📝 开发日志 (Dev Log)[2025-12-30]: 完善了无尽模式入口，优化了 InputHandler 的空指针问题，增加了 ESC 暂停菜单。[2025-12-14]: 确立项目代号 "FaZe LeKee"，致敬传奇战队。Created by Lekee | 2025 Ultimate Task
