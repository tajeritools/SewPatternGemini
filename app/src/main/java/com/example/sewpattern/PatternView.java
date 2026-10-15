package com.example.sewpattern;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class PatternView extends View {
    public float bust=92, waist=72, hip=98, length=100, allowance=1;
    public String garment="Dress";
    public PatternView(Context ctx) {super(ctx); setBackgroundColor(Color.WHITE);}
    public void setParams(float b,float w,float h,float l,float a,String g) {bust=b;waist=w;hip=h;length=l;allowance=a;garment=g;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c); drawSheet(c,getWidth(),getHeight());}
    public void drawSheet(Canvas c, float w, float h) {
        c.save(); c.drawColor(Color.WHITE);
        float scale=Math.min(w/760f,h/1050f); c.scale(scale,scale);
        Paint ink=new Paint(3); ink.setColor(Color.BLACK); ink.setTextSize(17); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        c.drawText("SEW PATTERN - "+garment.toUpperCase(),30,32,ink);
        ink.setTextSize(12); ink.setTypeface(Typeface.DEFAULT);
        c.drawText("B "+bust+" / W "+waist+" / H "+hip+" / L "+length+" cm | SA "+allowance+" cm",30,54,ink);
        c.drawText("SCHEMATIC ONLY - NOT TO SCALE - VERIFY FIT BEFORE CUTTING",30,73,ink);
        if(!garment.equals("Skirt")) {
            piece(c,32,108,305,350,"FRONT BODICE", "CUT 1 ON FOLD",true,0);
            piece(c,415,108,690,350,"BACK BODICE","CUT 2",false,0);
        }
        if(!garment.equals("Top")) {
            piece(c,32,garment.equals("Skirt")?125:445,310,garment.equals("Skirt")?865:980,"FRONT SKIRT","CUT 1 ON FOLD",true,1);
            piece(c,405,garment.equals("Skirt")?125:445,695,garment.equals("Skirt")?865:980,"BACK SKIRT","CUT 2",false,1);
        }
        if(garment.equals("Top")) {
            piece(c,32,420,305,720,"FRONT LOWER","CUT 1 ON FOLD",true,0);
            piece(c,415,420,690,720,"BACK LOWER","CUT 2",false,0);
        }
        c.restore();
    }
    private void piece(Canvas c,float x1,float y1,float x2,float y2,String name,String cut,boolean fold,int kind){
        Paint p=new Paint(3); p.setStrokeWidth(2);p.setColor(Color.BLACK);
        Path path=new Path();float ww=x2-x1, hh=y2-y1;
        if(kind==1){path.moveTo(x1+ww*.22f,y1);path.lineTo(x2-ww*.22f,y1);path.lineTo(x2,y2);path.quadTo(x1+ww*.5f,y2+16,x1,y2);path.close();}
        else {path.moveTo(x1+ww*.2f,y1);path.quadTo(x1+ww*.5f,y1+32,x2-ww*.2f,y1);path.lineTo(x2,y1+hh*.22f);path.lineTo(x2-ww*.07f,y2);path.lineTo(x1+ww*.07f,y2);path.lineTo(x1,y1+hh*.22f);path.close();}
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(208,235,250));c.drawPath(path,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(Color.BLACK);c.drawPath(path,p);
        p.setPathEffect(new DashPathEffect(new float[]{8,5},0));p.setStrokeWidth(1);
        c.save(); c.clipPath(path);c.drawRect(x1+8,y1+8,x2-8,y2-8,p);c.restore();p.setPathEffect(null);
        p.setStyle(Paint.Style.FILL);p.setTextSize(15);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        float mid=(x1+x2)/2; p.setTextAlign(Paint.Align.CENTER);c.drawText(name,mid,y1+hh*.5f,p);
        p.setTypeface(Typeface.DEFAULT);c.drawText(cut,mid,y1+hh*.5f+22,p);
        p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);
        c.drawLine(mid,y1+hh*.22f,mid,y1+hh*.38f,p);
        c.drawLine(mid,y1+hh*.22f,mid-6,y1+hh*.25f,p);c.drawLine(mid,y1+hh*.22f,mid+6,y1+hh*.25f,p);
        c.drawLine(mid,y1+hh*.38f,mid-6,y1+hh*.35f,p);c.drawLine(mid,y1+hh*.38f,mid+6,y1+hh*.35f,p);
        c.drawLine(x1+10,y1+hh*.5f,x1-2,y1+hh*.5f,p);c.drawLine(x2-10,y1+hh*.5f,x2+2,y1+hh*.5f,p);
        if(fold){p.setStyle(Paint.Style.FILL);p.setTextSize(11);c.drawText("FOLD (schematic)",mid,y2-17,p);}
    }
}
