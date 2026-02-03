import com.etome.tatademo.model.Shape
import com.etome.tatademo.model.Stroke
import com.etome.tatademo.model.TextItem

data class WhiteboardState(
    val strokes: MutableList<Stroke> = mutableListOf(),
    val shapes: MutableList<Shape> = mutableListOf(),
    val texts: MutableList<TextItem> = mutableListOf()
)