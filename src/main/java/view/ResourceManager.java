package view;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
public class ResourceManager {

    // 单例模式（确保全局只有一个资源加载器）
    private static ResourceManager instance;

    private ResourceManager() {} // 私有构造

    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    /**
     * 加载资源图片（优化版）
     * @param path 资源相对路径（如 "images/tank_blue.png"）
     * @return 加载后的Image对象，失败返回占位图
     */
    public Image loadImage(String path) {
        // 定义默认占位图路径常量
        final String DEFAULT_IMAGE_PATH = "images/floor.png";
        final String FALLBACK_IMAGE_URL = "https://via.placeholder.com/50?text=ERROR";

        // 1. 尝试加载目标图片
        try (InputStream is = getClass().getResourceAsStream("/" + path)) {
            if (is != null) {
                return new Image(is);
            }
            System.err.println("目标资源不存在：" + path);
        } catch (FileNotFoundException e) {
            System.err.println("目标资源路径无效：" + path);
        } catch (IOException e) {
            System.err.println("加载目标图片失败：" + e.getMessage());
        }

        // 2. 尝试加载本地默认占位图
        try (InputStream defaultIs = getClass().getResourceAsStream("/" + DEFAULT_IMAGE_PATH)) {
            if (defaultIs != null) {
                return new Image(defaultIs);
            }
            System.err.println("本地默认占位图不存在：" + DEFAULT_IMAGE_PATH);
        } catch (IOException e) {
            System.err.println("加载本地默认占位图失败：" + e.getMessage());
        }

        // 3. 加载网络兜底占位图（建议替换为本地资源）
        try {
            return new Image(FALLBACK_IMAGE_URL);
        } catch (IllegalArgumentException e) {
            System.err.println("加载网络占位图失败：" + e.getMessage());
            // 最终兜底：返回空图（或抛出明确异常，根据业务需求调整）
            return new Image(new ByteArrayInputStream(new byte[0]));
        }
    }

    public static void setInstance(ResourceManager instance) {
        ResourceManager.instance = instance;
    }
}
