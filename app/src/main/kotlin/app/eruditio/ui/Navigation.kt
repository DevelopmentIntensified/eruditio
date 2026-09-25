package app.eruditio.ui

import android.content.Context
import android.content.Intent
import app.eruditio.EruditioApp
import app.eruditio.MainActivity

internal fun returnToEruditioToday(context: Context) {
    (context.applicationContext as? EruditioApp)?.container?.pauseToHome?.value = true
    context.startActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    )
}
