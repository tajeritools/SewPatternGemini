package com.example.sewpattern;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.view.View;
import java.util.*;
import java.io.*;

public class PatternView extends View {
  public float bust=92,waist=72,hip=98,length=100,allowance=1;
  public String garment="Evening dress - fitted A-line";
  public int method=0;
  public float backWaist=40, shoulderLength=12.5f, armholeDepth=20, neckWidth=7.5f, bustHeight=27, bustSpacing=18;
  public void setExtra(float back,float shoulder,float armhole,float neck,float height,float spacing){backWaist=back;shoulderLength=shoulder;armholeDepth=armhole;neckWidth=neck;bustHeight=height;bustSpacing=spacing;invalidate();}
  static final float PT_PER_MM=72f/25.4f;
  static final float PAGE_W=210, PAGE_H=297, MARGIN=10;
  static final float TILE_W=190,TILE_H=277;
  static final float GRID=5;
  static class Piece {
    String name,cut; float[] xy; boolean fold; float grainX, grainY1,grainY2;
    Piece(String n,String cut,boolean fold,float... coords){this.name=n;this.cut=cut;this.fold=fold;this.xy=coords;}
  }
  public PatternView(Context c){super(c);setBackgroundColor(Color.WHITE);}
  public void setParams(float b,float w,float h,float l,float a,String g){
    bust=b;waist=w;hip=h;length=l;allowance=a;garment=g;invalidate();
  }
  // Coordinates are millimetres. Drafts are symmetrical half-body blocks.
  // Front and back waist add 2cm total wearing ease; bust +6cm; hips +4cm.
  // Basic bodice block uses a fixed 40cm shoulder-to-waist depth: requires fitting.
  ArrayList<Piece> draft(){
    if(bust<60||bust>180||waist<45||waist>160||hip<60||hip>190||
       length<45||length>220||allowance<0||allowance>3)
       throw new IllegalArgumentException("Check dimensions: bust 60-180, waist 45-160, hip 60-190, length 45-220, seam 0-3 cm");
    ArrayList<Piece> out=new ArrayList<>();
    float bustEase=method==1?3:method==2?10:6;
    float waistEase=method==1?1:method==2?6:2;
    float hipEase=method==1?2:method==2?8:4;
    float bw=(bust+bustEase)*2.5f;  // quarter-bust in mm
    float ww=(waist+waistEase)*2.5f; // quarter-waist
    float hw=(hip+hipEase)*2.5f;
    if(backWaist<25||backWaist>65||shoulderLength<8||shoulderLength>22||armholeDepth<12||armholeDepth>35||neckWidth<4||neckWidth>14||bustHeight<14||bustHeight>45||bustSpacing<10||bustSpacing>32) throw new IllegalArgumentException("اندازه‌های تکمیلی را بررسی کنید");
    float bodiceH=backWaist*10;
    float skirtH=Math.max(100,length*10-bodiceH);
    float shoulder=Math.min(bw-30,neckWidth*10+shoulderLength*10);
    float armY=armholeDepth*10;
    if(true){
      // Coordinates outer contour WITHOUT seam allowance; dart intake represented by waist-shaping seam.
      Piece f=new Piece("بالاتنه جلو","برش یک عدد روی دولای بسته",true,
        0,0, neckWidth*10,0, neckWidth*10,75, shoulder,42, bw-10,armY, ww, bodiceH, 0,bodiceH);
      Piece b=new Piece("بالاتنه پشت","برش دو عدد قرینه",false,
        0,0,neckWidth*10,0,neckWidth*10,25,shoulder,38,bw-13,armY,ww,bodiceH,0,bodiceH);
      out.add(f);out.add(b);
    }
    if(true){
      // A-line front/back skirt with fit-dependent width, moderate hem sweep
      float hem=garment.contains("flared")? Math.max(hw+180,ww+260) : garment.contains("straight")? Math.max(hw,ww+30) : Math.max(hw+75,ww+120);
      Piece f=new Piece("دامن جلو","برش یک عدد روی دولای بسته",true,
        0,0,ww,0,hw,200,hem,skirtH,0,skirtH);
      Piece b=new Piece("دامن پشت","برش دو عدد قرینه",false,
        0,0,ww,0,hw,200,hem,skirtH,0,skirtH);
      out.add(f);out.add(b);
    }
    return out;
  }
  static Paint pen(int color,float size){Paint p=new Paint(3);p.setColor(color);p.setStrokeWidth(size);return p;}
  static Path path(float[] xy){
    Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);p.close();return p;
  }
  // Polygon offset via adjacent edge intersections. Uses winding to choose outward normal.
  static float[] offset(float[] xy,float margin){
    int n=xy.length/2;float area=0;for(int i=0;i<n;i++){int j=(i+1)%n;area+=xy[2*i]*xy[2*j+1]-xy[2*j]*xy[2*i+1];}
    float sign=area>=0?1:-1;float[] out=new float[xy.length];
    for(int i=0;i<n;i++){
      int prev=(i+n-1)%n,next=(i+1)%n;
      float ax=xy[2*i]-xy[2*prev],ay=xy[2*i+1]-xy[2*prev+1];
      float bx=xy[2*next]-xy[2*i],by=xy[2*next+1]-xy[2*i+1];
      float al=(float)Math.hypot(ax,ay),bl=(float)Math.hypot(bx,by);
      ax/=al;ay/=al;bx/=bl;by/=bl;
      float n1x=sign*ay,n1y=-sign*ax,n2x=sign*by,n2y=-sign*bx;
      float denom=ax*by-ay*bx;
      float px=xy[2*i]+n1x*margin,py=xy[2*i+1]+n1y*margin;
      if(Math.abs(denom)<.0001){out[2*i]=px;out[2*i+1]=py;continue;}
      float qx=xy[2*i]+n2x*margin,qy=xy[2*i+1]+n2y*margin;
      float t=((qx-px)*by-(qy-py)*bx)/denom;
      float mx=px+t*ax,my=py+t*ay;
      if(Math.hypot(mx-xy[2*i],my-xy[2*i+1])>margin*6){
        mx=xy[2*i]+(n1x+n2x)*margin/2;my=xy[2*i+1]+(n1y+n2y)*margin/2;
      }
      out[2*i]=mx;out[2*i+1]=my;
    }
    return out;
  }
  static float[] bounds(float[] a){
    float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-1e6f,maxY=-1e6f;
    for(int i=0;i<a.length;i+=2){minX=Math.min(minX,a[i]);maxX=Math.max(maxX,a[i]);minY=Math.min(minY,a[i+1]);maxY=Math.max(maxY,a[i+1]);}
    return new float[]{minX,minY,maxX,maxY};
  }
  void drawPiece(Canvas c,Piece piece,float sa){
    float[] inner=piece.xy,outer=offset(inner,sa);
    Paint fill=pen(0xffd3eafa,0); fill.setStyle(Paint.Style.FILL);c.drawPath(path(outer),fill);
    Paint outline=pen(Color.BLACK,.55f);outline.setStyle(Paint.Style.STROKE);c.drawPath(path(outer),outline);
    Paint stitch=pen(0xff444444,.35f);stitch.setStyle(Paint.Style.STROKE);
    stitch.setPathEffect(new DashPathEffect(new float[]{3,2},0));c.drawPath(path(inner),stitch);
    float[] bb=bounds(inner);
    float mid=(bb[0]+bb[2])/2;
    Paint ink=pen(Color.BLACK,.4f);ink.setTextSize(6);ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
    float lineY=bb[1]+Math.min(95,(bb[3]-bb[1])*.48f);
    c.drawText(piece.name,bb[0]+10,lineY,ink);
    ink.setTypeface(Typeface.DEFAULT);ink.setTextSize(5);
    c.drawText(piece.cut,bb[0]+10,lineY+9,ink);
    c.drawText("زاپاس "+(sa/10)+" سانتی متر",bb[0]+10,lineY+18,ink);
    float gx=bb[0]+Math.max(24,(bb[2]-bb[0])*.53f);
    float gy1=bb[1]+Math.min(50,(bb[3]-bb[1])*.18f),gy2=gy1+Math.min(65,(bb[3]-bb[1])*.22f);
    c.drawLine(gx,gy1,gx,gy2,outline);
    c.drawLine(gx,gy1,gx-4,gy1+7,outline);c.drawLine(gx,gy1,gx+4,gy1+7,outline);
    c.drawLine(gx,gy2,gx-4,gy2-7,outline);c.drawLine(gx,gy2,gx+4,gy2-7,outline);
    // Mid-side match notches are short crossing lines; not for automatic notcher tooling.
    for(int i=0;i<inner.length;i+=2){
      int j=(i+2)%inner.length;
      float x=(inner[i]+inner[j])/2,y=(inner[i+1]+inner[j+1])/2;
      if(i==0 || i==inner.length-2)continue;
      c.drawLine(x-3,y-3,x+3,y+3,outline);
    }
    if(piece.fold){
      Paint blue=pen(0xff185c88,.5f);blue.setStyle(Paint.Style.STROKE);
      c.drawLine(0,0,0,bb[3],blue);
      ink.setTextSize(5);c.drawText("لبه روی دولای بسته",5,bb[3]-10,ink);
    }
  }
  // One separate multi-page A4 print sheet per pattern piece. Page coordinates remain true mm.
  public PdfDocument makeTiledPdf(){
    ArrayList<Piece> pieces=draft();PdfDocument doc=new PdfDocument();
    int page=0;float sa=allowance*10;
    for(Piece piece:pieces){
      float[] b=bounds(offset(piece.xy,sa));
      float minX=Math.min(0,b[0])-12,minY=b[1]-12;
      float width=b[2]-minX+12,height=b[3]-minY+12;
      int cols=Math.max(1,(int)Math.ceil(width/TILE_W));
      int rows=Math.max(1,(int)Math.ceil(height/TILE_H));
      for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
        PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(
          Math.round(PAGE_W*PT_PER_MM),Math.round(PAGE_H*PT_PER_MM),++page).create());
        Canvas canvas=pg.getCanvas();canvas.save();canvas.scale(PT_PER_MM,PT_PER_MM);
        Paint black=pen(Color.BLACK,.3f);black.setStyle(Paint.Style.STROKE);
        canvas.drawRect(MARGIN,MARGIN,PAGE_W-MARGIN,PAGE_H-MARGIN,black);
        Paint txt=pen(Color.BLACK,0);txt.setTextSize(4.5f);
        canvas.drawText(piece.name+" | صفحه "+(col+1)+"/"+cols+" - "+(row+1)+"/"+rows,12,6,txt);
        canvas.drawText("PRINT 100% / ACTUAL SIZE - NO FIT TO PAGE",12,294,txt);
        // 50mm scale verification line.
        if(row==0&&col==0){canvas.drawLine(115,6,165,6,black);canvas.drawText("۵۰ میلی متر",170,7,txt);}
        canvas.clipRect(MARGIN,MARGIN,PAGE_W-MARGIN,PAGE_H-MARGIN);
        canvas.translate(MARGIN-col*TILE_W-minX,MARGIN-row*TILE_H-minY);
        drawPiece(canvas,piece,sa);
        canvas.restore();doc.finishPage(pg);
      }
    }
    return doc;
  }
  @Override protected void onDraw(Canvas c){super.onDraw(c);
    c.drawColor(Color.WHITE);
    ArrayList<Piece> p;
    try{p=draft();}catch(Exception e){c.drawText(e.getMessage(),15,40,pen(Color.RED,1));return;}
    float x=15,y=20;
    Paint label=pen(Color.BLACK,1);label.setTextSize(13);
    c.drawText( "الگوی مجلسی | "+(method==0?"متریک":method==1?"جذب":method==2?"آزاد":"مرجع"),x,y,label);
    for(Piece piece:p){
      float[] b=bounds(offset(piece.xy,allowance*10));
      float scale=Math.min((getWidth()-35)/(b[2]-b[0]),170/(b[3]-b[1]));
      c.save();c.translate(15,y+18);c.scale(scale,scale);c.translate(-b[0],-b[1]);
      drawPiece(c,piece,allowance*10);c.restore();y+=195;
    }
  }
}
