package mx.udlap.is522.tedroid.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import mx.udlap.is522.tedroid.view.model.DefaultShape;
import mx.udlap.is522.tedroid.view.model.Direction;
import mx.udlap.is522.tedroid.view.model.Tetromino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tablero del juego donde los tetrominos se acumlan.
 * 
 * @author Daniel Pedraza-Arcega, Andrés Peña-Peralta, Wassim Lima Saad
 * @since 1.0
 */
public class GameBoardView extends View {

    private static final float MOVE_SENSITIVITY = 3.5f;
    private static final long DEFAULT_SPEED = 850l;
    private static final int DROPDOWN_FACTOR = 10;
    private static final int DEFAULT_COLUMNS = 10;
    private static final int DEFAULT_ROWS = 20;
    private static final String TAG = GameBoardView.class.getSimpleName();

    private Tetromino currentTetromino;
    private Tetromino nextTetromino;
    private int[][] boardMatrix;
    private int rows;
    private int columns;
    private int tetrominoDownMoves;
    private long speed;
    private float width;
    private float height;
    private boolean startDropingTetrominos;
    private boolean isPaused;
    private boolean isGameOver;
    private GestureDetector gestureDetector;
    private MoveDownCurrentTetrominoTask moveDownCurrentTetrominoTask;
    private Paint tetrominoBorder;
    private Paint tetrominoForeground;
    private Paint background;
    private OnCommingNextTetrominoListener onCommingNextTetrominoListener;
    private OnPointsGainedListener onPointsGainedListener;
    private OnGameOverListener onGameOverListener;

    /**
     * Construye un tablero de juego.
     * 
     * @see android.view.View#View(Context)
     */
    public GameBoardView(Context context) {
        super(context);
        setUp();
    }

    /**
     * Construye un tablero de juego mediante XML
     * 
     * @see android.view.View#View(Context, AttributeSet)
     */
    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    /**
     * Construye un tablero de juego mediante XML y aplicando un estilo.
     * 
     * @see android.view.View#View(Context, AttributeSet, int)
     */
    public GameBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUp();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w / ((float) boardMatrix[0].length);
        height = h / ((float) boardMatrix.length);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!startDropingTetrominos) {
            startDropingTetrominos = true;
            stopDropingTaskIfNeeded();
            currentTetromino = getNextTetromino();
            nextTetromino = getNextTetromino();
            if (onCommingNextTetrominoListener != null) onCommingNextTetrominoListener.onCommingNextTetromino(nextTetromino);
            startDropingTask(speed);
        }

        currentTetromino.drawOnParentGameBoardView(canvas);
        drawBoardMatrix(canvas);
        drawBackground(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (!isPaused && !isGameOver) {
                    stopDropingTaskIfNeeded();
                    startDropingTask(speed);
                }
                return true;
            default: return super.onTouchEvent(event);
        }
    }

    /**
     * Inicializa el layout de este tablero.
     */
    protected void setUp() {
        if (rows == 0 || columns == 0) {
            rows = DEFAULT_ROWS;
            columns = DEFAULT_COLUMNS;
        }

        if (speed == 0) speed = DEFAULT_SPEED;

        buildBoardMatrix();
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        isPaused = false;
        isGameOver = false;
        tetrominoBorder = new Paint();
        tetrominoBorder.setStyle(Paint.Style.STROKE);
        tetrominoBorder.setColor(getContext().getResources().getColor(android.R.color.black));
        tetrominoForeground = new Paint();
        tetrominoForeground.setStyle(Paint.Style.FILL);
        background = new Paint();
        background.setStyle(Paint.Style.STROKE);
        background.setStrokeWidth(2);
        background.setColor(getContext().getResources().getColor(android.R.color.black));
    }

    /**
     * Construye la matriz y la llena de color transparente.
     */
    private void buildBoardMatrix() {
        boardMatrix = new int[rows][columns];
        for (int[] row: boardMatrix) Arrays.fill(row, android.R.color.transparent);
    }

    /**
     * Dibuja el fondo del tablero.
     * 
     * @param canvas un canvas para dibujar.
     */
    protected void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), background);
    }

    /**
     * Actualiza la matriz del tablero con los valores del tetromino actual.
     */
    protected void updateBoardMatrix() {
        int[][] shapeMatrix = currentTetromino.getShapeMatrix();
        for (int row = 0; row < shapeMatrix.length; row++) {
            for (int column = 0; column < shapeMatrix[0].length; column++) {
                if (shapeMatrix[row][column] != android.R.color.transparent) {
                    int boardMatrixRow = currentTetromino.getPositionOnBoard().getY() + row;
                    int boardMatrixColumn = currentTetromino.getPositionOnBoard().getX() + column;
                    boardMatrix[boardMatrixRow][boardMatrixColumn] = shapeMatrix[row][column];
                }
            }
        }
    }

    /**
     * Dibuja los tetrominos acumlados en el tablero.
     * 
     * @param canvas un Canvas donde dibujar.
     */
    protected void drawBoardMatrix(Canvas canvas) {
        for (int row = 0; row < boardMatrix.length; row++) {
            for (int column = 0; column < boardMatrix[0].length; column++) {
                if (boardMatrix[row][column] != android.R.color.transparent) {
                    float x0 = column * width;
                    float y0 = row * height;
                    float x1 = (column + 1) * width;
                    float y1 = (row + 1) * height;
                    tetrominoForeground.setColor(getContext().getResources().getColor(boardMatrix[row][column]));
                    canvas.drawRect(x0, y0, x1, y1, tetrominoForeground);
                    canvas.drawRect(x0, y0, x1, y1, tetrominoBorder);
                }
            }
        }
    }

    /**
     * Checa si hay lineas completas para borrar.
     * 
     * @return la lista con los indicies de las filas completas o una lista
     *         vacia.
     */
    protected List<Integer> checkForCompletedLines() {
        ArrayList<Integer> rowsToClear = new ArrayList<Integer>(4);
        for (int row = 0; row < boardMatrix.length; row++) {
            boolean isComplete = true;
            for (int column = 0; column < boardMatrix[0].length; column++) {
                if (boardMatrix[row][column] == android.R.color.transparent) {
                    isComplete = false;
                    break;
                }
            }

            if (isComplete) rowsToClear.add(row);
        }

        return rowsToClear;
    }

    /**
     * Limpia las lineas completas y baja las lineas arriba de las lineas
     * completas.
     * 
     * @param rowsToClear los indicies de las filas que hay que limpiar.
     */
    protected void clearCompletedLines(List<Integer> rowsToClear) {
        for (int rowToClear : rowsToClear) {
            for (int row = rowToClear; row >= 0; row--) {
                boardMatrix[row] = new int[boardMatrix[row].length];
                if (row == 0) Arrays.fill(boardMatrix[0], android.R.color.transparent);
                else System.arraycopy(boardMatrix[row - 1], 0, boardMatrix[row], 0, boardMatrix[row].length);
            }
        }
    }

    /**
     * Detiene la caida del tetromino actual si esta callendo.
     */
    protected void stopDropingTaskIfNeeded() {
        if (moveDownCurrentTetrominoTask != null && moveDownCurrentTetrominoTask.getStatus() == AsyncTask.Status.RUNNING) {
            moveDownCurrentTetrominoTask.cancel(true);
            moveDownCurrentTetrominoTask = null;
        }
    }

    /**
     * Inicia la caida del tetromino actual.
     */
    protected void startDropingTask(long speed) {
        moveDownCurrentTetrominoTask = new MoveDownCurrentTetrominoTask();
        moveDownCurrentTetrominoTask.execute(speed);
    }

    /**
     * @return tetromino escogiendo al azar una de las figuras predefinadas.
     */
    protected Tetromino getNextTetromino() {
        int randomIndex = new Random().nextInt(DefaultShape.values().length);
        DefaultShape randomShape = DefaultShape.values()[randomIndex];
        return new Tetromino.Builder(this)
            .use(randomShape)
            .build();
    }

    /**
     * Reinicia el juego.
     */
    public void restartGame() {
        stopDropingTaskIfNeeded();
        buildBoardMatrix();
        startDropingTetrominos = false;
        isPaused = false;
        isGameOver = false;
        invalidate();
    }

    /**
     * @return la matriz del tablero.
     */
    public int[][] getBoardMatrix() {
        return boardMatrix;
    }

    /**
     * @return la altura del tablero.
     */
    public float getBoardHeight() {
        return height;
    }

    /**
     * @return la anchura del tablero.
     */
    public float getBoardWidth() {
        return width;
    }

    /**
     * @return si el juego esta pausado o no.
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * @return si el juego esta detenido o no.
     */
    public boolean isStopped() {
        return moveDownCurrentTetrominoTask == null || moveDownCurrentTetrominoTask.getStatus() != AsyncTask.Status.RUNNING;
    }

    /**
     * @return si el juego termino finalizo o no.
     */
    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * Pausa el juego.
     */
    public void pauseGame() {
        isPaused = true;
    }

    /**
     * Reanuda el juego.
     */
    public void resumeGame() {
        isPaused = false;
    }

    /**
     * Detiene el juego y no se podrá reinciar más
     */
    public void stopGame() {
        stopDropingTaskIfNeeded();
    }

    /**
     * @return el tetromino en juego.
     */
    public Tetromino getCurrentTetromino() {
        return currentTetromino;
    }

    /**
     * @param onCommingNextTetrominoListener el listener que escuchará cuando
     *        haya un nuevo tetromino listo.
     */
    public void setOnCommingNextTetrominoListener(OnCommingNextTetrominoListener onCommingNextTetrominoListener) {
        this.onCommingNextTetrominoListener = onCommingNextTetrominoListener;
    }

    /**
     * @param onPointsGainedListener listener que escuchará cuando caiga un
     *        tetromino y cuando haya lineas completas.
     */
    public void setOnPointsGainedListener(OnPointsGainedListener onPointsGainedListener) {
        this.onPointsGainedListener = onPointsGainedListener;
    }

    /**
     * @param onGameOverListener listener que escuchará cuando se acabe el
     *        juego.
     */
    public void setOnGameOverListener(OnGameOverListener onGameOverListener) {
        this.onGameOverListener = onGameOverListener;
    }

    /**
     * Listener que que escuchará cuando haya un nuevo tetromino listo.
     * 
     * @author Daniel Pedraza-Arcega
     * @since versión 1.0
     */
    public static interface OnCommingNextTetrominoListener {

        /**
         * Ejecuta este método cuando se genera el siguente tetromino.
         * 
         * @param nextTetromino el siguente tetromino en caer.
         */
        void onCommingNextTetromino(Tetromino nextTetromino);
    }

    /**
     * Listener que escuchará cuando caiga un tetromino y cuando haya 
     * lineas completas.
     * 
     * @author Daniel Pedraza-Arcega
     * @since versión 1.0
     */
    public static interface OnPointsGainedListener {

        /**
         * Ejecuta este método cuando hay lineas que se completaron.
         * 
         * @param linesCleared el numero de lineas completas.
         */
        void onClearedLines(int linesCleared);
    }

    /**
     * Listener que escuchará cuando se acabe el juego.
     * @author Daniel Pedraza-Arcega
     * @since versión 1.0
     */
    public static interface OnGameOverListener {

        /**
         * Ejecuta este método cuando se termina el juego.
         */
        void onGameOver();
    }
    
    /**
     * Inicializa las dimensiones del tablero de juego.
     * 
     * @param rows cuantas filas.
     * @param columns cuantas columnas.
     */
    public void setCustomDimensions(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        buildBoardMatrix();
    }

    /**
     * Inicializa la velocidad del juego
     * 
     * @param speed velociadad en milisegundos.
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }

    /**
     * Le velocidad del nivel aumentará.
     */
    public void levelUp() {
        speed *= 0.85;
    }

    /**
     * Tarea que lleva la cuenta de la velocidad de caida del tetromino en
     * juego.
     * 
     * @author Daniel Pedraza-Arcega
     * @since 1.0
     */
    private class MoveDownCurrentTetrominoTask extends AsyncTask<Long, Void, Void> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(Long... params) {
            while (!isCancelled()) {
                try {
                    Thread.sleep(params[0]);
                    publishProgress();
                } catch (InterruptedException e) { }
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onProgressUpdate(Void... values) {
            if (!isPaused && !isGameOver) {
                if (!currentTetromino.moveTo(Direction.DOWN)) {
                    if (tetrominoDownMoves == 0) {
                        Log.i(TAG, "Game over");
                        stopGame();
                        isGameOver = true;
                        if (onGameOverListener != null) onGameOverListener.onGameOver();
                    } else {
                        tetrominoDownMoves = 0;
                        updateBoardMatrix();
                        List<Integer> rowsToClear = checkForCompletedLines();
                        if (!rowsToClear.isEmpty()) {
                            clearCompletedLines(rowsToClear);
                            if (onPointsGainedListener != null) onPointsGainedListener.onClearedLines(rowsToClear.size());
                        }
                        currentTetromino = nextTetromino;
                        nextTetromino = getNextTetromino();
                        if (onCommingNextTetrominoListener != null) onCommingNextTetrominoListener.onCommingNextTetromino(nextTetromino);
                    }
                } else {
                    tetrominoDownMoves++;
                    Log.d(TAG, "Move down tetromino");
                }

                invalidate();
            }
        }
    }

    /**
     * Escucha los eventos del tablero para mover el tetromino en juego.
     * 
     * @author Daniel Pedraza-Arcega
     * @since 1.0
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            if (!isPaused && !isGameOver) {
                stopDropingTaskIfNeeded();
                startDropingTask(speed / DROPDOWN_FACTOR);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isPaused && !isGameOver) {
                if (distanceX < -MOVE_SENSITIVITY) {
                    if (currentTetromino.moveTo(Direction.RIGHT)){
                        Log.d(TAG, "Move tetromino to the right");
                        invalidate();
                        return true;
                    }
                } else if (distanceX > MOVE_SENSITIVITY) {
                    if (currentTetromino.moveTo(Direction.LEFT)) {
                        Log.d(TAG, "Move tetromino to the left");
                        invalidate();
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (!isPaused && !isGameOver) {
                if (currentTetromino.rotate()) {
                    Log.d(TAG, "Rotate tetromino");
                    invalidate();
                    return true;
                }
            }
            return false;
        }
    }
}