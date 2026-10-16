package com.example.sewpattern;
import java.util.*;

/** Explicit seam graph in millimetres; records which stitch-line segments must match.
 * This validates polygon segments, not yet curves, darts, fabric ease, or fitting.
 */
public final class SeamGraph {
  private SeamGraph(){}
  public static final class Edge {
    final String piece,role; final int[] indices;
    Edge(String piece,String role,int... indices){this.piece=piece;this.role=role;this.indices=indices;}
  }
  public static final class Pair {
    final Edge left,right;final double toleranceMm;
    Pair(Edge a,Edge b,double t){left=a;right=b;toleranceMm=t;}
  }
  public static final class Result {
    public final String name;public final double firstMm,secondMm,differenceMm,toleranceMm;
    public final boolean valid;
    Result(String n,double a,double b,double t){name=n;firstMm=a;secondMm=b;differenceMm=Math.abs(a-b);toleranceMm=t;valid=differenceMm<=t;}
  }
  static Map<String,PatternView.Piece> index(List<PatternView.Piece> pieces) {
    Map<String,PatternView.Piece> map=new HashMap<>();
    for(PatternView.Piece p:pieces)map.put(p.name,p);
    return map;
  }
  static double length(Map<String,PatternView.Piece> map,Edge e){
    PatternView.Piece p=map.get(e.piece);
    if(p==null)throw new IllegalArgumentException("قطعه اتصال پیدا نشد: "+e.piece);
    double d=0;for(int i=1;i<e.indices.length;i++){
      int a=e.indices[i-1],b=e.indices[i];if(a<0||b<0||a>=p.xy.length/2||b>=p.xy.length/2)throw new IllegalArgumentException("شماره نقطه نامعتبر: "+e.piece);
      d+=Math.hypot(p.xy[2*a]-p.xy[2*b],p.xy[2*a+1]-p.xy[2*b+1]);
    }
    return d;
  }
  static Pair pair(String label,String left,int[] li,String right,int[] ri,double tolerance){
    return new Pair(new Edge(left,label,li),new Edge(right,label,ri),tolerance);
  }
  public static List<Result> validate(List<PatternView.Piece> pieces){
    Map<String,PatternView.Piece> m=index(pieces);
    ArrayList<Pair> pairs=new ArrayList<>();
    pairs.add(pair("پهلوی بالاتنه جلو و پشت","بالاتنه جلو",new int[]{4,5},"بالاتنه پشت",new int[]{4,5},5));
    pairs.add(pair("سرشانه جلو و پشت","بالاتنه جلو",new int[]{2,3},"بالاتنه پشت",new int[]{2,3},5));
    pairs.add(pair("کمر بالاتنه جلو و دامن جلو","بالاتنه جلو",new int[]{5,6},"دامن جلو",new int[]{0,1},3));
    pairs.add(pair("کمر بالاتنه پشت و دامن پشت","بالاتنه پشت",new int[]{5,6},"دامن پشت",new int[]{0,1},3));
    pairs.add(pair("پهلوی دامن جلو و پشت","دامن جلو",new int[]{1,2,3},"دامن پشت",new int[]{1,2,3},3));
    ArrayList<Result> results=new ArrayList<>();
    for(Pair p:pairs)results.add(new Result(p.left.role,length(m,p.left),length(m,p.right),p.toleranceMm));
    return results;
  }
  public static int failures(List<Result> results){int n=0;for(Result r:results)if(!r.valid)n++;return n;}
}
