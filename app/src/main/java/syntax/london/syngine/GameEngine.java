package syntax.london.syngine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cache.AssetCache;

public class GameEngine extends AppCompatActivity {

    private GameView gameView;
    private static SparseArray<BitmapRegionDecoder> decoders;
    private static String TAG = GameEngine.class.getSimpleName();
    private static List<List<Bitmap>> level = null;
    private static int currDepth = 0;
    private static int currRoomX = 0;
    private static int currRoomY = 0;

    private Bitmap getBitmapForAsset(AssetCache asset) {
//        Log.d(TAG,asset.getRectangle().toString());
//        Log.d(TAG,asset.toString());
        Bitmap tile = decoders.get(asset.getDecoderId()).decodeRegion(asset.getRectangle(), null);
        Log.d(TAG, tile.toString());
        return tile;

    }
    public String getJsonFromFile(String filename) {
        String json = null;
        InputStream is = null;

        try {
            is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return json;
    }

    private boolean loadRoom() {

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(getJsonFromFile(String.format("rooms/level_%d_%d_%d.json",currDepth,currRoomX,currRoomY)));
        } catch (JSONException e) {
            Log.e(TAG, "Error loading room " + currDepth + currRoomX + currRoomY);
            e.printStackTrace();
        }
        String json = String.valueOf(jsonObject);
        Log.d(TAG, "Json: " + json);

        Gson gson = new Gson();
        LevelJson newLevel = gson.fromJson(json, LevelJson.class);
        Log.d(TAG, newLevel.toString());

        level = new ArrayList<>();
        for (int i = 0; i < newLevel.getTiles().length; i++) {
            level.add(new ArrayList<Bitmap>());
            for (char c : newLevel.getTiles()[i].toCharArray()) {
                level.get(i).add(getBitmapForAsset(AssetCache.getById(Character.getNumericValue(c))));
            }
        }

        Log.d(TAG, "Room loaded: " + level);

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_engine);
        try {
            decoders = new SparseArray<>();
            decoders.put(1, BitmapRegionDecoder.newInstance(getAssets().open("hyptosis_tile1.png"), true));
            decoders.put(2, BitmapRegionDecoder.newInstance(getAssets().open("hyptosis_tile2.png"), true));
            decoders.put(3, BitmapRegionDecoder.newInstance(getAssets().open("hyptosis_tile3.png"), true));
            decoders.put(4, BitmapRegionDecoder.newInstance(getAssets().open("hyptosis_tile4.png"), true));
            decoders.put(5, BitmapRegionDecoder.newInstance(getAssets().open("hyptosis_tile5.png"), true));

            Log.v(TAG, "Finished loading assets");
        } catch (IOException e) {
            e.printStackTrace();
        }

        gameView = new GameView(this);
        setContentView(gameView);

    }

    class GameView extends SurfaceView implements Runnable {
        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.setKeepScreenOn(true);
            holder.setFixedSize(32 * 20, 32 * 20);
            paint = new Paint();

        }

        Thread gameThread = null;
        SurfaceHolder holder;

        volatile boolean playing;
        Canvas canvas;
        Paint paint;
        long fps;
        private long timeThisFrame;
        boolean isMoving = false;
        float walkSpeedPerSecond = 150;
        float posX = 10;
        float posY = 200;

        @Override
        public void run() {
            while (playing) {
                long startFrameTime = System.currentTimeMillis();
                update();
                draw();

                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame > 0) {
                    fps = 1000 / timeThisFrame;
                }
            }

        }

        public void update() {
            if (level == null) {
                if (!loadRoom()) {
                    return;
                }
            }

            if (isMoving) {
                posX = posX + (walkSpeedPerSecond / fps);
            }
        }

        public void draw() {
            if (holder.getSurface().isValid()) {
                canvas = holder.lockCanvas();

                canvas.drawColor(Color.argb(255, 26, 128, 182));
                paint.setColor(Color.argb(255, 249, 129, 0));

                paint.setTextSize(45);
                canvas.drawText("FPS:" + fps, 20, 40, paint);

                drawLevel(canvas, paint);

                holder.unlockCanvasAndPost(canvas);
            }
        }

        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // The SurfaceView class implements onTouchListener
        // So we can override this method and detect screen touches.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:

                    // Set isMoving so Bob is moved in the update method
                    isMoving = true;

                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    // Set isMoving so Bob does not move
                    isMoving = false;

                    break;
            }
            return true;

        }
    }

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        gameView.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        gameView.pause();
    }

    public void drawLevel(Canvas canvas, Paint paint) {
        for (int i = 0; i < level.size(); i++) {
            for (int j = 0; j < level.get(i).size(); j++) {
//                Matrix matrix = new Matrix();
//                matrix.reset();
//                matrix.postScale(2.0f,2.0f);
//                matrix.postTranslate(getWidth(),)
                //Log.d(TAG, level.get(i).get(j).toString());
                canvas.drawBitmap(level.get(i).get(j), j * 32, i * 32, paint);
            }
        }
    }

    private class LevelJson {
        String width;
        String height;
        String[] tiles;

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }

        public String[] getTiles() {
            return tiles;
        }

        public void setTiles(String[] tiles) {
            this.tiles = tiles;
        }
    }
}
