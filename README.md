🛡️ FaZe LeKee’s TANK WAR
2025 Ultimate Task

“Twist the game, Ace the war.”

FaZe LeKee’s TANK WAR 是一个基于 JavaFX 开发的复刻与扩展版《坦克大战（Battle City）》项目。
在保留经典玩法的基础上，引入了 无尽模式、双人本地对战 与 现代化渲染与特效系统，强调可扩展性与工程结构设计。

✨ 核心特性（Features）
🎮 多种游戏模式

闯关模式（Stage Mode）
经典关卡推进，击败敌人并守护基地。

无尽模式（Endless Mode）
敌人持续刷新，挑战生存极限与操作上限。

双人对战（PvP Mode）
本地双人同屏对抗，支持同时按键输入。

⚡️ 现代化游戏引擎设计

分层渲染系统
地图 / 坦克 / 子弹 / 粒子特效分层绘制，逻辑与渲染解耦。

道具与特效系统
随机生成 Buff，道具拾取伴随粒子动画反馈。

音效管理模块
支持 BGM 与多种战斗音效（开火、移动、爆炸）。

⚙️ 完整游戏流程

主菜单 / 暂停菜单 / 设置窗口

场景平滑切换，支持 ESC 全局暂停

🛠️ 技术栈（Tech Stack）
类型	技术
开发语言	Java 17
UI 框架	JavaFX 17.0.8
构建工具	Apache Maven
数据库	MySQL 8.0（预留接口：排行榜 / 存档）
核心库	javafx-controls, javafx-fxml, javafx-media
🕹️ 操作说明（Controls）

支持键盘操作，双人模式可同时输入（支持多键无冲）：

玩家	移动	开火
Player 1	W / A / S / D	J
Player 2	↑ / ↓ / ← / →	Enter
通用	—	ESC（暂停 / 设置）
🚀 快速开始（Getting Started）
环境要求

JDK 17 或更高

Maven 3.x

运行方式

方式一：Maven 运行（推荐）

mvn clean javafx:run


pom.xml 已配置主类为 game.AppLauncher

方式二：IDE 运行

以 Maven 项目导入

运行 src/game/AppLauncher.java 中的 main 方法

📂 项目结构（Project Structure）
src/
├── controller/     # 输入控制（InputHandler，支持多键无冲）
├── game/           # 游戏入口（AppLauncher, Main）
├── infra/          # 基础设施（GameLoop, GameConfig, DBManager）
├── item/           # 道具与特效（ItemSpawner, ParticleEffect）
├── map/            # 地图系统（MapModel, MapFactory）
├── model/          # 实体模型（PlayerTank, EnemyTank, Bullet）
└── view/           # 渲染与视图层
    ├── StageGameScene.java
    ├── EndlessGameScene.java
    └── TwoPlayerGameScene.java

📝 开发日志（Dev Log）

[2025-12-30]

完善无尽模式入口

修复 InputHandler 空指针问题

新增 ESC 暂停菜单

[2025-12-14]

确立项目代号 “FaZe LeKee”，致敬传奇战队

👤 作者

Created by Lekee
2025 · Ultimate Task
