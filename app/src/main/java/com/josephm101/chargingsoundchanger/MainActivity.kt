package com.josephm101.chargingsoundchanger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SoundManager
import com.josephm101.chargingsoundchanger.helpers.soundmanager.Sounds
import com.josephm101.chargingsoundchanger.helpers.VersionHelper
import com.josephm101.chargingsoundchanger.helpers.VibrationHelper
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SaveSoundResult
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SoundManager.Companion.maxSoundDurationInMilliseconds
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SoundManager.Companion.maxSoundDurationInSeconds
import com.josephm101.chargingsoundchanger.helpers.soundmanager.SoundPlaybackResult
import com.josephm101.chargingsoundchanger.preferences.AppPreferences
import com.josephm101.chargingsoundchanger.preferences.ServicePreferences
import com.josephm101.chargingsoundchanger.service.ChargingSoundService
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardDefaultBodyTextStyle
import com.josephm101.chargingsoundchanger.ui.components.card.CustomCard.CustomCardWithTitleAndIconAndContent
import com.josephm101.chargingsoundchanger.ui.components.card.SwitchCard
import com.josephm101.chargingsoundchanger.ui.theme.BatterySoundChangerTheme
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.math.roundToInt

sealed class Screens(val route: String, val icon: ImageVector, val label: String) {
    data object Home : Screens("home", Icons.Default.Home, "Home")
    data object AdvancedSettings : Screens("advanced_settings", Icons.Default.Settings, "Advanced")

    companion object {
        fun getBottomNavigationItem(screen: Screens): BottomNavigationItem {
            return BottomNavigationItem(
                label = screen.label,
                icon = screen.icon,
                route = screen.route
            )
        }
    }
}

data class BottomNavigationItem(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Home,
    val route : String = ""
) {

    // function to get the list of bottomNavigationItems
    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            Screens.getBottomNavigationItem(screen = Screens.Home),
            Screens.getBottomNavigationItem(screen = Screens.AdvancedSettings)
        )
    }
}

class MainActivity : ComponentActivity() {
    lateinit var vibrator: VibrationHelper

    private val loopHandler = Handler(Looper.getMainLooper())
    private lateinit var servicePreferences: ServicePreferences
    private lateinit var appPreferences: AppPreferences

    private lateinit var appVersion: String
    private var appVersionCode: Long = 0

    val chargingSoundManager = SoundManager()

    private fun startChargingSoundService() {
        // Start ChargingSoundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(applicationContext, ChargingSoundService::class.java))
        } else {
            startService(Intent(applicationContext, ChargingSoundService::class.java))
        }
    }

    private fun notImplementedToast() {
        Toast.makeText(applicationContext, "Not implemented", Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences
        servicePreferences = ServicePreferences(applicationContext)
        appPreferences = AppPreferences(applicationContext)

        // (Try to) get app version information
        try {
            val pInfo: PackageInfo = VersionHelper.getAppPackageInfo(applicationContext)
            appVersion = pInfo.versionName
            appVersionCode = VersionHelper.getAppVersionCode(pInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            appVersion = "unknown (failed to retrieve)"
        }

        // Initialize vibration component
        vibrator = VibrationHelper(applicationContext)

        // startChargingSoundService() Now handled by PostNotificationPermissionsCard()

        val useNewNavigationSystem = true

        // Setup UI
        enableEdgeToEdge()
        setContent {
            BatterySoundChangerTheme {
                //var navigationSelectedItem by remember { mutableIntStateOf(0) }
                val navController = rememberNavController()
                val slideTransitionDuration = 600
                val scaleTransitionScale = 0.9f

                if (useNewNavigationSystem) {
                    NavHost(
                        navController = navController,
                        startDestination = Screens.Home.route
                    ) {
                        composable(
                            Screens.Home.route,
                            enterTransition = {
                                return@composable fadeIn(tween(1000))
                            }, exitTransition = {
                                return@composable slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    tween(slideTransitionDuration)
                                )
                            }, popEnterTransition = {
                                return@composable slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    tween(slideTransitionDuration)
                                ).plus(
                                    scaleIn(
                                        tween(slideTransitionDuration),
                                        initialScale = scaleTransitionScale
                                    )
                                )
                            }
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    /// TODO: Add "More" menu icon button with links to GitHub repo, a link to the "Issues" section, and an "About" dialog with credits
                                    TopAppBar(
                                        title = {
                                            Text(stringResource(id = R.string.app_display_name))
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = {
                                                    navController.navigate(Screens.AdvancedSettings.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    stringResource(R.string.settings_screen_title)
                                                )
                                            }
                                        }
                                    )
                                },
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    MainAppContent(navController)
                                }
                            }
                        }
                        composable(
                            Screens.AdvancedSettings.route,
                            enterTransition = {
                                return@composable slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    tween(slideTransitionDuration)
                                ).plus(
                                    scaleIn(
                                        tween(slideTransitionDuration),
                                        initialScale = scaleTransitionScale
                                    )
                                )
                            },
                            popExitTransition = {
                                return@composable slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    tween(slideTransitionDuration)
                                ).plus(
                                    scaleOut(
                                        tween(slideTransitionDuration),
                                        targetScale = scaleTransitionScale
                                    )
                                )
                            },
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text(stringResource(id = R.string.settings_screen_title))
                                        },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = {
                                                    if (navController.previousBackStackEntry != null) {
                                                        navController.navigateUp()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                    )
                                },
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    AdvancedSettingsScreen(navController)
                                }
                            }
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            /// TODO: Add "More" menu icon button with links to GitHub repo, a link to the "Issues" section, and an "About" dialog with credits
                            TopAppBar(
                                title = {
                                    Text(stringResource(id = R.string.app_display_name))
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                BottomNavigationItem().bottomNavigationItems()
                                    .forEachIndexed { _, navigationItem ->
                                        NavigationBarItem(
                                            //selected = index == navigationSelectedItem,
                                            selected = currentDestination?.hierarchy?.any { it.route == navigationItem.route } == true,
                                            label = {
                                                Text(navigationItem.label)
                                            },
                                            icon = {
                                                Icon(
                                                    navigationItem.icon,
                                                    contentDescription = navigationItem.label
                                                )
                                            },
                                            onClick = {
                                                //navigationSelectedItem = index
                                                navController.navigate(navigationItem.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                            }
                        }
                    ) { innerPadding ->
                        val transitionDurationMs = 300
                        val transitionIn = fadeIn(animationSpec = tween(transitionDurationMs))
                        val transitionOut = fadeOut(animationSpec = tween(transitionDurationMs))

                        NavHost(
                            navController,
                            startDestination = Screens.Home.route,
                            Modifier.padding(innerPadding),
                            //enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
                            //exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
                            //popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) },
                            //popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) }

                            enterTransition = { transitionIn },
                            exitTransition = { transitionOut },
                            popEnterTransition = { transitionIn },
                            popExitTransition = { transitionOut }
                        ) {
                            composable(
                                Screens.Home.route,
                                /*
                                popEnterTransition = {
                                    return@composable slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(slideTransitionDuration)
                                    )
                                },
                                exitTransition = {
                                    return@composable slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(slideTransitionDuration)
                                    )
                                }
                                 */
                            ) {
                                MainAppContent(navController)
                            }
                            composable(
                                Screens.AdvancedSettings.route,
                                /*
                                enterTransition = {
                                    return@composable slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(slideTransitionDuration)
                                    )//.plus(scaleIn(tween(slideTransitionDuration), initialScale = scaleTransitionScale))
                                },
                                popExitTransition = {
                                    return@composable slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.End,
                                        tween(slideTransitionDuration)
                                    )//.plus(scaleOut(tween(slideTransitionDuration), targetScale = scaleTransitionScale))
                                }
                                 */
                            ) {
                                AdvancedSettingsScreen(navController)
                            }
                        }
                    }
                }
                AndroidFreedomWarningDialog()
            }
        }
    }

    @Composable
    fun accentedText(text: String): AnnotatedString {
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(text)
            }
        }

        return annotatedText
    }

    @Composable
    fun AndroidFreedomWarningDialog() {
        val dialogIsShown = remember {
            mutableStateOf(appPreferences.keepAndroidOpenWarningWasShownForThisVersionCode != appVersionCode)
        }

        fun rememberCurrentVersionCode() {
            appPreferences.keepAndroidOpenWarningWasShownForThisVersionCode = appVersionCode
        }

        when {
            dialogIsShown.value -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogIsShown.value = false
                    },
                    title = {
                        Text(text = stringResource(R.string.keepAndroidOpenWarning_DialogTitle))
                    },
                    text = {
                        Text(stringResource(R.string.keepAndroidOpenWarning_DialogBody))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        getString(R.string.keepAndroidOpenURL).toUri()
                                    )
                                startActivity(intent)
                                dialogIsShown.value = false
                                rememberCurrentVersionCode()
                            },
                        ) {
                            Text(stringResource(R.string.keepAndroidOpenWarning_OpenWebpageButtonText))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                dialogIsShown.value = false
                                rememberCurrentVersionCode()
                            }
                        ) {
                            Text(getString(R.string.dismiss))
                        }
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AdvancedSettingsScreen(navController: NavHostController) {
        val vibrationDurationPreferenceCardShouldBeEnabled =
            remember { mutableStateOf(servicePreferences.vibrationEnabled) }
        Column(
            modifier = Modifier
                // .padding(8.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            PreferencesTextLine("Advanced settings")
            SoundVolumePreferenceCard()
            //RespectDoNotDisturbPreferenceCard()
            ShowDevMessagePreferenceCard()
            //DebounceEnabledPreferenceCard()
            PreferencesTextLine("Vibration")
            VibrationEnabledPreferenceCard(vibrationDurationPreferenceCardShouldBeEnabled)
            VibrationDurationPreferenceCard(vibrationDurationPreferenceCardShouldBeEnabled)
        }
    }

    @Composable
    fun MainAppContent(navController: NavHostController) {
        Column(
            modifier = Modifier
                // .padding(8.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            PostNotificationPermissionsCard()

            if (!permissionToIgnoreBatteryOptimizationsIsGranted()) {
                BatteryOptimizationPermissionCard()
            }

            if (!appPreferences.hideOverviewInfoMessageCard) {
                OverviewChangeSoundSettingsMessageCard()
            }

            if (!appPreferences.hideOverviewDevMessageCard) {
                AppBetaMessageCard()
            }

            // HorizontalDivider(modifier = Modifier.fillMaxWidth())
            PreferencesTextLine("Settings")
            SoundEnabledPreferenceCard()
            ChooseSoundPreferenceCard()

            // Draw footer
            Column(
                modifier = Modifier.padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
            ) {
                CenteredFooterText(stringResource(R.string.app_display_name))
                CenteredFooterText(
                    stringResource(
                        R.string.ui_overview_footer_appVersionString,
                        appVersion,
                        appVersionCode
                    )
                )
            }
        }
    }

    @Composable
    fun PreferencesTextLine(text: String) {
        Spacer(modifier = Modifier.size(width = 0.dp, height = 8.dp))
        Text(
            text = accentedText(text = text),
            modifier = Modifier.padding(
                //start = 24.dp,
                start = 8.dp,
                bottom = 4.dp,
                top = 10.dp
            )
        )
        //Text(text, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 10.dp))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun CenteredFooterText(text: String) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.6f)
                .basicMarquee(),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 14.sp,
            )
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PostNotificationPermissionsCard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val showRationaleDialog = remember { mutableStateOf(false) }

            // Check the status of the POST_NOTIFICATIONS permission
            val hasNotificationPermission = remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            // Request notification permissions
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (!isGranted) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            showRationaleDialog.value = true
                        }
                    } else {
                        hasNotificationPermission.value = true
                    }
                }
            )

            when {
                hasNotificationPermission.value -> {
                    Log.d("PostNotificationPermissions", "Permissions granted! Starting service...")
                    startChargingSoundService() // Start service
                }
            }

            AnimatedVisibility(
                visible = !hasNotificationPermission.value,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                CustomCardWithTitleAndIconAndContent(
                    title = stringResource(R.string.ui_notificationPermissionsRequired_dialogTitle),
                    iconResId = R.drawable.baseline_notifications_24,
                    //cardContainerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.ui_notificationPermissionsRequired_dialogBody),
                        style = cardDefaultBodyTextStyle
                    )
                    Spacer(modifier = Modifier.size(width = 0.dp, height = 12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = "package:$packageName".toUri()
                                startActivity(intent)
                            } else {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.request_permissions),
                            modifier = Modifier
                                .basicMarquee(velocity = 50.dp)
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = "package:$packageName".toUri()
                            startActivity(intent)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.open_android_settings_for_this_app),
                            modifier = Modifier
                                .basicMarquee(velocity = 50.dp)
                        )
                    }
                }
            }

            when {
                showRationaleDialog.value -> {
                    AlertDialog(
                        onDismissRequest = {
                            showRationaleDialog.value = false
                        },
                        title = {
                            Text(text = stringResource(R.string.ui_notificationPermissionsRationale_dialogTitle))
                        },
                        text = {
                            Text(stringResource(R.string.ui_notificationPermissionsRationale_dialogBody))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRationaleDialog.value = false
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = "package:$packageName".toUri()
                                    startActivity(intent)
                                },
                            ) {
                                Text(stringResource(R.string.open_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showRationaleDialog.value = false
                                }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            // Set up a loop to check the status of the permission
            val loopDuration: Long = 1000 // ms
            val notificationPermissionsGrantedTimerCheck = object : Runnable {
                override fun run() {
                    if (!hasNotificationPermission.value) {
                        if (ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            hasNotificationPermission.value = true
                        } else {
                            // Restart the timer
                            loopHandler.postDelayed(this, loopDuration)
                        }
                    }
                }
            }
            // Start the timer
            loopHandler.postDelayed(notificationPermissionsGrantedTimerCheck, loopDuration)
        } // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        else {
            startChargingSoundService()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    //@Preview
    @Composable
    fun BatteryOptimizationPermissionCard() {
        val hidden = remember { mutableStateOf(false) }
        AnimatedVisibility(
            visible = !hidden.value,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            CustomCardWithTitleAndIconAndContent(
                title = stringResource(R.string.ui_batteryOptimizationsCard_title),
                iconResId = R.drawable.baseline_battery_saver_24,
                //cardContainerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = stringResource(R.string.ui_batteryOptimizationsCard_description),
                    style = cardDefaultBodyTextStyle
                )
                Spacer(modifier = Modifier.size(width = 0.dp, height = 12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    /*
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.errorContainer),
                        contentColor = ButtonDefaults.buttonColors().contentColor,
                        disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                        disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                    ),
                     */
                    onClick = {
                        requestPermissionToIgnoreBatteryOptimization()
                    }
                ) {
                    Text(
                        text = "Disable Battery Optimization",
                        modifier = Modifier
                            .basicMarquee(velocity = 50.dp) // I need the text to scroll, because the message I put in the button won't fit. Plus, it's a kinda fun effect :)
                    )
                }
            }
        }

        // Set up a loop to check the status of the permission
        val loopDuration: Long = 500 // ms
        val batteryOptimizationGrantedTimerCheck = object : Runnable {
            override fun run() {
                if (permissionToIgnoreBatteryOptimizationsIsGranted()) {
                    hidden.value = true
                } else {
                    // Restart the timer
                    loopHandler.postDelayed(this, loopDuration)
                }
            }
        }
        loopHandler.postDelayed(batteryOptimizationGrantedTimerCheck, loopDuration)
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    //@Preview
    @Composable
    fun OverviewChangeSoundSettingsMessageCard() {
        val hidden = remember { mutableStateOf(false) }
        val openAlertDialog = remember { mutableStateOf(false) }

        when {
            openAlertDialog.value -> {
                AlertDialog(
                    onDismissRequest = {
                        openAlertDialog.value = false
                    },
                    title = {
                        Text(text = stringResource(R.string.ui_overview_openAndroidSoundSettings_dialog_title))
                    },
                    text = {
                        Text(text = stringResource(R.string.ui_overview_openAndroidSoundSettings_dialog_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                openAlertDialog.value = false
                                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.ok)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                openAlertDialog.value = false
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.cancel)
                            )
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = !hidden.value,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            CustomCardWithTitleAndIconAndContent(
                title = stringResource(id = R.string.ui_overview_importantInformationCard_title),
                iconResId = R.drawable.baseline_info_24
            ) {
                Text(
                    text = stringResource(id = R.string.ui_overview_importantInformationCard_message),
                    style = cardDefaultBodyTextStyle
                )
                Spacer(modifier = Modifier.size(width = 0.dp, height = 12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        openAlertDialog.value = true
                    }
                ) {
                    Text(
                        text = stringResource(R.string.open_android_sound_settings)
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        appPreferences.hideOverviewInfoMessageCard = true
                        hidden.value = true
                    }
                ) {
                    Text(
                        text = stringResource(R.string.got_it)
                    )
                }
            }
        }
    }

    //@Preview
    @Composable
    fun AppBetaMessageCard() {
        CustomCardWithTitleAndIconAndContent(
            title = stringResource(R.string.ui_alphaBuildNoticeCard_title),
            iconResId = R.drawable.baseline_handyman_24
        ) {
            Text(
                text = stringResource(R.string.ui_alphaBuildNoticeCard_message),
                style = cardDefaultBodyTextStyle
            )
        }
    }

    @Composable
    fun SoundEnabledPreferenceCard() {
        SwitchCard(
            title = stringResource(R.string.ui_soundEnabledPreferenceCard_title),
            description = stringResource(R.string.ui_soundEnabledPreferenceCard_description),
            booleanValue = servicePreferences.soundsEnabled,
            onCheckedChange = { value ->
                servicePreferences.soundsEnabled = value
            }
        )
    }

    @Composable
    fun VibrationEnabledPreferenceCard(vibrationDurationPreferenceCardShouldBeEnabled: MutableState<Boolean>) {
        SwitchCard(
            title = stringResource(R.string.ui_vibrationEnabledPreferenceCard_title),
            description = stringResource(R.string.ui_vibrationEnabledPreferenceCard_description),
            booleanValue = servicePreferences.vibrationEnabled,
            onCheckedChange = { value ->
                servicePreferences.vibrationEnabled = value
                vibrationDurationPreferenceCardShouldBeEnabled.value = value
                if (value) {
                    vibrator.defaultVibrate()
                }
            }
        )
    }

    @Composable
    fun VibrationDurationPreferenceCard(vibrationDurationPreferenceCardShouldBeEnabled: MutableState<Boolean>) {
        when {
            vibrationDurationPreferenceCardShouldBeEnabled.value -> {
                CustomCardWithTitleAndIconAndContent(
                    title = "Vibration length",
                    iconResId = R.drawable.baseline_vibration_24
                ) {
                    val sliderPosition =
                        remember { mutableIntStateOf(servicePreferences.vibrationLengthMs) }

                    Slider(
                        value = sliderPosition.intValue.toFloat(),
                        onValueChange = {
                            sliderPosition.intValue = it.toInt()
                            servicePreferences.vibrationLengthMs = it.roundToInt()
                        },
                        onValueChangeFinished = {
                            vibrator.vibrateMs(servicePreferences.vibrationLengthMs.toLong())
                        },
                        valueRange = 100f..1000f,
                        steps = 17, // Comes out to 10 steps, including beginning and end.
                    )
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        text = "${servicePreferences.vibrationLengthMs}ms"
                    )
                }
            }
        }
    }

    /*
    @Composable
    fun DebounceEnabledPreferenceCard() {
        SwitchCard(
            title = stringResource(R.string.ui_debounceEnabledPreferenceCard_title),
            description = stringResource(R.string.ui_debounceEnabledPreferenceCard_description),
            booleanValue = servicePreferences.debounceEnabled,
            onCheckedChange = { value ->
                servicePreferences.debounceEnabled = value
            }
        )
    }
     */

    /*
    @Composable
    fun RespectDoNotDisturbPreferenceCard() {
        SwitchCard(
            title = stringResource(R.string.ui_respectDnDPreferenceCard_title),
            description = stringResource(R.string.ui_respectDnDPreferenceCard_description),
            booleanValue = servicePreferences.respectDoNotDisturb,
            onCheckedChange = { value ->
                servicePreferences.respectDoNotDisturb = value
            }
        )
    }
     */

    @Composable
    fun ShowDevMessagePreferenceCard() {
        SwitchCard(
            title = "Show alpha build message",
            description = "Enable or disable the appearance of the alpha build message on the home screen",
            booleanValue = !appPreferences.hideOverviewDevMessageCard,
            onCheckedChange = { value ->
                appPreferences.hideOverviewDevMessageCard = !value
            }
        )
    }

    @Composable
    fun SoundVolumePreferenceCard() {
        CustomCardWithTitleAndIconAndContent(
            title = "Sound Volume",
            iconResId = R.drawable.volume
        ) {
            val sliderPosition =
                remember { mutableFloatStateOf(servicePreferences.chargingStartedSoundPlaybackVolume) }
            Slider(
                value = sliderPosition.floatValue,
                onValueChange = {
                    sliderPosition.floatValue = it
                    servicePreferences.chargingStartedSoundPlaybackVolume = it
                },
                onValueChangeFinished = {
                    // Send a broadcast to ChargingSoundService to test the sound
                    //val testSoundBroadcast = Intent(IntentStrings.ChargingSoundServiceTestSoundIntent)
                    //sendBroadcast(testSoundBroadcast)
                    testChargingSound()
                },
                valueRange = 0.1f..1.0f,
                steps = 4,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SoundFilePicker(onFileSelected: (Uri, String) -> Unit) {
        //val context = LocalContext.current
        //val coroutineScope = rememberCoroutineScope()
        val selectedFileUri = remember { mutableStateOf<Uri?>(null) }

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    selectedFileUri.value = uri

                    // Get filename
                    lateinit var fileName: String
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayName =
                                it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                            // Do something with the file name
                            fileName = displayName
                        }
                    }

                    // Return the URI of the selected file to the attached callback (can be found in ChooseSoundPreferenceCard())
                    onFileSelected(uri, fileName)
                }
            }
        )

        // Define supported audio file types for the file selector
        val mimeTypes = arrayOf("audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg", "audio/opus")

        val openFilePermissionsAlertDialog = remember { mutableStateOf(false) }
        when {
            openFilePermissionsAlertDialog.value -> {
                AlertDialog(
                    onDismissRequest = {
                        openFilePermissionsAlertDialog.value = false
                    },
                    title = {
                        Text(text = stringResource(R.string.ui_chooseSoundPreferenceCard_filePermissions_dialog_title))
                    },
                    text = {
                        Text(text = stringResource(R.string.ui_chooseSoundPreferenceCard_filePermissions_dialog_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                openFilePermissionsAlertDialog.value = false
                                requestPermissionToAccessExternalStorage()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.request_permission)
                            )
                        }
                    }
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!hasExternalStoragePermissions()) {
                    openFilePermissionsAlertDialog.value = true
                } else {
                    filePickerLauncher.launch(mimeTypes)
                }
            }
        ) {
            Text("Import sound")
        }
    }

    fun testChargingSound() {
        val result = chargingSoundManager.playSound(
            context = applicationContext,
            servicePreferences = servicePreferences,
            soundToPlay = Sounds.ChargingStartedSound,
            logTag = LoggerTags.MainActivity.DEFAULT,
            logMessagePrefix = "testChargingSound()"
        )

        if (result == SoundPlaybackResult.DoNotDisturbIsEnabled) {
            Toast.makeText(applicationContext, "Do Not Disturb is enabled", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun importSoundFile(uri: Uri, fileName: String, soundToSave: Sounds): SaveSoundResult {
        val TAG = "importSoundFile"

        Log.i(TAG, "Importing sound file for ${soundToSave.name}")

        // Check the sound file to make sure it's valid
        Log.d(TAG, "User selected file: $uri")
        Log.d(TAG, "Verifying sound file")

        // Load audio file
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this.applicationContext, uri)

        // Extract duration
        val soundDuration: String? =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        // Make sure duration isn't null before continuing (source file may be corrupted/invalid in this case, so just dump it; we'll pop up an error message)
        if (soundDuration == null) {
            Log.e(
                TAG,
                "Could not retrieve audio file length. Assuming file is invalid"
            )
            return SaveSoundResult.DurationWasNull
        }

        // Convert string duration to Int so we can compare it
        val soundDurationInMilliseconds: Int? = soundDuration.toIntOrNull()
        if (soundDurationInMilliseconds == null) {
            Log.e(
                TAG,
                "Audio file length retrieved, but failed to convert to Int (original value: $soundDuration). Assuming file is invalid"
            )
            return SaveSoundResult.InvalidOrDamagedFile
        }

        // Check to make sure the chosen sound doesn't exceed the allowed length set by maxSoundDurationInMilliseconds
        if (soundDurationInMilliseconds > maxSoundDurationInMilliseconds) {
            Log.e(
                TAG,
                "Audio file duration is too long! ($soundDurationInMilliseconds > $maxSoundDurationInMilliseconds)"
            )
            return SaveSoundResult.DurationLimitExceeded
        }

        // Because of the stupid Scoped Storage system on newer versions of Android, we'll need to copy the audio file to the app's user data folder so that we can access it later.
        // A bit of a pain, but hey, at least if the source file goes missing, it'll still work!
        Log.d(
            TAG,
            "Copying audio file to internal app data directory"
        )

        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.d(
                TAG,
                "inputStream was null"
            )
            inputStream?.close()
            return SaveSoundResult.InputStreamWasNull
        }

        // Create a new file to write to
        val localSoundFile = File(applicationContext.filesDir, when(soundToSave){
            Sounds.ChargingStartedSound -> "chargingSound"
            Sounds.ChargingStoppedSound -> "chargingStoppedSound"
        })

        // Delete existing file
        if (localSoundFile.exists()) {
            localSoundFile.delete() // Delete existing file
        }
        localSoundFile.createNewFile()

        // Copy the source file to the new file
        inputStream.copyTo(localSoundFile.outputStream())
        inputStream.close()

        Log.d(
            TAG,
            "New URI: ${localSoundFile.absolutePath}"
        )

        when (soundToSave) {
            Sounds.ChargingStartedSound -> {
                servicePreferences.chargingStartedSoundFilePath = localSoundFile.absolutePath
                servicePreferences.chargingStartedSoundFileName = fileName
            }
            Sounds.ChargingStoppedSound -> {
                servicePreferences.chargingStoppedSoundFilePath = localSoundFile.absolutePath
                servicePreferences.chargingStoppedSoundFileName = fileName
            }
        }

        return SaveSoundResult.Success
    }

    @SuppressLint("Range")
    @Composable
    fun ChooseSoundPreferenceCard() {
        CustomCardWithTitleAndIconAndContent(
            title = stringResource(R.string.ui_chooseSoundPreferenceCard_title),
            iconResId = R.drawable.baseline_audiotrack_24
        ) {
            var chargingStartedSoundFileName by remember { mutableStateOf(servicePreferences.chargingStartedSoundFileName) }
            var chargingStoppedSoundFileName by remember { mutableStateOf(servicePreferences.chargingStoppedSoundFileName) }
            var testButtonIsEnabled by remember { mutableStateOf(servicePreferences.chargingStartedSoundFileName.isNotEmpty()) }
            val showFileOpenErrorDialog = remember { mutableStateOf(false) }
            var fileOpenErrorDialogMessageTitle by remember { mutableStateOf(getString(R.string.ui_soundChooser_errorOpeningFile_dialogTitle)) }
            var fileOpenErrorDialogMessageBodyText by remember { mutableStateOf("") }

            when {
                showFileOpenErrorDialog.value -> {
                    AlertDialog(
                        onDismissRequest = {
                            showFileOpenErrorDialog.value = false
                        },
                        title = {
                            Text(text = fileOpenErrorDialogMessageTitle)
                        },
                        text = {
                            Text(text = fileOpenErrorDialogMessageBodyText)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showFileOpenErrorDialog.value = false
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.ok)
                                )
                            }
                        }
                    )
                }
            }

            Text(
                text = stringResource(R.string.ui_chooseSoundPreferenceCard_description),
                style = cardDefaultBodyTextStyle
            )
            Spacer(modifier = Modifier.size(width = 0.dp, height = 8.dp))

            SoundFilePicker { uri, fileName -> //OnFileSelected
                val result = importSoundFile(uri, fileName, Sounds.ChargingStartedSound)
                when (result) {
                    SaveSoundResult.Success -> {
                        chargingStartedSoundFileName = fileName
                        testButtonIsEnabled = true
                    }
                    SaveSoundResult.DurationLimitExceeded -> {
                        // Show error dialog
                        fileOpenErrorDialogMessageBodyText =
                            getString(
                                R.string.dialog_soundChooser_errorOpeningFile_durationTooLongMessage,
                                maxSoundDurationInSeconds
                            )
                        fileOpenErrorDialogMessageTitle = "Sound length too long"
                        showFileOpenErrorDialog.value = true
                    }
                    SaveSoundResult.InputStreamWasNull -> {
                        // Show error dialog
                        fileOpenErrorDialogMessageBodyText =
                            getString(R.string.dialog_soundChooser_errorOpeningFile_fileCorruptedOrUnsupported)
                        showFileOpenErrorDialog.value = true
                        return@SoundFilePicker
                    }
                    SaveSoundResult.InvalidOrDamagedFile -> {
                        // Show error dialog
                        fileOpenErrorDialogMessageBodyText =
                            getString(R.string.dialog_soundChooser_errorOpeningFile_fileCorruptedOrUnsupported)
                        showFileOpenErrorDialog.value = true
                    }
                    SaveSoundResult.DurationWasNull -> {
                        // Show error dialog
                        fileOpenErrorDialogMessageBodyText =
                            getString(R.string.dialog_soundChooser_errorOpeningFile_fileCorruptedOrUnsupported)
                        showFileOpenErrorDialog.value = true
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    testChargingSound()
                },
                enabled = testButtonIsEnabled
            ) {
                Text(
                    text = "Test",
                )
            }
            if (testButtonIsEnabled) {
                Text(
                    text = chargingStartedSoundFileName,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp, 6.dp, 0.dp, 0.dp)
                )
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestPermissionToIgnoreBatteryOptimization() {
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:$packageName".toUri()
            )
        )
    }

    private fun permissionToIgnoreBatteryOptimizationsIsGranted(): Boolean {
        val packageName = applicationContext.packageName
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager

        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    /// TODO: Random thought, but it might be wiser to move the permissions requests to a separate paginated activity, and have that activity run before the main activity when any required permissions aren't yet granted. That way, the service wouldn't be started until every required permission has been granted. Plus, having them be full-screen might emphasize their importance.

    private fun requestPermissionToAccessExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // If running on Android 11 or newer, request MANAGER_EXTERNAL_STORAGE permission
            if (!Environment.isExternalStorageManager()) { // check if we already have permission
                val uri =
                    String.format(Locale.ENGLISH, "package:%s", applicationContext.packageName)
                        .toUri()
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        uri
                    )
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) { // check if we already have permission
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 80
                )
            }
        }
    }

    private fun hasExternalStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }
}