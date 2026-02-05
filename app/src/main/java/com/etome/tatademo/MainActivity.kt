package com.etome.tatademo

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.etome.tatademo.model.ShapeType
import com.etome.tatademo.model.ToolType
import com.etome.tatademo.view.WhiteboardView
import com.etome.tatademo.viewmodel.WhiteboardViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: WhiteboardViewModel by viewModels()
    private lateinit var player: ExoPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)
        val playPauseBtn = findViewById<ImageView>(R.id.btnPlayPause)


        val playerView = findViewById<PlayerView>(R.id.playerView)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        playerView.useController = false

        val uri = Uri.parse("android.resource://${packageName}/${R.raw.sample}")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()


        playPauseBtn.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                playPauseBtn.setImageResource(R.drawable.play)
            } else {
                player.play()
                playPauseBtn.setImageResource(R.drawable.pause)
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPauseBtn.setImageResource(
                    if (isPlaying) R.drawable.pause else R.drawable.play
                )
            }
        })

        val whiteboardView = findViewById<WhiteboardView>(R.id.whiteboardView)
        whiteboardView.viewModel = viewModel

        val penBtn = findViewById<ImageView>(R.id.btnPen)
        val eraserBtn = findViewById<ImageView>(R.id.btnEraser)

        penBtn.setOnClickListener { viewModel.selectPen() }
        eraserBtn.setOnClickListener { viewModel.selectEraser() }

        viewModel.tool.observe(this) { tool ->
            penBtn.alpha = if (tool == ToolType.PEN) 1f else 0.5f
            eraserBtn.alpha = if (tool == ToolType.ERASER) 1f else 0.5f
        }

        val progressBar = findViewById<SeekBar>(R.id.videoProgress)
        val timeText = findViewById<TextView>(R.id.tvTime)
        val handler = android.os.Handler(mainLooper)

        val updateProgress = object : Runnable {
            override fun run() {
                if (player.duration > 0) {
                    val progress =
                        (player.currentPosition * 100 / player.duration).toInt()
                    progressBar.progress = progress

                    timeText.text =
                        "${formatTime(player.currentPosition)} / ${formatTime(player.duration)}"
                }
                handler.postDelayed(this, 500)
            }
        }

        handler.post(updateProgress)


        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && player.duration > 0) {
                    val seekPosition = player.duration * progress / 100
                    player.seekTo(seekPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ---- Colors ----
        val colorViews = mapOf(
            "#000000" to findViewById<View>(R.id.colorBlack),
            "#F44336" to findViewById<View>(R.id.colorRed),
            "#2196F3" to findViewById<View>(R.id.colorBlue),
            "#4CAF50" to findViewById<View>(R.id.colorGreen),
            "#FFEB3B" to findViewById<View>(R.id.colorYellow)
        )

        colorViews.forEach { (color, view) ->
            view.setOnClickListener { viewModel.setColor(color) }
        }

        viewModel.color.observe(this) { selected ->
            colorViews.forEach { (color, view) ->
                highlightColor(view, color == selected)
            }
        }

        // ---- Stroke Width ----
        findViewById<ImageView>(R.id.btnThin).setOnClickListener {
            viewModel.setStrokeWidth(4f)
        }
        findViewById<ImageView>(R.id.btnMedium).setOnClickListener {
            viewModel.setStrokeWidth(8f)
        }
        findViewById<ImageView>(R.id.btnThick).setOnClickListener {
            viewModel.setStrokeWidth(14f)
        }

        // ---- Shapes ----
        findViewById<ImageView>(R.id.btnRect).setOnClickListener {
            viewModel.selectShape(ShapeType.RECTANGLE)
        }

        findViewById<ImageView>(R.id.btnCircle).setOnClickListener {
            viewModel.selectShape(ShapeType.CIRCLE)
        }

        findViewById<ImageView>(R.id.btnLine).setOnClickListener {
            viewModel.selectShape(ShapeType.LINE)
        }

        findViewById<ImageView>(R.id.btnPolygon).setOnClickListener {
            viewModel.setPolygonSides(5) // default polygon
            viewModel.selectShape(ShapeType.POLYGON)
        }
        findViewById<ImageView>(R.id.btnText).setOnClickListener {
            Toast.makeText(this, "Double-tap on the whiteboard to add text", Toast.LENGTH_LONG)
                .show()
            viewModel.selectTextTool()
        }

        findViewById<Button>(R.id.buttonSave).setOnClickListener {
            viewModel.saveWhiteboard(this)
            Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show()

        }


        loadLatestWhiteboardSafe(whiteboardView)

    }

    private fun highlightColor(view: View, active: Boolean) {
        view.alpha = if (active) 1f else 0.4f
        view.scaleX = if (active) 1.15f else 1f
        view.scaleY = if (active) 1.15f else 1f
    }


    private fun loadLatestWhiteboardSafe(
        whiteboardView: WhiteboardView
    ) {
        val files = filesDir.listFiles()
            ?.filter { it.name.startsWith("whiteboard_") && it.name.endsWith(".json") }

        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "No saved whiteboard found", Toast.LENGTH_SHORT).show()
            return
        }

        val latestFile = files.maxByOrNull { it.lastModified() } ?: run {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            return
        }

        val json = latestFile.readText()
        viewModel.loadFromJson(json)
        whiteboardView.invalidate()

        Toast.makeText(this, "Loaded: ${latestFile.name}", Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
