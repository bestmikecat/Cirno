package nep.timeline.cirno.ui.page.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nep.timeline.cirno.BuildConfig
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialAboutPage(
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current

    MaterialPageScaffold(
        title = stringResource(R.string.about),
        padding = padding,
        navigationIcon = {
            IconButton(
                onClick = { navigator.pop() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.moon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF3A6B35),
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}-${BuildConfig.BUILD_TIME})",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MaterialNavigationCard(
                    title = stringResource(R.string.about_github_source),
                    icon = { MaterialIcon(Icons.Outlined.Code) },
                    onClick = { uriHandler.openUri("https://github.com/Adkimsm/Cirno") },
                )
                MaterialNavigationCard(
                    title = stringResource(R.string.about_telegram_channel),
                    icon = { MaterialIcon(Icons.Outlined.ChatBubble) },
                    onClick = { uriHandler.openUri("https://t.me/cirnoadk") },
                )
                MaterialNavigationCard(
                    title = stringResource(R.string.about_qq_group),
                    icon = { MaterialIcon(Icons.Outlined.Forum) },
                    onClick = { uriHandler.openUri("https://qm.qq.com/q/jPqwiLpHs6") },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
