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
    String pdfName="SewPattern.pdf";
    final int PICK_IMAGE=10, CREATE_PDF=11;
    int bg=0xfff3f7fb;
    @Override public void onCreate(Bundle b){super.onCreate(b);buildUI();}
    EditText number(LinearLayout parent,String title,String value){
        TextView label=new TextView(this);label.setText(title);parent.addView(label);
        EditText e=new EditText(this);e.setSingleLine(true);e.setInputType(8194);e.setText(value);parent.addView(e);return e;
    }
    Button button(LinearLayout parent,String title,Runnable run){Button x=new Button(this);x.setText(title);parent.addView(x);x.setOnClickListener(v->run.run());return x;}
    void buildUI(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);setContentView(scroll);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,18,20,30);root.setBackgroundColor(bg);scroll.addView(root);
        TextView header=new TextView(this);header.setText("Sew Pattern Studio | Android");header.setTextSize(24);header.setTextColor(0xff18364f);root.addView(header);
        status=new TextView(this);status.setText("Gemini: not checked | Offline: ready");status.setPadding(0,10,0,10);root.addView(status);
        button(root,"Choose garment photo (optional)",this::pickPhoto);
        photoLabel=new TextView(this);photoLabel.setText("No image selected");root.addView(photoLabel);
        keyField=new EditText(this);keyField.setHint("Gemini API key (optional)");keyField.setSingleLine(true);keyField.setInputType(129);
        keyField.setText(getPreferences(0).getString("gemini_key",""));root.addView(keyField);
        button(root,"Check Gemini connection",()->geminiRequest(false));
        button(root,"Analyze garment photo with Gemini",()->geminiRequest(true));
        suggestion=new TextView(this);suggestion.setText("Photo analysis requires a Gemini key and internet. Offline pattern drafting does not.");suggestion.setTextIsSelectable(true);root.addView(suggestion);
        TextView offline=new TextView(this);offline.setText("OFFLINE PARAMETRIC DRAFTING (cm)");offline.setTextSize(19);offline.setPadding(0,18,0,6);root.addView(offline);
        garment=new Spinner(this);String[] options={"Dress","Skirt","Top"};garment.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));root.addView(garment);
        bust=number(root,"Bust circumference (cm)","92");waist=number(root,"Waist circumference (cm)","72");hip=number(root,"Hip circumference (cm)","98");length=number(root,"Garment length (cm)","100");seam=number(root,"Seam allowance (cm)","1");
        button(root,"Generate measured block preview",this::updatePattern);
        pattern=new PatternView(this);root.addView(pattern,new LinearLayout.LayoutParams(-1,1150));
        button(root,"Export tiled A4 pattern PDF (1:1)",()->{updatePattern(); Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pdfName);startActivityForResult(i,CREATE_PDF);});
        TextView note=new TextView(this);note.setText("IMPORTANT: A4 PDF is drafted in true millimetres with size-based basic blocks and seam allowances. These are NOT validated custom-fitted patterns. Bodice depth, armholes, waist darts, fastenings and complex sleeves still require fitting/engineering. Print at 100%, check 50 mm line, assemble tiles and sew a test garment first. Gemini suggestions are not measurements.");root.addView(note);
    }
    float parse(EditText e,float fallback){try{return Float.parseFloat(e.getText().toString());}catch(Exception x){return fallback;}}
    void updatePattern(){pattern.setParams(parse(bust,92),parse(waist,72),parse(hip,98),parse(length,100),parse(seam,1),garment.getSelectedItem().toString());}
    void pickPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        if(req==PICK_IMAGE){imageUri=data.getData();photoLabel.setText("Photo selected: "+imageUri);try{getContentResolver().takePersistableUriPermission(imageUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
        if(req==CREATE_PDF){try{updatePattern();PdfDocument doc=pattern.makeTiledPdf();try(OutputStream out=getContentResolver().openOutputStream(data.getData())){doc.writeTo(out);}doc.close();toast("True-size tiled A4 PDF saved; print 100% and measure 50 mm reference.");}catch(Exception ex){toast("Export failed: "+ex.getMessage());}}
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
    void geminiRequest(boolean analyze){String key=keyField.getText().toString().trim();if(key.length()<10){status.setText("Gemini: API key missing | Offline: ready");return;}
        getPreferences(0).edit().putString("gemini_key",key).apply();status.setText("Gemini: checking... | Offline: ready");
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
            runOnUiThread(()->{status.setText("Gemini: CONNECTED | Offline: ready");suggestion.setText(analyze?answer:"Gemini connection verified. Offline mode works independently.");});
        }catch(Exception e){runOnUiThread(()->{status.setText("Gemini: DISCONNECTED / ERROR | Offline: ready");suggestion.setText(e.getMessage());});}}).start();
    }
    String readAll(InputStream in)throws IOException{if(in==null)return "No response";try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=source.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");}}
}
