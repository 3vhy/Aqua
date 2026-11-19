package dev.aqua.client.render;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Aqua's original outline icon set, described as Java2D vector paths on a 24x24 grid and
 * rasterised per pixel size by {@link IconAtlas}.
 */
public enum Icon {
    DROP {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(12, 2.5);
            p.curveTo(12, 2.5, 5, 10, 5, 14.5);
            p.curveTo(5, 18.5, 8.2, 21.5, 12, 21.5);
            p.curveTo(15.8, 21.5, 19, 18.5, 19, 14.5);
            p.curveTo(19, 10, 12, 2.5, 12, 2.5);
            p.closePath();
            g.fill(p);
        }
    },
    USER {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
            g.draw(new Arc2D.Double(4.5, 13.5, 15, 13, 0, 180, Arc2D.OPEN));
        }
    },
    MEMORY {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 7, 18, 10, 3, 3));
            for (int i = 0; i < 3; i++) {
                g.draw(new Line2D.Double(7.5 + i * 4.5, 17, 7.5 + i * 4.5, 20));
            }
            g.draw(new Line2D.Double(7, 11, 7, 13));
            g.draw(new Line2D.Double(10.5, 11, 10.5, 13));
            g.draw(new Line2D.Double(13.5, 11, 13.5, 13));
            g.draw(new Line2D.Double(17, 11, 17, 13));
        }
    },
    GLOBE {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Ellipse2D.Double(8, 3, 8, 18));
            g.draw(new Line2D.Double(3.5, 12, 20.5, 12));
        }
    },
    WIFI {
        void draw(Graphics2D g) {
            for (int i = 1; i <= 3; i++) {
                double r = 4.5 * i;
                g.draw(new Arc2D.Double(12 - r, 19 - r, r * 2, r * 2, 45, 90, Arc2D.OPEN));
            }
            g.fill(new Ellipse2D.Double(10.5, 17.5, 3, 3));
        }
    },
    COMMAND {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(9, 9);
            p.lineTo(15, 9);
            p.lineTo(15, 15);
            p.lineTo(9, 15);
            p.closePath();
            g.draw(p);
            g.draw(new Arc2D.Double(3.5, 3.5, 5.5, 5.5, -90, 270, Arc2D.OPEN));
            g.draw(new Arc2D.Double(15, 3.5, 5.5, 5.5, 180, -270, Arc2D.OPEN));
            g.draw(new Arc2D.Double(3.5, 15, 5.5, 5.5, 90, -270, Arc2D.OPEN));
            g.draw(new Arc2D.Double(15, 15, 5.5, 5.5, 90, 270, Arc2D.OPEN));
        }
    },
    FLASK {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(10, 3.5);
            p.lineTo(10, 9.5);
            p.lineTo(4.8, 18.6);
            p.curveTo(4.2, 19.7, 5, 21, 6.3, 21);
            p.lineTo(17.7, 21);
            p.curveTo(19, 21, 19.8, 19.7, 19.2, 18.6);
            p.lineTo(14, 9.5);
            p.lineTo(14, 3.5);
            g.draw(p);
            g.draw(new Line2D.Double(8.5, 3.5, 15.5, 3.5));
            g.draw(new Line2D.Double(7.2, 15, 16.8, 15));
        }
    },
    DISC {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Ellipse2D.Double(9.5, 9.5, 5, 5));
        }
    },
    NOTE {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(9, 18);
            p.lineTo(9, 5.5);
            p.lineTo(19, 3.5);
            p.lineTo(19, 16);
            g.draw(p);
            g.draw(new Ellipse2D.Double(3.5, 15.5, 5.5, 5));
            g.draw(new Ellipse2D.Double(13.5, 13.5, 5.5, 5));
        }
    },
    CLOCK {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            Path2D p = new Path2D.Double();
            p.moveTo(12, 7);
            p.lineTo(12, 12);
            p.lineTo(15.5, 14);
            g.draw(p);
        }
    },
    COMPASS {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            Path2D p = new Path2D.Double();
            p.moveTo(15.5, 8.5);
            p.lineTo(13.5, 13.5);
            p.lineTo(8.5, 15.5);
            p.lineTo(10.5, 10.5);
            p.closePath();
            g.fill(p);
        }
    },
    SHIELD {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(12, 2.8);
            p.lineTo(19.5, 5.8);
            p.curveTo(19.5, 13, 16.5, 18.5, 12, 21.2);
            p.curveTo(7.5, 18.5, 4.5, 13, 4.5, 5.8);
            p.closePath();
            g.draw(p);
        }
    },
    GAUGE {
        void draw(Graphics2D g) {
            g.draw(new Arc2D.Double(3, 5, 18, 18, -20, 220, Arc2D.OPEN));
            g.draw(new Line2D.Double(12, 14, 16, 9.5));
            g.fill(new Ellipse2D.Double(10.5, 12.5, 3, 3));
        }
    },
    MOUSE {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(6.5, 3, 11, 18, 10, 10));
            g.draw(new Line2D.Double(12, 6.5, 12, 10));
        }
    },
    KEYBOARD {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(2.5, 6, 19, 12, 4, 4));
            for (int i = 0; i < 4; i++) {
                g.fill(new Ellipse2D.Double(5.6 + i * 3.6, 9.2, 1.8, 1.8));
            }
            g.draw(new Line2D.Double(8, 14.5, 16, 14.5));
        }
    },
    BOX {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(12, 3);
            p.lineTo(20, 7.5);
            p.lineTo(20, 16.5);
            p.lineTo(12, 21);
            p.lineTo(4, 16.5);
            p.lineTo(4, 7.5);
            p.closePath();
            g.draw(p);
            Path2D q = new Path2D.Double();
            q.moveTo(4.3, 7.7);
            q.lineTo(12, 12);
            q.lineTo(19.7, 7.7);
            q.moveTo(12, 12);
            q.lineTo(12, 20.7);
            g.draw(q);
        }
    },
    BOLT {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(13.5, 2.5);
            p.lineTo(5, 13.5);
            p.lineTo(11.5, 13.5);
            p.lineTo(10.5, 21.5);
            p.lineTo(19, 10.5);
            p.lineTo(12.5, 10.5);
            p.closePath();
            g.draw(p);
        }
    },
    EYE {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(2.5, 12);
            p.curveTo(5.5, 6.5, 9, 5, 12, 5);
            p.curveTo(15, 5, 18.5, 6.5, 21.5, 12);
            p.curveTo(18.5, 17.5, 15, 19, 12, 19);
            p.curveTo(9, 19, 5.5, 17.5, 2.5, 12);
            p.closePath();
            g.draw(p);
            g.draw(new Ellipse2D.Double(9, 9, 6, 6));
        }
    },
    SEARCH {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(4, 4, 12.5, 12.5));
            g.draw(new Line2D.Double(14.8, 14.8, 20, 20));
        }
    },
    CHEVRON_RIGHT {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(9.5, 6);
            p.lineTo(15.5, 12);
            p.lineTo(9.5, 18);
            g.draw(p);
        }
    },
    CHEVRON_DOWN {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(6, 9.5);
            p.lineTo(12, 15.5);
            p.lineTo(18, 9.5);
            g.draw(p);
        }
    },
    CHEVRON_LEFT {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(14.5, 6);
            p.lineTo(8.5, 12);
            p.lineTo(14.5, 18);
            g.draw(p);
        }
    },
    CLOSE {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(6, 6, 18, 18));
            g.draw(new Line2D.Double(18, 6, 6, 18));
        }
    },
    CHECK {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(5, 12.5);
            p.lineTo(10, 17.5);
            p.lineTo(19, 7);
            g.draw(p);
        }
    },
    PLUS {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(12, 5, 12, 19));
            g.draw(new Line2D.Double(5, 12, 19, 12));
        }
    },
    TRASH {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(4, 6.5, 20, 6.5));
            Path2D p = new Path2D.Double();
            p.moveTo(9, 6.5);
            p.lineTo(9, 3.5);
            p.lineTo(15, 3.5);
            p.lineTo(15, 6.5);
            g.draw(p);
            Path2D b = new Path2D.Double();
            b.moveTo(6, 6.5);
            b.lineTo(7, 20.5);
            b.lineTo(17, 20.5);
            b.lineTo(18, 6.5);
            g.draw(b);
        }
    },
    COPY {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(8.5, 8.5, 12, 12, 3, 3));
            Path2D p = new Path2D.Double();
            p.moveTo(15.5, 8.5);
            p.lineTo(15.5, 5.5);
            p.quadTo(15.5, 3.5, 13.5, 3.5);
            p.lineTo(5.5, 3.5);
            p.quadTo(3.5, 3.5, 3.5, 5.5);
            p.lineTo(3.5, 13.5);
            p.quadTo(3.5, 15.5, 5.5, 15.5);
            p.lineTo(8.5, 15.5);
            g.draw(p);
        }
    },
    IMPORT {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(12, 3.5, 12, 15));
            Path2D p = new Path2D.Double();
            p.moveTo(7.5, 10.5);
            p.lineTo(12, 15);
            p.lineTo(16.5, 10.5);
            g.draw(p);
            Path2D t = new Path2D.Double();
            t.moveTo(4, 15.5);
            t.lineTo(4, 20);
            t.lineTo(20, 20);
            t.lineTo(20, 15.5);
            g.draw(t);
        }
    },
    EXPORT {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(12, 15, 12, 3.5));
            Path2D p = new Path2D.Double();
            p.moveTo(7.5, 8);
            p.lineTo(12, 3.5);
            p.lineTo(16.5, 8);
            g.draw(p);
            Path2D t = new Path2D.Double();
            t.moveTo(4, 15.5);
            t.lineTo(4, 20);
            t.lineTo(20, 20);
            t.lineTo(20, 15.5);
            g.draw(t);
        }
    },
    EDIT {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(15.5, 4.5);
            p.lineTo(19.5, 8.5);
            p.lineTo(8.5, 19.5);
            p.lineTo(4, 20);
            p.lineTo(4.5, 15.5);
            p.closePath();
            g.draw(p);
        }
    },
    RESET {
        void draw(Graphics2D g) {
            g.draw(new Arc2D.Double(4, 4, 16, 16, 100, 290, Arc2D.OPEN));
            Path2D p = new Path2D.Double();
            p.moveTo(5.5, 3.5);
            p.lineTo(5.5, 8.5);
            p.lineTo(10.5, 8.5);
            g.draw(p);
        }
    },
    LAYOUT {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
            g.draw(new Line2D.Double(3, 9.5, 21, 9.5));
            g.draw(new Line2D.Double(9.5, 9.5, 9.5, 20));
        }
    },
    PALETTE {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(12, 3);
            p.curveTo(6.5, 3, 3, 7, 3, 12);
            p.curveTo(3, 17, 7, 21, 12, 21);
            p.curveTo(13.5, 21, 14, 19.8, 13.2, 18.6);
            p.curveTo(12.4, 17.4, 13.2, 15.8, 14.8, 15.8);
            p.lineTo(17, 15.8);
            p.curveTo(19.4, 15.8, 21, 14, 21, 11.5);
            p.curveTo(21, 6.8, 17, 3, 12, 3);
            p.closePath();
            g.draw(p);
            g.fill(new Ellipse2D.Double(6.6, 10.2, 2.4, 2.4));
            g.fill(new Ellipse2D.Double(9.6, 6.2, 2.4, 2.4));
            g.fill(new Ellipse2D.Double(14.2, 6.6, 2.4, 2.4));
        }
    },
    FOLDER {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(3, 7);
            p.quadTo(3, 5, 5, 5);
            p.lineTo(9.5, 5);
            p.lineTo(11.5, 7.5);
            p.lineTo(19, 7.5);
            p.quadTo(21, 7.5, 21, 9.5);
            p.lineTo(21, 17);
            p.quadTo(21, 19, 19, 19);
            p.lineTo(5, 19);
            p.quadTo(3, 19, 3, 17);
            p.closePath();
            g.draw(p);
        }
    },
    GEAR {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(9, 9, 6, 6));
            Path2D p = new Path2D.Double();
            int teeth = 8;
            for (int i = 0; i < teeth * 2; i++) {
                double a = Math.PI * 2 * i / (teeth * 2);
                double a2 = Math.PI * 2 * (i + 1) / (teeth * 2);
                double r = (i % 2 == 0) ? 8.6 : 6.6;
                double x1 = 12 + Math.cos(a) * r, y1 = 12 + Math.sin(a) * r;
                double x2 = 12 + Math.cos(a2) * r, y2 = 12 + Math.sin(a2) * r;
                if (i == 0) {
                    p.moveTo(x1, y1);
                } else {
                    p.lineTo(x1, y1);
                }
                p.lineTo(x2, y2);
            }
            p.closePath();
            g.draw(p);
        }
    },
    SPARKLE {
        void draw(Graphics2D g) {
            star(g, 11, 12, 8.5, 2.2);
            star(g, 18.5, 5, 3, 0.9);
        }
    },
    STAR {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + Math.PI * i / 5;
                double r = i % 2 == 0 ? 9 : 4;
                double x = 12 + Math.cos(a) * r, y = 12.6 + Math.sin(a) * r;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            g.draw(p);
        }
    },
    HANGER {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(9.5, 6.5);
            p.curveTo(9.5, 4.5, 11, 3.5, 12, 3.5);
            p.curveTo(13.6, 3.5, 14.5, 4.6, 14.5, 6);
            p.curveTo(14.5, 7.5, 12, 8, 12, 10);
            p.lineTo(3.5, 16.5);
            p.quadTo(2.8, 18, 4.5, 18);
            p.lineTo(19.5, 18);
            p.quadTo(21.2, 18, 20.5, 16.5);
            p.lineTo(12, 10);
            g.draw(p);
        }
    },
    SLIDERS {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(4, 7, 20, 7));
            g.draw(new Line2D.Double(4, 12, 20, 12));
            g.draw(new Line2D.Double(4, 17, 20, 17));
            g.fill(new Ellipse2D.Double(13, 5, 4, 4));
            g.fill(new Ellipse2D.Double(6, 10, 4, 4));
            g.fill(new Ellipse2D.Double(11, 15, 4, 4));
        }
    },
    LOCK {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(5, 10.5, 14, 10, 3, 3));
            g.draw(new Arc2D.Double(8, 4, 8, 10, 0, 180, Arc2D.OPEN));
            g.draw(new Line2D.Double(8, 9, 8, 10.5));
            g.draw(new Line2D.Double(16, 9, 16, 10.5));
        }
    },
    UNLOCK {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(5, 10.5, 14, 10, 3, 3));
            g.draw(new Arc2D.Double(8, 4, 8, 10, 15, 165, Arc2D.OPEN));
            g.draw(new Line2D.Double(8, 9, 8, 10.5));
        }
    },
    MOVE {
        void draw(Graphics2D g) {
            g.draw(new Line2D.Double(12, 3, 12, 21));
            g.draw(new Line2D.Double(3, 12, 21, 12));
            arrowHead(g, 12, 3, 0, -1);
            arrowHead(g, 12, 21, 0, 1);
            arrowHead(g, 3, 12, -1, 0);
            arrowHead(g, 21, 12, 1, 0);
        }
    },
    CAMERA {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(3, 8.5);
            p.quadTo(3, 7, 4.5, 7);
            p.lineTo(7.5, 7);
            p.lineTo(9, 4.5);
            p.lineTo(15, 4.5);
            p.lineTo(16.5, 7);
            p.lineTo(19.5, 7);
            p.quadTo(21, 7, 21, 8.5);
            p.lineTo(21, 18);
            p.quadTo(21, 19.5, 19.5, 19.5);
            p.lineTo(4.5, 19.5);
            p.quadTo(3, 19.5, 3, 18);
            p.closePath();
            g.draw(p);
            g.draw(new Ellipse2D.Double(8.5, 9.5, 7, 7));
        }
    },
    PLAY {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(7.5, 4.5);
            p.lineTo(19, 12);
            p.lineTo(7.5, 19.5);
            p.closePath();
            g.fill(p);
        }
    },
    PAUSE {
        void draw(Graphics2D g) {
            g.fill(new RoundRectangle2D.Double(6, 4.5, 4, 15, 1.5, 1.5));
            g.fill(new RoundRectangle2D.Double(14, 4.5, 4, 15, 1.5, 1.5));
        }
    },
    NEXT {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(5.5, 5);
            p.lineTo(15, 12);
            p.lineTo(5.5, 19);
            p.closePath();
            g.fill(p);
            g.fill(new RoundRectangle2D.Double(15.5, 5, 3, 14, 1, 1));
        }
    },
    PREV {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(18.5, 5);
            p.lineTo(9, 12);
            p.lineTo(18.5, 19);
            p.closePath();
            g.fill(p);
            g.fill(new RoundRectangle2D.Double(5.5, 5, 3, 14, 1, 1));
        }
    },
    POWER {
        void draw(Graphics2D g) {
            g.draw(new Arc2D.Double(4, 5, 16, 16, 125, -250, Arc2D.OPEN));
            g.draw(new Line2D.Double(12, 3, 12, 11));
        }
    },
    BELL {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(6, 16.5);
            p.lineTo(6, 11);
            p.curveTo(6, 7.5, 8.7, 5, 12, 5);
            p.curveTo(15.3, 5, 18, 7.5, 18, 11);
            p.lineTo(18, 16.5);
            p.lineTo(19.5, 18);
            p.lineTo(4.5, 18);
            p.closePath();
            g.draw(p);
            g.draw(new Arc2D.Double(10, 18, 4, 3.5, 180, 180, Arc2D.OPEN));
        }
    },
    PIN {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(12, 21);
            p.curveTo(12, 21, 5, 14.5, 5, 9.5);
            p.curveTo(5, 5.6, 8.1, 2.8, 12, 2.8);
            p.curveTo(15.9, 2.8, 19, 5.6, 19, 9.5);
            p.curveTo(19, 14.5, 12, 21, 12, 21);
            p.closePath();
            g.draw(p);
            g.draw(new Ellipse2D.Double(9.5, 7, 5, 5));
        }
    },
    SMILE {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Arc2D.Double(7.5, 8.5, 9, 8, 200, 140, Arc2D.OPEN));
            g.fill(new Ellipse2D.Double(8.4, 8.4, 2.2, 2.2));
            g.fill(new Ellipse2D.Double(13.4, 8.4, 2.2, 2.2));
        }
    },
    INFO {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Line2D.Double(12, 11, 12, 16.5));
            g.fill(new Ellipse2D.Double(10.9, 6.6, 2.2, 2.2));
        }
    },
    SUN {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(8, 8, 8, 8));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4;
                g.draw(new Line2D.Double(12 + Math.cos(a) * 6.5, 12 + Math.sin(a) * 6.5, 12 + Math.cos(a) * 9, 12 + Math.sin(a) * 9));
            }
        }
    },
    CLOUD {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(7, 18.5);
            p.curveTo(4.2, 18.5, 2.8, 16.2, 3.4, 14);
            p.curveTo(3.9, 12.2, 5.6, 11, 7.4, 11.3);
            p.curveTo(8, 7.8, 11, 5.5, 14.3, 6.2);
            p.curveTo(17.1, 6.8, 18.8, 9.2, 18.8, 11.8);
            p.curveTo(20.8, 12.2, 21.6, 14, 21.2, 15.8);
            p.curveTo(20.8, 17.5, 19.4, 18.5, 17.6, 18.5);
            p.closePath();
            g.draw(p);
        }
    },
    GRID {
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 7, 2, 2));
            g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 7, 2, 2));
            g.draw(new RoundRectangle2D.Double(3.5, 13.5, 7, 7, 2, 2));
            g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7, 7, 2, 2));
        }
    },
    LIST {
        void draw(Graphics2D g) {
            for (int i = 0; i < 3; i++) {
                g.fill(new Ellipse2D.Double(3.5, 5.8 + i * 5.5, 2.4, 2.4));
                g.draw(new Line2D.Double(9, 7 + i * 5.5, 20.5, 7 + i * 5.5));
            }
        }
    },
    CROSSHAIR {
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(5, 5, 14, 14));
            g.draw(new Line2D.Double(12, 2, 12, 7));
            g.draw(new Line2D.Double(12, 17, 12, 22));
            g.draw(new Line2D.Double(2, 12, 7, 12));
            g.draw(new Line2D.Double(17, 12, 22, 12));
        }
    },
    HAND {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(8, 21);
            p.lineTo(6, 13);
            p.quadTo(5.5, 11, 7.2, 10.8);
            p.quadTo(8.6, 10.7, 9, 12.5);
            p.lineTo(9, 4.5);
            p.quadTo(9, 3, 10.5, 3);
            p.quadTo(12, 3, 12, 4.5);
            p.lineTo(12, 10);
            p.lineTo(12, 8.5);
            p.quadTo(12, 7, 13.5, 7);
            p.quadTo(15, 7, 15, 8.5);
            p.lineTo(15, 10.5);
            p.quadTo(15, 9.2, 16.4, 9.2);
            p.quadTo(18, 9.2, 18, 10.8);
            p.lineTo(18, 16);
            p.quadTo(18, 19, 16, 21);
            g.draw(p);
        }
    },
    MAGNET {
        void draw(Graphics2D g) {
            Path2D p = new Path2D.Double();
            p.moveTo(5, 4);
            p.lineTo(5, 12);
            p.curveTo(5, 16.5, 8, 20, 12, 20);
            p.curveTo(16, 20, 19, 16.5, 19, 12);
            p.lineTo(19, 4);
            g.draw(p);
            g.draw(new Line2D.Double(5, 8, 9.5, 8));
            g.draw(new Line2D.Double(14.5, 8, 19, 8));
            Path2D q = new Path2D.Double();
            q.moveTo(9.5, 4);
            q.lineTo(9.5, 12);
            q.curveTo(9.5, 14, 10.5, 15, 12, 15);
            q.curveTo(13.5, 15, 14.5, 14, 14.5, 12);
            q.lineTo(14.5, 4);
            g.draw(q);
        }
    },
    WAVE {
        void draw(Graphics2D g) {
            for (int k = 0; k < 2; k++) {
                Path2D p = new Path2D.Double();
                double y = 9 + k * 6;
                p.moveTo(3, y);
                p.curveTo(6, y - 3, 9, y - 3, 12, y);
                p.curveTo(15, y + 3, 18, y + 3, 21, y);
                g.draw(p);
            }
        }
    };

    abstract void draw(Graphics2D g);

    static void star(Graphics2D g, double cx, double cy, double r, double inner) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - r);
        p.quadTo(cx + inner * 0.4, cy - inner * 0.4, cx + r, cy);
        p.quadTo(cx + inner * 0.4, cy + inner * 0.4, cx, cy + r);
        p.quadTo(cx - inner * 0.4, cy + inner * 0.4, cx - r, cy);
        p.quadTo(cx - inner * 0.4, cy - inner * 0.4, cx, cy - r);
        p.closePath();
        g.draw(p);
    }

    static void arrowHead(Graphics2D g, double x, double y, double dx, double dy) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.atan2(dy, dx));
        Path2D p = new Path2D.Double();
        p.moveTo(-3, -3);
        p.lineTo(0, 0);
        p.lineTo(-3, 3);
        g.draw(p);
        g.setTransform(old);
    }

    /** Stroke used by the icon set (scaled with the canvas). */
    static BasicStroke stroke() {
        return new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
