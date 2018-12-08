package uk.ac.ed.coinz;
/*
* DownloadCompleteRunner
*
* used in download file task
*
* */
public class DownloadCompleteRunner{
    static String result;
    public static void downloadComplete(String result){
        DownloadCompleteRunner.result = result;
    }
}