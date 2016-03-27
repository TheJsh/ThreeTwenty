package thejsh.threetwenty;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Image;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Values for the timer
    private long totalTime;
    private Vibrator vibrator; // giggity
    private TextView timeRemainText, timeTypeText, infoText;
    private CustomCountDownTimer timer;
    private ObjectAnimator timerAnimation;
    private TranslateAnimation message1Animation, message2Animation;
    private ValueAnimator timerTextAnimation;
    private ProgressBar progressBar;
    private ImageButton settingsButton;
    private Button centerButton;
    final private int MAX_PROGRESS = 2000;
    private long[] vibrationPattern = {0, 100, 100, 200};
    private long[] vibrationPatternReverse = {0, 200, 100, 100};

    // Interpolators
    private DecelerateInterpolator setupInterpolator = new DecelerateInterpolator(2F);
    private LinearInterpolator linearInterpolator = new LinearInterpolator();

    // Preferences
    private SharedPreferences preferences;

    // Stage of the app
    // 0 - inactive
    // 1 - counting down minutes
    // 2 - awaiting user tap
    // 3 - counting down seconds
    public int stage;
    public boolean timerRunning, inStages, infoHidden, currentlySkipping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean use_amoled = preferences.getBoolean("amoled_key", false);
        setTheme(use_amoled ? R.style.AppThemeAmoled : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Progress bar stuff
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);

        // Center button stuff
        centerButton = (Button) this.findViewById(R.id.centerButton);
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                centerClick();
            }
        });
        centerButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                // Skip current segment
                if (stage == 1) {
                    Log.d("Long clicked", "skipping stage 1");
                    skipStage();
                    return true;
                }
                return false;
            }
        });

        // Settings button stuff
        settingsButton = (ImageButton) this.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsClick();
            }
        });



        totalTime = 5000; // 5 seconds debug

        // Vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Timer and animation
        timeRemainText = (TextView) this.findViewById(R.id.timeRemain);
        timeTypeText = (TextView) this.findViewById(R.id.timeType);
        timer = new CustomCountDownTimer(0, 1000);
        timerAnimation = ObjectAnimator.ofInt(progressBar, "progress", MAX_PROGRESS, 0);
        timerAnimation.setDuration(totalTime);
        timerAnimation.setInterpolator(linearInterpolator);
        timerTextAnimation = ValueAnimator.ofInt(0, 20);
        timerTextAnimation.setDuration(totalTime);

        timerTextAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                timeRemainText.setText(valueAnimator.getAnimatedValue().toString());
            }
        });
        timerTextAnimation.start();

        // Miscellaneous animation setup
        infoText = (TextView) this.findViewById(R.id.infoText);
        TranslateAnimation settingsButtonAnimation = new TranslateAnimation(0, 0, 0, 48);
        settingsButtonAnimation.setDuration(1000);
        settingsButtonAnimation.setInterpolator(setupInterpolator);


        // Setup broadcast stuff, messageReceiver declared further below
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("settings_menu_closed_reset_animations"));

        setupAnimation(0, 0, 1000, true);
        showSettingsButton(true);
        showInfoText(true, 0, 0);
    }

    public void centerClick() {

        // Ignore click if skip is in progress
        if (currentlySkipping) return;

        // Stop all animations first
        timerAnimation.cancel();
        timerTextAnimation.cancel();
        //message1Animation.cancel();
        //message2Animation.cancel();

        if (timerRunning) { // Stop everything
            Log.d("Interrupting timer", "stage reset to 0");
            timerRunning = false;
            inStages = false;
            stage = 0;
            // Show settings and info
            showSettingsButton(true);
            showInfoText(true, 0, 0);
            // Cancel timer and reset animations
            timer.cancel();
            int currentProgress = progressBar.getProgress();
            int currentText = Integer.parseInt((String)timeRemainText.getText());
            setupAnimation(currentProgress, currentText, 0, true);
            return;
        }

        timerAnimation.cancel();
        timerTextAnimation.cancel();
        int startTime;
        switch (stage) {

            case 0: // Inactive - start timer
                Log.d("Stage 0", "Activating timer");
                stage = 1;
                // Start new animations from the start
                startTime = Integer.parseInt(preferences.getString("timer_key", "20"));
                setAnimationProperties(0, 0, startTime, 0, linearInterpolator, false);
                timer = new CustomCountDownTimer(startTime*60000, 30000);
                timerAnimation.start();
                timerTextAnimation.start();
                timer.start();
                if (!inStages) {
                    // Hide settings and info animations
                    showSettingsButton(false);
                    showInfoText(false, 0, 0);
                    showInfoText(true, 1000, 5000);
                    inStages = true;
                }
                break;

            case 1: // Done counting minutes - await user input
                Log.d("Stage 1", "Awaiting tap");
                stage = 2;
                setupAnimation(0, 0, 1000, false);
                showInfoText(true, 1000, 0);
                break;

            case 2: // Done with user input - start last timer
                Log.d("Stage 2", "Starting last timer");
                stage = 3;
                // Start new animations from the start
                showInfoText(false, 0, 0);
                startTime = Integer.parseInt(preferences.getString("cooldown_key", "20"));
                setAnimationProperties(0, 0, startTime, 0, linearInterpolator, false);
                timer = new CustomCountDownTimer(startTime*1000, 100);
                timerAnimation.start();
                timerTextAnimation.start();
                timer.start();
                break;

            case 3: // Done with last timer, start over
                Log.d("Stage 3", "Starting over");
                stage = 0;
                setupAnimation(0, 0, 0, true);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {if (!timerRunning) centerClick();}}, 1010);
                break;

            default:
                Log.e("This shouldn't happen", "you broked it");

        }

    }

    // Skips the first timer stage and directly begins the cooldown
    private void skipStage() {
        currentlySkipping = true;
        stage = 2;
        timer.cancel();
        timerRunning = false;
        timerAnimation.cancel();
        timerTextAnimation.cancel();
        int currentProgress = progressBar.getProgress();
        int currentText = Integer.parseInt((String) timeRemainText.getText());
        setupAnimation(currentProgress, currentText, 0, false);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                currentlySkipping = false;
                centerClick();
            }
        }, 1010);
    }

    // Sets up both the timer animation and progress bar animation
    private void setAnimationProperties(long startProgress, long startTime, long endTime, long delay,
                                       Interpolator interpolator, boolean startup) {
        if (startup) {
            timerAnimation.setIntValues((int) startProgress, MAX_PROGRESS);
            timerTextAnimation.setIntValues((int) startTime, (int) endTime);
        } else {
            timerAnimation.setIntValues(MAX_PROGRESS, 0);
            timerTextAnimation.setIntValues((int) endTime, 0);
        }
        timerAnimation.setInterpolator(interpolator);
        timerTextAnimation.setInterpolator(interpolator);
        timerAnimation.setStartDelay(delay);
        timerTextAnimation.setStartDelay(delay);
        endTime *= (stage == 1 ? 60000 : 1000);
        if (startup) {
            timerAnimation.setDuration(1000);
            timerTextAnimation.setDuration(1000);
        } else {
            timerAnimation.setDuration(endTime);
            timerTextAnimation.setDuration(endTime);
        }
    }

    // Sets up and starts the animations for app startup and preference changes
    private void setupAnimation(int startProgress, int startTime, int delay, boolean first) {
        timeTypeText.setText(first ? "minutes" : "seconds");
        int endTime = Integer.parseInt(preferences.getString(first ? "timer_key" : "cooldown_key", "20"));
        setAnimationProperties(startProgress, startTime, endTime, delay, setupInterpolator, true);
        timerAnimation.start();
        timerTextAnimation.start();
    }

    // Shows or hides the settings button with a nifty animation
    private void showSettingsButton(boolean show) {
        settingsButton.setEnabled(show);
        Animation buttonAnimation = AnimationUtils.loadAnimation(this,
                show ? R.anim.settings_show : R.anim.settings_hide);
        buttonAnimation.setInterpolator(setupInterpolator);
        if (!show) {
            buttonAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    settingsButton.setVisibility(View.INVISIBLE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
        settingsButton.startAnimation(buttonAnimation);
        settingsButton.setVisibility(View.VISIBLE);
    }

    // Shows or hides the info text and changes it based on current stage
    private void showInfoText(final boolean show, long delay, final long timeout) {

        // No-op
        if (!show && infoHidden) return;

        if (delay > 0) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    showInfoText(show, 0, timeout);}}, delay);
            return;
        }

        if (show) { // Update info text when we're showing it
            switch (stage) {
                case 0:
                    infoText.setText(getString(R.string.help_text));
                    break;
                case 1:
                    infoText.setText(getString(R.string.help_text_show_skip));
                    break;
                case 2:
                    infoText.setText(getString(R.string.help_text_awaiting_user));
                    break;
            }
        }
        TranslateAnimation infoAnimation = new TranslateAnimation(
                0, 0, show ? 200 : 0, show ? 0 : 200);
        infoAnimation.setInterpolator(setupInterpolator);
        infoAnimation.setFillAfter(true);
        infoAnimation.setDuration(1000);
        infoAnimation.setStartTime(delay);
        infoText.startAnimation(infoAnimation);
        infoHidden = !show;

        if (timeout > 0) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    if (inStages) showInfoText(false, 0, 0);
                }
            }, timeout);
        }
    }

    // This doesn't really have to be a whole method but oh well
    private void settingsClick() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // Changes the height of the center button so that it resembles the circle a little better
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        ViewGroup.LayoutParams centerButtonParams = centerButton.getLayoutParams();
        ViewGroup.LayoutParams progressBarParams = progressBar.getLayoutParams();
        progressBarParams.height = progressBar.getWidth();
        centerButtonParams.height = progressBar.getWidth();
        progressBar.setLayoutParams(progressBarParams);
        centerButton.setLayoutParams(progressBarParams);
    }

    // TODO: Stop animations and resume them when necessary to conserve battery life
    @Override
    protected void onStop() {
        Log.d("onStop() fired", "stopping animations");
        super.onStop();
    }
    @Override
    protected void onResume() {
        Log.d("onResume() fired", "");
        if (timerRunning && (!timerAnimation.isRunning() || !timerTextAnimation.isRunning())) {
            Log.d("Animations not running", "resuming from position");
        }
        super.onResume();
    }

    // Receives an intent message from the settings activity to refresh the progress bar
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Reset animations, start from setup animations
            progressBar.setProgress(0);
            timeRemainText.setText("0");
            setupAnimation(0, 0, 500, true);
        }
    };

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    // Custom CountDownTimer class
    public class CustomCountDownTimer extends CountDownTimer
    {

        public CustomCountDownTimer(long startTime, long interval)
        {
            super(startTime, interval);
        }

        @Override
        public void onFinish()
        {
            timerRunning = false;

            timeRemainText.setText("0");
            timeTypeText.setText("time's up!");

            // Vibrate when done
            if (preferences.getBoolean("vibrations_key", true)) {
                vibrator.vibrate(stage == 1 ? vibrationPattern : vibrationPatternReverse, -1);
            }

            // TODO: Actually rewrite this whole mess (copied from old project)
            // Notification sound when done if configured
            if (stage == 1 && preferences.getBoolean("notifications_key", true)) {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();

                // Notify user via the notification panel
                Intent intent = new Intent();
                PendingIntent pIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
                // Build notification
                String timerTime = preferences.getString("timer_key", "20");
                String cooldownTime = preferences.getString("cooldown_key", "20");
                Notification noti = new Notification.Builder(MainActivity.this)
                        .setTicker(timerTime + " minutes are up!")
                        .setContentTitle(timerTime + " minutes are up!")
                        .setContentText("Tap here to rest your eyes for " + cooldownTime + " seconds")
                        .setSmallIcon(R.drawable.ic_settings_white_24dp)
                        .setContentIntent(pIntent).getNotification();
                noti.flags = Notification.FLAG_AUTO_CANCEL;

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0, noti);
            }

            centerClick();

        }

        @Override
        public void onTick(long millisUntilFinished) {
            timerRunning = true;
            int remainingTimeText = Integer.parseInt((String) timeRemainText.getText());
            if (stage == 1) { // Grammar is wonderful
                timeTypeText.setText(remainingTimeText == 1 ? "minute" : "minutes");
            } else {
                timeTypeText.setText(remainingTimeText == 1 ? "second" : "seconds");
            }
        }
    }



}
