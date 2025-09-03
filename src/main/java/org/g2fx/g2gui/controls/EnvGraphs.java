package org.g2fx.g2gui.controls;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import org.g2fx.g2gui.ui.UIElements;

public interface EnvGraphs {

    enum CurveType {
        exp,lin,log;
        public static final CurveType[][] ADSR = {
                {exp, exp, exp},
                {lin, exp, exp},
                {log, exp, exp},
                {lin, lin, lin}
        };
        public static final CurveType[][] ADDSR = {
                {exp,exp,exp,exp},
                {lin,exp,exp,exp},
                {log,exp,exp,exp},
                {lin,lin,lin,lin}
        };
        public static final CurveType[][] MULTI = {
            {exp,exp,exp,exp},
            {lin,exp,exp,exp},
            {log,exp,exp,exp},
            {lin,lin,lin,lin}
        };
        public static final CurveType[][] AHD = {
                {exp,lin,exp},
                {lin,lin,exp},
                {log,lin,exp},
                {lin,lin,lin}
        };
        public static final CurveType[][] ADR = {
                {exp,exp},
                {lin,exp},
                {log,exp},
                {lin,lin}
        };
    }

    record Graph(SVGPath g, Node control) {}

    static Graph mkEnvGraphPane(UIElements.Graph c) {

        Group control = new Group();

        Rectangle bg = new Rectangle(c.XPos(), c.YPos(), c.Width(), c.Height());
        bg.setFill(Color.web("#088"));
        control.getChildren().add(bg);

        SVGPath svgPath = new SVGPath();
        svgPath.setLayoutX(c.XPos() + 0.5);
        svgPath.setLayoutY(c.YPos());
        svgPath.setStroke(Color.web("#AFA"));
        svgPath.setFill(Color.web("#00A4A4"));
        svgPath.setStrokeWidth(1.0);

        control.getChildren().add(svgPath);

        return new Graph(svgPath,control);
    }

    static void ahdGraph(Graph g, int attack1,int hold2,int decay4,int shape0,int posNegInv5) {
        EnvProperties ep = (posNegInv5&1)!=0?new EnvProperties(-28,20,28):new EnvProperties(28,20,0);
        EnvSegment[] en = {new EnvSegment(attack1,0,ep),new EnvSegment(hold2,0,ep),new EnvSegment(decay4,127,ep)};

        CurveType[] sm = CurveType.AHD[shape0];
        ep.sustime = computeSustime(en,0);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }

    static void multiEnvGraph(Graph g,
                              int l1, int l2, int l3, int l4,
                              int t1, int t2, int t3, int t4,
                              int susMode, int posNegInvBip, int shape) {
        EnvProperties ep = (posNegInvBip&1)!=0?new EnvProperties(-28,15,28):new EnvProperties(28,15,0);
        EnvSegment[] en = new EnvSegment[]{
                new EnvSegment(t1, 127 - l1, ep), new EnvSegment(t2, 127 - l2, ep),
                new EnvSegment(t3, 127 - l3, ep), new EnvSegment(t4, 127 - l4, ep)
        };
        if (susMode<3)
            en[susMode].sustain = true;

        CurveType[] sm = CurveType.MULTI[shape];
        ep.sustime = computeSustime(en,4.5);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }


    static void adsrGraph(Graph g, int shape, int attack, int decay, int sustain, int release, int posNegBipInv) {

        EnvProperties ep = (posNegBipInv & 1) != 0
                ? new EnvProperties(-28,15,28)
                : new EnvProperties(28,15,0);

        EnvSegment[] en = new EnvSegment[] {
                new EnvSegment(attack, 0, ep),
                new EnvSegment(decay, 127 - sustain, ep, true),
                new EnvSegment(release, 127, ep)
        };

        CurveType[] sm = CurveType.ADSR[shape];

        ep.sustime = computeSustime(en, 3.0);


        String d = drawEnv(en, sm);

        g.g.setContent(d);
    }

    static void adrGraph(Graph g,int shape,int attack,int release,int posNegInv,int dcyRel) {
        EnvProperties ep = (posNegInv&1)!=0?new EnvProperties(-22,15,22):new EnvProperties(22,15,0);
        EnvSegment[] en = {new EnvSegment(attack,0,ep,dcyRel!=0),new EnvSegment(release,127,ep)};
        CurveType[] sm = CurveType.ADR[shape];
        ep.sustime = computeSustime(en, 2.0);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }

    static void addsrGraph(Graph g,int shape,int attack,int decay1,int level1,int decay2,int level2,
                           int release,int sustainMode,int posNegInv) {
        EnvProperties ep = (posNegInv&1)!=0?new EnvProperties(-28,15,28):new EnvProperties(28,15,0);
        EnvSegment[] en = {new EnvSegment(attack,0,ep),new EnvSegment(decay1,127-level1,ep),
                new EnvSegment(decay2,127-level2,ep),new EnvSegment(release,127,ep)};
        en[sustainMode&2|1].sustain = true;
        CurveType[] sm = CurveType.ADDSR[shape];
        ep.sustime = computeSustime(en, 4.0);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }
    static void denvGraph(Graph g,int decay,int posNegInv) {
        EnvProperties ep = (posNegInv&1)!=0?new EnvProperties(-22,31,22):new EnvProperties(22,31,0);
        EnvSegment[] en = {new EnvSegment(0,0,ep),new EnvSegment(decay,127,ep)};
        CurveType[] sm = {CurveType.lin,CurveType.exp};
        ep.sustime = computeSustime(en, 0.0);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }
    static void henvGraph(Graph g,int hold,int posNegInv) {
        EnvProperties ep = (posNegInv&1)!=0?new EnvProperties(-22,31,22):new EnvProperties(22,31,0);
        EnvSegment[] en = {new EnvSegment(0,0,ep),new EnvSegment(hold,0,ep),
                new EnvSegment(0,127,ep),new EnvSegment(200,127,ep)};
        CurveType[] sm ={CurveType.lin,CurveType.lin,CurveType.lin,CurveType.lin};
        ep.sustime = computeSustime(en, 0.0);
        var d = drawEnv(en,sm);
        g.g.setContent(d);
    }


    private static double computeSustime(EnvSegment[] en, double susInit) {
        double sumNonSustain = 0;
        for (EnvSegment seg : en) {
            if (!seg.sustain) sumNonSustain += seg.t;
        }
        double st = susInit - sumNonSustain;
        return st;
    }

    static String drawEnv(EnvSegment[] en, CurveType[] segs) {
        StringBuilder d = new StringBuilder();
        d.append(en[0].initial(en));
        for (int i = 0; i < en.length; i++) {
            CurveType curveType = segs[i];
            switch (curveType) {
                case exp -> d.append(en[i].exp(i, en));
                case lin -> d.append(en[i].lin(i, en));
                case log -> d.append(en[i].log(i, en));
            }
            if (en[i].sustain) {
                d.append(en[i].sseg(i, en));
            }
        }
        return d.toString();
    }


    class EnvProperties {
        public final double ys, xs, yo;

        public EnvProperties(double ys, double xs, double yo) {
            this.ys = ys;
            this.xs = xs;
            this.yo = yo;
        }

        public double sustime;
        public double tacc = 0.0;
    }

    class EnvSegment {
        private static final double UNIT = 1.0 / 128.0;
        private final double t, l;
        private final EnvProperties ep;

        public boolean sustain;


        public EnvSegment(int tRaw, int lRaw, EnvProperties ep) {
            this(tRaw,lRaw, ep, false);
        }

        public EnvSegment(int tRaw, int lRaw, EnvProperties ep, boolean sustain) {
            this.t = tRaw * UNIT;
            this.l = lRaw * UNIT;
            this.ep = ep;
            this.sustain = sustain;
        }

        public String initial(EnvSegment[] en) {
            double startL = en[en.length - 1].l;
            return String.format("M0,%.2f", (startL * ep.ys + ep.yo));
        }

        public String lin(int i, EnvSegment[] en) {
            ep.tacc += this.t;
            return String.format("L%.2f,%.2f", ep.tacc * ep.xs, (this.l * ep.ys + ep.yo));
        }

        public String exp(int i, EnvSegment[] en) {
            double emt1 = ep.tacc + (this.t * 0.16);
            ep.tacc += this.t;
            double emt2 = ep.tacc;
            double y = this.l * ep.ys + ep.yo;
            return String.format("Q%.2f,%.2f %.2f,%.2f", emt1 * ep.xs, y, emt2 * ep.xs, y);
        }

        public String log(int i, EnvSegment[] en) {
            int prevIdx = (i != 0) ? i - 1 : en.length - 1;
            double pli = en[prevIdx].l * ep.ys + ep.yo;
            double emt1 = ep.tacc + (this.t * 0.8);
            ep.tacc += this.t;
            double emt2 = ep.tacc;
            double y = this.l * ep.ys + ep.yo;
            return String.format("Q%.2f,%.2f %.2f,%.2f", emt1 * ep.xs, pli, emt2 * ep.xs, y);
        }

        public String sseg(int i, EnvSegment[] en) {
            ep.tacc += ep.sustime;
            return String.format("L%.2f,%.2f", ep.tacc * ep.xs, (this.l * ep.ys + ep.yo));
        }
    }

    }
