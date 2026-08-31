package com.agustinazorin.finanzas.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.agustinazorin.finanzas.R

/** Rutas de nivel superior, visibles en la barra de navegación inferior. */
sealed class FinanzasDestination(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Home : FinanzasDestination("home", R.string.nav_home, Icons.Filled.Home)
    object Transactions : FinanzasDestination("transactions", R.string.nav_transactions, Icons.Filled.List)
    object Accounts : FinanzasDestination("accounts", R.string.nav_accounts, Icons.Filled.AccountBalanceWallet)
    object Summary : FinanzasDestination("summary", R.string.nav_summary, Icons.Filled.PieChart)
    object More : FinanzasDestination("more", R.string.nav_more, Icons.Filled.MoreHoriz)

    companion object {
        // `by lazy` (no un `val` directo): un `val` acá se inicializa como parte del <clinit> de
        // esta clase sealed, en el mismo momento en que la JVM está inicializando
        // `FinanzasDestination` — y esa inicialización todavía no llegó a crear las instancias de
        // Home/Transactions/etc (cada `object` es una subclase separada, inicializada en su propio
        // <clinit>, que a su vez requiere que el de esta clase ya haya terminado). La JVM detecta
        // esa dependencia circular en el mismo hilo y la corta devolviendo instancias sin inicializar,
        // así que la lista termina con elementos `null` — de ahí el NPE en tiempo de ejecución. `by
        // lazy` difiere la creación de la lista hasta el primer acceso real, momento en el que el
        // <clinit> de esta clase ya terminó y las instancias se resuelven bien.
        val bottomBarDestinations: List<FinanzasDestination> by lazy {
            listOf(Home, Transactions, Accounts, Summary, More)
        }
    }
}

/** Rutas secundarias, alcanzables desde Home o desde la pantalla "Más". */
object SecondaryRoutes {
    const val QUICK_ADD = "quick_add"
    const val CATEGORIES = "categories"
    const val HOUSEHOLD_MEMBERS = "household_members"
    const val HOUSEHOLD_REPORT = "household_report"
    const val COMMITTED = "committed"
    const val CASH_FLOW = "cash_flow"
    const val PATRIMONIO = "patrimonio"
    const val CURRENCY = "currency"
    const val RECEIPTS = "receipts"
    const val SECURITY = "security"
    const val INCOME = "income"
    const val RECURRING = "recurring"
    const val CAPTURE_SETTINGS = "capture_settings"
    const val CAPTURE_REVIEW = "capture_review"
    const val CATEGORY_RULES = "category_rules"

    private const val CREDIT_CARD_DETAIL_BASE = "credit_card_detail"
    const val CREDIT_CARD_DETAIL_PATTERN = "$CREDIT_CARD_DETAIL_BASE/{accountId}"
    fun creditCardDetail(accountId: Long) = "$CREDIT_CARD_DETAIL_BASE/$accountId"
}
