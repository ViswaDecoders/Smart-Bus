package contextawaresmartbus;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BusBody {
  static Vector<Integer> tempSenVal;


  static String strTime;
  static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
  static Date d;

  static Date date;
  static DateFormat dateFor = new SimpleDateFormat("yyyy-MM-dd");
  static String strDate;

//  static boolean fuelLvlSenStatus;
//  static int fuelLvlSenid;
//  static int fuelLevel;

//  static boolean batteryLvlSenStatus;
//  static int batteryLvlSenid;
//  static int batteryLevel;
//  static int batterLvlLeft;

  static int sysWt;
  static float wtThresh;

  static Vector<Integer> ProximitySendistance;
  static long ProximitySenobstacle;
  

  static Vector<Integer> vibSenVal;
  

//  static boolean GravitySenStatus;
//  static int GravityId;
//  static int GravityTilt;

//  static float accl;
//  static int Direction;
//  static int PresentSpeed;
//  static int AccelerometerId;
//  static boolean AccelerometerStatus;

  static boolean GPSStatus;
  static long x_coodinate;
  static long y_coodinate;
  static boolean gpsSig;
  static String location;

  static boolean RFIDstatus;

  static int RFIDcodeDetected;

  static int presentLocation;

  static int freqThesh = 1000;
  static int wtFreqThresh = 700;

  static int count;
  static Alarm alarm;
  static final int alarmId = 0;
  static int extinguisherState;
  static final int weightSenId = 2;
  static final int vibSenId=1;
  static final int personId=7;
  static final int camId=6;
  static final int locSenId=5;
  static final int proximitySenId=4;
  static final int tempSenId = 3;
  static final int RFIDid = 8;

  static NetworkAdaptor net;
  static Maintenance M;
  static int pCount;
  static Vector<Person> per;
  static Vector<Camera> cam;
  static Person person;

  static long extWeight;
  static boolean wtFaulty = false;

  public static void main(String[] args) {
      sysWt = 3000;
      pCount = 0;
      extinguisherState = 0;
      wtThresh=3000;


      person = new Person();
      net = new NetworkAdaptor();
      M = new Maintenance();
      alarm = new Alarm();
      tempSenVal = new Vector();
      for(int i = 0; i<5;i++ )tempSenVal.add(0);
      ProximitySendistance = new Vector();
      for(int i = 0; i<6;i++ )ProximitySendistance.add(0);
      vibSenVal = new Vector();
      for(int i = 0; i<6;i++ )vibSenVal.add(0);
      per = new Vector();
      //JSON parser object to parse read file
      JSONParser jsonParser = new JSONParser();
      cam = new Vector();
      cam.add(new Camera("front"));
      cam.add(new Camera("back"));

      System.out.println("\t\t**********************************");
      System.out.println("\t\tContext Sensing Smart Bus System");
      System.out.println("\t\t**********************************");

      try (FileReader reader = new FileReader("data/Bcontexts.json"))
      {
          //Read JSON file
          Object obj = jsonParser.parse(reader);

          JSONArray list = (JSONArray) obj;
          //System.out.println(list);

          list.forEach( i -> executeData((JSONObject)i));

      } catch (FileNotFoundException e) {
//          e.printStackTrace();
          System.out.println("Could not find the file");
      } catch (IOException e) {
//          e.printStackTrace();
          System.out.println("Could not find the file");
      } catch (ParseException e) {
//          e.printStackTrace();
          System.out.println("Could not Parse the file");
      }

  }

  private static String getLocation(){
      return location;
  }

  private static void executeData(JSONObject s){
      System.out.println();
      System.out.println();

      System.out.println("_____________________");
      System.out.println("\n\tContext "+(++count));
      System.out.println("_____________________");
      JSONArray contextList = (JSONArray)s.get("context");
      //System.out.println(contextList);

      date = Calendar.getInstance().getTime();
      strDate = dateFor.format(date);

      contextList.forEach(i -> busSensing((JSONObject) i));

      try {
          TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
          e.printStackTrace();
      }
      if( !gpsSig ){
          System.out.println("No GPS Signal, depending in Station RFID");
      }
  }

  private static void busSensing(JSONObject context) {
      System.out.println();
      long ID;
      int device, ind;
      try {
          ID = (long) context.get("id");
          strTime = (String) context.get("t");

          strTime = strDate + " " + strTime;
          //System.out.println(ID);
          device = (int) ID / 1000;
          ind = (int) ID % 1000;
          d = dateFormat.parse(strTime);
      }
      catch (Exception e){
          System.out.println("\nBasic Configuration Missing... <format not supported>");
          return;
      }


      //System.out.println(device);

      switch (device) {

          case alarmId:
              System.out.println("\nAlarm Pressed");
              alarm.toggle();
              break;
          case vibSenId: // for collision detection

              System.out.println("\nThis is from Vibration Sensor");
              long freq = (long) context.get("Freq");
              if( freq > freqThesh) {
                  System.out.println("Ind : " + ind);
                  System.out.println("Abnormal Vibrations detected");
                  vibSenVal.set(ind, (int) freq);
                  if (proxyDetected()) {
                      alarm.raise(1);
                      System.out.println("Calling Maintenance Team");
                      net.send("Accident detected <at time " + d + ">, Calling Helpline...");
                      M.call("Accident <at time " + d + ">");
                  }
              }
              else if(wtFaulty == true){
                  if(freq > wtFreqThresh){
                      System.out.println("Suspecting Heavy wt (faulty wt sensor), better Decrease the wt in bus");

                  }
              }
              break;
          case weightSenId:
              try {
                  System.out.println("\nThis is from weight Sensor");
                  extWeight = (long) context.get("Wt") - sysWt;
                  System.out.println("External Weight : " + extWeight);

                  if (extWeight < 0) {
                      wtFaulty = true;
                      System.out.println("Uncertainty Detected!!! May be Faulty Weight Sensor");
                      net.send("Abnormal Behavior by Weight Sensor, Weight : " + extWeight + " <at time "+d+">");
                      M.call("Abnormal Behavior by Weight Sensor, Weight :  " + extWeight + " <at time "+d+">");
                  }
                  else if (extWeight < wtThresh) {
                      System.out.println("Safe Weight...");
                      if (alarm.status == 1) {
                          alarm.down();
                      }
                  }
                  else if(extWeight > wtThresh){
                      System.out.println("Too Heavy, Raising alarms");
                      alarm.raise(0);
                  }
              }
              catch (Exception e) {
                  wtFaulty = true;
                  System.out.println("Unexpected Behavior from Weight system... Reporting to Maintenance");
                  net.send("Abnormal Behavior by Weight System" + " <at time "+d+">");
                  M.call("Abnormal Behavior by Weight System" + " <at time "+d+">");
              }
              break;
          case tempSenId:
              try {
                  System.out.println("\nThis is from Temp Sensor");
                  long temp = (long) context.get("Temp");
                  tempSenVal.set(ind, (int) temp);
                  if (temp > 75) {
                      if (ind == 0 && temp > 200){
                          System.out.println("High temperature Detected at Engine, warning Driver");
                      }
                      else if(ind!=0){
                          System.out.println("High temperature Detected inside the cabin");
                          System.out.println("Activating extinguishers");
                          alarm.raise(2);
                          extinguisherState = 1;
                      }
                  }
                  else if(temp < 5){ // this can change based on the region
                      System.out.println("Suspecting Temperature sensor of index : "+ind + " <at time "+d+">");
                      net.send("Suspicious Behavior by Temperature sensor" + " <at time "+d+">");
                      M.call("Suspicious Behavior by Temperature sensor" + " <at time "+d+">");
                  }
                  else {
                      System.out.println("Temperature is Normal");
                  }
              } catch (Exception e) {
                  System.out.println("Abnormal Behavior from Temperature System");
                  net.send("Abnormal Behavior by Temperature System" + " <at time "+d+">");
                  M.call("Abnormal Behavior by Temperature System" + " <at time "+d+">");
              }
              break;
          case proximitySenId:
              try{
                  long distance = (long) context.get("Dist");
                  if(ProximitySendistance.get(ind) == distance){
                      pCount ++;
                  }
                  else{
                      pCount = 0;
                      ProximitySendistance.set(ind, (int)distance);
                  }
                  if(distance <5){
                      System.out.println("Detected Obstacle at sensor "+ ind);
                      ProximitySenobstacle =  ind;
                  }
                  if(pCount > 3){
                      alarm.raise(3);
                      System.out.print("Suspecting Faulty Proximity Sensor");
                      net.send("Abnormal Behavior by Proximity System <at time "+d+">");
                      M.call("Abnormal Behavior by Proximity System <at time "+d+">");
                  }
              }
              catch (Exception e) {
                  System.out.println("Abnormal Behavior from Proximity System");
                  net.send("Abnormal Behavior by Proximity System" + " <at time "+d+">");
                  M.call("Abnormal Behavior by Proximity System" + " <at time "+d+">");
                  e.printStackTrace();
              }
              break;

          case locSenId:
              gpsSig = true;
              presentLocation = ind;
              System.out.println("Arrived at location "+ ind);
              for(Person i : per){
                  if(i.location == presentLocation){
                      System.out.println("Waiting for "+i.name+" ...");
                  }
              }
              x_coodinate = (long) context.get("x");
              y_coodinate = (long) context.get("y");
              if(x_coodinate > 0 && x_coodinate <500 && y_coodinate>0 && y_coodinate <900)
                  System.out.println("hyderabad");
              else if(x_coodinate > 500 && x_coodinate <700 && y_coodinate >0 && y_coodinate <700)
                  System.out.println("karimnagar");
              else if(x_coodinate > 500 && x_coodinate <700 && y_coodinate >900 && y_coodinate <1200)
                  System.out.println("Trimulgary");
              else if(x_coodinate > 350 && x_coodinate <500 && y_coodinate >900 && y_coodinate <1200)
                  System.out.println("Begumpet");
              else if(x_coodinate > 0 && x_coodinate <500 && y_coodinate >900 && y_coodinate <1200)
                  System.out.println("kukutpally");
              else
                  System.out.println("Unknown Location");
              break;
          case camId:
              try {
                  Camera c = cam.get(ind);
                  System.out.println("\nThis is from Internal Camera");
                  String info = (String) context.get("info");
                  //System.out.println(info);
                  if (info.equals("person")) {
                      // if the person is unknown, it will ask the details and save them
                      System.out.println("Detected person " + context.get("person") + " at location " + presentLocation + " <at time "+d +">");
                  }
                  else if (info.equals("alert")) {
                      System.out.println("Unfavourable Situation detected, Raising alarms" + " <at time "+d+">");
                      c.resItems = (String) context.get("ResItem");
                      net.send(c.resItems + " Detected by camera " + ind + " <at time "+d+">");
                      alarm.raise(1);
                  }
              } catch (Exception e) {
                  System.out.println("Unexpected Behavior From Camera... Reporting to Maintenance");
                  net.send("Abnormal Behavior by Camera System" + " <at time "+d+">");
                  M.call("Abnormal Behavior by Camera System" + " <at time "+d+">");
              }
              break;
          case personId:
              try {
                  Person p = new Person();
                  p.name = (String) context.get("name");
                  if(p.name == null){
                      System.out.println("\nInvalid Information...");
                      return;
                  }
                  System.out.println("\nThis is a " + p.name + "'s profile update");

                  p.location = (long) context.get("loc");
                  p.phoneNo = (long) context.get("Ph No");
                  p.seatNo = (long) context.get("Seat No");
                  p.amountPaid = (long) context.get("Amt");
                  p.ConfigProf = (JSONObject) context.get("config");
                  per.add(p);

              } catch (Exception e) {
                  System.out.println("\nUnexpected Data arrived From Person Profile Update <at time "+d+">");

              }
              break;
          case RFIDid:
              presentLocation = ind;
              System.out.println("Arrived at location "+ ind);
              for(Person i : per){
                  if(i.location == presentLocation){
                      System.out.println("Waiting for "+i.name+" ...");
                  }
              }
          default:
              System.out.println("\nUnable to Recognize the device..." + " <at time "+d+">");
      }
  }

  static boolean proxyDetected(){
      for(int i : ProximitySendistance){
          if(i < 5){
              return true;
          }
      }
      return false;
  }
}

class Person{
  String name;
  long location;
  long phoneNo;
  long seatNo;
  long amountPaid;
  JSONObject ConfigProf;
}

class Camera{
  int state;
  String resItems;
  static int cloudID;
  String loc;

  Camera(String flo){
      loc = flo;
      state = 1;
  }
}


class Alarm{
  int status;
  int emerCode; // 0 - heavy Weight, 1 - unfavourable situation from camera

  public void raise(int i){
      if(status != 1) {
          status = 1;
          emerCode = i;
          System.out.println("!!! Alarms Raised !!!");
      }
  }

  public void down(){
      status = 0;
      System.out.println("...Alarms Down...");
  }

  public void toggle(){
      if(status == 0){
          System.out.println("!!! Alarms Raised !!!");
          status = 1;
          return;
      }
      status = 0;
      System.out.println("...Alarms Down...");
  }
}

class NetworkAdaptor{
  Vector<Integer> portNo;
  String info;

  public void send(String s){
      info = s;
      System.out.println("Transmitting info :"+ s);
  }

}


class Maintenance{
  String Prob;
  int status = 0;

  void call(String s){
      System.out.println("Received Maintenance call for "+s);
      Prob = s;
      status = 1;
  }
}
