package ranking;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 普通导入PlayerRecord（若未导入，直接使用全路径即可）
// import 你的包名.PlayerRecord;

public class RankingManager {
    // 3种模式对应3个独立文件（项目根目录下）
    private static final String SINGLE_FILE = "single_challenge_ranking.txt";
    private static final String DOUBLE_FILE = "double_battle_ranking.txt";
    private static final String ENDLESS_FILE = "endless_mode_ranking.txt";
    private static final int TOP_LIMIT = 10; // 每种模式只保留前10名

    // 根据游戏模式获取对应文件路径（直接引用 PlayerRecord.GameMode）
    private static String getFilePathByMode(PlayerRecord.GameMode gameMode) {
        return switch (gameMode) {
            case SINGLE_CHALLENGE -> SINGLE_FILE;
            case DOUBLE_BATTLE -> DOUBLE_FILE;
            case ENDLESS_MODE -> ENDLESS_FILE;
        };
    }

    // 写入记录：指定游戏模式，自动存入对应文件
    public static void addRecord(int score, int playTime, PlayerRecord.GameMode gameMode) {
        PlayerRecord newRecord = new PlayerRecord(score, playTime, gameMode);
        String filePath = getFilePathByMode(gameMode);

        // 1. 读取该模式已有记录
        List<PlayerRecord> records = loadAllRecords(gameMode);
        // 2. 添加新记录
        records.add(newRecord);
        // 3. 按分数降序排序（分数相同按时间戳降序，新记录在前）
        records.sort(Comparator.comparingInt(PlayerRecord::getScore).reversed()
                .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed());
        // 4. 保留前10名
        if (records.size() > TOP_LIMIT) {
            records = records.subList(0, TOP_LIMIT);
        }
        // 5. 写回对应文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (PlayerRecord record : records) {
                // 存储格式：分数,游玩时长,时间戳（便于读取还原）
                writer.write(String.format("%d,%d,%d",
                        record.getScore(),
                        record.getPlayTime(),
                        record.getFinishTimeStamp()));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取指定模式的所有记录（已排序）
    public static List<PlayerRecord> loadAllRecords(PlayerRecord.GameMode gameMode) {
        List<PlayerRecord> records = new ArrayList<>();
        String filePath = getFilePathByMode(gameMode);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int score = Integer.parseInt(parts[0].trim());
                    int playTime = Integer.parseInt(parts[1].trim());
                    long timeStamp = Long.parseLong(parts[2].trim());
                    // 还原记录（自动生成格式化时间）
                    PlayerRecord record = new PlayerRecord(score, playTime, gameMode);
                    // 手动设置时间戳（保证读取的记录时间与存储一致）
                    java.lang.reflect.Field field = PlayerRecord.class.getDeclaredField("finishTimeStamp");
                    field.setAccessible(true);
                    field.set(record, timeStamp);
                    // 重新生成格式化时间
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    field = PlayerRecord.class.getDeclaredField("finishTimeStr");
                    field.setAccessible(true);
                    field.set(record, sdf.format(new java.util.Date(timeStamp)));

                    records.add(record);
                }
            }
        } catch (FileNotFoundException e) {
            // 文件不存在（首次运行该模式），返回空列表
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 再次排序，确保数据有序
        records.sort(Comparator.comparingInt(PlayerRecord::getScore).reversed()
                .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed());
        return records;
    }
}