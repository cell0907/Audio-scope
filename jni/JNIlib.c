#include <jni.h>
#include <stdlib.h>

jintArray
Java_com_cell0907_scope2_ProcessorThread_processjni (JNIEnv *env, jobject thisObj, jintArray inJNIArray) {
	int speed=200;
	static int reminder[200]; 	// The reminder samples can't be longer than
								// the speed
	static int left=0;

   // Step 1: Convert the incoming JNI jintarray to C's jint[]
   jint *audioin = (*env)->GetIntArrayElements(env, inJNIArray, NULL);
   if (NULL == audioin) return NULL;
   jsize length = (*env)->GetArrayLength(env, inJNIArray);

   // Put together whatever was left from the past (see below)
   // plus the new audio samples
   int audio[(int)length+left];
   memcpy(audio,reminder,left*sizeof(int));
   memcpy(&audio[left],audioin,(int)length*sizeof(int));

   // Step 2: Perform its intended operations
   // speed is the number of original audio samples that form one
   // pixel in the screen. As long as we got enough for one, we write it
   // in.
   jint outCArray[(int)length/speed+1];
   int x=0,i=0,j;
   int maximum;
   //left+=(int)length;
   left=(int)length;
   while (left>=speed){
	   maximum=0;
	   for (j=0;j<speed;j++)
		   if (audio[x+j]>maximum) maximum=audio[x+j];
	   outCArray[i]=maximum;
	   x+=speed;
	   left-=speed;
	   i++;
   }
   // Whatever was left, save it for next
   if (x>0) x-=speed;
   memcpy(reminder,&audio[x],left*sizeof(int));

   (*env)->ReleaseIntArrayElements(env, inJNIArray, audio, 0); // release resources

   // Step 3: Convert the C's Native jdouble[] to JNI jdoublearray, and return
   jintArray outJNIArray = (*env)->NewIntArray(env, i);  // allocate
   if (NULL == outJNIArray) return NULL;
   (*env)->SetIntArrayRegion(env, outJNIArray, 0 , i, outCArray);  // copy
   return outJNIArray;
}
