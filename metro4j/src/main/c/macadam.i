%module metro4j

%include <typemaps.i>
%include <cpointer.i>
%include <carrays.i>
%include <arrays_java.i>
%include <various.i>

%array_class(double, DoubleArray);
%array_class(long, LongArray);
%array_class(int, IntArray);
%array_class(float, FloatArray);
%array_class(short, ShortArray);

%pointer_class(int, intptr);
%pointer_class(double, doubleptr);
%pointer_class(long, longptr);
%pointer_class(float, floatptr);
%pointer_class(short, shortptr);

//%apply int[] { int * }
//%apply long[] { long * }
//%apply double[] { double * }
//%apply float[] { float * }

%apply char **STRING_ARRAY { char** }

//%typemap(in) struct doubleStruct * %{
//	$1 = *(struct doubleStruct **)&$input; /* cast jlong into C ptr */
//%}
//
//%typemap(out) struct doubleStruct * %{
//	*(struct doubleStruct **)&$result = $1; /* cast C ptr into jdouble */
//%}
//
//%typemap(in) struct longStruct * %{
//	$1 = *(struct longStruct **)&$input; /* cast jlong into C ptr */
//%}
//
//%typemap(out) struct longStruct * %{
//	*(struct longStruct **)&$result = $1; /* cast C ptr into jlong */
//%}
//
//%typemap(in) struct intStruct * %{
//	$1 = *(struct intStruct **)&$input; /* cast jlong into C ptr */
//%}
//
//%typemap(out) struct intStruct * %{
//	*(struct intStruct **)&$result = $1; /* cast C ptr into jlong */
//%}
//
//%typemap(in) struct floatStruct * %{
//	$1 = *(struct floatStruct **)&$input; /* cast jlong into C ptr */
//%}
//
//%typemap(out) struct floatStruct * %{
//	*(struct floatStruct **)&$result = $1; /* cast C ptr into jlong */
//%}
//
//%typemap(in) struct shortStruct * %{
//	$1 = *(struct shortStruct **)&$input; /* cast jlong into C ptr */
//%}
//
//%typemap(out) struct shortStruct * %{
//	*(struct shortStruct **)&$result = $1; /* cast C ptr into jlong */
//%}

/*extern void Do_Metro(long, double, double, double[], long, long[], double[], double[], double[], double[], double[], double[], double[], double[], double[], double[],  double[], double[], double[], double[], long[],  long[], double, long, long, long, double *OUTPUT, double *OUPUT, long *OUTPUT, double *OUTPUT, double *OUTPUT, long *OUTPUT, long);*/

%inline %{
  #include <macadam.h>
  #include <global.h>
  #include <number.h>
%}
