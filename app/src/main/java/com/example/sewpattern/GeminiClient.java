package com.example.sewpattern;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GeminiClient {
  private GeminiClient(){}
  static String read(InputStream input)throws IOException {
    if(input==null)return "";
    try(InputStream in=input;ByteArrayOutputStream b=new ByteArrayOutputStream()){
      byte[] buf=new byte[8192];int n;
      while((n=in.read(buf))!=-1)b.write(buf,0,n);
      return b.toString("UTF-8");
    }
  }
  static JSONObject request(String url,String key,JSONObject body)throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setConnectTimeout(15000);c.setReadTimeout(60000);
    c.setRequestProperty("x-goog-api-key",key);
    try {
      if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
      int status=c.getResponseCode();String raw=read(status>=200&&status<300?c.getInputStream():c.getErrorStream());
      if(status<200||status>=300)throw new ApiError(status,raw);
      return new JSONObject(raw);
    }finally{c.disconnect();}
  }
  public static class ApiError extends IOException {
    public final int status;
    ApiError(int n,String response){super(response);status=n;}
  }
  public static ArrayList<String> models(String key)throws Exception {
    ArrayList<String> names=new ArrayList<>();
    String token="";
    for(int page=0;page<10;page++){
      String url="https://generativelanguage.googleapis.com/v1beta/models?pageSize=100"+(token.isEmpty()?"":"&pageToken="+URLEncoder.encode(token,"UTF-8"));
      JSONObject list=request(url,key,null);
      JSONArray models=list.optJSONArray("models");if(models==null)break;
      for(int i=0;i<models.length();i++){
        JSONObject m=models.getJSONObject(i),methods=m.optJSONArray("supportedGenerationMethods");
        if(methods==null)continue;
        boolean can=false;for(int j=0;j<methods.length();j++)if("generateContent".equals(methods.optString(j)))can=true;
        String n=m.optString("name");
        if(can&&n.startsWith("models/gemini")&&!n.contains("embedding")&&!n.contains("tts")&&!n.contains("image")&&!n.contains("live"))names.add(n);
      }
      token=list.optString("nextPageToken","");if(token.isEmpty())break;
    }
    names.sort((a,b)->Integer.compare(score(b),score(a)));
    return names;
  }
  static int score(String n){int s=0;if(n.contains("flash"))s+=100;if(n.contains("3."))s+=70;if(n.contains("2.5"))s+=30;if(n.contains("pro"))s-=35;if(n.contains("preview"))s-=10;return s;}
  public static String[] generate(String key,JSONObject payload)throws Exception {
    ArrayList<String> list=models(key);
    if(list.isEmpty())throw new IOException("هیچ مدل مناسب تولید محتوا برای این کلید یافت نشد.");
    Exception last=null;
    for(String model:list){
      try{
        JSONObject obj=request("https://generativelanguage.googleapis.com/v1beta/"+model+":generateContent",key,payload);
        JSONArray candidates=obj.optJSONArray("candidates");
        if(candidates==null||candidates.length()==0)throw new IOException("پاسخی تولید نشد؛ محدودیت ایمنی یا سهمیه را بررسی کنید.");
        JSONArray parts=candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder answer=new StringBuilder();
        for(int i=0;i<parts.length();i++)answer.append(parts.getJSONObject(i).optString("text"));
        return new String[]{model,answer.toString()};
      }catch(ApiError e){
        last=e;
        if(e.status==401||e.status==403)throw new IOException("کلید API یا مجوز این حساب معتبر نیست.");
        if(e.status==429)throw new IOException("سهمیه API تمام شده یا محدودیت درخواست فعال است.");
        if(e.status!=404&&e.status!=400)throw e;
      }
    }
    throw new IOException("هیچ مدل سازگاری پاسخ نداد. "+(last==null?"":last.getMessage()));
  }
}
