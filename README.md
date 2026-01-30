# 坦克大战游戏 (TankWar)

## 项目概述
这是一个现代化的JavaFX坦克大战游戏，具有精美的UI界面、流畅的游戏体验和丰富的功能特性。

## 主要特性
- 现代化的JavaFX用户界面
- 多种游戏模式（单人挑战、无尽模式、双人对战）
- 精美的视觉效果和动画
- 完整的音效系统
- 排行榜功能
- 自适应主题系统
- 智能AI敌人

## 技术栈
- Java 17
- JavaFX 17+
- Maven 3.6+
- MySQL (可选，用于持久化排行榜)

## 构建与运行

### 构建项目
```bash
mvn clean package
```

### 运行游戏
```bash
java -jar target/TankWar-1.0-SNAPSHOT.jar
```

## 修复记录
此版本经过以下修复和改进：
- 修复了Maven构建配置，使项目能够正确编译
- 解决了JavaFX GraphicsContext中不存在setShadow方法的问题
- 补充了ThemeManager中缺失的颜色常量
- 修正了StartSceneOptimized中对ModernRankingDisplay的引用
- 配置了Maven Shade Plugin以创建包含所有依赖的可执行JAR包

## 游戏操作
- W/A/S/D: 控制坦克移动
- 空格键: 发射子弹
- ESC: 打开暂停菜单
- P: 暂停/恢复游戏

## 开发者
LeKee团队