package com.example.sewpattern;

import java.util.ArrayList;
import java.util.Locale;

/** Geometry QA on seam lines, independent of display and PDF scaling.
 * Does not certify garment fit, fabric stretch, dart rotation, or human measurements.
 */
public final class PatternEngineering {
    private PatternEngineering(){}
    static double distance(double x,double y,double a,double b){return Math.hypot(x-a,y-b);}
    static double edge(PatternView.Piece p,int a,int b) {
        float[] x=p.xy;return distance(x[2*a],x[2*a+1],x[2*b],x[2*b+1]);
    }
    static double area(float[] x){
        double a=0;int n=x.length/2;
        for(int i=0;i<n;i++){int j=(i+1)%n;a+=(double)x[2*i]*x[2*j+1]-(double)x[2*j]*x[2*i+1];}
        return a/2;
    }
    static double cross(double ax,double ay,double bx,double by,double cx,double cy) {
        return (bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
    }
    static boolean intersects(float[] v,int i,int j) {
        int n=v.length/2;int a=(i+1)%n,b=(j+1)%n;
        double d1=cross(v[2*i],v[2*i+1],v[2*a],v[2*a+1],v[2*j],v[2*j+1]);
        double d2=cross(v[2*i],v[2*i+1],v[2*a],v[2*a+1],v[2*b],v[2*b+1]);
        double d3=cross(v[2*j],v[2*j+1],v[2*b],v[2*b+1],v[2*i],v[2*i+1]);
        double d4=cross(v[2*j],v[2*j+1],v[2*b],v[2*b+1],v[2*a],v[2*a+1]);
        return (d1*d2<-1e-6)&&(d3*d4<-1e-6);
    }
    static double length(float[] x){double d=0;for(int i=0;i<x.length;i+=2){int j=(i+2)%x.length;d+=distance(x[i],x[i+1],x[j],x[j+1]);}return d;}
    public static String audit(PatternView view) {
        ArrayList<PatternView.Piece> list=view.draft();
        StringBuilder report=new StringBuilder("آزمون تخصصی اولیه الگو — واحد: میلی‌متر\n");
        int errors=0,warnings=0;
        for(PatternView.Piece p:list){
            float[] x=p.xy;int n=x.length/2;boolean bad=false;
            if(n<3){errors++;report.append("خطا: قطعه با کمتر از سه نقطه: ").append(p.name).append("\n");continue;}
            for(float a:x)if(!Float.isFinite(a))bad=true;
            if(bad){errors++;report.append("خطا: مختصات نامعتبر: ").append(p.name).append("\n");continue;}
            if(Math.abs(area(x))<10){errors++;report.append("خطا: مساحت نزدیک صفر: ").append(p.name).append("\n");}
            for(int i=0;i<n;i++){
                int next=(i+1)%n;
                if(edge(p,i,next)<1){errors++;report.append("خطا: درز کوتاه/صفر: ").append(p.name).append("\n");}
                for(int j=i+2;j<n;j++){
                    if(i==0&&j==n-1)continue;
                    if(intersects(x,i,j)){errors++;report.append("خطا: تقاطع خودی در ").append(p.name).append("\n");}
                }
            }
            if(length(x)<30){errors++;report.append("خطا: محیط قطعه: ").append(p.name).append("\n");}
        }
        PatternView.Piece front=null,back=null,skirtFront=null,skirtBack=null;
        for(PatternView.Piece p:list){
            if(p.name.equals("بالاتنه جلو"))front=p;
            if(p.name.equals("بالاتنه پشت"))back=p;
            if(p.name.equals("دامن جلو"))skirtFront=p;
            if(p.name.equals("دامن پشت"))skirtBack=p;
        }
        if(front!=null&&back!=null) {
            double sideFront=edge(front,4,5),sideBack=edge(back,4,5);
            double mismatch=Math.abs(sideFront-sideBack);
            report.append(String.format(Locale.US,"اختلاف درز پهلوی بالاتنه: %.1f میلی‌متر\n",mismatch));
            if(mismatch>5){warnings++;report.append("هشدار: پهلوهای بالاتنه نیازمند اصلاح هستند.\n");}
            double shoulderFront=edge(front,2,3),shoulderBack=edge(back,2,3);
            double shoulderMismatch=Math.abs(shoulderFront-shoulderBack);
            report.append(String.format(Locale.US,"اختلاف درز سرشانه: %.1f میلی‌متر\n",shoulderMismatch));
            if(shoulderMismatch>5)warnings++;
        }
        if(skirtFront!=null&&skirtBack!=null){
            double mismatch=Math.abs(edge(skirtFront,2,3)-edge(skirtBack,2,3));
            report.append(String.format(Locale.US,"اختلاف پهلوی دامن: %.1f میلی‌متر\n",mismatch));
            if(mismatch>5)warnings++;
        }
        report.append("کنترل خط دوخت جفت‌درزهای تعریف‌شده:\n");
        for(SeamGraph.Result seam:SeamGraph.validate(list)){
            report.append(String.format(Locale.US,"%s | %.1f / %.1f | اختلاف %.1f | مجاز %.1f میلی‌متر | %s\n",seam.name,seam.firstMm,seam.secondMm,seam.differenceMm,seam.toleranceMm,seam.valid?"قبول":"رد"));
            if(!seam.valid)errors++;
        }
        if(view.neckStyle!=0||view.sleeveStyle>=2){warnings++;report.append("هشدار: مدل یقه یا آستین انتخابی هنوز قالب تخصصی تأییدشده ندارد.\n");}
        warnings++;
        report.append("هشدار: تاج آستین/حلقه، تبدیل صحیح ساسون به کاپ یا برش پرنسسی، تطبیق همه درزهای آستر و سجاف، چرت‌ها و کشش پارچه آزمون و تأیید نشده‌اند.\n");
        report.append("تعداد قطعات: ").append(list.size()).append(" | خطا: ").append(errors).append(" | هشدار: ").append(warnings).append("\n");
        report.append("وضعیت: ").append(errors==0?"کنترل هندسی اولیه گذرانده شد؛ مجوز برش پارچه اصلی صادر نشده است.":"خطای هندسی وجود دارد؛ خروجی برای برش مردود است.");
        return report.toString();
    }
}
