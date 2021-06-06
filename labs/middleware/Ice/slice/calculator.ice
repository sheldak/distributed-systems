
#ifndef CALC_ICE
#define CALC_ICE

module Demo
{
  enum operation { MIN, MAX, AVG };
  
  exception NoInput {};

  struct A
  {
    short a;
    long b;
    float c;
    string d;
  }

  sequence<long> B;


  interface Calc
  {
    long add(int a, int b);
    long subtract(int a, int b);
    void op(A a1, short b1); //zalomy, ze to tez jest operacja arytmetyczna ;)
    float avg(B b);
  };

};

#endif
