package nep.timeline.cirno.ui.custom

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset

suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (position: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    detectDragGestures(
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        onDrag = onDrag,
    )
}
