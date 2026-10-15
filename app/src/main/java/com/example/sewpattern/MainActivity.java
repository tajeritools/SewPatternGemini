package com.example.sewpattern;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {
    PatternView pattern;
    TextView status, suggestion, photoLabel;
    EditText keyField, bust, waist, hip, length, seam;
    Spinner garment;
    Uri imageUri;
    String pdfName="SewPatternGemini-v0.3.1-Metric.pdf";
    Spinner method; TextView methodInfo;
    final int PICK_IMAGE=10, CREATE_PDF=11, CREATE_ZIP=12;
    int bg=0xfff3f7fb;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);buildUI();}
    EditText number(LinearLayout parent,String title,String value){
        TextView label=new TextView(this);label.setText(title);parent.addView(label);
        EditText e=new EditText(this);e.setSingleLine(true);e.setInputType(8194);e.setText(value);parent.addView(e);return e;
    }
    Button button(LinearLayout parent,String title,Runnable run){Button x=new Button(this);x.setText(title);parent.addView(x);x.setOnClickListener(v->run.run());return x;}
    void buildUI(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);setContentView(scroll);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,18,20,30);root.setBackgroundColor(bg);scroll.addView(root);
        TextView header=new TextView(this);header.setText("SewPatternGemini  نسخه ۰.۳.۰ | لباس مجلسی");header.setTextSize(24);header.setTextColor(0xff18364f);root.addView(header);
        status=new TextView(this);status.setText("جمنای: بررسی نشده | حالت آفلاین: آماده");status.setPadding(0,10,0,10);root.addView(status);
        button(root,"انتخاب عکس لباس مجلسی",this::pickPhoto);
        photoLabel=new TextView(this);photoLabel.setText("هنوز عکسی انتخاب نشده");root.addView(photoLabel);
        keyField=new EditText(this);keyField.setHint("کلید API جمنای (اختیاری)");keyField.setSingleLine(true);keyField.setInputType(129);
        keyField.setText(getPreferences(0).getString("gemini_key",""));root.addView(keyField);
        button(root,"بررسی اتصال جمنای",()->geminiRequest(false));
        button(root,"تحلیل عکس لباس با جمنای",()->geminiRequest(true));
        suggestion=new TextView(this);suggestion.setText("تحلیل عکس به اینترنت و کلید جمنای نیاز دارد؛ رسم الگوی پایه آفلاین است.");suggestion.setTextIsSelectable(true);root.addView(suggestion);
        TextView offline=new TextView(this);offline.setText("الگوسازی آفلاین لباس مجلسی (سانتی‌متر)");offline.setTextSize(19);offline.setPadding(0,18,0,6);root.addView(offline);
        garment=new Spinner(this);String[] options={"مجلسی جذب با دامن فون","مجلسی با دامن گشاد","مجلسی با دامن راسته"};garment.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));root.addView(garment);
        method=new Spinner(this);String[] methods={"الگوی متریک سفارشی | آزادی معمول","الگوی جذب | آزادی کمتر","الگوی آزاد | آزادی بیشتر","آلدریچ / مولر | فقط مرجع، اجرا نشده"};method.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,methods));root.addView(method);
        methodInfo=new TextView(this);root.addView(methodInfo);method.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> parent){} public void onItemSelected(android.widget.AdapterView<?> parent,View v,int position,long id){updatePattern();}});
        button(root,"مقایسه روش‌های الگو",()->new AlertDialog.Builder(this).setTitle("توضیح روش‌های محاسباتی").setMessage("متریک: بلوک سفارشی با آزادی سینه ۶، کمر ۲ و باسن ۴ سانتی‌متر.\\nجذب: آزادی کمتر؛ پرو الزامی است.\\nآزاد: آزادی بیشتر.\\nآلدریچ و مولر صرفاً مرجع هستند و فرمول اصلی آن‌ها اجرا نشده است. هیچ‌یک هنوز الگوی تضمین‌شده آماده دوخت نیست.").setPositiveButton("تأیید",null).show());
        bust=number(root,"دور سینه (سانتی‌متر)","92");waist=number(root,"دور کمر (سانتی‌متر)","72");hip=number(root,"دور باسن (سانتی‌متر)","98");length=number(root,"قد لباس (سانتی‌متر)","100");seam=number(root,"زاپاس دوخت (سانتی‌متر)","1");
        button(root,"نمایش الگوی اندازه‌گذاری‌شده",this::updatePattern);
        pattern=new PatternView(this);root.addView(pattern,new LinearLayout.LayoutParams(-1,1150));
        button(root,"خروجی هر سه روش در ZIP (PDF A4)",()->{updatePattern(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"SewPatternGemini-v0.3.1-patterns.zip");startActivityForResult(i,CREATE_ZIP);});
        button(root,"خروجی PDF الگو در مقیاس ۱:۱",()->{updatePattern(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pdfName);startActivityForResult(i,CREATE_PDF);});
        TextView note=new TextView(this);note.setText("توجه: این نسخه هنوز الگوی آماده برش لباس مجلسی عکس نیست. الگوهای پایه با اندازه‌های ورودی ترسیم می‌شوند، ولی طراحی دقیق کاپ، ساسون، آستین افتاده، سجاف، آستر و کنترل تن‌خور کامل نیست. روی پارچه اصلی برش نزنید؛ ابتدا الگوی چاپی و نمونه پرو را بررسی کنید.");root.addView(note);
    }
    float parse(EditText e,float fallback){try{return Float.parseFloat(e.getText().toString());}catch(Exception x){return fallback;}}
    void updatePattern(){if(pattern==null||method==null||method.getSelectedItem()==null)return;pattern.setParams(parse(bust,92),parse(waist,72),parse(hip,98),parse(length,100),parse(seam,1),(garment.getSelectedItemPosition()==1?"Evening dress - flared skirt":garment.getSelectedItemPosition()==2?"Evening dress - straight skirt":"Evening dress - fitted A-line"));pattern.method=method.getSelectedItemPosition();methodInfo.setText(method.getSelectedItem().toString()+" — "+(pattern.method==3?"فقط مرجع؛ خروجی همان الگوی متریک است":"بلوک محاسباتی؛ پرو ضروری است"));pdfName="SewPatternGemini-v0.3.1-"+(pattern.method==0?"Metric":pattern.method==1?"CloseFit":pattern.method==2?"Relaxed":"Reference")+".pdf";pattern.invalidate();}
    void pickPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        if(req==PICK_IMAGE){imageUri=data.getData();photoLabel.setText("عکس انتخاب‌شده: "+imageUri);try{getContentResolver().takePersistableUriPermission(imageUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
        if(req==CREATE_ZIP){try{updatePattern();int original=pattern.method;String[] names={"Metric","CloseFit","Relaxed"};try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(getContentResolver().openOutputStream(data.getData()))){for(int k=0;k<3;k++){pattern.method=k;PdfDocument doc=pattern.makeTiledPdf();ByteArrayOutputStream output=new ByteArrayOutputStream();doc.writeTo(output);doc.close();zip.putNextEntry(new java.util.zip.ZipEntry("SewPatternGemini-v0.3.1-"+names[k]+".pdf"));zip.write(output.toByteArray());zip.closeEntry();}String instructions="سه روش بلوک محاسباتی سفارشی هستند و نسخه رسمی آلدریچ یا مولر نیستند. در مقیاس ۱۰۰٪ چاپ کنید، خط کنترل ۵۰ میلی‌متری را اندازه بگیرید و قبل از برش پارچه اصلی نمونه پرو بدوزید. آستین افتاده، آستر، سجاف و زیپ هنوز مدل‌سازی نشده‌اند.\\n";zip.putNextEntry(new java.util.zip.ZipEntry("READ_BEFORE_SEWING.txt"));zip.write(instructions.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}pattern.method=original;pattern.invalidate();toast("سه فایل PDF ذخیره شدند؛ پیش از برش پارچه اصلی، نمونه پرو بدوزید.");}catch(Exception ex){toast("خطای ZIP: "+ex.getMessage());}}
        if(req==CREATE_PDF){try{updatePattern();PdfDocument doc=pattern.makeTiledPdf();try(OutputStream out=getContentResolver().openOutputStream(data.getData())){doc.writeTo(out);}doc.close();toast("PDF ذخیره شد؛ در اندازه واقعی چاپ کنید و خط ۵۰ میلی‌متری را اندازه بگیرید.");}catch(Exception ex){toast("خطای خروجی: "+ex.getMessage());}}
    }
    void toast(String t){runOnUiThread(()->Toast.makeText(this,t,Toast.LENGTH_LONG).show());}
    byte[] imageBytes() throws Exception{
        if(imageUri==null)throw new IllegalStateException("Select a photo first");
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        try(InputStream in=getContentResolver().openInputStream(imageUri)){BitmapFactory.decodeStream(in,null,o);}
        o.inSampleSize=1;while(Math.max(o.outWidth/o.inSampleSize,o.outHeight/o.inSampleSize)>1024)o.inSampleSize*=2;o.inJustDecodeBounds=false;
        Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(imageUri)){bitmap=BitmapFactory.decodeStream(in,null,o);}
        if(bitmap==null)throw new IOException("Could not read image");ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,75,bytes);bitmap.recycle();return bytes.toByteArray();
    }
    void geminiRequest(boolean analyze){String key=keyField.getText().toString().trim();if(key.length()<10){status.setText("جمنای: کلید وارد نشده | آفلاین: آماده");return;}
        getPreferences(0).edit().putString("gemini_key",key).apply();status.setText("جمنای: در حال بررسی | آفلاین: آماده");
        new Thread(()->{try{
            String endpoint="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
            HttpURLConnection conn=(HttpURLConnection)new URL(endpoint).openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(12000);conn.setReadTimeout(45000);conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("x-goog-api-key",key);conn.setDoOutput(true);
            JSONObject payload=new JSONObject();JSONArray parts=new JSONArray();JSONObject textPart=new JSONObject();textPart.put("text",analyze?"Describe the garment structure in concise English: front/back bodice, sleeve type, seams, darts, skirt, straps, and any uncertain features. Do not invent real measurements. Explain what cannot be inferred from this photograph.":"Reply with CONNECTED only.");parts.put(textPart);
            if(analyze){JSONObject inline=new JSONObject();inline.put("mime_type","image/jpeg");inline.put("data",android.util.Base64.encodeToString(imageBytes(),android.util.Base64.NO_WRAP));parts.put(new JSONObject().put("inline_data",inline));}
            payload.put("contents",new JSONArray().put(new JSONObject().put("parts",parts)));
            try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            int code=conn.getResponseCode();InputStream in=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();String response=readAll(in);conn.disconnect();
            if(code<200||code>=300)throw new IOException("HTTP "+code+": "+response.substring(0,Math.min(220,response.length())));
            JSONObject obj=new JSONObject(response);String answer=obj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
            runOnUiThread(()->{status.setText("جمنای: متصل | آفلاین: آماده");suggestion.setText(analyze?answer:"اتصال جمنای تأیید شد. حالت آفلاین مستقل است.");});
        }catch(Exception e){runOnUiThread(()->{status.setText("جمنای: قطع یا خطا | آفلاین: آماده");suggestion.setText(e.getMessage());});}}).start();
    }
    String readAll(InputStream in)throws IOException{if(in==null)return "No response";try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=source.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");}}
}
