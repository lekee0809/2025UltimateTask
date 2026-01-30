package view;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 主题管理器 - 统一管理游戏UI主题
 */
public class ThemeManager {
    // 颜色主题
    public static class Colors {
        // 主色调
        public static final Color PRIMARY = Color.web("#00d4ff");        // 主蓝色
        public static final Color SECONDARY = Color.web("#f39c12");      // 橙色
        public static final Color SUCCESS = Color.web("#2ecc71");        // 绿色
        public static final Color DANGER = Color.web("#e74c3c");         // 红色
        public static final Color WARNING = Color.web("#f1c40f");        // 黄色
        public static final Color DARK_BG = Color.web("#0a0e17");        // 深蓝背景
        public static final Color DARK_PANEL = Color.web("#1a202c");     // 深色面板
        public static final Color LIGHT_TEXT = Color.web("#ffffff");      // 亮色文字
        public static final Color MEDIUM_TEXT = Color.web("#d1d5db");    // 中等亮度文字
        public static final Color DISABLED = Color.web("#6b7280");       // 禁用状态
        
        // 坦克颜色
        public static final Color PLAYER_TANK = Color.web("#3498db");    // 玩家坦克
        public static final Color ENEMY_NORMAL = Color.web("#e74c3c");   // 普通敌人
        public static final Color ENEMY_FAST = Color.web("#9b59b6");     // 快速敌人
        public static final Color ENEMY_HEAVY = Color.web("#7f8c8d");    // 重型敌人
    }
    
    // 字体主题
    public static class Fonts {
        public static final Font TITLE = Font.font("Microsoft YaHei", FontWeight.BOLD, 48);
        public static final Font SUBTITLE = Font.font("Microsoft YaHei", FontWeight.BOLD, 32);
        public static final Font MENU_ITEM = Font.font("Microsoft YaHei", FontWeight.NORMAL, 24);
        public static final Font HUD_SMALL = Font.font("Consolas", FontWeight.NORMAL, 16);
        public static final Font HUD_MEDIUM = Font.font("Consolas", FontWeight.BOLD, 20);
        public static final Font HUD_LARGE = Font.font("Consolas", FontWeight.BOLD, 32);
        public static final Font GAME_OVER = Font.font("Impact", FontWeight.BOLD, 80);
    }
    
    // 按钮样式
    public static class ButtonStyles {
        public static final String PRIMARY = 
            "-fx-background-color: linear-gradient(to bottom, #444444, #222222); " +
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Microsoft YaHei'; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: #00d4ff; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: 200px; " +
            "-fx-min-height: 50px;";
            
        public static final String PRIMARY_HOVER = 
            "-fx-background-color: linear-gradient(to bottom, #00d4ff, #00a8cc); " +
            "-fx-text-fill: black; " +
            "-fx-font-family: 'Microsoft YaHei'; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: white; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: 200px; " +
            "-fx-min-height: 50px;";
            
        public static final String DANGER = 
            "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b); " +
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Microsoft YaHei'; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: #ffffff; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: 200px; " +
            "-fx-min-height: 50px;";
            
        public static final String DANGER_HOVER = 
            "-fx-background-color: linear-gradient(to bottom, #ff6b6b, #ff4757); " +
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Microsoft YaHei'; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-border-color: #ffffff; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: 200px; " +
            "-fx-min-height: 50px;";
    }
    
    // 面板样式
    public static class PanelStyles {
        public static final String MAIN_MENU = 
            "-fx-background-color: linear-gradient(to bottom right, #0a0e17, #1a202c);";
            
        public static final String GAME_PANEL = 
            "-fx-background-color: rgba(10, 14, 23, 0.85); " +
            "-fx-border-color: rgba(0, 212, 255, 0.3); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10;";
            
        public static final String HUD_PANEL = 
            "-fx-background-color: rgba(0, 0, 0, 0.7); " +
            "-fx-border-color: rgba(0, 212, 255, 0.5); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;";
    }
}