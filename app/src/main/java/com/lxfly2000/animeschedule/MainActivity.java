/*TODO:
* 读取json文件并显示（OK）
* 异步加载图片，更新列表显示
* 根据json中项目的更新日期排序（OK）
* 可增加/删除/长按修改项目，并保存至本地文件
* 对于B站链接，可根据链接自动获取所需信息（简介，时间，分类等）
* 可直接在列表上标记观看集数
* 点击打开链接（OK）
* 更新集数提示（用对话框显示，可选择今日不再提示(Neu)/关闭(Posi)）（OK）
* JGit上传/下载数据
*/

package com.lxfly2000.animeschedule;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.JSONFormatter;
import com.lxfly2000.utilities.YMDDate;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private AnimeJson animeJson;
    private ArrayList<Integer>jsonSortTable;
    private int sortOrder=0;
    ListView listAnime;
    FloatingActionButton fab;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        fab=(FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnActionSettings();
            }
        });

        if(!AndroidUtility.CheckPermissionWithFinishOnDenied(this,
                "android.permission.READ_EXTERNAL_STORAGE","No reading permission."))
            return;
        preferences=Values.GetPreference(this);
        if(preferences.getString(Values.keyRepositoryUrl,Values.vDefaultString).contentEquals(Values.vDefaultString)){
            Toast.makeText(this,R.string.message_build_default_settings,Toast.LENGTH_LONG).show();
            Values.BuildDefaultSettings(this);
        }
        listAnime=(ListView)findViewById(R.id.listAnime);
        listAnime.setOnItemClickListener(listAnimeCallback);
        ReadJsonFile();
        DisplayList();
        GetAnimeUpdateInfo(true);
    }

    AdapterView.OnItemClickListener listAnimeCallback=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(animeJson.GetWatchUrl(jsonSortTable.get(i)))));
        }
    };

    private void ReadJsonFile(){
        animeJson=new AnimeJson(Values.GetJsonDataFullPath());
    }

    private void SaveJsonFile(){
        FileUtility.WriteFile(Values.GetJsonDataFullPath(), JSONFormatter.Format(animeJson.toString()));
    }

    private void DisplayList(){
        BuildSortTable(2);
        ArrayList<HashMap<String,Object>>listItems=new ArrayList<>();
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            HashMap<String,Object>listItem=new HashMap<>();
            listItem.put("title",animeJson.GetTitle(jsonSortTable.get(i)));
            listItem.put("description",animeJson.GetDescription(jsonSortTable.get(i)));
            StringBuilder rankingString=new StringBuilder();
            for(int j=0;j<5;j++){
                rankingString.append(j<animeJson.GetRank(jsonSortTable.get(i))?"★":"☆");
            }
            listItem.put("ranking",rankingString.toString());
            listItem.put("schedule",animeJson.GetLastUpdateYMDDate(jsonSortTable.get(i)).ToYMDString()+"更新"+animeJson.GetLastUpdateEpisode(jsonSortTable.get(i))+"话");
            listItem.put("cover", R.mipmap.ic_launcher);
            listItems.add(listItem);
        }
        String[]keyStrings={"title","description","ranking","schedule","cover"};
        int[]viewIds={R.id.textAnimeTitle,R.id.textAnimeDescription,R.id.textRanking,R.id.textSchedule,R.id.imageCover};
        listAnime.setAdapter(new SimpleAdapter(this,listItems,R.layout.item_anime,keyStrings,viewIds));
    }

    //排序，order:0=不排序，1=升序，2=降序
    private void BuildSortTable(final int order){
        sortOrder=order;
        int listCount=animeJson.GetAnimeCount();
        jsonSortTable=new ArrayList<>(listCount);
        for(int i=0;i<listCount;i++)
            jsonSortTable.add(i);
        if(order==0)
            return;
        jsonSortTable.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                switch (order){
                    case 1:return animeJson.GetLastUpdateYMDDate(a).IsEarlierThanDate(animeJson.GetLastUpdateYMDDate(b))?-1:1;
                    case 2:return animeJson.GetLastUpdateYMDDate(a).IsLaterThanDate(animeJson.GetLastUpdateYMDDate(b))?-1:1;
                }
                return 0;
            }
        });
        int processingTotal=listCount;
        for(int i=0;i<processingTotal;){
            if(animeJson.GetAbandoned(jsonSortTable.get(i))){
                jsonSortTable.add(jsonSortTable.get(i));
                jsonSortTable.remove(i);
                processingTotal--;
            }else{
                i++;
            }
        }
    }

    private void GetAnimeUpdateInfo(boolean onStartup){
        if(onStartup&&preferences.getString(Values.keyAnimeInfoDate,Values.vDefaultString).contentEquals(YMDDate.GetTodayDate().ToYMDString()))
            return;
        StringBuilder msg=new StringBuilder();
        if(animeJson.GetAnimeCount()==0)
            msg.append("无数据\n");
        else
            msg.append("上次观看：").append(animeJson.GetLastWatchDateString()).append(" ").append(animeJson.GetTitle(animeJson.GetLastWatchIndex())).
                    append(" 第").append(animeJson.GetLastWatchEpisode()).append("话\n");
        msg.append("更新信息：");
        int behindCount=0;
        ArrayList<Integer>uTable=new ArrayList<>();
        for(int i=0;i<jsonSortTable.size();i++) {
            if(sortOrder==2)
                uTable.add(i, jsonSortTable.get(jsonSortTable.size() - i - 1));
            else
                uTable.add(i,jsonSortTable.get(i));
        }
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            if(!animeJson.GetAbandoned(uTable.get(i))){
                boolean haveNotWatched=false;
                for(int j=0;j<animeJson.GetLastUpdateEpisode(uTable.get(i));j++){
                    if(!animeJson.GetEpisodeWatched(uTable.get(i), j+1)){
                        haveNotWatched=true;
                        break;
                    }
                }
                if(haveNotWatched) {
                    behindCount++;
                    msg.append("\n").append(animeJson.GetTitle(uTable.get(i))).append(" ").append(animeJson.GetLastUpdateYMDDate(uTable.get(i)).ToYMDString()).
                            append(" 已更新至").append(animeJson.GetLastUpdateEpisode(uTable.get(i))).append("话");
                }
            }
        }
        if(behindCount==0){
            msg.append("你已跟上所有番剧的更新进度。");
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_show_anime_update)
                .setMessage(msg.toString())
                .setPositiveButton(android.R.string.ok,null)
                .setNeutralButton("今日不再提示", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        preferences.edit().putString(Values.keyAnimeInfoDate,YMDDate.GetTodayDate().ToYMDString()).apply();
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_settings:OnActionSettings();return true;
            case R.id.action_show_anime_update:OnShowAnimeUpdate();return true;
            case R.id.action_view_web_page:OnViewWebPage();return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void OnShowAnimeUpdate(){
        GetAnimeUpdateInfo(false);
    }

    private void OnViewWebPage(){
        String url=preferences.getString(Values.keyRepositoryUrl,"");
        if(!url.startsWith("https://github.com")){
            AndroidUtility.MessageBox(this,getString(R.string.message_not_supported_url));
            return;
        }
        url=url.substring(19);
        if(url.endsWith(".git"))
            url=url.substring(0,url.length()-4);
        String[]urlParts=url.split("/");
        url="https://"+urlParts[0]+".github.io/"+urlParts[1];
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }

    private void OnActionSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.id.action_settings&0xFFFF);
    }
}
