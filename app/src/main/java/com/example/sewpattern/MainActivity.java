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
    EditText keyField, bust, waist, hip, length, seam, backWaist, shoulderLength, armholeDepth, neckWidth, bustHeight, bustSpacing;
    Spinner units;
    Spinner garment, neckline, sleeves;
    Uri imageUri;
    JSONObject detectedDesign;
    String pdfName="SewPatternGemini-v0.8.1-Metric.pdf";
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
        TextView header=new TextView(this);header.setText("SewPatternGemini  نسخه ۰.۸.۱ | لباس مجلسی");header.setTextSize(24);header.setTextColor(0xff18364f);root.addView(header);
        status=new TextView(this);status.setText("جمنای: بررسی نشده | حالت آفلاین: آماده");status.setPadding(0,10,0,10);root.addView(status);
        button(root,"انتخاب عکس لباس مجلسی",this::pickPhoto);
        photoLabel=new TextView(this);photoLabel.setText("هنوز عکسی انتخاب نشده");root.addView(photoLabel);
        keyField=new EditText(this);keyField.setHint("کلید API جمنای (اختیاری)");keyField.setSingleLine(true);keyField.setInputType(129);
        keyField.setText(getPreferences(0).getString("gemini_key",""));root.addView(keyField);
        button(root,"بررسی اتصال جمنای",()->geminiRequest(false));
        button(root,"تحلیل عکس لباس با جمنای",()->geminiRequest(true));
        suggestion=new TextView(this);suggestion.setText("تحلیل عکس به اینترنت و کلید جمنای نیاز دارد؛ رسم الگوی پایه آفلاین است.");suggestion.setTextIsSelectable(true);root.addView(suggestion);
        TextView offline=new TextView(this);offline.setText("الگوسازی آفلاین لباس مجلسی (سانتی‌متر)");offline.setTextSize(19);offline.setPadding(0,18,0,6);root.addView(offline);
        garment=new Spinner(this);String[] options={"مجلسی با دامن فون","مجلسی با دامن گشاد","مجلسی با دامن راسته","مجلسی دکلته (نیازمند طراحی اختصاصی)","مجلسی ماهی (نیازمند طراحی اختصاصی)","مجلسی آستین‌دار (نیازمند طراحی اختصاصی)"};garment.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));root.addView(garment);
        neckline=new Spinner(this);neckline.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"یقه معمولی","دکلته","یقه هفت","یقه قایقی"}));root.addView(neckline);
        sleeves=new Spinner(this);sleeves.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"بدون آستین","آستین افتاده","آستین معمولی","آستین بلند"}));root.addView(sleeves);
        button(root,"اعمال جزئیات تشخیص‌داده‌شده عکس",this::applyDetectedDesign);
        method=new Spinner(this);String[] methods={"الگوی متریک سفارشی | آزادی معمول","الگوی جذب | آزادی کمتر","الگوی آزاد | آزادی بیشتر","آلدریچ / مولر | فقط مرجع، اجرا نشده"};method.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,methods));root.addView(method);
        methodInfo=new TextView(this);root.addView(methodInfo);method.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> parent){} public void onItemSelected(android.widget.AdapterView<?> parent,View v,int position,long id){updatePattern();}});
        button(root,"مقایسه روش‌های الگو",()->new AlertDialog.Builder(this).setTitle("توضیح روش‌های محاسباتی").setMessage("متریک: بلوک سفارشی با آزادی سینه ۶، کمر ۲ و باسن ۴ سانتی‌متر.\\nجذب: آزادی کمتر؛ پرو الزامی است.\\nآزاد: آزادی بیشتر.\\nآلدریچ و مولر صرفاً مرجع هستند و فرمول اصلی آن‌ها اجرا نشده است. هیچ‌یک هنوز الگوی تضمین‌شده آماده دوخت نیست.").setPositiveButton("تأیید",null).show());
        units=new Spinner(this);units.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"سانتی‌متر","میلی‌متر"}));root.addView(units);
        TextView unitLabel=new TextView(this);unitLabel.setText("واحد اندازه‌گیری همه فیلدها:");root.addView(unitLabel);
        bust=number(root,"دور سینه (سانتی‌متر)","92");waist=number(root,"دور کمر (سانتی‌متر)","72");hip=number(root,"دور باسن (سانتی‌متر)","98");length=number(root,"قد لباس (سانتی‌متر)","100");seam=number(root,"زاپاس دوخت","1");
        backWaist=number(root,"بلندی پشت تا کمر","40");shoulderLength=number(root,"طول سرشانه","12.5");armholeDepth=number(root,"عمق حلقه آستین","20");neckWidth=number(root,"عرض یقه","7.5");bustHeight=number(root,"بلندی سینه","27");bustSpacing=number(root,"فاصله دو سینه","18");
        units.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){int previous=0;public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){if(position==previous)return;float factor=position==1?10f:.1f;for(EditText e:new EditText[]{bust,waist,hip,length,seam,backWaist,shoulderLength,armholeDepth,neckWidth,bustHeight,bustSpacing}){try{e.setText(String.format(java.util.Locale.US,"%.2f",Float.parseFloat(e.getText().toString())*factor));}catch(Exception ignored){}}previous=position;}});
        button(root,"آزمون درزها، تقاطع قطعات و بررسی آماده‌سازی",()->{try{updatePattern();new AlertDialog.Builder(this).setTitle("گزارش کنترل هندسی").setMessage(PatternEngineering.audit(pattern)).setPositiveButton("بستن",null).show();}catch(Exception ex){toast(ex.getMessage());}});
        button(root,"نمایش الگوی اندازه‌گذاری‌شده",()->{try{updatePattern();pattern.draft();toast("پیش‌نمایش به‌روز شد");}catch(Exception ex){toast(ex.getMessage());}});
        pattern=new PatternView(this);root.addView(pattern,new LinearLayout.LayoutParams(-1,1150));
        button(root,"خروجی هر سه روش در ZIP (PDF A4)",()->{try{updatePattern();pattern.draft();}catch(Exception ex){toast(ex.getMessage());return;} Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"SewPatternGemini-v0.8.1-patterns.zip");startActivityForResult(i,CREATE_ZIP);});
        button(root,"خروجی PDF الگو در مقیاس ۱:۱",()->{try{updatePattern();pattern.draft();}catch(Exception ex){toast(ex.getMessage());return;} Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pdfName);startActivityForResult(i,CREATE_PDF);});
        TextView note=new TextView(this);note.setText("توجه: موتور الگوسازی، تطبیق درزها و ارزیابی پایه هندسی در حال توسعه‌اند. این نسخه هنوز مجوز برش مستقیم هر لباس مجلسی نیست؛ قطعات آستین افتاده، بند، سجاف و آستر به‌عنوان قطعات آزمایشی اضافه شدند؛ بلندی و فاصله سینه در راهنمای ساسون اعمال می‌شوند. این قطعات هنوز از نظر طول اتصال، کاپ و تن‌خور تأیید نشده‌اند و برای برش مستقیم پارچه اصلی مناسب نیستند. نخست روی پارچه ارزان نمونه پرو بدوزید.");root.addView(note);
    }
    float parse(EditText e,float fallback){try{String value=e.getText().toString().trim().replace("٫",".");if(value.isEmpty())throw new IllegalArgumentException("اندازه خالی است");float n=Float.parseFloat(value);if(!Float.isFinite(n)||n<=0)throw new IllegalArgumentException("اندازه نامعتبر است");return n/(units.getSelectedItemPosition()==1?10f:1f);}catch(Exception x){throw new IllegalArgumentException("مقدار نامعتبر در فیلد: "+e.getHint());}}
    void updatePattern(){if(pattern==null||method==null||method.getSelectedItem()==null)return;pattern.setParams(parse(bust,92),parse(waist,72),parse(hip,98),parse(length,100),parse(seam,1),(garment.getSelectedItemPosition()==1?"Evening dress - flared skirt":garment.getSelectedItemPosition()==2?"Evening dress - straight skirt":"Evening dress - fitted A-line"));if(garment.getSelectedItemPosition()>2)throw new IllegalArgumentException("برای این مدل، موتور الگوسازی تخصصی هنوز پیاده‌سازی نشده است؛ الگوی عمومی را به جای آن چاپ نکنید.");pattern.setExtra(parse(backWaist,40),parse(shoulderLength,12.5f),parse(armholeDepth,20),parse(neckWidth,7.5f),parse(bustHeight,27),parse(bustSpacing,18));pattern.method=method.getSelectedItemPosition();pattern.neckStyle=neckline.getSelectedItemPosition();pattern.sleeveStyle=sleeves.getSelectedItemPosition();methodInfo.setText(method.getSelectedItem().toString()+" — "+(pattern.method==3?"فقط مرجع؛ خروجی همان الگوی متریک است":"بلوک محاسباتی؛ پرو ضروری است"));pdfName="SewPatternGemini-v0.8.1-"+(pattern.method==0?"Metric":pattern.method==1?"CloseFit":pattern.method==2?"Relaxed":"Reference")+".pdf";pattern.invalidate();}
    void applyDetectedDesign(){if(detectedDesign==null){toast("ابتدا عکس را با جمنای تحلیل کنید");return;}String s=detectedDesign.optString("skirt","unknown"),n=detectedDesign.optString("neckline","unknown"),sl=detectedDesign.optString("sleeve","unknown");if(s.equals("aline"))garment.setSelection(0);else if(s.equals("flared"))garment.setSelection(1);else if(s.equals("straight"))garment.setSelection(2);else if(s.equals("mermaid"))garment.setSelection(4);if(n.equals("regular"))neckline.setSelection(0);else if(n.equals("strapless"))neckline.setSelection(1);else if(n.equals("vneck"))neckline.setSelection(2);else if(n.equals("boat"))neckline.setSelection(3);if(sl.equals("none"))sleeves.setSelection(0);else if(sl.equals("offshoulder"))sleeves.setSelection(1);else if(sl.equals("setin"))sleeves.setSelection(2);else if(sl.equals("long"))sleeves.setSelection(3);toast("جزئیات قابل‌تشخیص اعمال شد؛ قبل از چاپ بررسی کنید");}
    void pickPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        if(req==PICK_IMAGE){imageUri=data.getData();photoLabel.setText("عکس انتخاب‌شده: "+imageUri);try{getContentResolver().takePersistableUriPermission(imageUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
        if(req==CREATE_ZIP){try{updatePattern();int original=pattern.method;String[] names={"Metric","CloseFit","Relaxed"};try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(getContentResolver().openOutputStream(data.getData()))){for(int k=0;k<3;k++){pattern.method=k;PdfDocument doc=pattern.makeTiledPdf();ByteArrayOutputStream output=new ByteArrayOutputStream();doc.writeTo(output);doc.close();zip.putNextEntry(new java.util.zip.ZipEntry("SewPatternGemini-v0.8.1-"+names[k]+".pdf"));zip.write(output.toByteArray());zip.closeEntry();}String instructions="سه روش بلوک محاسباتی سفارشی هستند و نسخه رسمی آلدریچ یا مولر نیستند. در مقیاس ۱۰۰٪ چاپ کنید، خط کنترل ۵۰ میلی‌متری را اندازه بگیرید و قبل از برش پارچه اصلی نمونه پرو بدوزید. قطعات آستین افتاده، آستر و سجاف اولیه و نیازمند اصلاح پرو هستند. زیپ و ساخت کامل کاپ هنوز نهایی نشده‌اند.\\n";zip.putNextEntry(new java.util.zip.ZipEntry("READ_BEFORE_SEWING.txt"));zip.write(instructions.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}pattern.method=original;pattern.invalidate();toast("سه فایل PDF ذخیره شدند؛ پیش از برش پارچه اصلی، نمونه پرو بدوزید.");}catch(Exception ex){toast("خطای ZIP: "+ex.getMessage());}}
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
    void geminiRequest(boolean analyze){
      String key=keyField.getText().toString().trim();
      if(key.length()<10){status.setText("کلید API جمنای وارد نشده است | آفلاین آماده");return;}
      getPreferences(0).edit().putString("gemini_key",key).apply();
      status.setText("در حال شناسایی مدل‌های فعال جمنای...");
      new Thread(()->{
        try{
          JSONObject payload=new JSONObject();
          JSONArray parts=new JSONArray();
          parts.put(new JSONObject().put("text",analyze?
            "Only valid JSON, no markdown. Identify dress design: skirt (aline|flared|straight|mermaid|unknown), neckline (regular|strapless|vneck|boat|unknown), sleeve (none|offshoulder|setin|long|unknown), lining (boolean), straps (boolean), closure (zipper|buttons|unknown), uncertain (array of strings). Never invent measurements.":
            "Reply with CONNECTED only."));
          if(analyze){
            JSONObject img=new JSONObject().put("mime_type","image/jpeg")
                .put("data",android.util.Base64.encodeToString(imageBytes(),android.util.Base64.NO_WRAP));
            parts.put(new JSONObject().put("inline_data",img));
          }
          payload.put("contents",new JSONArray().put(new JSONObject().put("parts",parts)));
          String[] result=GeminiClient.generate(key,payload);
          runOnUiThread(()->{
            status.setText("جمنای متصل است | مدل: "+result[0]+" | آفلاین آماده");
            if(!analyze){suggestion.setText("اتصال جمنای با موفقیت بررسی شد.");return;}
            try {
              String clean=result[1].trim().replace("```json","").replace("```","").trim();
              detectedDesign=new JSONObject(clean);
              suggestion.setText("تحلیل عکس انجام شد. مدل تشخیص‌داده‌شده را بررسی و سپس اعمال کنید: "+detectedDesign.toString(2));
            }catch(Exception e){detectedDesign=null;suggestion.setText("پاسخ مدل در قالب مورد انتظار نبود: "+result[1]);}
          });
        }catch(Exception e){runOnUiThread(()->{
          status.setText("جمنای: اتصال ناموفق | آفلاین آماده");
          suggestion.setText("خطای اتصال: "+e.getMessage());
        });}
      }).start();
    }
    String readAll(InputStream in)throws IOException{if(in==null)return "No response";try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=source.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");}}
}
