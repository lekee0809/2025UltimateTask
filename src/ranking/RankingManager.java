package ranking;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RankingManager {
    // 3种模式对应3个独立文件（项目根目录下）
    private static final String SINGLE_FILE = "single_challenge_ranking.txt";
    private static final String DOUBLE_FILE = "double_battle_ranking.txt";
    private static final String ENDLESS_FILE = "endless_mode_ranking.txt";
    private static final int TOP_LIMIT = 50;

    private static String getFilePathByMode(PlayerRecord.GameMode gameMode) {
        return switch (gameMode) {
            case SINGLE_CHALLENGE -> SINGLE_FILE;
            case DOUBLE_BATTLE -> DOUBLE_FILE;
            case ENDLESS_MODE -> ENDLESS_FILE;
        };
    }

    public static void addRecord(int score, int playTime, PlayerRecord.GameMode gameMode) {
        PlayerRecord newRecord = new PlayerRecord(score, playTime, gameMode);
        String filePath = getFilePathByMode(gameMode);

        List<PlayerRecord> records = loadAllRecords(gameMode);
        records.add(newRecord);
        // 写入时排序（高分→低分）
        records.sort(
                Comparator.comparingInt(PlayerRecord::getScore).reversed()
                        .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed()
        );
        if (records.size() > TOP_LIMIT) {
            records = new ArrayList<>(records.subList(0, TOP_LIMIT));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (PlayerRecord record : records) {
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

    // 修复：替换反射，用带参构造方法还原记录
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

                    // 关键：用 PlayerRecord 的带时间戳构造方法，避免反射赋值失败
                    PlayerRecord record = new PlayerRecord(
                            score,
                            playTime,
                            gameMode,
                            false, // 胜负状态（此处不影响排序，填默认值）
                            0,     // 道具数（此处不影响排序，填默认值）
                            timeStamp // 直接传入存储的时间戳
                    );
                    records.add(record);
                }
            }
        } catch (FileNotFoundException e) {
            // 文件不存在，返回空列表
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 读取后再次排序（双重保障）
        records.sort(
                Comparator.comparingInt(PlayerRecord::getScore).reversed()
                        .thenComparingLong(PlayerRecord::getFinishTimeStamp).reversed()
        );
        return records;
    }
}