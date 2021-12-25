//
// Created by admin on 2021/12/25.
//

#include"polygon.h"
#include<cmath>
namespace Polygon{
//class point {

    bool point::operator==(const point &ro)const {
        if (x == ro.x&&y == ro.y) {
            return true;
        }
        else {
            return false;
        }
    }

    double distancebetweenpoint(const point &a, const point &b)//calculate the distance between two difference point
    {
        double x = (a.x - b.x);
        double y = (a.y - b.y);
        x = pow(x, 2);
        y = pow(y, 2);
        double dis = sqrt(x + y);
        return dis;
    }

    double point::distancetoline(const line &line)const//calculate the distance between this point and line
    {
        double A, B, C;
        line.getStandardEquation(A, B, C);
        C = -C;

        //calculate the distance
        double dis =abs( (A*x + B*y + C) / sqrt(A*A + B*B));
        return dis;
    }

//class line {

    void line::getStandardEquation(double &A, double &B, double &C)const {
        //give the line's equation
        double dy1 = end.y - start.y;
        double dx1 = end.x - start.x;
        A = dy1; B = -dx1; C = start.y*dx1 - start.x*dy1;
        C = -C;
        return;
    }

    double line::length()const {//return the length of line
        return distancebetweenpoint(start, end);
    }

    bool line::iflinemeet(const line &other)const//return a bool value represent whether this line meets the other line (1 stand for yes, 0 for no)
    {
        if (end.x<other.start.x || end.y<other.start.y || start.x>other.end.x || start.y>other.end.y)
        {
            return 0;
        }
        //get the line's equation
        double A1, B1, C1;
        getStandardEquation(A1, B1, C1);

        //get the line's equation
        double A2, B2, C2;
        other.getStandardEquation(A2, B2, C2);

        if ((A1 / A2) == (B1 / B2)) {
            if (C1 != C2)
                return 0;
            else
                return 1;
        }

        double rx, ry;//calculate the meet point
        ry = (C1*A2 / A1 - C2) / (B2 - A2*B1 / A1);
        rx = ((-1)*C2 + (-1)*B2*ry) / A2;

        if (rx<start.x || rx<other.start.x || rx>end.x || rx>other.end.x ||
            ry<start.y || ry<other.start.y || ry>end.y || ry>other.end.y) {
            return 0;
        }
        return 1;

    }

//class shape {

    polygon::polygon(const polygon &obj) {
        sidenum = obj.sidenum;
        originalvertex = obj.originalvertex;
        if (!obj.vertexes) {
            vertexes = nullptr;
        }
        else {
            vertexes = new point[obj.sidenum];
            for (int i = 0; i < sidenum; i++) {
                vertexes[i] = obj.vertexes[i];
            }
        }
        if (!obj.sides) {
            sides = nullptr;
        }
        else {
            sides = new line[obj.sidenum];
            for (int i = 0; i < sidenum; i++) {
                sides[i] = obj.sides[i];
            }
        }
    }

    double polygon::perimeter()const throw(Polygon::error) {
        if (!sides) {//the shape's can't be represent/calculate this way
            throw error(cantcalculate);
        }
        double perimeter = 0;
        for (int i = 0; i < sidenum; i++) {
            perimeter += sides[i].length();
        }
        return perimeter;
    }

//the way of calculate the area is refered from the website below:
//https://blog.csdn.net/hemmingway/article/details/7814494
    double polygon::area()const throw(Polygon::error) {
        if (!vertexes) {//the shape has no vertex
            throw error(cantcalculate);
        }
        double s=0;
        int n = sidenum - 1;
        for (int i = 0; i <= n - 1; i++) {
            s += (vertexes[i].x - vertexes[0].x)*(vertexes[i + 1].y - vertexes[0].y) - (vertexes[i].y - vertexes[0].y)*(vertexes[i + 1].x - vertexes[0].x);
        }
        s = abs(s) / 2;
        return s;
    }

    void polygon::setoriginalvertex(point ov) {
        originalvertex = ov;
    }

    int polygon::getnumofsides()const {
        return sidenum;
    }

    point polygon::getoriginalvertex()const {
        return originalvertex;
    }

    polygon::~polygon() {
        if (vertexes != nullptr) {
            delete[]vertexes;
        }
        if (sides != nullptr) {
            delete[]sides;
        }
    }


    polygon::polygon(point Uoriginalvertex, unsigned int numofside ):vertexes(nullptr),sides(nullptr),sidenum(numofside),originalvertex(Uoriginalvertex) {}

    polygon::polygon(unsigned int numofside):sidenum(numofside),vertexes(nullptr),sides(nullptr),originalvertex(O) {}

    bool compareSlope(const line &l, const line &r) {//compare(>) two lines' slope( no numercially, but geometrically)
        bool flagl, flagr;
        flagl = flagr = 0;
        if (l.end.x == l.start.x)//whether the slope is existed
            flagl = 1;
        if (r.end.x == r.start.x)
            flagr = 1;
        if (flagl&&flagr)
            return 0;

        if (flagl) {
            double k = (r.end.y - r.start.y) / (r.end.x - r.start.x);
            if (k >= 0)
                return 1;
            else
                return 0;
        }
        if (flagr) {
            double k = (l.end.y - l.start.y) / (l.end.x - l.start.x);
            if (k >= 0)
                return 0;
            else
                return 1;
        }
        double kr = (r.end.y - r.start.y) / (r.end.x - r.start.x);
        double kl = (l.end.y - l.start.y) / (l.end.x - l.start.x);
        if (kl >= 0 ) {
            if (kr >= 0)
                return kl > kr;
            else
                return 0;
        }
        if (kl < 0) {
            if (kr < 0)
                return kl > kr;
            else
                return 1;
        }
    }

    bool aTempCompareFunctionfor_polygonConstructor(double kl, double kr) {
        if (kl >= 0) {
            if (kr >= 0)
                return kl > kr;
            else
                return 0;
        }
        if (kl < 0) {
            if (kr < 0)
                return kl > kr;
            else
                return 1;
        }
    }

    polygon::polygon(unsigned int numofside, point vertex[]) throw(Polygon::error) {
        if (numofside == 0) {
            throw error(errortype(0));
        }

        //find the originalvertex
        sidenum = numofside; double min = distancebetweenpoint(O, vertex[0]);
        for (int i = 0; i < numofside; i++) {
            if (distancebetweenpoint(O, vertex[i]) <= min) {
                originalvertex = vertex[i];
            }
        }

        //set the vertexes in anti-clock order( only make sense when all the vertexes are in the first quadrant )
        class dot {//use a list structure to reorder
        public:
            point v;
            double k;//the slope of the line which go through the original vertex and this point
            bool flag;//whether the point is the original vertex
            dot *next;
            dot() = default;
            dot(point vertex, point original) :v(vertex),  next(nullptr), flag(0) {
                if (vertex.x == original.x) {//the slope doesn't exist, so I use a really big numerical value to represent it
                    k = 1000000000000;
                    if (vertex.y == original.y)
                        flag = 1;
                }
                else {
                    k = (vertex.y - original.y) / (vertex.x - original.x);
                }
            }
            dot& operator=(const dot &ro) { v = ro.v;  k = ro.k; next = ro.next; flag = ro.flag; }
            dot(const dot &ro) { v = ro.v; k = ro.k; next = nullptr; flag = ro.flag;	}
        };
        typedef dot* dotp;

        int i=1;
        dot *head = new dot(vertex[0],originalvertex);//create the head node
        while (head->flag) {
            delete head;
            head = new dot(vertex[i], originalvertex);
            i++;
        }

        dot *p1, *b1; b1 = head;
        for ( ; i < numofside; i++) {//create the list
            p1 = new dot(vertex[i], originalvertex);
            if (p1->flag) { delete p1; continue; }
            b1->next = p1;
            b1 = p1;
        }

        //the reorder operation
        int num = sidenum-1;
        dotp *arr = new dotp[num];
        dotp b, p, *t, *todelete;
        t = new dotp;//这里只是为了堵住编译器的嘴
        todelete = t;
        b = p = head;

        for ( i = 0; i < num; i++) {
            arr[i] = p;
            p = p->next;
        }

        for (int j = 0; j<num; j++) {
            double temp;
            for (int i = j; i<num; i++) {
                if (i == j) {
                    temp = arr[i]->k;
                    t = &arr[i];
                }
                if (!aTempCompareFunctionfor_polygonConstructor(arr[i]->k, temp)){
                    temp = arr[i]->k;
                    t = &arr[i];
                }
            }
            dotp lin = arr[j];
            arr[j] = *t;
            *t = lin;
        }

        head = arr[0];
        b = head;
        for ( i = 1; i < num; i++) {
            b->next = arr[i];
            b = arr[i];
        }
        b->next = nullptr;


        //create the polygon's vertexes and sides
        vertexes = new point[sidenum];
        sides = new line[sidenum];
        vertexes[0] = originalvertex;
        p1 =  head;
        for ( i = 1; i < num+1; i++) {
            vertexes[i] = p1->v;
            line ts(vertexes[i - 1], vertexes[i]);
            sides[i-1] = ts;
            p1 = p1->next;
        }
        line ts(vertexes[sidenum - 1], vertexes[0]);
        sides[sidenum - 1] = ts;

        //clean up
        delete todelete;
        for ( i = 0; i < num; i++) {
            p = head->next;
            delete head;
            head = p;
        }
    }


}