package com.lxfly2000.animeschedule;

import android.content.Context;

public class BilibiliQueryInfo {
    private Context ctx;
    public BilibiliQueryInfo(Context context){
        ctx=context;
        episodeInfo=new EpisodeInfo();
    }

    public class EpisodeInfo{
        public String ssidString,epidString,cidString,avidString;
        public int videoQuality;

        public int parts;
        public String[]urls;
        public int[]downloadBytes;

        public int queryResult;
        public String resultMessage;
        public int GetDownloadBytesSum(){
            int s=0;
            for (int a : downloadBytes) {
                s += a;
            }
            return s;
        }
    }

    private EpisodeInfo episodeInfo;

    public BilibiliQueryInfo SetParam(String ssidString, String epidString, String avidString, String cidString, int videoQuality){
        episodeInfo.ssidString=ssidString;
        episodeInfo.epidString=epidString;
        episodeInfo.avidString=avidString;
        episodeInfo.cidString=cidString;
        episodeInfo.videoQuality=videoQuality;
        return this;
    }

    public static abstract class OnReturnEpisodeInfoFunction{
        public abstract void OnReturnEpisodeInfo(EpisodeInfo info);
    }
    private OnReturnEpisodeInfoFunction returnEpisodeInfoFunction;

    public BilibiliQueryInfo SetOnReturnEpisodeInfoFunction(OnReturnEpisodeInfoFunction f){
        returnEpisodeInfoFunction=f;
        return this;
    }

    public BilibiliQueryInfo Query(){
        //TODO:获取URL
        return this;
    }
}
