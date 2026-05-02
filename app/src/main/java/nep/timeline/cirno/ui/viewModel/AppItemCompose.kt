package nep.timeline.cirno.ui.viewModel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import nep.timeline.cirno.R
import nep.timeline.cirno.entity.AppItem
import nep.timeline.cirno.ui.custom.CustomBasicComponent
import nep.timeline.cirno.ui.utils.AppContext
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun AppItemCompose(
    app: AppItem,
) {
    val configured = app.white || app.backgroundPlay || app.locationCheck != 0 || app.networkCheck
    var subtitle: String? = null
    var subtitleColor = Color(60, 179, 113)
    if (configured) {
        if (app.backgroundLevel == 1)
            subtitle = stringResource(R.string.direct_app)
        else if (app.backgroundLevel == 2)
            subtitle = stringResource(R.string.foreground_service)
        else if (app.idle)
            subtitle = stringResource(R.string.battery_opt)
    }

    CustomBasicComponent(
        title = app.appName,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        summary = app.packageName + if (app.userId != 0) "#" + app.userId else "",
        leftAction = {
            Image(
                painter = rememberDrawablePainter(drawable = app.appIcon),
                contentDescription = app.appName,
                modifier = Modifier.size(58.dp).padding(end = 16.dp)
            )
        },
        rightActions = {
            if (configured) {
                StatusTag(
                    label = when {
                        app.white -> stringResource(R.string.white_app)
                        app.backgroundPlay -> stringResource(R.string.background_play)
                        app.locationCheck != 0 -> stringResource(R.string.location_check)
                        app.networkCheck -> stringResource(R.string.netreceive_unfreeze)
                        else -> stringResource(R.string.other_config)
                    },
                    backgroundColor = if (app.white) (if (isSystemInDarkTheme()) Color.White else Color.Black) else Color(60, 179, 113),
                    contentColor = if (app.white && isSystemInDarkTheme()) Color.Black else Color.White
                )
            }
        },
        onClick = {
            AppContext.enterAppPage(app)
        },
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun StatusTag(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            text = label,
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight(750),
            maxLines = 1,
            softWrap = false
        )
    }
}
