#pragma once
#include<iostream>
using namespace std;
#define PI 3.1415926535

namespace Polygon{
    enum errortype { noside,cantcalculate };//the type in exceptions class which may occured in shape's functions
    class line;
    class point;
    class error;

//the exception class
    class error {
    private:
        errortype eor;
    public:
        error(errortype e) :eor(e) {}//constructor
        const errortype what()const { return eor; }//for inquiry
    };

//class point used to represent a point in a rectangular coordinate system
    class point {
        friend double distancebetweenpoint(const point &a, const point &b);//calculate the distance between two difference point(which isn't a member function of point)
    public:
        double x;
        double y;
        point(double setx = 0, double sety = 0) { x = setx; y = sety; }//constructor
        double distancetoline(const line &line)const;//calculate the distance between this point and a line
        bool operator==(const point &ro)const;//compare by the x and y of the point
        void show()const { cout << "(" << x << "," << y << ")"; }//a simple output function for test
    };

    const point O(0, 0);//the origin in the rectangular coordinate system

//class line used to represent a line in a rectangular coordinate system
//with two different point, which may used to derive a class vector(for now, the difference between start point and end point doesn't make any sense)
    class line {
        friend bool compareSlope(const line &l, const line &r);//compare(>) two lines' slope( no numercially, but geometrically)
    public:
        point start;
        point end;
        line():start(0,0),end(1,0){}//default constructor which construct the line which go through (0,0) and (1,0)
        line(point startpoint, point endpoint) { start = startpoint; end = endpoint; }//constructor( construct with any two point on the line)
        void getStandardEquation(double &A, double &B, double &C)const;//get the standard equation of line( Ax+By+C=0)
        double length()const;//return the length of line
        bool iflinemeet(const line &other)const;//return a bool value represent whether this line meets the other line (1 stand for yes, 0 for no(coincide situation included))
    };

//class polygon can represent arbitrary planar polygon in the rectangular coordinate system( in the first quadrant)
    class polygon {
    protected:
        point originalvertex;//the vertex of polygon which is the closest to original point
        //( if this shape doesn't have any vertex, then originalvertex is the center point of the shape
        point *vertexes;//save the vertexes of the polygon( the order of the vertexes follow the anti-clockwise and begin in orginalvertex)
        line *sides;//save the sides of the polygon( follow the same rule of the vertexes)
        unsigned int sidenum;//the number of the polygon's sides
        void setoriginalvertex(point ov);//reset the originalvertex of the polygon

    public:
        polygon( unsigned int numofside=0);//contructor which only set number of sides, thus its *sides and *vertexes is null pointer(originalvertex is (0,0))
        polygon(point Uoriginalvertex,unsigned int numofside = 0);//contructor which set number of sides and originalvertex, thus its *sides and *vertexes is null pointer
        polygon(unsigned int numofside, point vertex[]) throw(error);//contructor, whose parameter is a point array saved the vertexes of the polygon
        //if numofside is 0, function will throw a exception( within the exception class is the error type "noside")
        ~polygon();//destructor
        polygon(const polygon &obj);//copy constructor
        point getoriginalvertex()const;//return the originalvertex
        int getnumofsides()const;//return the sides' number of  ploygon
        virtual double area()const throw(error);//return polygon's area( if polygon has no vertexes, this function will throw exception( within the exception class is the error type "cantcalculate")
        virtual double perimeter()const throw(error);//return polygon's perimeter( if polygon has no sides, this function will throw exception( within the exception class is the error type "cantcalculate")
        virtual void show()const {//a simple output function for test
            cout << "original vertex is "; originalvertex.show(); cout << " number of sides is"
                                                                       << sidenum;
        }
    };
    bool compareSlope(const line &l, const line &r);
}