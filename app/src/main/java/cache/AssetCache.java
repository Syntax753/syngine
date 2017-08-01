package cache;

import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

public enum AssetCache {



    DIRTFLOOR_1(1, 3, 0, 0),
    DIRTFLOOR_2(2, 3, 1, 0),
    DIRTFLOOR_3(3, 3, 2, 0),
    DIRTFLOOR_4(4, 3, 0, 1),
    DIRTFLOOR_5(5, 3, 1, 1),
    DIRTFLOOR_6(6, 3, 2, 1),
    ;

    private static final class AssetMap {
        static final Map<Integer, AssetCache> tileMap = new HashMap<>();
    }

    private int id;
    private int decoderId;
    private int x;
    private int y;

    AssetCache(int id, int decoderId, int x, int y) {
        AssetMap.tileMap.put(id, this);

        this.id = id;
        this.decoderId = decoderId;
        this.x = x;
        this.y = y;
    }

    public static AssetCache getById(int id) {
        return AssetMap.tileMap.get(id);
    }

    public int getDecoderId() {
        return decoderId;
    }

    public void setDecoderId(int decoderId) {
        this.decoderId = decoderId;
    }

    public Rect getRectangle() {
        return new Rect(x * 32, y * 32, x * 32 + 32, y * 32 + 32);
    }


    private enum AssetGroup {

    }

}
