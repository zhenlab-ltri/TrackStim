// This code is for Arduino leonardo that has a shield consist of
// a 6bit(64step) DA converter x2, and a trigger output for control external device (hamamatsu orca R2)
// Aim to synchronize DAC and trigger signal.
//
//
//-----Hardware specification
//port 2-7 used for 6bit DA1. The port 2 is top bit and 7 is lowest.
//port 8-13 is DA2 (R2-R ladder was constructed. output line is not connected, so cant use for now.)
//each DA1 DA2 output is connected to analog4 and 2 so that can sense of output volatages.
//(not implemented in this code)
//To output 5V at DA1 anytime, toggle switch connect 5V-2k resistor-ground.
//The potential of 2k resistor is sensed at analog0 port.
//The trigger sends via analog1 port connected base of NPN pull upped with 1k.
//So use negative polarity signal.

//-----Serial connection format
//input is 8bit and if 8th digit 0=DA1 order, 1=DA2 order
//-> changed to use both 8th and 7th. 00=DA1, 10=DA2, 01 and 11=trigger cycle setting
//eg. B00111111 is DA1 with max (63) strength.
//B11000000 is chage cyclelength and triggerlength to 0.
//In this case, lower 3 bits set triggerlength, next 3 are for cycletength

//ver4 add toggle switch as force on switch. analog0 port sense it.
//ver3 modify for leonardo and 6bit 2ch
//ver2 try serial communication to set output value
//byte is unsigned. be carefull.


//serial sending
long beforetime=0;

//cycle stuff
unsigned long currentmicro=0;
unsigned long premicrotime=0;
unsigned long difftime=0;
unsigned long cyclelength=10000000;//microsec
//trigger exposure
unsigned long triggerlength=5000000;
boolean triggerflag=false;
boolean DACflag=false;
int triggerlengtharray[]={
  0,1,10,50,100,200,500,1000};//3bit 8 values msec.
int cyclelengtharray[]={
  0,50,100,200,500,1000,2000};//use 3bit

//voltage sensing
int sensorPin = A4;    //  the input pin for the potentiometer
//int sensorPin2 = A2;    //  the input pin for the potentiometer
int counter=0;
//int ledPin = 13;      //  the pin for the LED
int ordervalue=0; // 0-63
int oldordervalue=0;
int sensorValue = 0;  // variable to store the value coming from the sensor. signed 2 bytes

byte MASK=B00000001;

//sending data format
//1-4th byte; time (micros), 5-6th byte; analogread, 7th byte; escape flag, 8thbyte; period signal.
//byte sendingdata[8];
byte ESC=B00000000;
byte PERIOD=B11111111;
unsigned long time;//put micros(). 4 bytes

boolean forceswitch=false;

boolean testflag=true;
void setup()
{
  //sendingdata = new byte[8];
  //pinMode(13, OUTPUT);//portB 6th
  //pinMode(0, OUTPUT);//0 and 1 used for serial connection
  //pinMode(1, OUTPUT);
  pinMode(2, OUTPUT);//DA1 6th bit (most upper)
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(7, OUTPUT);

  pinMode(8, OUTPUT);//DA2 most upper
  pinMode(9, OUTPUT);
  pinMode(10, OUTPUT);
  pinMode(11, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(13, OUTPUT);//most lower bit, so even=off, odd=on
  //DDRD = DDRD | B11111100;
  //DDRB=B00100011;//need two more bit for 256
  //Serial.begin(19200);
  pinMode(14, INPUT);//set analog0 as digital input. for force switch
  pinMode(15, OUTPUT);//set analog1 as digital output. for trigger
  Serial.begin(9600);
}





void loop()
{
  currentmicro=micros();
  //serial order. read any time. use when trigger is off
  if(Serial.available()>0)
  {
    ordervalue = Serial.read();
    if((ordervalue&(MASK<<6)))
    {
      setCycleprop();
    }
  }

  //checking A1 pin
  /*
  if(currentmicro-premicrotime>10000000)
   {
   premicrotime=currentmicro;
   digitalWrite(A1, testflag);//trigger on  
   digitalWrite(13, testflag);
   testflag=!testflag;
   }*/

  difftime=currentmicro-premicrotime;
  //need overflow counter plan.
  if(premicrotime>currentmicro)
  {
    difftime=(4294967295-premicrotime)+currentmicro;
  }
 
  if(difftime >= cyclelength)
  {
    //checking code
    digitalWrite(13, testflag);
    testflag=!testflag;
    //-checking code
  }
  //if(difftime>cyclelength)
  //if(cyclelength<0)
  //{
  //checking code
  //digitalWrite(13, HIGH);
  //testflag=!testflag;
  //-checking code
  //} 
  //beginning of a period. trigger signal on.
  // if trigger length is 0, dont set trigger.
  if(triggerlength!=0 && !triggerflag && (difftime >= cyclelength))
  {

    DACflag=false;
    triggerflag=true;   
    premicrotime=currentmicro;
    if(forceswitch)
    {
      forceswitch=false;
      //anyway off the DA1 below, so comment out three lines
      //for(int i=0;i<6;i++)
      //{
      //digitalWrite(i+2, LOW);
      //}     
    }
    for(int i=0;i<6;i++)
    {
      digitalWrite(i+2, LOW);//DA1 set 0
      //digitalWrite(i+8, LOW);//DA2 not using now
    }
    //need wait? -> no. If too strong light hit CCD, it may not cancel?
    //if there is some decline device, like filter, the led not deteced at all. seems works
    //delayMicroseconds(1000);
    digitalWrite(A1, HIGH);//trigger on
    //delayMicroseconds(1000);
   
  }
  //during trigger on
  else if(triggerlength!=0 && triggerflag && (difftime < triggerlength))
  {
    //do nothing
  }
  //off edge of trigger
  else if(triggerlength!=0 && triggerflag && (difftime >= triggerlength))
  {
    triggerflag=false; 
    //delayMicroseconds(1000);
    digitalWrite(A1, LOW);//trigger off
    //delayMicroseconds(1000);
  }
  //rest part of period. 
  else
  {

    //toggle switch is off
    if(!digitalRead(A0))//digitalRead(14) doesn't work
    {
      //
      if(!DACflag || oldordervalue!=ordervalue)//first time of the period or order has changed.
      {
        oldordervalue=ordervalue;
        DACflag=true;
        if(forceswitch)
        {
          forceswitch=false;
          for(int i=0;i<6;i++)
          {
            digitalWrite(i+2, LOW);
          }     
        }
        //these two lines are for older arduino. leonardo has different port-pin assign.
        //using port resister allow fast regulation. about MHz?
        //PORTD = ordervalue<<2;//lower 2 bits used for serial port
        //PORTB = ordervalue>>6;//

        //digitalWrite function slower than port resister. 10kHz?
        //if((ordervalue>>7)&MASK==0)//DA1 0 this code doesn't work.
        if(!(ordervalue&(MASK<<6)))//this bit flag means setting of cycle property
        {
          //if(!(ordervalue&(MASK<<7)))
          if(!(ordervalue&(MASK<<7)))
          {
            //digitalWrite(2, );//most upper value of 6bit
            for(int i=0;i<6;i++)
            {
              digitalWrite(i+2, ordervalue&(MASK<<5-i));
            }
          }
          else//DA2
          {
            for(int i=0;i<6;i++)
            {
              //digitalWrite(i+8, ordervalue&(MASK<<5-i));
            }
          }
        }
      }
    }//if(!digitalRead(A0))
    else//toggle switch is on. so max strength
    {
      if(!forceswitch)
      {
        forceswitch=true;
        for(int i=0;i<6;i++)
        {
          digitalWrite(i+2, HIGH);
        }
      }
    }
  }




  //sending analog read every 10 msec
  //if(millis()-beforetime >= 10)
  if(millis()-beforetime >= 1000)
  {
    beforetime=millis();
    //time=millis();
    //Serial.print((int)byte(time>>8));
    //Serial.print(" ");
    //Serial.println(lowByte(time>>8));
    //int testval=1024;
    //Serial.println(testval);
    byte sendingdata[8];
    time=micros();
    //sensorValue is 10bit (-1024)
    //sensorValue = analogRead(sensorPin);
    //to check reciving, just send order value
    sensorValue = ordervalue;
    //upper bit
    sendingdata[0]=lowByte(time);
    sendingdata[1]=lowByte(time>>8);      
    sendingdata[2]=lowByte(time>>16);      
    sendingdata[3]=lowByte(time>>24);     
    sendingdata[4]=lowByte(sensorValue);     
    sendingdata[5]=lowByte(sensorValue>>8);
    sendingdata[6]=B00000000;//this line is needed. prep new valiable is not enough to clean?
    for(int i=0; i<6; i++)
    {
      if(sendingdata[i]==PERIOD)
      {
        sendingdata[i]=ESC;
        sendingdata[6]=sendingdata[6]|(B00000001<>3)&B00000111];
  cyclelength=cyclelength*1000;
}
