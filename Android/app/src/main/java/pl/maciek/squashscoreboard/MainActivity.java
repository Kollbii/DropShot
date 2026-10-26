package pl.maciek.squashscoreboard;

import android.app.*;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.media.session.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private int background,surface,foreground,muted,playerOneColor,playerTwoColor,remoteOne,remoteTwo,remoteUndo,remoteReset;
    private boolean darkTheme, paused;
    private final MatchState match=new MatchState();
    private String playerOneName="GRACZ 1",playerTwoName="GRACZ 2",playerOneProfileId,playerTwoProfileId;
    private long currentMatchId,matchElapsedMs,currentSetElapsedMs,lastClockMs;
    private final ArrayList<Long> setDurationsMs=new ArrayList<>();
    private final ArrayList<Long> pointTimesMs=new ArrayList<>();
    private TextView playerOnePoints,playerTwoPoints,playerOneSets,playerTwoSets,status,playerOneTitle,playerTwoTitle;
    private Button pauseButton;
    private TextView pauseOverlay;
    private MediaSession mediaSession;
    private WatchCommandServer watchCommandServer;
    private GradientDrawable playerOneBackground,playerTwoBackground;
    private MatchFinishedView finishedOverlay;
    private FireworksView fireworksView;
    private ValueAnimator playerOnePulse,playerTwoPulse;
    private Button courtButton;
    private GradientDrawable courtBackground;
    private CourtAlertView courtAlertView;
    private long courtStartMs;
    private final Handler clockHandler=new Handler(Looper.getMainLooper());
    private final Runnable clockTicker=new Runnable(){public void run(){if(!paused&&!match.isMatchFinished())tickClock();refreshScreen();clockHandler.postDelayed(this,1000);}};

    @Override public void onCreate(Bundle state){super.onCreate(state);loadStyle();courtStartMs=AppPreferences.courtStart(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);setContentView(createContent());currentMatchId=System.currentTimeMillis();startClock();restoreMatchFromIntent();createMediaSession();watchCommandServer=new WatchCommandServer(new Handler(Looper.getMainLooper()),this::handleWatchCommand);refreshScreen();}
    @Override protected void onResume(){super.onResume();if(mediaSession!=null)mediaSession.setActive(true);if(watchCommandServer!=null)watchCommandServer.start();clockHandler.removeCallbacks(clockTicker);clockHandler.post(clockTicker);refreshScreen();}
    @Override protected void onPause(){if(watchCommandServer!=null)watchCommandServer.stop();pauseMatch();hideFireworks();stopBallPulse(1);stopBallPulse(2);if(mediaSession!=null)mediaSession.setActive(false);super.onPause();}
    @Override protected void onStop(){archiveCurrentMatch();super.onStop();}
    @Override protected void onDestroy(){clockHandler.removeCallbacks(clockTicker);if(watchCommandServer!=null)watchCommandServer.stop();if(mediaSession!=null)mediaSession.release();super.onDestroy();}

    private View createContent(){LinearLayout root=box(background);root.setPadding(dp(16),dp(10),dp(16),dp(12));LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.squash_logo);logo.setPadding(0,dp(5),dp(8),dp(5));status=text("",12,muted);status.setGravity(Gravity.CENTER);courtButton=button("KORT · START",surface);courtButton.setTextSize(14);courtButton.setAllCaps(false);courtButton.setMinWidth(0);courtButton.setMinimumWidth(0);courtButton.setPadding(dp(10),0,dp(10),0);courtButton.setContentDescription("Ustaw początek wynajmu kortu na ostatnią pełną godzinę");courtBackground=new GradientDrawable();courtBackground.setColor(surface);courtBackground.setCornerRadius(dp(10));courtBackground.setStroke(dp(2),muted);courtButton.setBackground(courtBackground);courtButton.setOnClickListener(v->onCourtTimerClick());Button pause=iconButton("Ⅱ","Pauza lub wznowienie",surface);pause.setOnClickListener(v->togglePause());pauseButton=pause;Button undo=iconButton("↶","Cofnij ostatni punkt",surface);undo.setOnClickListener(v->{if(!paused){int before=match.getPlayerOneSets()+match.getPlayerTwoSets();match.undo();restoreSetAfterUndo(before);refreshScreen();}});Button reset=iconButton("↺","Nowy mecz",Color.rgb(44,111,88));reset.setOnClickListener(v->confirmReset());Button history=iconButton("≡","Historia meczow",surface);history.setOnClickListener(v->startActivity(new Intent(this,HistoryActivity.class)));Button menu=iconButton("⚙","Ustawienia meczu",surface);menu.setOnClickListener(v->showSettings());header.addView(logo,new LinearLayout.LayoutParams(dp(44),-1));header.addView(courtButton,new LinearLayout.LayoutParams(-2,dp(42)));header.addView(status,new LinearLayout.LayoutParams(0,-1,1));header.addView(pause,new LinearLayout.LayoutParams(dp(45),-1));header.addView(undo,new LinearLayout.LayoutParams(dp(45),-1));header.addView(reset,new LinearLayout.LayoutParams(dp(45),-1));header.addView(history,new LinearLayout.LayoutParams(dp(45),-1));header.addView(menu,new LinearLayout.LayoutParams(dp(50),-1));root.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));LinearLayout scores=new LinearLayout(this);scores.setGravity(Gravity.CENTER);LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,-1,1);first.setMargins(0,dp(8),dp(5),dp(8));LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,-1,1);second.setMargins(dp(5),dp(8),0,dp(8));scores.addView(playerPanel(1,playerOneColor),first);scores.addView(playerPanel(2,playerTwoColor),second);root.addView(scores,new LinearLayout.LayoutParams(-1,0,1));FrameLayout screen=new FrameLayout(this);screen.setBackgroundColor(background);screen.addView(root,new FrameLayout.LayoutParams(-1,-1));courtAlertView=new CourtAlertView(this);screen.addView(courtAlertView,new FrameLayout.LayoutParams(-1,-1));return screen;}
    private View playerPanel(int player,int accent){LinearLayout panel=box(surface);panel.setPadding(dp(12),dp(8),dp(12),dp(8));panel.setGravity(Gravity.CENTER);GradientDrawable bg=new GradientDrawable();bg.setColor(surface);bg.setCornerRadius(dp(18));bg.setStroke(dp(2),accent);panel.setBackground(bg);panel.setOnClickListener(v->addPoint(player));TextView title=text(player==1?playerOneName:playerTwoName,20,foreground);title.setGravity(Gravity.CENTER);title.setSingleLine(true);TextView sets=text("",29,muted);sets.setGravity(Gravity.CENTER);TextView points=text("0",112,foreground);points.setGravity(Gravity.CENTER);TextView hint=text("DOTKNIJ, ABY DODAC PUNKT",11,accent);hint.setGravity(Gravity.CENTER);panel.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));panel.addView(sets,new LinearLayout.LayoutParams(-1,dp(40)));panel.addView(points,new LinearLayout.LayoutParams(-1,0,1));panel.addView(hint,new LinearLayout.LayoutParams(-1,dp(24)));if(player==1){playerOnePoints=points;playerOneSets=sets;playerOneTitle=title;playerOneBackground=bg;}else{playerTwoPoints=points;playerTwoSets=sets;playerTwoTitle=title;playerTwoBackground=bg;}return panel;}

    private void showSettings(){LinearLayout form=box(surface);form.setPadding(dp(24),dp(10),dp(24),dp(12));List<PlayerProfile> profiles=ProfileStore.load(this);String[] choices=new String[profiles.size()+1];choices[0]="Gość";for(int i=0;i<profiles.size();i++)choices[i+1]=profiles.get(i).name;Spinner profileOne=spinner(choices,profileIndex(profiles,playerOneProfileId));Spinner profileTwo=spinner(choices,profileIndex(profiles,playerTwoProfileId));EditText nameOne=field(playerOneName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES),nameTwo=field(playerTwoName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES),points=field(String.valueOf(match.getPointsToWinSet()),InputType.TYPE_CLASS_NUMBER),sets=field(String.valueOf(match.getSetsToWinMatch()),InputType.TYPE_CLASS_NUMBER);Spinner colorOne=spinner(colorNames(),colorIndex(playerOneColor)),colorTwo=spinner(colorNames(),colorIndex(playerTwoColor));LinearLayout nameOneRow=settingRow("Nazwa gracza 1",nameOne),colorOneRow=settingRow("Kolor gracza 1",colorOne),nameTwoRow=settingRow("Nazwa gracza 2",nameTwo),colorTwoRow=settingRow("Kolor gracza 2",colorTwo);addSetting(form,"Profil gracza 1",profileOne);form.addView(nameOneRow);form.addView(colorOneRow);addSetting(form,"Profil gracza 2",profileTwo);form.addView(nameTwoRow);form.addView(colorTwoRow);profileOne.setOnItemSelectedListener(profileVisibility(profileOne,nameOneRow,colorOneRow));profileTwo.setOnItemSelectedListener(profileVisibility(profileTwo,nameTwoRow,colorTwoRow));addSetting(form,"Punkty do wygrania seta",points);addSetting(form,"Do ilu wygranych setow gramy",sets);CheckBox theme=new CheckBox(this);theme.setText("Tryb ciemny");theme.setTextColor(foreground);theme.setChecked(darkTheme);form.addView(theme);Spinner pilotOne=spinner(remoteNames(),remoteIndex(remoteOne)),pilotTwo=spinner(remoteNames(),remoteIndex(remoteTwo)),pilotUndo=spinner(remoteNames(),remoteIndex(remoteUndo)),pilotReset=spinner(remoteNames(),remoteIndex(remoteReset));addSetting(form,"Pilot: punkt gracza 1",pilotOne);addSetting(form,"Pilot: punkt gracza 2",pilotTwo);addSetting(form,"Pilot: cofnij",pilotUndo);addSetting(form,"Pilot: reset",pilotReset);Button courtSettings=button("USTAWIENIA WYNAJMU KORTU",surface);form.addView(courtSettings,new LinearLayout.LayoutParams(-1,dp(48)));TextView note=text("Zmiana zasad lub zawodnikow rozpoczyna nowy mecz. Ustawienia pilota dzialaja od razu.",12,muted);note.setPadding(0,dp(10),0,0);form.addView(note);ScrollView scroll=new ScrollView(this);scroll.addView(form);View[] settingsInputs={profileOne,profileTwo,nameOne,nameTwo,points,sets,theme,colorOne,colorTwo,pilotOne,pilotTwo,pilotUndo,pilotReset};
String baseline=settingsSignature(settingsInputs);
AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Menu meczu").setView(scroll)
    .setNegativeButton("ANULUJ",null).setPositiveButton("ZAPISZ",null).create();
courtSettings.setOnClickListener(v->leaveSettings(dialog,baseline,()->showCourtSettings(),settingsInputs));
dialog.setOnShowListener(x->{
    if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(playerOneColor);
    Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    save.setTextColor(playerOneColor);
    save.setOnClickListener(v->{if(saveSettings(profiles,profileOne,profileTwo,nameOne,nameTwo,points,sets,theme,colorOne,colorTwo,pilotOne,pilotTwo,pilotUndo,pilotReset))dialog.dismiss();});
    protectSettingsDialog(dialog,baseline,settingsInputs);
});
dialog.show();}
    private AdapterView.OnItemSelectedListener profileVisibility(final Spinner spinner,final View... rows){return new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?>p,View v,int pos,long id){for(View row:rows)row.setVisibility(pos==0?View.VISIBLE:View.GONE);}};}
    private int profileIndex(List<PlayerProfile> p,String id){if(id!=null)for(int i=0;i<p.size();i++)if(id.equals(p.get(i).id))return i+1;return 0;}
    private LinearLayout settingRow(String label,View input){LinearLayout row=box(surface);TextView c=text(label,13,muted);c.setPadding(0,dp(10),0,0);row.addView(c);row.addView(input,new LinearLayout.LayoutParams(-1,dp(48)));return row;}
    private void addSetting(LinearLayout f,String label,EditText input){TextView c=text(label,13,muted);c.setPadding(0,dp(10),0,0);f.addView(c);f.addView(input,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void addSetting(LinearLayout f,String label,Spinner input){TextView c=text(label,13,muted);c.setPadding(0,dp(10),0,0);f.addView(c);f.addView(input,new LinearLayout.LayoutParams(-1,dp(48)));}
    private Spinner spinner(String[] values,int selected){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){@Override public View getView(int p,View v,ViewGroup parent){TextView t=(TextView)super.getView(p,v,parent);t.setTextColor(foreground);t.setTextSize(18);t.setPadding(dp(12),0,dp(12),0);return t;}@Override public View getDropDownView(int p,View v,ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,v,parent);t.setTextColor(foreground);t.setBackgroundColor(surface);t.setPadding(dp(12),dp(10),dp(12),dp(10));return t;}};s.setAdapter(a);GradientDrawable b=new GradientDrawable();b.setColor(background);b.setCornerRadius(dp(10));s.setBackground(b);s.setPopupBackgroundDrawable(new ColorDrawable(surface));s.setSelection(selected);return s;}
    private EditText field(String value,int type){EditText e=new EditText(this);e.setText(value);e.setTextColor(foreground);e.setTextSize(18);e.setSingleLine(true);e.setInputType(type);e.setImeOptions(EditorInfo.IME_ACTION_DONE);e.setImeActionLabel("OK",EditorInfo.IME_ACTION_DONE);e.setOnEditorActionListener((view,action,event)->{if(action!=EditorInfo.IME_ACTION_DONE&&(event==null||event.getKeyCode()!=KeyEvent.KEYCODE_ENTER))return false;InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(view.getWindowToken(),0);return true;});GradientDrawable b=new GradientDrawable();b.setColor(background);b.setCornerRadius(dp(10));b.setStroke(dp(1),muted);e.setBackground(b);e.setPadding(dp(12),0,dp(12),0);return e;}
    private String settingsSignature(View... inputs){
        StringBuilder signature=new StringBuilder();
        for(View input:inputs){
            if(input instanceof EditText){String value=((EditText)input).getText().toString();signature.append('E').append(value.length()).append(':').append(value);}
            else if(input instanceof Spinner)signature.append('S').append(((Spinner)input).getSelectedItemPosition());
            else if(input instanceof CompoundButton)signature.append('B').append(((CompoundButton)input).isChecked());
            signature.append(';');
        }
        return signature.toString();
    }
    private void leaveSettings(AlertDialog dialog,String initial,Runnable after,View... inputs){
        if(initial.equals(settingsSignature(inputs))){dialog.dismiss();if(after!=null)after.run();return;}
        new AlertDialog.Builder(this).setTitle("Wyjść bez zapisywania zmian?")
            .setMessage("Wprowadzone zmiany zostaną utracone.")
            .setNegativeButton("WRÓĆ DO USTAWIEŃ",null)
            .setPositiveButton("ODRZUĆ ZMIANY",(ignored,which)->{dialog.dismiss();if(after!=null)after.run();})
            .show();
    }
    private void protectSettingsDialog(AlertDialog dialog,String initial,View... inputs){
        dialog.setCanceledOnTouchOutside(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->leaveSettings(dialog,initial,null,inputs));
        dialog.setOnKeyListener((ignored,key,event)->{
            if(key!=KeyEvent.KEYCODE_BACK)return false;
            if(event.getAction()==KeyEvent.ACTION_UP)leaveSettings(dialog,initial,null,inputs);
            return true;
        });
    }
    private boolean saveSettings(List<PlayerProfile> profiles,Spinner profileOne,Spinner profileTwo,EditText nameOne,EditText nameTwo,EditText points,EditText sets,CheckBox theme,Spinner colorOne,Spinner colorTwo,Spinner pilotOne,Spinner pilotTwo,Spinner pilotUndo,Spinner pilotReset){try{int pts=Integer.parseInt(points.getText().toString()),sts=Integer.parseInt(sets.getText().toString());if(pts<1||sts<1)throw new NumberFormatException();archiveCurrentMatch();playerOneProfileId=selectedProfileId(profiles,profileOne);playerTwoProfileId=selectedProfileId(profiles,profileTwo);PlayerProfile one=ProfileStore.find(this,playerOneProfileId),two=ProfileStore.find(this,playerTwoProfileId);playerOneName=one==null?cleanName(nameOne.getText().toString(),"GRACZ 1"):one.name;playerTwoName=two==null?cleanName(nameTwo.getText().toString(),"GRACZ 2"):two.name;int c1=one==null?colorAt(colorOne.getSelectedItemPosition()):one.color,c2=two==null?colorAt(colorTwo.getSelectedItemPosition()):two.color;match.configure(pts,sts);resetClock();currentMatchId=System.currentTimeMillis();AppPreferences.save(this,theme.isChecked(),c1,c2,remoteAt(pilotOne.getSelectedItemPosition()),remoteAt(pilotTwo.getSelectedItemPosition()));AppPreferences.saveExtra(this,remoteAt(pilotUndo.getSelectedItemPosition()),remoteAt(pilotReset.getSelectedItemPosition()));loadStyle();setContentView(createContent());refreshScreen();return true;}catch(NumberFormatException e){new AlertDialog.Builder(this).setMessage("Wpisz dodatnie liczby.").setPositiveButton("OK",null).show();return false;}}
    private String selectedProfileId(List<PlayerProfile> p,Spinner s){int i=s.getSelectedItemPosition();return i==0?null:p.get(i-1).id;}
    private String cleanName(String s,String fallback){s=s.trim();return s.isEmpty()?fallback:s.toUpperCase();}

    private void createMediaSession(){mediaSession=new MediaSession(this,"SquashScoreboard");mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS).setState(PlaybackState.STATE_PLAYING,0,1f).build());mediaSession.setCallback(new MediaSession.Callback(){@Override public void onSkipToNext(){addPoint(1);}@Override public void onSkipToPrevious(){addPoint(2);}@Override public void onPlay(){togglePause();}@Override public void onPause(){togglePause();}@Override public boolean onMediaButtonEvent(Intent i){KeyEvent e=i.getParcelableExtra(Intent.EXTRA_KEY_EVENT);return e!=null&&e.getAction()==KeyEvent.ACTION_DOWN?handleRemoteKey(e.getKeyCode()):super.onMediaButtonEvent(i);}});}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return e.getAction()==KeyEvent.ACTION_DOWN&&handleRemoteKey(e.getKeyCode())||super.dispatchKeyEvent(e);}
    private boolean handleRemoteKey(int key){if(finishedOverlay!=null){if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS){finishedOverlay.triggerBack();return true;}if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||key==KeyEvent.KEYCODE_MEDIA_PLAY){finishedOverlay.triggerReset();return true;}if(key==KeyEvent.KEYCODE_MEDIA_NEXT){finishedOverlay.triggerContinue();return true;}}switch(key){case KeyEvent.KEYCODE_VOLUME_UP:case KeyEvent.KEYCODE_VOLUME_DOWN:case KeyEvent.KEYCODE_MEDIA_NEXT:case KeyEvent.KEYCODE_MEDIA_PREVIOUS:if(key==remoteOne){addPoint(1);return true;}if(key==remoteTwo){addPoint(2);return true;}if(key==remoteUndo&&!paused){int before=match.getPlayerOneSets()+match.getPlayerTwoSets();match.undo();restoreSetAfterUndo(before);refreshScreen();return true;}if(key==remoteReset){confirmReset();return true;}return true;case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:case KeyEvent.KEYCODE_MEDIA_PLAY:togglePause();return true;case KeyEvent.KEYCODE_MEDIA_PAUSE:pauseMatch();return true;default:return false;}}
    private boolean handleWatchCommand(String command){if("POINT_1".equals(command)){if(paused||match.isMatchFinished())return false;addPoint(1);return true;}if("POINT_2".equals(command)){if(paused||match.isMatchFinished())return false;addPoint(2);return true;}if("UNDO".equals(command)){if(paused)return false;int before=match.getPlayerOneSets()+match.getPlayerTwoSets();match.undo();restoreSetAfterUndo(before);refreshScreen();return true;}if("RESET".equals(command)){resetMatchSafely();return true;}return false;}
    private void addPoint(int player){if(paused||match.isMatchFinished())return;tickClock();int oldSets=match.getPlayerOneSets()+match.getPlayerTwoSets();match.addPoint(player);pointTimesMs.add(matchElapsedMs);if(match.getPlayerOneSets()+match.getPlayerTwoSets()>oldSets){setDurationsMs.add(currentSetElapsedMs);currentSetElapsedMs=0;}if(match.isMatchFinished()){pauseMatch();archiveCurrentMatch();showMatchFinished();}refreshScreen();}
    private void restoreSetAfterUndo(int before){if(match.getPlayerOneSets()+match.getPlayerTwoSets()<before&&!setDurationsMs.isEmpty())currentSetElapsedMs=setDurationsMs.remove(setDurationsMs.size()-1);while(pointTimesMs.size()>match.getPointWinners().size())pointTimesMs.remove(pointTimesMs.size()-1);}
    private void togglePause(){if(match.isMatchFinished())return;if(paused)resumeMatch();else pauseMatch();refreshScreen();}
    private void startClock(){paused=false;lastClockMs=SystemClock.elapsedRealtime();}
    private void resetClock(){matchElapsedMs=0;currentSetElapsedMs=0;setDurationsMs.clear();pointTimesMs.clear();startClock();hidePauseOverlay();}
    private void tickClock(){if(!paused){long now=SystemClock.elapsedRealtime();long delta=Math.max(0,now-lastClockMs);matchElapsedMs+=delta;currentSetElapsedMs+=delta;lastClockMs=now;}}
    private void pauseMatch(){if(!paused){tickClock();paused=true;}showPauseOverlay();}
    private void resumeMatch(){paused=false;lastClockMs=SystemClock.elapsedRealtime();hidePauseOverlay();}
    private void showPauseOverlay(){if(match.isMatchFinished()||pauseOverlay!=null)return;pauseOverlay=text("Ⅱ\nPAUZA\n\nDOTKNIJ, ABY WZNOWIC",48,AppPreferences.GOLD);pauseOverlay.setGravity(Gravity.CENTER);pauseOverlay.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);pauseOverlay.setLineSpacing(dp(5),1f);pauseOverlay.setBackgroundColor(Color.argb(235,13,16,22));pauseOverlay.setOnClickListener(v->togglePause());((FrameLayout)courtAlertView.getParent()).addView(pauseOverlay,new FrameLayout.LayoutParams(-1,-1));courtAlertView.bringToFront();}
    private void hidePauseOverlay(){if(pauseOverlay!=null){((ViewGroup)pauseOverlay.getParent()).removeView(pauseOverlay);pauseOverlay=null;}}
    private void refreshScreen(){if(playerOnePoints==null)return;playerOnePoints.setText(String.valueOf(match.getPlayerOnePoints()));playerTwoPoints.setText(String.valueOf(match.getPlayerTwoPoints()));playerOneSets.setText("SETY  "+match.getPlayerOneSets());playerTwoSets.setText("SETY  "+match.getPlayerTwoSets());playerOneTitle.setText(playerOneName);playerTwoTitle.setText(playerTwoName);updateBallBorder(1);updateBallBorder(2);pauseButton.setText(paused?"▶":"Ⅱ");String clock=formatDuration(matchElapsedMs);status.setText(match.isMatchFinished()?"MECZ WYGRYWA "+(match.getWinner()==1?playerOneName:playerTwoName)+"  "+clock:(paused?"PAUZA  ":"")+clock+"  |  "+ballStatus());syncFireworks();if(paused&&!match.isMatchFinished()){showPauseOverlay();if(pauseOverlay!=null)pauseOverlay.bringToFront();}else hidePauseOverlay();updateCourtTimer();courtAlertView.bringToFront();}
    private String formatDuration(long ms){long s=ms/1000,h=s/3600;s%=3600;return String.format(Locale.US,"%02d:%02d:%02d",h,s/60,s%60);}
    private void archiveCurrentMatch(){if(!match.hasPoints())return;MatchHistoryStore.add(this,new MatchRecord(currentMatchId,playerOneName,playerTwoName,playerOneProfileId,playerTwoProfileId,match.getPlayerOneSets(),match.getPlayerTwoSets(),match.getPlayerOnePoints(),match.getPlayerTwoPoints(),match.getPointsToWinSet(),match.getSetsToWinMatch(),match.isMatchFinished(),System.currentTimeMillis(),match.getPointWinners(),matchElapsedMs,setDurationsMs,pointTimesMs,paused,playerOneColor,playerTwoColor));}
    private void restoreMatchFromIntent(){String source=getIntent().getStringExtra(MatchDetailActivity.EXTRA_RECORD);if(source==null)return;try{MatchRecord r=MatchRecord.fromJson(new org.json.JSONObject(source));playerOneName=r.playerOneName;playerTwoName=r.playerTwoName;playerOneProfileId=r.playerOneProfileId;playerTwoProfileId=r.playerTwoProfileId;playerOneColor=r.playerOneColor;playerTwoColor=r.playerTwoColor;PlayerProfile one=ProfileStore.find(this,playerOneProfileId),two=ProfileStore.find(this,playerTwoProfileId);if(one==null&&playerOneProfileId==null)one=ProfileStore.findByName(this,r.playerOneName);if(two==null&&playerTwoProfileId==null)two=ProfileStore.findByName(this,r.playerTwoName);if(one!=null){playerOneProfileId=one.id;playerOneName=one.name;}if(two!=null){playerTwoProfileId=two.id;playerTwoName=two.name;}currentMatchId=r.id;match.resume(r.pointsToWinSet,r.setsToWinMatch,r.playerOnePoints,r.playerTwoPoints,r.playerOneSets,r.playerTwoSets,r.finished,r.pointWinners);matchElapsedMs=r.durationMs;setDurationsMs.clear();setDurationsMs.addAll(r.setDurationsMs);pointTimesMs.clear();pointTimesMs.addAll(r.pointTimesMs);long completed=0;for(long d:r.setDurationsMs)completed+=d;currentSetElapsedMs=Math.max(0,r.durationMs-completed);paused=r.paused||r.finished;if(getIntent().getBooleanExtra("continue",false)&&r.finished){match.continueWithThreeSets();currentSetElapsedMs=0;paused=false;lastClockMs=SystemClock.elapsedRealtime();}}catch(Exception ignored){}}
    private boolean isFireworksScore(){int a=match.getPlayerOnePoints(),b=match.getPlayerTwoPoints();return(a==6&&b==7)||(a==7&&b==6)||(a==6&&b==9)||(a==9&&b==6);}
    private String ballStatus(){int a=match.getPlayerOnePoints(),b=match.getPlayerTwoPoints(),target=match.getPointsToWinSet();int p=(a>=target-1&&a>b)?1:(b>=target-1&&b>a)?2:0;if(p==0)return "SET DO "+target+"  |  MECZ DO "+match.getSetsToWinMatch();boolean mb=(p==1?match.getPlayerOneSets():match.getPlayerTwoSets())==match.getSetsToWinMatch()-1;return(mb?"PILKA MECZOWA: ":"PILKA SETOWA: ")+(p==1?playerOneName:playerTwoName);}
    private void loadStyle(){darkTheme=AppPreferences.dark(this);background=darkTheme?Color.rgb(13,16,22):Color.rgb(245,247,250);surface=darkTheme?Color.rgb(28,34,44):Color.WHITE;foreground=darkTheme?Color.WHITE:Color.rgb(24,30,40);muted=darkTheme?Color.rgb(174,185,201):Color.rgb(80,92,108);playerOneColor=AppPreferences.color(this,"color1",AppPreferences.BLUE);playerTwoColor=AppPreferences.color(this,"color2",AppPreferences.RED);remoteOne=AppPreferences.remote(this,"remote1",KeyEvent.KEYCODE_VOLUME_UP);remoteTwo=AppPreferences.remote(this,"remote2",KeyEvent.KEYCODE_VOLUME_DOWN);remoteUndo=AppPreferences.remote(this,"undo",KeyEvent.KEYCODE_MEDIA_PAUSE);remoteReset=AppPreferences.remote(this,"reset",KeyEvent.KEYCODE_MEDIA_PLAY);}
    private int[] colors(){return new int[]{AppPreferences.BLUE,AppPreferences.RED,AppPreferences.GREEN,AppPreferences.GOLD};}private String[] colorNames(){return new String[]{"Niebieski","Czerwony","Zielony","Zloty"};}private int colorAt(int i){return colors()[i];}private int colorIndex(int c){int[]x=colors();for(int i=0;i<x.length;i++)if(x[i]==c)return i;return 0;}private String[] remoteNames(){return new String[]{"Volume +","Volume -","Nastepny utwor","Poprzedni utwor"};}private int[] remotes(){return new int[]{KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT,KeyEvent.KEYCODE_MEDIA_PREVIOUS};}private int remoteAt(int i){return remotes()[i];}private int remoteIndex(int k){int[]r=remotes();for(int i=0;i<r.length;i++)if(r[i]==k)return i;return 0;}
    private void syncFireworks(){if(isFireworksScore()&&!match.isMatchFinished()){if(fireworksView==null){fireworksView=new FireworksView(this);((FrameLayout)courtAlertView.getParent()).addView(fireworksView,new FrameLayout.LayoutParams(-1,-1));}}else hideFireworks();}
    private void hideFireworks(){if(fireworksView!=null){if(fireworksView.getParent() instanceof ViewGroup)((ViewGroup)fireworksView.getParent()).removeView(fireworksView);fireworksView=null;}}private boolean isBallFor(int p){return p==1?match.getPlayerOnePoints()>=match.getPointsToWinSet()-1&&match.getPlayerOnePoints()>match.getPlayerTwoPoints():match.getPlayerTwoPoints()>=match.getPointsToWinSet()-1&&match.getPlayerTwoPoints()>match.getPlayerOnePoints();}
    private void onCourtTimerClick(){
        if(courtStartMs==0){startCourtRental();return;}
        new AlertDialog.Builder(this).setTitle("Licznik wynajmu kortu")
            .setMessage("Ustawić początek ponownie na ostatnią pełną godzinę?")
            .setNegativeButton("ANULUJ",null)
            .setNeutralButton("WYŁĄCZ",(dialog,which)->{courtStartMs=0;AppPreferences.saveCourtStart(this,0);updateCourtTimer();})
            .setPositiveButton("USTAW OD NOWA",(dialog,which)->startCourtRental()).show();
    }
    private void startCourtRental(){
        Calendar lower=Calendar.getInstance();
        lower.set(Calendar.MINUTE,0);lower.set(Calendar.SECOND,0);lower.set(Calendar.MILLISECOND,0);
        courtStartMs=lower.getTimeInMillis();
        AppPreferences.saveCourtStart(this,courtStartMs);
        updateCourtTimer();
    }
    private void updateCourtTimer(){
        if(courtButton==null)return;
        if(courtStartMs==0){courtButton.setText("KORT WYŁ. · START");courtButton.setTextColor(foreground);courtBackground.setStroke(dp(2),muted);courtAlertView.setAlertColor(0);return;}
        long now=System.currentTimeMillis();
        long duration=AppPreferences.courtDuration(this)*60000L;
        long remaining=courtStartMs+duration-now;
        String start=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(courtStartMs));
        if(now<courtStartMs){
            long minutes=(courtStartMs-now+59999)/60000;
            courtButton.setText(String.format(Locale.getDefault(),"KORT WŁ. · START %s ZA %d:%02d",start,minutes/60,minutes%60));
            courtButton.setTextColor(foreground);
            courtBackground.setStroke(dp(2),muted);
            courtAlertView.setAlertColor(0);
            return;
        }
        if(remaining<=0){courtButton.setText("KORT WŁ. · "+start+" · KONIEC");courtButton.setTextColor(AppPreferences.RED);courtBackground.setStroke(dp(6),AppPreferences.RED);courtAlertView.setAlertColor(AppPreferences.RED);return;}
        long minutes=(remaining+59999)/60000;
        courtButton.setText(String.format(Locale.getDefault(),"KORT WŁ. · %s · %d:%02d",start,minutes/60,minutes%60));
        int color=muted,width=2,level=0;
        if(remaining<=AppPreferences.courtGreen(this)*60000L){color=AppPreferences.GREEN;level=1;}
        if(remaining<=AppPreferences.courtYellow(this)*60000L){color=AppPreferences.GOLD;width=5;level=2;}
        if(remaining<=AppPreferences.courtRed(this)*60000L){color=AppPreferences.RED;width=6;level=3;}
        courtButton.setTextColor(color==muted?foreground:color);
        courtBackground.setStroke(dp(width),color);
        courtAlertView.setAlertColor(color==muted?0:color);
        maybeVibrateCourtWarning(level);
    }
    private void maybeVibrateCourtWarning(int level){
        if(level==0||!AppPreferences.courtVibration(this)||level<=AppPreferences.courtVibratedLevel(this))return;
        AppPreferences.saveCourtVibratedLevel(this,level);
        Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
        if(vibrator==null||!vibrator.hasVibrator())return;
        long[] pattern=new long[level*2];
        for(int i=0;i<level;i++){pattern[i*2]=i==0?0:150;pattern[i*2+1]=180;}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)vibrator.vibrate(VibrationEffect.createWaveform(pattern,-1));
        else vibrator.vibrate(pattern,-1);
    }
    private void showCourtSettings(){
        LinearLayout form=box(surface);form.setPadding(dp(24),dp(12),dp(24),dp(12));
        EditText duration=field(String.valueOf(AppPreferences.courtDuration(this)),InputType.TYPE_CLASS_NUMBER);
        EditText green=field(String.valueOf(AppPreferences.courtGreen(this)),InputType.TYPE_CLASS_NUMBER);
        EditText yellow=field(String.valueOf(AppPreferences.courtYellow(this)),InputType.TYPE_CLASS_NUMBER);
        EditText red=field(String.valueOf(AppPreferences.courtRed(this)),InputType.TYPE_CLASS_NUMBER);
        addSetting(form,"Czas wynajmu (minuty)",duration);
        addSetting(form,"Zielone ostrzeżenie (minut przed końcem)",green);
        addSetting(form,"Żółte ostrzeżenie (minut przed końcem)",yellow);
        addSetting(form,"Czerwone ostrzeżenie (minut przed końcem)",red);
        CheckBox vibration=new CheckBox(this);
        vibration.setText("Wibracje przy ostrzeżeniach: 1, 2 i 3 razy");
        vibration.setTextColor(foreground);
        vibration.setChecked(AppPreferences.courtVibration(this));
        form.addView(vibration);
        View[] courtInputs={duration,green,yellow,red,vibration};
        String baseline=settingsSignature(courtInputs);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Wynajem kortu").setView(scroll)
            .setNegativeButton("ANULUJ",null).setPositiveButton("ZAPISZ",null).create();
        dialog.setOnShowListener(ignored->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                int d=Integer.parseInt(duration.getText().toString()),g=Integer.parseInt(green.getText().toString()),y=Integer.parseInt(yellow.getText().toString()),r=Integer.parseInt(red.getText().toString());
                if(d<1||d>1440||r<0||r>=y||y>=g||g>=d)throw new NumberFormatException();
                AppPreferences.saveCourtSettings(this,d,g,y,r);
                AppPreferences.saveCourtVibration(this,vibration.isChecked());
                updateCourtTimer();dialog.dismiss();
            }catch(NumberFormatException error){
                new AlertDialog.Builder(this).setMessage("Wpisz minuty tak, aby: 0 ≤ czerwony < żółty < zielony < czas wynajmu (maks. 1440).")
                    .setPositiveButton("OK",null).show();
            }
            });
            protectSettingsDialog(dialog,baseline,courtInputs);
        });
        dialog.show();
    }
    private void updateBallBorder(int player){
        GradientDrawable border=player==1?playerOneBackground:playerTwoBackground;
        int color=player==1?playerOneColor:playerTwoColor;
        ValueAnimator pulse=player==1?playerOnePulse:playerTwoPulse;
        if(!isBallFor(player)||match.isMatchFinished()){
            stopBallPulse(player);
            border.setStroke(dp(2),color);
            return;
        }
        if(pulse!=null)return;
        pulse=ValueAnimator.ofInt(dp(9),dp(15));
        pulse.setDuration(750);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation->border.setStroke((Integer)animation.getAnimatedValue(),color));
        if(player==1)playerOnePulse=pulse;else playerTwoPulse=pulse;
        pulse.start();
    }
    private void stopBallPulse(int player){
        ValueAnimator pulse=player==1?playerOnePulse:playerTwoPulse;
        if(pulse!=null)pulse.cancel();
        if(player==1)playerOnePulse=null;else playerTwoPulse=null;
    }
    private void showMatchFinished(){finishedOverlay=new MatchFinishedView(this,match.getWinner()==1?playerOneName:playerTwoName,()->{finishedOverlay=null;match.reset();currentMatchId=System.currentTimeMillis();resetClock();refreshScreen();},()->{finishedOverlay=null;match.continueWithThreeSets();currentSetElapsedMs=0;resumeMatch();refreshScreen();});((FrameLayout)courtAlertView.getParent()).addView(finishedOverlay,new FrameLayout.LayoutParams(-1,-1));courtAlertView.bringToFront();}
    private void confirmReset(){new AlertDialog.Builder(this).setTitle("Nowy mecz?").setMessage("Biezacy mecz zostanie zakonczony i zapisany w historii.").setNegativeButton("ANULUJ",null).setPositiveButton("NOWY MECZ",(d,w)->resetMatchSafely()).show();}
    private void resetMatchSafely(){if(finishedOverlay!=null){if(finishedOverlay.getParent() instanceof ViewGroup)((ViewGroup)finishedOverlay.getParent()).removeView(finishedOverlay);finishedOverlay=null;}if(match.hasPoints())match.conclude();pauseMatch();archiveCurrentMatch();match.reset();currentMatchId=System.currentTimeMillis();resetClock();refreshScreen();}
    private LinearLayout box(int color){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setBackgroundColor(color);return x;}private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}private Button button(String label,int color){Button b=new Button(this);b.setText(label);b.setTextColor(darkTheme||color!=surface?Color.WHITE:foreground);b.setTextSize(15);b.setAllCaps(false);b.setPadding(dp(8),0,dp(8),0);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(12));b.setBackground(g);return b;}private Button iconButton(String symbol,String desc,int color){Button b=button(symbol,color);b.setTextSize(24);b.setContentDescription(desc);b.setPadding(0,0,0,0);return b;}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
