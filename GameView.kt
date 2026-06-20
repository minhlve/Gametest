package com.example.breakout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * ====================================================================
 *  GAMEVIEW.KT — "BỘ NÃO" CỦA GAME PHÁ GẠCH
 * ====================================================================
 *
 * GameView kế thừa từ "View" — lớp cơ bản nhất của Android dùng để
 * vẽ hình lên màn hình và nhận sự kiện chạm tay.
 *
 * Một game 2D đơn giản luôn xoay quanh 3 việc, lặp đi lặp lại ~60 lần/giây:
 *   1. UPDATE: cập nhật vị trí/trạng thái (bóng di chuyển tới đâu, có va chạm gì không)
 *   2. DRAW:   vẽ lại mọi thứ lên màn hình dựa theo trạng thái mới
 *   3. INPUT:  lắng nghe người chơi chạm/kéo tay để điều khiển
 *
 * Trong file này, 3 việc đó tương ứng với 3 hàm: update(), onDraw(), onTouchEvent().
 */
class GameView(context: Context) : View(context) {

    // ---------- "PAINT" = CÂY CỌ VẼ, QUYẾT ĐỊNH MÀU SẮC ----------
    // Mỗi Paint chỉ là một "cây cọ" được tô sẵn 1 màu, dùng để vẽ hình tương ứng.
    private val paddlePaint = Paint().apply { color = Color.WHITE }
    private val ballPaint = Paint().apply { color = Color.YELLOW }
    private val brickPaint = Paint().apply { color = Color.parseColor("#E53935") }
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#101010") }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true // làm chữ mượt, không bị răng cưa
    }

    // ---------- KÍCH THƯỚC MÀN HÌNH (đơn vị: pixel) ----------
    // "var" = biến có thể thay đổi giá trị sau này.
    // Ta chưa biết kích thước màn hình lúc khai báo, nên gán tạm 0f.
    private var screenWidth = 0f
    private var screenHeight = 0f

    // ---------- PADDLE (THANH TRƯỢT NGƯỜI CHƠI ĐIỀU KHIỂN) ----------
    private var paddleWidth = 0f
    private val paddleHeight = 40f
    private var paddleX = 0f   // tọa độ X của góc TRÁI paddle
    private var paddleY = 0f   // tọa độ Y (cố định, gần đáy màn hình)

    // ---------- BÓNG ----------
    private val ballRadius = 22f
    private var ballX = 0f
    private var ballY = 0f
    private var ballSpeedX = 14f   // mỗi khung hình, bóng di chuyển bao nhiêu pixel theo X
    private var ballSpeedY = -14f  // âm = đang bay lên, dương = đang rơi xuống

    // ---------- LƯỚI GẠCH ----------
    private val rows = 5
    private val cols = 6
    private var brickWidth = 0f
    private val brickHeight = 80f
    private val brickGap = 12f
    // "Array(rows) { BooleanArray(cols) { true } }" tạo ra một bảng 2 chiều (5 hàng x 6 cột),
    // mỗi ô là true/false. true = viên gạch còn sống, false = đã bị phá vỡ.
    private val bricks = Array(rows) { BooleanArray(cols) { true } }

    // ---------- TRẠNG THÁI CHUNG CỦA GAME ----------
    private var score = 0
    private var isGameOver = false
    private var isGameWon = false
    private var hasStarted = false

    // ====================================================================
    //  VÒNG LẶP GAME (GAME LOOP)
    // ====================================================================
    // Handler cho phép "hẹn giờ" chạy lại một đoạn code sau X mili giây.
    private val handler = Handler(Looper.getMainLooper())
    private val frameDelayMs = 16L // ~60 khung hình/giây (1000ms ÷ 60 ≈ 16ms)

    // "Runnable" là một đoạn code có thể được chạy theo yêu cầu.
    // Đây chính là "trái tim" của game: nó tự gọi lại CHÍNH NÓ liên tục,
    // tạo thành một vòng lặp vô tận chạy suốt thời gian chơi game.
    private val gameLoop = object : Runnable {
        override fun run() {
            update()                                   // 1. cập nhật trạng thái
            invalidate()                                // 2. yêu cầu Android gọi lại onDraw()
            handler.postDelayed(this, frameDelayMs)      // 3. hẹn giờ tự gọi lại sau 16ms
        }
    }

    // "init" là đoạn code chạy ngay khi đối tượng GameView được tạo ra lần đầu.
    init {
        handler.post(gameLoop) // bắt đầu vòng lặp game ngay từ đầu
    }

    /**
     * onSizeChanged được Android TỰ ĐỘNG gọi ngay khi nó đo được
     * kích thước thật (pixel) của màn hình điện thoại.
     * Ta dùng thời điểm này để đặt vị trí ban đầu cho paddle, bóng, gạch.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        paddleWidth = screenWidth / 5f
        paddleX = (screenWidth - paddleWidth) / 2f
        paddleY = screenHeight - 150f

        ballX = screenWidth / 2f
        ballY = paddleY - ballRadius - 5f

        // Chia đều chiều rộng màn hình cho số cột gạch (trừ đi khoảng cách giữa các viên)
        brickWidth = (screenWidth - brickGap * (cols + 1)) / cols
    }

    // ====================================================================
    //  1. UPDATE — "LUẬT CHƠI": bóng di chuyển & va chạm như thế nào
    // ====================================================================
    private fun update() {
        // Nếu game chưa bắt đầu, hoặc đã thua/thắng -> không cập nhật gì thêm
        if (!hasStarted || isGameOver || isGameWon) return

        // Di chuyển bóng theo vận tốc hiện tại
        ballX += ballSpeedX
        ballY += ballSpeedY

        // Va tường trái/phải -> bật ngược hướng X (giống quả bóng nảy vào tường)
        if (ballX - ballRadius <= 0 || ballX + ballRadius >= screenWidth) {
            ballSpeedX = -ballSpeedX
        }

        // Va tường trên -> bật ngược hướng Y
        if (ballY - ballRadius <= 0) {
            ballSpeedY = -ballSpeedY
        }

        // Va paddle -> bật bóng lên trên (chỉ khi bóng đang RƠI XUỐNG, tránh bật 2 lần dính)
        val ballChamPaddleTheoY = ballY + ballRadius >= paddleY && ballY - ballRadius <= paddleY + paddleHeight
        val ballChamPaddleTheoX = ballX >= paddleX && ballX <= paddleX + paddleWidth
        if (ballChamPaddleTheoY && ballChamPaddleTheoX && ballSpeedY > 0) {
            ballSpeedY = -ballSpeedY
        }

        // Bóng rơi qua khỏi đáy màn hình -> Thua
        if (ballY - ballRadius > screenHeight) {
            isGameOver = true
        }

        checkBrickCollision()
    }

    /**
     * Kiểm tra bóng có đang chạm viên gạch nào không.
     * Nếu có: viên đó vỡ (false), cộng điểm, bóng bật ngược hướng Y.
     */
    private fun checkBrickCollision() {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!bricks[row][col]) continue // gạch này đã vỡ rồi -> bỏ qua, xét viên tiếp theo

                val left = brickGap + col * (brickWidth + brickGap)
                val top = 100f + row * (brickHeight + brickGap)
                val right = left + brickWidth
                val bottom = top + brickHeight

                // So sánh gần đúng: bóng (hình vuông bao quanh nó) có chạm vào ô gạch không?
                val vaCham = ballX + ballRadius >= left && ballX - ballRadius <= right &&
                        ballY + ballRadius >= top && ballY - ballRadius <= bottom

                if (vaCham) {
                    bricks[row][col] = false   // viên gạch vỡ
                    score += 10
                    ballSpeedY = -ballSpeedY    // bóng bật ngược lại

                    if (isAllBricksBroken()) {
                        isGameWon = true
                    }
                    return // mỗi lần chỉ vỡ tối đa 1 viên, rồi dừng kiểm tra
                }
            }
        }
    }

    /** Trả về true nếu TẤT CẢ viên gạch đã bị phá vỡ (người chơi thắng). */
    private fun isAllBricksBroken(): Boolean {
        for (row in bricks) {
            for (brickConSong in row) {
                if (brickConSong) return false
            }
        }
        return true
    }

    // ====================================================================
    //  2. DRAW — VẼ MỌI THỨ LÊN MÀN HÌNH
    // ====================================================================
    /**
     * onDraw() được Android TỰ GỌI mỗi khi cần vẽ lại màn hình
     * (ta yêu cầu điều này bằng cách gọi invalidate() trong gameLoop ở trên).
     * "Canvas" giống như một tấm vải trắng để ta vẽ hình lên đó.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ nền phủ toàn màn hình
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)

        // Vẽ từng viên gạch còn sống
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!bricks[row][col]) continue
                val left = brickGap + col * (brickWidth + brickGap)
                val top = 100f + row * (brickHeight + brickGap)
                canvas.drawRect(left, top, left + brickWidth, top + brickHeight, brickPaint)
            }
        }

        // Vẽ paddle (hình chữ nhật)
        canvas.drawRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, paddlePaint)

        // Vẽ bóng (hình tròn)
        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)

        // Vẽ điểm số ở trên cùng
        canvas.drawText("Điểm: $score", screenWidth / 2f, 70f, textPaint)

        // Vẽ thông báo tùy theo trạng thái game
        when {
            !hasStarted -> canvas.drawText("Chạm màn hình để bắt đầu", screenWidth / 2f, screenHeight / 2f, textPaint)
            isGameOver -> canvas.drawText("Thua rồi! Chạm để chơi lại", screenWidth / 2f, screenHeight / 2f, textPaint)
            isGameWon -> canvas.drawText("Thắng rồi! Chạm để chơi lại", screenWidth / 2f, screenHeight / 2f, textPaint)
        }
    }

    // ====================================================================
    //  3. INPUT — XỬ LÝ CẢM ỨNG CỦA NGƯỜI CHƠI
    // ====================================================================
    /**
     * onTouchEvent được Android gọi mỗi khi người dùng chạm/kéo tay
     * trên vùng màn hình của GameView này.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (isGameOver || isGameWon) {
                    // Nếu game đã kết thúc, chạm vào màn hình -> chơi lại từ đầu
                    resetGame()
                } else {
                    hasStarted = true
                    // Di chuyển paddle để TÂM của nó nằm dưới đúng vị trí ngón tay.
                    // coerceIn() đảm bảo paddle không bị kéo ra ngoài màn hình.
                    paddleX = (event.x - paddleWidth / 2f)
                        .coerceIn(0f, screenWidth - paddleWidth)
                }
            }
        }
        return true // trả về true = "tôi đã xử lý sự kiện chạm này"
    }

    /** Đặt lại toàn bộ trạng thái game để bắt đầu một lượt chơi mới. */
    private fun resetGame() {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                bricks[row][col] = true
            }
        }
        score = 0
        isGameOver = false
        isGameWon = false
        hasStarted = false
        ballX = screenWidth / 2f
        ballY = paddleY - ballRadius - 5f
        ballSpeedX = 14f
        ballSpeedY = -14f
    }
}
