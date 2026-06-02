package nep.timeline.cirno.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nep.timeline.cirno.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import kotlin.system.exitProcess

@Composable
fun RootDialog(showDialog: MutableState<Boolean>, useMaterial: Boolean = false) {
    if (!showDialog.value) {
        return
    }

    if (useMaterial) {
        MaterialRootDialog(showDialog)
    } else {
        MiuixRootDialog(showDialog)
    }
}

@Composable
private fun MiuixRootDialog(showDialog: MutableState<Boolean>) {
    OverlayDialog(
        title = stringResource(R.string.warning),
        summary = stringResource(R.string.not_root),
        show = showDialog.value,
        onDismissRequest = {
            showDialog.value = false
            exitProcess(0)
        },
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            MiuixTextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ok),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    showDialog.value = false
                    exitProcess(0)
                }
            )
        }
    }
}

@Composable
private fun MaterialRootDialog(showDialog: MutableState<Boolean>) {
    AlertDialog(
        onDismissRequest = {
            showDialog.value = false
            exitProcess(0)
        },
        title = {
            Text(text = stringResource(R.string.warning))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.not_root))
            }
        },
        confirmButton = {
            Button(onClick = {
                showDialog.value = false
                exitProcess(0)
            }) {
                Text(text = stringResource(R.string.ok))
            }
        },
    )
}
