package edu.mah.www;

import g4p_controls.*;
import geomerative.*;
import papaya.Mat;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.*;
import processing.serial.Serial;
import sojamo.drop.DropEvent;
import sojamo.drop.DropListener;
import sojamo.drop.SDrop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DIWire extends PApplet {

/* DIWire Bender
 * 3D Wire Bender by Pensa - www.PensaNYC.com
 * Written by Marco Perry, Chad Ingerick and Clay Budin
 * Email DIWire@PensaNYC.com for questions
 * Drives on 2 Stepper Motors to bender wire in 2D space
 *
 *    The hardware portion is licenced under the Creative Commons-Attributions-Share Alike License 3.0
 *    The CC BY SA licence can be seen here: http://creativecommons.org/licenses/by-sa/3.0/us/
 *
 *    DIWIre is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.    See the
 *    GNU General Public License and CC-BY-SA for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with DIWire.    If not, see <http://www.gnu.org/licenses/>.
 *    and http://creativecommons.org/licenses/by-sa/3.0/us/legalcode
 *
 *    No portion of this header can be removed from the code.
 *    Now enjoy and start making something!
 *
 *
 The following program uploads a svg or dxf file, draws the 2D shape, allows user to set the print resolution,
   calculates the feed and bend angles to print the 2D shape, and sends these values to the DIWire
*/

    String softwareVersion = "1.1.5";

    String errorFilename = "errors.txt";
    String defaultsFilename = "data/defaults.txt";

//////////////////////// Imports /////////////////////////////




























//////////// G4P GUI //////////////////////////////////////////


    // lists of all G4P GUI elements for bulk show/hide
    ArrayList<GAbstractControl> gAll = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gLoad = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gEdit = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gCalibrate = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gManual = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gManualInputs = new ArrayList<GAbstractControl>();
    ArrayList<GAbstractControl> gTextFields = new ArrayList<GAbstractControl>();

    ArrayList<GImageToggleButton> gMouseSelect = new ArrayList<GImageToggleButton>();

    Menu mbPoints;

    // Load mode
    GImageButton btnOpenFile;
    PImage dropArea, dropHover;

    // Edit mode
    GDropList dlMatProfEdit;
    GOption optMM, optIn;
    GToggleGroup togUnits,togInputs;
    GTextField txScale, txResolution, txWireLength, txRepeat, txKfactor;
    GCustomSlider sldScale;
    GCustomSlider sldResolution;
    GDropList dlSampling;
    GCheckbox cbPreview;
    GCustomSlider sldGapThresh;
    GImageButton btnCloseGaps;
    GImageButton btnBeginSend;
    GImageButton btnStopSend, btnPauseSend, btnResumeSend;
    GImageToggleButton itbArrow, itbZoomIn, itbHand;
    GImageButton btnCancelSendError, btnMidBendError, btnIgnoreError, btnSerialError, btnBackOffLimitError,itbZoomRect, itbZoomOut;


    // Calibrate mode
    GDropList dlMatProfCalib;
    GImageButton btnCalibCreate, btnCalibEdit, btnCalibDelete;
    GTextField txCalibName;
    GTextField txMaterialName;
    GDropList dlBendHead, dlFeedWheel;
    Map<String, Float> mapBH2Dia = new HashMap<String, Float>();        // convert menu item to wire diameter value
    Map<String, Float[]> mapBH2P2W = new HashMap<String, Float[]>();    // convert menu item to P2W
    Map<String, Float[]> mapBH2W2P = new HashMap<String, Float[]>();    // convert menu item to W2P
    Map<String, Float[]> mapBHcornerComp = new HashMap<String, Float[]>();  // convert menu item to wire diameter, mandrel Radius, mandrel offset ( using float array in createBendAndFeedDropdowns() )

    GTextField txTypeIn;
    GOption optCalibHigh, optCalibStandard, optCalibQuick,optNoInput,optYesInput;
    GToggleGroup togCalibAccuracy;
    GImageButton btnStartCalib;
    GImageButton btnOkCalib, btnBeginCalib, btnCancelCalib;
    GImageButton btnRefineCalib, btnDoneCalib;
    GImageButton btnReCalib, btnRefineRangeCalib;
    GTextField txLow, txHi, txSteps;

    // Manual mode
    GImageButton btnArrowUp, btnArrowDown, btnArrowLeft, btnArrowRight, btnHome, btnZeroRight, btnZeroLeft;
    GOption optVeryFast, optFast, optSlow, optVerySlow;
    GToggleGroup togJogSpeed, togInput;
    // testing/debugging
    GButton btnTest;
    GTextField txTest,txPosFeed,txNegFeed,txPosBend,txNegBend;
    boolean manInputs=false;


///////////////////// SDrop /////////////////////////////////////
// used for drag-n-drop of files onto app

    SDrop drop;
    DIWDropListener dropListener;


    // limits on GUI sliders - min, max, start
    float[] GUIScaleLimits = { .01f, 100, 1.0f };
    float[] GUIResolutionLimits = { 1, 100, 17 }; //{1, 50, 12}
    float[] GUIkFactorLimits = { -10, 10, 0.5f};
    float[] GUIGapLimits = { 1.0f, 100.0f, 1.0f };

    // GUI params
    float GUIScale = GUIScaleLimits[2];           // scale value from slider
    float GUIRes = GUIResolutionLimits[2];        // resolution value from slider
    boolean GUIDrawSVG = true;    // SVG preview flag
    boolean GUIDrawSegments = true;
    boolean GUIDrawPoints = true;

    PImage logo, icon;

    //Cursors
    PImage zoomCursor;





    // the view mode MouseCursor.pngissplayed in the main window and settings panel
    final int VIEW_NONE = 0;
    final int VIEW_LOAD = 1;
    final int VIEW_EDIT = 2;
    final int VIEW_CALIBRATE = 3;
    final int VIEW_MANUAL = 4;
    int curViewMode = VIEW_NONE;



    // Mouse pointer types
    final int MOUSE_POINTER = 0;
    final int MOUSE_HAND = 1;
    final int MOUSE_ZIN = 2;
    final int MOUSE_ZOUT = 3;
    int mouseMode = MOUSE_POINTER;
    int[] selectRect={-1,-1,-1,-1};
    boolean ctrlFlag=false;
    boolean shiftFlag=false;
    int pointClicked;
    float[][] dragOffsets= new float [0][2];


    /////////////////// Geomerative SVG Shapes ///////////////////////
    String curFile = "";    // file being viewed. changes with selectfile
    boolean isDXFFile = false;
    RShape curShape,saveShp;
    ArrayList<RPath> curPaths = new ArrayList<RPath>();
    int nPaths = 0, curPath = -1;
    int[] curPoint =  new int[0];    // make a list for multiple selection, or use selection flag below


    // these can be or'ed together
// for some reason, Processing only supports enums if they are in a separate file with a .java extension
//  http://stackoverflow.com/questions/13370090/enums-in-processing-2-0
    final int POINT_NONE = 0;
    final int POINT_FEED_ERROR = 1;
    final int POINT_ANGLE_ERROR = 2;
    final int POINT_ERROR = POINT_FEED_ERROR | POINT_ANGLE_ERROR;
    final int POINT_SELECTED = 4;
    final int POINT_DELETED = 8;

    class WirePoint {
        RPoint pt;
        int stat;
        boolean pointPause;

        WirePoint ()  { stat = POINT_NONE; pointPause = false;}
        WirePoint (RPoint _pt)  { pt = _pt; stat = POINT_NONE; pointPause = false;}
        WirePoint (RPoint _pt, int _stat)  { pt = _pt; stat = _stat; pointPause = false;}
        public String toString ()  { return ("[(" + pt.x + "," + pt.y + ") stat = " + stat + "]"); }
    }

    class WirePoints  {
        ArrayList<WirePoint> pts;
        FloatList angles, feeds, diameters;
        int nAngleWarnings, nFeedWarnings;
        boolean modified;
        boolean bendDir;
        boolean curveIsClosed;
        float continuousBendDist;
        int startPoint;

        WirePoints ()  {
            pts = new ArrayList<WirePoint>();
            angles = new FloatList();
            feeds = new FloatList();
            diameters = new FloatList();
            nAngleWarnings = nFeedWarnings = 0;
            modified = false;
            bendDir=false;
            curveIsClosed=false;
            startPoint=0;
        }
    }

    ArrayList<WirePoints> curWirePoints = new ArrayList<WirePoints>();

    // camera/view params
    float camScl = 1.0f, camX = 0.0f, camY = 0.0f;
    PGraphics previewGraphics;        // draw buffer for preview area - allows for clipping

    // previously computed wire length
    float prevPcl = -1.0f;


    // keeps track of app state
    final int DS_NOFILE = 1;
    final int DS_LOADED = 2;
    final int DS_CONNECTING = 3;
    final int DS_SENDING = 4;
    final int DS_DRAWING = 5;
    final int DS_WARNING = 6;
    final int DS_HOMING = 7;
    int displayState = DS_NOFILE;
    int prevDisplayState=0;


    /////////////////////// COLORS ////////////////////////////
    final int settingsPanelColor =      color(232, 232, 232);
    final int backgroundColor =         color(241, 242, 242);
    final int headerLineColor =         color(65, 64, 66, 255);
    final int previewBackgroundColor =  color(255, 255, 255);
    final int gridColor = 224;
    final int headerLineWeight = 1;

    final int pathNormal = color(0,0,0);
    final int pathHighlight = color(160,160,160); //color(255,0,0);

    // how to draw points in preview area
    final int pointNormal = color(0,0,0);
    final int pointStart =  color(0,255,0);
    final int pointError =  color(255,0,0);
    final int pointPause =  color(0,0,255);
    final int pointSent =   color(0,0,200); //color(0,0,200);
    final int pointSelected = color(255,255,0);

    // how to draw segments in preview area
    final int segmentNormal = color(0,200,0,50);
    final int segmentError = color(200,0,0,50);
    final int segmentSending = color(0,0,200,100);
    final int segmentSent = color(0,0,0,100);


    /////////////////////// SIZES /////////////////////////////
    final int[] minPanelSize =    { 600, 600 };    // const
    final int[] previewPaddingX = { 60, 30 };      // const
    final int[] previewPaddingY = { 35, 100 };     // const
    int[] mainPanelSize =   { 700, 700 };    // changed in handleResize()
    int[] previewSize =     { 400, 400 };    // changed in handleResize()
    int[] previewCenter =   { 0, 0 };        // changed in handleResize() - not really used
    final int headerSize = 0; //40;    // no more logo at top
    final int footerSize = 40;
    final int settingsPanelWidth = 200;


    ////////// Footer Status //////////////////////////////////
    final int STATUS_STARTING = 1;
    final int STATUS_OK = 2;
    final int STATUS_HAS_ERRORS = 3;
    final int STATUS_SENDING = 4;
    final int STATUS_CONNECTING = 5;
    final int STATUS_HOMING = 6;
    final int STATUS_CONNECTED = 7;
    final int STATUS_NOT_CONNECTED = 8;
    int status = STATUS_STARTING;


    // various flags
    boolean windowWasResized = false;
    boolean showSettingsPanel = false;
    boolean displayNeedsUpdate = true;               // signals the need for a redraw - not used?
    boolean sendPaused = false;


    /////////////////// UNITS ////////////////////////////////
    final int UNITS_MM = 1;
    final int UNITS_INCH = 2;
    int curUnits = UNITS_MM;

    final float conversionFactor = 0.039370f;    // inches to mm // Todo Double check
    final int gridSize = 50;    // mm


    // warning states
    final int WARNING_NONE = 0;
    final int WARNING_FEED = 1;
    final int WARNING_CONNECTION = 2;
    final int WARNING_LIMIT_SWITCH = 3;
    final int WARNING_MID_BEND = 4;
    final int WARNING_CONT_HOME = 5;
    int popUpWarning = WARNING_NONE;


    // methods of sampling the curve
    final int SPLIT_ADAPTIVE = 3;
    final int SPLIT_CURVE = 2;
    final int SPLIT_PATHS = 1;
    int splitMode = SPLIT_PATHS;



    //////////////////// Mouse ///////////////////////////////
    boolean mouseDragged = false;
    int clickedPoint = -1;
    int[] mouseClickStart = { 0, 0 };


    // file directory for important persistent data files
    String rootDataFolder;


    ///////////////////// Serial Declarations //////////////
    Serial TinyG;

    boolean serialConnected = false;
    boolean serialFlashed = false;
    boolean tinyGCommandReady = false;
    boolean tinyGFlashReady = true;

    String curSerialNumber;

    int lastFeedSentIdx = 0;
    int lastAngleSentIdx = 0;

    boolean tinyGHomed = false;
    boolean homeResponse = false;

    int limitTripped;


    final float NO_POSITION = -9999999;

    //final float xrev = 14.767;             // mm per rev
    final float xrev = (14.767f / .7f)/15;             // mm per rev // Todo Double check
//final float xrev = 6.666666;             // mm per rev

    final float XFeedRate = 320*xrev;
    final float XG0Rate = 320*xrev;
    final float XMaxRate = 426*xrev;
    final float XSearchVelocity = 360*xrev;
    final float XLatchOffVelocity = 250*xrev;
    final float XLatchBackoff = 1.4f*xrev;
    final float XAcceleration = 240000000*xrev;
    final float XTravelMax = 106*xrev;
    final float XHomingJerk = 240000000*xrev;
    final float microSteps = 8; // microstepping for feed // Todo Microstepping Settings

    //final float yrev = 119.7 / 1.125;      // scale on feed
//final float yrev = (119.7 / 1.125)/0.92363636363;   //added correction factor for 1/8" feed wheels
    final float yrev = 360.00f*( 16.00f / 20.00f); // Todo Double check
    final float YG0Rate = 40;
    final float YAcceleration = 8000000;

    float fwDiam=48; // defaults to 1/8" value.  Updates later to actual value  // Todo Double check


    //FEED COMPENSATION FOR CORNER CREEP TEST PARAMETERS// CBarbre 10/15/2015
//default constants are for 1/8" wire.  updated by selection of new material profile
    float kFactor = 0.5f; // factor to determine location of neutral axis in bending
    float wireDiam = 3.2258f; //mm (.127")   1.5875; //mm (0.0625in) // Todo Revise for 3mm material
    float nAxisRad = kFactor*wireDiam; // radius to the neutral axis of the wire
    float mandrelRad = 3.1751f; //mm (.125")  1.5875; //(0.0625" in bend head mandrel radius) // Todo Double check
    float feedAdjustStartEnd = 6; //11.151741; //mm. defaults to 1/8" offset.  Updates when a material profile is chosen


    // this is used to flash the DIWire to establish the x == 0 position in the middle, not at the far CCW end
    final float homeOffsetPos = 21.33f*xrev; //31.5; //30.7; // Todo Double check

// Calibration

    // cubic coeffs for converting angle in degrees to mm: 1,x,x^2,x^3
// default is taken from an existing calibration of a 1/16" wire/bend head - already has spring-back baked in
//   and has homeOffset
// poly formulas will be computed from calibration data if there are enough points
//final float[] defaultFormulaPos = { 32.007904, 0.106508255, -3.3214688E-4, 5.4453267E-6 };
//final float[] defaultFormulaNeg = { 20.098206, -0.113582134, 5.997345E-4, -6.7329092E-6 };
//
// replaced with data from recent calibration: GalvinizedSteel_1_16in.json
// Todo replace coefficients with Excel cubic formula from ANSYS Simulation

    final float[] defaultFormulaPos = { 1.0757598f, 0.1144433021f, -4.9139559e-4f, 5.95545861e-6f };
    final float[] defaultFormulaNeg = { -10.0765075f, 0.108063697f, 3.34933400e-4f, 5.083100404e-6f };
    float[] polyFormulaPos = defaultFormulaPos;
    float[] polyFormulaNeg = defaultFormulaNeg;

// Todo find 5th order formula , otherwise createe 5th ordecoefficients in excel or Python.

    final float[] polyFormulaPosArcDefault = { -0.0000000004521928119f, 0.0000002660923664f, -0.00006072026499f, 0.006829055185f,-0.3962220234f,12.07506791f };
    final float[] polyFormulaNegArcDefault = { 0.0000000004521928119f, -0.0000002660923664f, 0.00006072026499f, -0.006829055185f, 0.3962220234f,-17.98606791f};

    final float[] polyFormulaPosArc = { -0.0000000004521928119f, 0.0000002660923664f, -0.00006072026499f, 0.006829055185f,-0.3962220234f,12.07506791f };
    final float[] polyFormulaNegArc = { 0.0000000004521928119f, -0.0000002660923664f, 0.00006072026499f, -0.006829055185f, 0.3962220234f,-17.98606791f};


    // x positions that are just outside contact with the wire.  These are used to
//  position the bend pins out of the way after a bend so they won't collide with
//  the wire during feeding
// these were figured for 1/16" wire - need to check there's no contact with a wider wire
// now getting contact during calibration so these values are more arbitrary
// need to ensure that opposite pin is ducked below level of plate for these values
//final float xHomePos = 0.711044897*xrev; //1.75;  // 0.0;
    // Todo Remeasure based on 3mm material

    final float xHomePos = 0.0711044897f*xrev; //1.75;  // 0.0;
    final float xHomeNeg = -4.977314282f*xrev;  // -8.9;

// x positions at which bend pins are just touching wire - measured from 1/16" wire
// for 1/8" wire (.120 actually) xContactPos = .1
// these are set during calibration
//float xContactPos = 0.6;
//float xContactNeg = -9.5;

    // coefficients of 5th-order poly fit to convert pin angle to wire angle
// taken from Excel spreadsheet "dwire 3 calibration shiz.xlsx", Sheet 2 top formula, on the Desktop of Pensa2
// 1/16" wire
// as x^5, x^4, x^3, x^2, x, 1
// seem to only have positive side values here - for negative, need to reverse and take into account offset to pin touching wire
// maps 0 pin angle to almost 0 wire angle - in reality pins need to move from home positions before they touch/bend wire
// no spring-back here - assumes pin is in contact with wire
//
// 1/8" versions generated by Chad via simulation
// and by me empirically - note: have to take it out to at least 12 decimal places
    // Todo find 5th order formula , otherwise create 5th order coefficients in excel or Python.

    Float[] pinAng2wireAng = { -0.000000016280f, 0.000005376702f, -0.000617376437f, 0.021473090347f, 1.624960296666f, -0.031455220600f };
//final float[] pinAng2wireAng16 = { -0.000000016280, 0.000005376702, -0.000617376437, 0.021473090347, 1.624960296666, -0.031455220600 };
//final float[] pinAng2wireAng8 =  { 0.000000003553247368, -0.000001480041388724, 0.000121318598981901, -0.000437673461306076, 1.377890550116720000, -0.046032608208406600 };  // Paper Wire??
//final float[] pinAng2wireAng8 = { -0.000107550128, 0.006248079473, -0.143874995292, 1.529105530593, 0.756033437574, -0.024378545138 };  //old 1/8??
//final float[] pinAng2wireAng8 = { 0.00000000430650127, -0.000001247412214633, 0.00006165063942376, 0.00397768433947476, 1.18771010298224, -0.182295935414004 };  //new 1/8



    // same spreadsheet, reversed columns to get inverse function
    Float[] wireAng2pinAng = { -0.000000001242f, 0.000000394249f, -0.000017065958f, -0.000499238393f, 0.563986323202f, 0.142395303355f };
//final float[] wireAng2pinAng16 = { -0.000000001242, 0.000000394249, -0.000017065958, -0.000499238393, 0.563986323202, 0.142395303355 };
//final float[] wireAng2pinAng8 = { 0.000000000334144, -0.000000010850633, -0.000000711122896, -0.000958256372314, 0.738394279515433, 0.014700754847581  };  // Paper Wire??
//final float[] wireAng2pinAng8 = { 0.000000002032, -0.000000725225, 0.000099309430, -0.006225598968, 0.294752358225, 0.015136546090 };  //old 1/8??
//final float[] wireAng2pinAng8 = { 0.000000000490478522, -0.000000122514952093, 0.000024646024650465, -0.00329379565437193, 0.848710463062162, 0.146567496811258 };  //new 1/8



    // converts pin x-offset to pin angle
// computed as: 10800 gear ratio from motor to pin seat / (14.767 mm/rev * 360 deg/rev)
// units don't seem to work out here (???????)
//final float pinMM2pinAng = 2.0315568497325116814518859619422;
//
// the above value is very off from my (CB) empirical measurements of 1/16" wire
// the value below produces much closer results in pinMM2WireAng()
// slightly less accurate on negative side, but still pretty close
//final float pinMM2pinAng16 = 5.1;
//final float pinMM2pinAng8 = 1.0;
//final float pinMM2pinAng8 = 4.5;

    final float pinMM2pinAng = 6.6666666f/xrev; // Todo Double check


    // index into dlMatProfEdit of currently active calibration (Material Profile)
    String curInstalledCalibFileName;
    String curInstalledCalibPathName;

    String curWorkingCalibFileName;
    String curWorkingCalibPathName;
    int nCalibrations = 0;

    final float NO_CALIBRATION = -999999;

    // Calibration points.  Values are in degrees (angles)
//
// switch to specifying input and output in degrees of bend - more user friendly
// include contact value first
    final float[] defaultPosCalPts = { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130 }; //{ 0, 5, 10, 15, 20, 30, 45, 60, 75, 90, 110, 130 };
    final float[] defaultNegCalPts = { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130 }; //{ 0, 5, 10, 15, 20, 30, 45, 60, 75, 90, 110, 130 };

// calibration pts for paper-covered steel - much springier
//final float[] defaultPosCalPts = { 0, 15, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130 };
//final float[] defaultNegCalPts = { 0, 15, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130 };

    float[] posCalPts;
    float[] negCalPts;

    // calibration results - these are bend positions in mm after spring-back
// now these are bend angle in degrees
    float[] posCalRes;
    float[] negCalRes;

    // saves last position of DIWire heads
    float lastPositionX = NO_POSITION;
    float lastPositionY = NO_POSITION;

    float calibXContactPos;
    float calibXContactNeg;

    float calibFeedAmt = 50.0f;

    final int CALIB_SIZE_NONE = 0;
    final int CALIB_SIZE_1_16 = 1;
    final int CALIB_SIZE_1_8 = 2;
    int calibSize = CALIB_SIZE_NONE;    // 1/16" or 1/8" currently


    // where we are in calibration process
    final int CALIB_NONE = 0;
    final int CALIB_START = 1;
    final int CALIB_NEW = 2;
    final int CALIB_EDIT = 3;
    final int CALIB_REFINE = 4;
    final int CALIB_RUNNING = 5;
    int curCalibMode = CALIB_NONE;


    // calibration direction
    final int CALIB_DIR_NONE = 0;
    final int CALIB_DIR_CW = 1;
    final int CALIB_DIR_CCW = 2;
    int curCalibDir = CALIB_DIR_NONE;

    // index of point we are currently calibrating
    int curCalibIdx = -1;

    // calibration state
    final int CALIB_STATE_NONE = 0;
    final int CALIB_STATE_STARTING = 1;
    final int CALIB_STATE_FEEDING = 2;
    final int CALIB_STATE_READYTOBEND = 3;
    final int CALIB_STATE_BENDING = 4;
    final int CALIB_STATE_WAITING = 5;        // waiting for user to adjust bend pin and click OK
    final int CALIB_STATE_SKIP_POINT = 6;
    final int CALIB_STATE_FINISHED = 7;
    int curCalibState = CALIB_STATE_NONE;

    // for type-in of values
    int curCalibTypeInDir = CALIB_DIR_NONE;
    int curCalibTypeInIdx = -1;



    // jogging
    final int JOGDIR_NONE = 0;
    final int JOGDIR_FORWARD = 1;
    final int JOGDIR_BACKWARD = 2;
    final int JOGDIR_CW = 3;
    final int JOGDIR_CCW = 4;
    int curJogDir = JOGDIR_NONE;

    final int JOGSPEED_NONE = 0;
    final int JOGSPEED_VERY_SLOW = 1;
    final int JOGSPEED_SLOW = 2;
    final int JOGSPEED_FAST = 3;
    final int JOGSPEED_VERY_FAST = 4;
    int curJogSpeed = JOGSPEED_FAST;

    boolean isJogging = false;



    // cycle testing - repetitive stress testing of DIWire unit
    boolean doCycleTest = false;
    boolean cycleDir = false;
    int cycleAng = 0;
    int cycleCount = 0;

    Map<String, Float> feedDiam = new HashMap<String, Float>();
    String[] gcodeStrings;
    boolean txtFileLoaded=false;
    int lastTextLineSent=0;

    int repeatQty=1;
    int repeatCurrent=1;

    boolean continuousFlag=false;

    boolean zeroRequest=false;

    float zeroRight=-5;


    public void setup ()  {
        // set up data files
        debugC("DIWire/setup()");

        setupDataFolder();
        //setupMaterials();
        //setupSerialNumbers();
        println("");

        // set up window
        setupMainWindow();                   // sets window initial size and min size (Setup Tab)
        setupLogo();                         // loads the logo image and resizes it (Setup Tab)
        initializeDrop();                    // sets up the file drop area (drop Tab)
        initializeGUI();                     // sets up control buttons, sliders, and drop menus (GUI Tab)
        initializeGeom();                    // initialize Geomerative library

        zoomCursor = loadImage("images/Cursor_zoom_in.png");
        thread("async");

        loadDefaults();
    }



    public void draw ()  {
//  debugC("DIWire/draw()");

        // this is a test of the hardware in which it is repeatedly sent bend and feed commands
        if (doCycleTest)  {
            cycleTest();                     // functions tab
        }

        // see if we need to switch view modes - hide and show GUI elements
        updateViewMode();
        updateDisplayState();

        if (windowWasResized)  {
            handleResize();                  // re-sets positions once the window is resized (draw Tab)
        }

        drawClearAll();                      // clears the background (draw tab)
        drawWindowFrame();                   // draws the background, lines, settings panel and status bar (draw tab)

        if (curViewMode == VIEW_LOAD)  {
            drawLoadScreen();                // draws the graphics for the load screen (draw Tab)
        } else if (curViewMode == VIEW_EDIT)  {
            preparePreviewArea();            // draws the background and grid of the preview area (draw Tab)

            // optionally draw the preview SVG shape
            if (GUIDrawSVG)                  // GUIDrawSVG is a bool controlled by the "Preview" checkbox
                drawSVG();                  // draws the current SVG outline (draw tab)

            if (GUIDrawSegments)
                drawSegments();              // draws the straight lines between the bend points (located in the "draw" tab)

            if (GUIDrawPoints)
                drawPoints();                // draws bend points (draw tab)

            drawPreviewArea();               // finish drawing into the preview area buffer and draw it to the main window

            drawSelectRect();

            drawBoundingArea();              // draws the dimension list and side dimensions, descriptive text (draw tab)

            if (displayState == DS_LOADED || displayState == DS_DRAWING)  {
                setDrawWarnings();           // Function that sets status based on presence of errors (draw tab)
            } else if (displayState == DS_CONNECTING)  {
                //status = STATUS_CONNECTING;
                //flashTinyG();
            } else if (displayState == DS_SENDING)  {
                // status = STATUS_SENDING;
                //sendBendCommands();
            } else if (displayState == DS_WARNING)  {
                drawWarning();        // draws warning message based on popUpWarning
            }
        } else if (curViewMode == VIEW_CALIBRATE)  {
            doCalibration();
        } else if (curViewMode == VIEW_MANUAL)  {
            // nothing to update here
        } else  {
            println("ERROR: Unknown view mode: " + curViewMode);
        }
    }


    //This is to avoid hangs in the rendering due to the serial connections and such
    public void async(){
        while (true){
            //println("async");
            delay(50);
            if (curViewMode == VIEW_EDIT)  {
                if (displayState == DS_CONNECTING)  {
                    status = STATUS_CONNECTING;
                    if (testSerial()){
                        if(flashTinyG()){
                            if (!checkHomed()){
                                displayState = DS_HOMING;
                            }
                            else{
                                displayState = DS_SENDING;
                                lastFeedSentIdx = 0;
                                lastAngleSentIdx = 0;
                                repeatCurrent=1;
                            }
                        }
                    }
                    else{
                        popUpWarning = WARNING_CONNECTION;
                        displayState = DS_WARNING;
                    }
                }else if (displayState == DS_HOMING)  {
                    status = STATUS_HOMING;
                    if(tinyGHome()){
                        displayState = DS_SENDING;
                        lastFeedSentIdx = 0;
                        lastAngleSentIdx = 0;
                        repeatCurrent=1;
                    }
                    else if(continuousFlag){
                        popUpWarning = WARNING_CONT_HOME;
                        displayState = DS_WARNING;
                    }

                }
                else if (displayState == DS_SENDING)  {
                    status = STATUS_SENDING;
                    sendBendCommands();
                } else if (displayState == DS_WARNING)  {
                    //drawWarning();        // draws warning message based on popUpWarning
                }
            }
        }
    }

    // convert an x-offset of the DIWire in mm to the angle of the wire in degrees
// based on a number of measurements, both simulated and real (see above)
// based on 1/16" wire and 1/16" bend head - other wire and BH combinations will require separate handling
// used in calibration process to establish conversion between input offset and output angle with spring-back
//
// this function attempts to correct for the fact that at x == 0 (in the positive CW bend direction)
//  the bend pin is not touching the wire, and all input values up to about 0.6 don't touch the wire
//  so the angle should be 0.  The formula used below seems to assume that the bend pin is in contact
//  with the wire at x == 0 and any positive value produces a positive bend angle.  Similarly for
//  the negative CCW bend direction.  This function uses calibXContactPos and calibXContactNeg values to compensate
//  for this.  Again, these were set for 1/16" wire and will change with wire thickness
    public float pinMM2WireAng (float mm)  {
        // the conversion poly is only in the + direction, so we need to reverse it for negative offset values
        boolean isNeg = false;
        if (mm < 0)  {
            isNeg = true;
            mm = -mm;
        }

        // first, we compensate for the fact that the pins don't contact the wire until there is some offset
        // whereas in the poly fit formula 0 is touching the wire
        float pinMM = mm - (isNeg ? -calibXContactNeg : calibXContactPos);
        if (pinMM < 0.0f) pinMM = 0.0f;

        // the pin angle is linearly related to the pin offset in mm
        //float pinAng = pinMM * pinMM2pinAng16;
        //if (calibSize == CALIB_SIZE_1_8) pinAng = pinMM * pinMM2pinAng8;
        float pinAng = pinMM * pinMM2pinAng;

        // compute wire angle from pinAngle using fit polynomial
        // this is a slightly more efficient way of evaluating the fit polynomial
        //float[] p2w = pinAng2wireAng16;
        //if (calibSize == CALIB_SIZE_1_8) p2w = pinAng2wireAng8;
        Float[] p2w = pinAng2wireAng;
        float wireAng = p2w[0];
        wireAng = wireAng*pinAng + p2w[1];
        wireAng = wireAng*pinAng + p2w[2];
        wireAng = wireAng*pinAng + p2w[3];
        wireAng = wireAng*pinAng + p2w[4];
        wireAng = wireAng*pinAng + p2w[5];

        if (isNeg) wireAng = -wireAng;

        return wireAng;
    }

    // go the other way
    public float wireAng2PinMM (float ang)  {
        // the conversion poly is only in the + direction, so we need to reverse it for negative offset values
        boolean isNeg = false;
        if (ang < 0)  {
            isNeg = true;
            ang = -ang;
        }

        // compute wire angle from pinAngle using fit polynomial
        // this is a slightly more efficient way of evaluating the fit polynomial
//    float[] w2p = wireAng2pinAng16;
//    if (calibSize == CALIB_SIZE_1_8) w2p = wireAng2pinAng8;
        Float[] w2p = wireAng2pinAng;
        float pinAng = w2p[0];
        pinAng = pinAng*ang + w2p[1];
        pinAng = pinAng*ang + w2p[2];
        pinAng = pinAng*ang + w2p[3];
        pinAng = pinAng*ang + w2p[4];
        pinAng = pinAng*ang + w2p[5];

        if (isNeg) pinAng = -pinAng;

        // the pin angle is linearly related to the pin offset in mm
//    float pinMM = pinAng / pinMM2pinAng16;
//    if (calibSize == CALIB_SIZE_1_8) pinMM = pinAng / pinMM2pinAng8;
        float pinMM = pinAng / pinMM2pinAng;

        // first, we compensate for the fact that the pins don't contact the wire until there is some offset
        // whereas in the poly fit formula 0 is touching the wire
        pinMM += (isNeg ? calibXContactNeg : calibXContactPos);

        return pinMM;
    }

    // convert from wire angle in degrees to x-axis offset in mm
// uses data from calibration table
    public float wireAng2SpringBackMM (float ang)  {
        if (polyFormulaPos.length < 4)
            calculateAngle2MMPolyFit();

        if (polyFormulaPos.length < 4)  {
            println("ERROR: wireAng2SpringBackMM() called without calibration data");
            return 0.0f;
        }

        // evaluate polynomial - index order is reversed from pinAng2wireAng[]
        float mm = 0;
        if (ang >= 0)  {
            mm = polyFormulaPos[3];
            mm = mm*ang + polyFormulaPos[2];
            mm = mm*ang + polyFormulaPos[1];
            mm = mm*ang + polyFormulaPos[0];
        } else  {
            mm = polyFormulaNeg[3];
            mm = mm*ang + polyFormulaNeg[2];
            mm = mm*ang + polyFormulaNeg[1];
            mm = mm*ang + polyFormulaNeg[0];
        }

        return mm;
    }


    public float wireArc2MM(float diam){

//      if (polyFormulaPosArc.length < 4)
//        calculateAngle2MMPolyFit();

        if (polyFormulaPosArc.length < 4)  {
            println("ERROR: wireArc2MM() called without calibration data");
            return 0.0f;
        }

        // evaluate polynomial - index order is reversed from pinAng2wireAng[]
        float mm = 0;
        if (diam >= 0)  {
            mm = polyFormulaPosArc[0]*pow(diam, 5)+polyFormulaPosArc[1]*pow(diam, 4)+polyFormulaPosArc[2]*pow(diam, 3)+polyFormulaPosArc[3]*pow(diam, 2)+polyFormulaPosArc[4]*diam+polyFormulaPosArc[5];
            if (mm<0)
                mm=0;
            if (mm>7)
                mm=7;

        } else  {
            diam=abs(diam);
            mm = zeroRight -1*(polyFormulaPosArc[0]*pow(diam, 5)+polyFormulaPosArc[1]*pow(diam, 4)+polyFormulaPosArc[2]*pow(diam, 3)+polyFormulaPosArc[3]*pow(diam, 2)+polyFormulaPosArc[4]*diam+polyFormulaPosArc[5]);
            if (mm>-5.5f)
                mm=-5.5f;
        }
        return mm;
    }
/*
    Read DXF file and convert to SVG file

    Filters Entities to keep only certain criteria (color, line style)
    Converts arcs to bezier curves to get around arc problem in Geomerative library
    Joins segments together to form larger paths

    Clay Budin
    clay_budin@hotmail.com
    Pensa Labs
    Jul 22 2014
*/

//import java.util.HashMap;



//String DXFFile = "../24-7000_889mm FPCW FC UF CHRYSLER.dxf";    // blue centerline, clean
//String DXFFile = "../24-7000_Test13oneent.dxf";
//String DXFFile = "../24-7000_Test14_3ent.dxf";

//String DXFFile = "../24-7176_UF_Wire_20130729.dxf";           // blue centerline, a little bit of noise, overlapping lines
//String DXFFile = "../PI60118_24-7205_UF_FSB_WIRE.DXF";        // no blue, only dash-dot lines
//String DXFFile = "../PI60662_24-7228_UF_FSB_WIRE.DXF";        // DASHED lines, some noise
//String DXFFile = "../PI60663_24-7229_UF_FSB_WIRE.DXF";        // DASHED lines, no noise
//String DXFFile = "../PI60708_24-7174_UF_Wire.dxf";            // same as 7176 above, but dotdash center line - noisy, incomplete line, line doesn't follow center
//String DXFFile = "../PI61408_24-7207_UF_FSC_WIRE.DXF";        // dotdash center line - noisy, slightly incomplete line, line doesn't follow center

//String DXFFile = "../../AI/bendMe_flower.dxf";      // from SVG file via AI - uses SPLINE Entity

// internally generated test file
//String DXFFile = "../LineSegments.DXF";        // LINE, ELLIPSE, ARC, LWPOLYLINE - instead of SPLINE
//String DXFFile = "../lines2.DXF";        // LINE, ELLIPSE, ARC, LWPOLYLINE - instead of SPLINE
//String DXFFile = "../lines3.DXF";        // LINE, ELLIPSE, ARC, POLYLINE - instead of SPLINE



    // types of drawing Entities in DXF file (so far encountered)
    final int ENT_NONE = 0;
    final int ENT_POINT = 1;
    final int ENT_LINE = 2;
    final int ENT_POLYLINE = 3;
    final int ENT_LWPOLYLINE = 4;    // like POLYLINE, but specified differently
    final int ENT_ARC = 5;
    final int ENT_CIRCLE = 6;
    final int ENT_ELLIPSE = 7;
    final int ENT_SPLINE = 8;
    final int ENT_END = 9;        // ENDSEC - end of section (not a draw command)

    // DXF color codes - there are more than these, 0 is white also
    final int COL_RED = 1;
    final int COL_YELLOW = 2;
    final int COL_GREEN = 3;
    final int COL_CYAN = 4;
    final int COL_BLUE = 5;
    final int COL_MAGENTA = 6;
    final int COL_WHITE = 7;

    // Filter types
    final int FILTER_NONE = 0;
    final int FILTER_BLUE = 1;
    final int FILTER_DASHDOT = 2;
    final int FILTER_DASHED = 3;
    final int FILTER_HATCH = 4;


    // globals
    int curEnt = ENT_NONE;
    StringDict entData = new StringDict();
    String curSub = "";
    String prevLine = "";
    int vCt = 0, kCt = 0, lCt = 0;
    int entCt = 0, keepCt = 0;
    boolean inEnt = false;
    int filterType = FILTER_NONE;


    class Segment  {
        float sx,sy;    // start coords
        float ex,ey;    // end coords
        ArrayList<StringDict> ents;    // list of Entities

        Segment ()  { ents = new ArrayList<StringDict>(); }

        public String toString ()  {
            String ret = "S(" + sx + "," + sy + ") E(" + ex + "," + ey + ") nEnts = " + ents.size();
            for (int i = 0; i < ents.size(); ++i) ret += "    #" + i + ": " + ents.get(i);
            return ret;
        }

        public void reverse ()  {
            Collections.reverse(ents);    // reverse Entities array
            for (int i = 0; i < ents.size(); ++i) ents.get(i).set("rev", ents.get(i).get("rev").startsWith("0") ? "1" : "0");    // toggle reversed flag in Entities
            // reverse start and end points
            float tmp;
            tmp = sx; sx = ex; ex = tmp;
            tmp = sy; sy = ey; ey = tmp;
        }
    };
    LinkedList<Segment> segments = new LinkedList<Segment>();



    IntDict seenWords = new IntDict();
    public int str2ent (String es)  {
        println(es);
        if (es.equals("POINT")) return ENT_POINT;
        if (es.equals("LINE")) return ENT_LINE;
        if (es.equals("POLYLINE")) return ENT_POLYLINE;
        if (es.equals("LWPOLYLINE")) return ENT_LWPOLYLINE;
        if (es.equals("ARC")) return ENT_ARC;
        if (es.equals("CIRCLE")) return ENT_CIRCLE;
        if (es.equals("ELLIPSE")) return ENT_ELLIPSE;
        if (es.equals("SPLINE")) return ENT_SPLINE;
        if (es.equals("ENDSEC")) return ENT_END;

        // need to save when we find one, prints SEQEND a lot
        if (inEnt && es.matches("[A-Z]+") && !seenWords.hasKey(es))  {
            if (!es.equals("ENTITIES") && !es.equals("VIEWPORT") && !es.equals("VERTEX") &&
                    !es.equals("SEQEND") && !es.equals("DASHDOT") && !es.equals("DASHED") && !es.equals("HATCH"))  {
                // filter out short HEX values - could miss a word like ACE here
                if (es.length() > 3 || !es.matches("[A-F]+"))  {
                    println("Possible New Entity: " + es);
                    seenWords.set(es, 1);
                }
            }
        }
        return ENT_NONE;
    }

    public String ent2str (int e)  {
        if (e == ENT_NONE) return "NONE";
        if (e == ENT_POINT) return "POINT";
        if (e == ENT_LINE) return "LINE";
        if (e == ENT_POLYLINE) return "POLYLINE";
        if (e == ENT_LWPOLYLINE) return "LWPOLYLINE";
        if (e == ENT_ARC) return "ARC";
        if (e == ENT_CIRCLE) return "CIRCLE";
        if (e == ENT_ELLIPSE) return "ELLIPSE";
        if (e == ENT_SPLINE) return "SPLINE";
        if (e == ENT_END) return "END";
        return "UNKNOWN";
    }


    public void initDXF ()  {
        curEnt = ENT_NONE;
        entData = new StringDict();
        curSub = "";
        prevLine = "";
        vCt = 0;
        kCt = 0;
        lCt = 0;
        entCt = 0;
        keepCt = 0;
        inEnt = false;
        filterType = FILTER_NONE;

        segments = new LinkedList<Segment>();
        seenWords = new IntDict();
    }


    // read the contents of a DXF file and pull out the drawing Entities into the segments list
// should specify the type of filtering (if any) to use
    public void parseDXF (String DXFFile)  {

        // parse DXF file
        String[] lines = loadStrings(DXFFile);

        for (int i = 0; i < lines.length; ++i)  {
            String line = lines[i].trim();

            // track when in Entities section
            if (line.equals("ENTITIES")) inEnt = true;

            // only look at Entities in the ENITITES section of the file
            if (!inEnt)
                continue;

            // see if we are the start of a drawing Entity, or at end of section
            int newEnt = str2ent(line);
            if (newEnt > 0)  {
                //println(line + " " + ent2str(newEnt));
                //println(line + " " + ent2str(curEnt));

                // if we are already in an Entity, we need to decide what to do with it
                if (curEnt > 0)  {
                    //println("Current Entity: " + ent2str(curEnt) + "    Data: " + entData);

                    // filter: could be color, or dashdot or dashed or ...
                    //if (true)  {
                    //if (Integer.parseInt(entData.get("color")) == COL_BLUE)  {
                    //if (int(entData.get("color")) == COL_BLUE)  {
                    //if (entData.hasKey("dashdot"))  {
                    //if (entData.hasKey("dashed"))  {
                    if (filterType == FILTER_NONE ||
                            (filterType == FILTER_BLUE && Integer.parseInt(entData.get("color")) == COL_BLUE) ||
                            (filterType == FILTER_DASHDOT && entData.hasKey("dashdot")) ||
                            (filterType == FILTER_DASHED && entData.hasKey("dashed")) ||
                            (filterType == FILTER_HATCH && entData.hasKey("hatch")))  {

                        // if we pass the filter, create a Segment with just this Entity and add it to the segments list
                        // do everything in untransformed space

                        keepCt += 1;

                        //println("Entity: " + ent2str(curEnt) + "    Data: " + entData);

                        if (curEnt == ENT_POINT)  {
                            //noSmooth(); point(off+float(entData.get("x")), sz-float(entData.get("y"))-off); smooth();

                            // don't add to segments list
                            println("WARNING: POINT Entity not output in segments list: " + entData);
                        }

                        if (curEnt == ENT_LINE)  {
                            // add to seqments list for joining
                            Segment s = new Segment();
                            s.sx = PApplet.parseFloat(entData.get("sx"));
                            s.sy = PApplet.parseFloat(entData.get("sy"));
                            s.ex = PApplet.parseFloat(entData.get("ex"));
                            s.ey = PApplet.parseFloat(entData.get("ey"));

                            s.ents.add(entData);
                            segments.add(s);

                        }

                        if (curEnt == ENT_POLYLINE || curEnt == ENT_LWPOLYLINE)  {
                            // add to seqments list for joining
                            int npts = PApplet.parseInt(entData.get("npts"));

                            Segment s = new Segment();
                            s.sx = PApplet.parseFloat(entData.get("x1"));
                            s.sy = PApplet.parseFloat(entData.get("y1"));
                            s.ex = PApplet.parseFloat(entData.get("x"+npts));
                            s.ey = PApplet.parseFloat(entData.get("y"+npts));
                            s.ents.add(entData);
                            segments.add(s);
                        }

                        if (curEnt == ENT_ARC || curEnt == ENT_CIRCLE)  {
                            // in DXF file, arc starts at x-axis and proceeds counterclockwise from start to end, even if ea < sa
                            // in Processing arc starts at x-axis and proceeds clockwise
                            float rad = PApplet.parseFloat(entData.get("rad"));
                            float cx = PApplet.parseFloat(entData.get("cx"));
                            float cy = PApplet.parseFloat(entData.get("cy"));
                            float sa = 0, ea = TWO_PI;    // start/end point for circle is arbitrary
                            if (curEnt == ENT_ARC)  {
                                sa = radians(PApplet.parseFloat(entData.get("sa")));
                                ea = radians(PApplet.parseFloat(entData.get("ea")));
                            }

                            // add to seqments list for joining
                            Segment s = new Segment();
                            s.sx = cx + rad*cos(sa);
                            s.sy = cy + rad*sin(sa);
                            s.ex = cx + rad*cos(ea);
                            s.ey = cy + rad*sin(ea);
                            s.ents.add(entData);
                            segments.add(s);
                        }

                        if (curEnt == ENT_ELLIPSE)  {
                            // this is not quite working
                            // in DXF file, ellipse starts at (mx,my) and proceeds counterclockwise, can be angled
                            // in Processing ellipse segment is done via arc() and can't be angled without rotate() call
                            // params in DXF are very different from Processing - angles are in radians, mx/my rel to cx/cy
                            //stroke(255,0,255);
                            //float rad = float(entData.get("rad"));
                            //arc(off+float(entData.get("cx")), sz-float(entData.get("cy"))-off, rad, rad,
                            //        radians(360-float(entData.get("ea"))), radians(360-float(entData.get("sa"))));
//                        pushMatrix();
//                        translate(10,-10);
//                        translate(float(entData.get("cx")), sz-float(entData.get("cy")));
//                        float mx = float(entData.get("mx"));
//                        float my = float(entData.get("my"));
//                        rotate(atan2(mx, -my));
//                        float mrad = sqrt(mx*mx+my*my);
//                        arc(0,0, mrad, mrad*float(entData.get("rat")), TWO_PI-float(entData.get("ea")), TWO_PI-float(entData.get("sa")));
//                        popMatrix();

                            // don't add to segments list yet
                            println("WARNING: ELLIPSE Entity not output in segments list: " + entData);
                        }

                        if (curEnt == ENT_SPLINE)  {
                            //println("WARNING: SPLINE Entitiy not output in segments list: " + entData);
                            // add to seqments list for joining
                            int npts = PApplet.parseInt(entData.get("npts"));

                            Segment s = new Segment();
                            s.sx = PApplet.parseFloat(entData.get("cx1"));
                            s.sy = PApplet.parseFloat(entData.get("cy1"));
                            s.ex = PApplet.parseFloat(entData.get("cx"+npts));
                            s.ey = PApplet.parseFloat(entData.get("cy"+npts));
                            s.ents.add(entData);
                            segments.add(s);
                        }
                    }
                }

                // clear Entity data for new Entity
                curEnt = newEnt;
                if (curEnt == ENT_END)  {
                    curEnt = ENT_NONE;
                    inEnt = false;        // leaving ENTITIES section
                } else  {
                    entData = new StringDict();
                    curSub = "";
                    prevLine = "";
                    vCt = 0;
                    kCt = 0;

                    entData.set("type", ent2str(curEnt));    // save type for join phase
                    entData.set("rev", "0");                 // not reversed
                    entData.set("npts", "0");                // sometimes not set - avoids exception
                    entCt += 1;
                }

                continue;
            }

            // if we are currently in an Entity, attempt to parse out its data
            if (curEnt > 0)  {
                // see if we are starting a new subclass region
                if (line.startsWith("Ac") || line.startsWith("CONT") )  {
                    curSub = line;
                    prevLine = "";
                    lCt = 0;
                    //continue;
                } else  {
                    // line count - param values are only on even-numbered lines
                    ++lCt;
                }

                // distinguish between a parameter and a code number
                boolean evenLine = ((lCt & 0x1) == 0);

                // look for color code in AcDbEntity - all Entity types
                // 0 = White, 1 = Red, 2 = Yellow, 3 = Green, 4 = Cyan, 5 = Blue, 6 = Magenta, 7 = White, ... more
                if (evenLine && (curSub.equals("AcDbEntity") ) && prevLine.equals("62"))  {
                    entData.set("color", line);
                    //prevLine = "";
                    //continue;
                }

                // parse point data
                if (evenLine && curEnt == ENT_POINT && curSub.equals("AcDbPoint"))  {
                    if (prevLine.equals("10")) entData.set("x", line);
                    if (prevLine.equals("20")) entData.set("y", line);
                }

                // parse line data
                if (evenLine && curEnt == ENT_LINE && (curSub.equals("AcDbLine") || curSub.equals("CONTINUOUS")))  {
                    if (prevLine.equals("10")) entData.set("sx", line);
                    if (prevLine.equals("20")) entData.set("sy", line);
                    if (prevLine.equals("11")) entData.set("ex", line);
                    if (prevLine.equals("21")) entData.set("ey", line);
                }

                // parse polyline data
                if (evenLine && curEnt == ENT_POLYLINE && curSub.equals("AcDb2dPolyline"))  {
                    if (prevLine.equals("70")) entData.set("closed", line);
                    if (prevLine.equals("71")) entData.set("npts", line);
                }
                if (curEnt == ENT_POLYLINE && line.equals("VERTEX"))  {
                    vCt += 1;
                    entData.set("npts", String.valueOf(vCt));    // sometimes this isn't set in header part
                }
                if (evenLine && curEnt == ENT_POLYLINE && curSub.equals("AcDb2dVertex"))  {
                    if (prevLine.equals("10")) entData.set("x" + vCt, line);
                    if (prevLine.equals("20")) entData.set("y" + vCt, line);
                }

                // parse lwpolyline data
                if (evenLine && curEnt == ENT_LWPOLYLINE && curSub.equals("AcDbPolyline"))  {
                    if (prevLine.equals("70")) entData.set("closed", line);
                    if (prevLine.equals("90")) entData.set("npts", line);
                    if (prevLine.equals("10")) entData.set("x" + ++vCt, line);
                    if (prevLine.equals("20")) entData.set("y" + vCt, line);
                }

                // parse arc and circle data
                if (evenLine && (curEnt == ENT_ARC || curEnt == ENT_CIRCLE) &&
                        (curSub.equals("AcDbCircle") || curSub.equals("AcDbArc")))  {
                    if (prevLine.equals("10")) entData.set("cx", line);
                    if (prevLine.equals("20")) entData.set("cy", line);
                    if (prevLine.equals("40")) entData.set("rad", line);
                    if (prevLine.equals("50")) entData.set("sa", line);
                    if (prevLine.equals("51")) entData.set("ea", line);
                }

                // parse ellispe data
                if (evenLine && curEnt == ENT_ELLIPSE && curSub.equals("AcDbEllipse"))  {
                    if (prevLine.equals("10")) entData.set("cx", line);
                    if (prevLine.equals("20")) entData.set("cy", line);
                    if (prevLine.equals("11")) entData.set("mx", line);
                    if (prevLine.equals("21")) entData.set("my", line);
                    if (prevLine.equals("40")) entData.set("rat", line);
                    if (prevLine.equals("41")) entData.set("sa", line);
                    if (prevLine.equals("42")) entData.set("ea", line);
                }

                // parse spline data
                // there are a lot of parameters in here
                // not handling fit points, knot weights, tolerance values
                if (evenLine && curEnt == ENT_SPLINE && curSub.equals("AcDbSpline"))  {
                    if (prevLine.equals("70")) entData.set("closed", line);
                    if (prevLine.equals("71")) entData.set("degree", line);
                    if (prevLine.equals("72")) entData.set("nknots", line);
                    if (prevLine.equals("73")) entData.set("npts", line);
                    //if (prevLine.equals("74")) entData.set("nfitpts", line);

                    if (prevLine.equals("10")) entData.set("cx" + ++vCt, line);
                    if (prevLine.equals("20")) entData.set("cy" + vCt, line);
                    if (prevLine.equals("40")) entData.set("k" + ++kCt, line);
                    if (prevLine.equals("41")) println("Spline Weights Found");

                    // start/end tangency info - not needed to reconstruct spline
                    //if (prevLine.equals("12")) entData.set("stx", line);
                    //if (prevLine.equals("22")) entData.set("sty", line);
                    //if (prevLine.equals("13")) entData.set("etx", line);
                    //if (prevLine.equals("23")) entData.set("ety", line);
                }

                // look for DASHDOT and DASHED line styles - also CONTINUOUS?
                if (line.equals("DASHDOT"))  {
                    entData.set("dashdot", "yes");
                } else if (line.equals("DASHED"))  {
                    entData.set("dashed", "yes");
                } else if (line.equals("HATCH"))  {
                    entData.set("hatch", "yes");
                }

            }

            prevLine = line;
        }
    }


    // iterate over the segments list, joining segments whose end points are within the tolerance distance
// should probably fill gaps above certain tolerance with a LINE Entity
    public void joinSegments (float tol)  {
        if (segments.size() == 0)  {
            println("joinSegments() ERROR: No segments");
            return;
        }

        // terminate when we see the last modified segment again
        Segment lastMod = null; //segments.getLast();

        while (true)  {
            //printSegments();

            Segment s = segments.removeFirst();

            if (s == lastMod)  {
                segments.add(s);    // ending, so put this segment back
                break;
            }

            if (lastMod == null)
                lastMod = s;

            // search for another segment which matched up with it
            // may need to reverse segments - check start and end against start and end
            for (int i = 0; i < segments.size(); ++i)  {
                // just check start with end for now
                Segment ss = segments.get(i);

                float dx, dy, dd;

                // look for end of seg1 connecting to start of seg2
                dx = s.ex - ss.sx;
                dy = s.ey - ss.sy;
                dd = sqrt(dx*dx+dy*dy);    // could be squared dist
                if (dd <= tol)  {
                    //println("MATCH ES: " + i);
                    segments.remove(i);
                    for (int j = 0; j < ss.ents.size(); ++j)
                        s.ents.add(ss.ents.get(j));
                    s.ex = ss.ex;
                    s.ey = ss.ey;
                    lastMod = s;
                    break;
                }

                // start to end
                dx = s.sx - ss.ex;
                dy = s.sy - ss.ey;
                dd = sqrt(dx*dx+dy*dy);    // could be squared dist
                if (dd <= tol)  {
                    //println("MATCH SE: " + i);
                    segments.remove(i);
                    for (int j = 0; j < s.ents.size(); ++j)
                        ss.ents.add(s.ents.get(j));
                    ss.ex = s.ex;
                    ss.ey = s.ey;
                    s = ss;    // ss is now at start - could also reverse both
                    lastMod = s;
                    break;
                }

                // start to start
                dx = s.sx - ss.sx;
                dy = s.sy - ss.sy;
                dd = sqrt(dx*dx+dy*dy);    // could be squared dist
                if (dd <= tol)  {
                    //println("MATCH SS: " + i);
                    segments.remove(i);
                    s.reverse();
                    for (int j = 0; j < ss.ents.size(); ++j)  {
                        s.ents.add(ss.ents.get(j));
                    }
                    s.ex = ss.ex;
                    s.ey = ss.ey;
                    lastMod = s;
                    break;
                }

                // end to end
                dx = s.ex - ss.ex;
                dy = s.ey - ss.ey;
                dd = sqrt(dx*dx+dy*dy);    // could be squared dist
                if (dd <= tol)  {
                    //println("MATCH EE: " + i);
                    segments.remove(i);
                    ss.reverse();
                    for (int j = 0; j < ss.ents.size(); ++j)  {
                        s.ents.add(ss.ents.get(j));
                    }
                    s.ex = ss.ex;
                    s.ey = ss.ey;
                    lastMod = s;
                    break;
                }
            }

            segments.add(s);    // add possibly processed segment back to end of queue
        }
    }

    public void printSegments ()  {
        println("\nSegments num = " + segments.size());
        for (int i = 0; i < segments.size(); ++i)
            println("Seg " + (i+1) + ": " + segments.get(i));
    }

/*
// draw the current segments list in varying colors
// note: doesn't (yet) draw reversed segments in reverse order
float offxAnim = 0, offxAnimDir = 0, offxAnimMax = 15;
void drawSegments (float offx, float offy, float scl)  {
    if (segments.size() == 0)  {
        println("drawSegments() ERROR: No segments");
        return;
    }

    colorMode(HSB, 255);
    int h = 255 / segments.size();

    float offxSave = offx;

    for (int i = 0; i < segments.size(); ++i)  {
        Segment s = segments.get(i);

        offx = offxSave + i*offxAnim;

        //stroke(127+random(128), 127+random(128), 127+random(128));
        //stroke(h, 255, 255);
        //h = (h + 19) % 255;
        //stroke(random(255), 255, 255);
        stroke(h*i, 255, 255);

        for (int j = 0; j < s.ents.size(); ++j)  {
            StringDict entData = s.ents.get(j);

            if (entData.get("type").equals("LINE"))  {
                line(offx+float(entData.get("sx"))*scl, height-float(entData.get("sy"))*scl-offy,
                         offx+float(entData.get("ex"))*scl, height-float(entData.get("ey"))*scl-offy);
            }

            // doesn't handle closing polyline yet
            if (entData.get("type").equals("POLYLINE") || entData.get("type").equals("LWPOLYLINE"))  {
                int npts = int(entData.get("npts"));
                float sx = offx+float(entData.get("x1"))*scl;
                float sy = height-float(entData.get("y1"))*scl-offy;
                for (int k = 2; k <= npts; ++k)  {
                    float ex = offx+float(entData.get("x" + k))*scl;
                    float ey = height-float(entData.get("y" + k))*scl-offy;
                    line(sx,sy, ex,ey);
                    sx = ex;
                    sy = ey;
                }
            }

            if (entData.get("type").equals("ARC") || entData.get("type").equals("CIRCLE"))  {
                float rad = float(entData.get("rad"))*scl;
                float cx = float(entData.get("cx"))*scl;
                float cy = float(entData.get("cy"))*scl;
                float sa = 0.0, ea = radians(360);
                if (entData.get("type").equals("ARC"))  {
                    sa = radians(360-float(entData.get("ea")));
                    ea = radians(360-float(entData.get("sa")));
                }
                while (sa > ea) ea += TWO_PI;    // Processing is very particular about this
                arc(offx+cx, height-cy-offy, rad, rad, sa, ea);
            }

            // don't draw ELLIPSE or SPLINE yet
        }
    }
}
*/


    // arc to bezier
// http://hansmuller-flex.blogspot.com/2011/10/more-about-approximating-circular-arcs.html
    class BezSpec { float x1,y1, x2,y2, x3,y3, x4,y4; }

    public String arc2bez (float cx, float cy, float rad, float sa, float ea)  {
        boolean rev = false;
        if (sa > ea)  {
            rev = true;
            float tmp = sa; sa = ea; ea = tmp;
        }

        String res = "";
        ArrayList<BezSpec> curves = createArc(rad, sa, ea);

        if (rev)
            for (int i = curves.size()-1; i >= 0; --i)  {
                BezSpec bs = curves.get(i);
                res += "C " + (bs.x3+cx) + " " + (bs.y3+cy) + " " +
                        (bs.x2+cx) + " " + (bs.y2+cy) + " " +
                        (bs.x1+cx) + " " + (bs.y1+cy) + " ";
            }
        else
            for (int i = 0; i < curves.size(); ++i)  {
                BezSpec bs = curves.get(i);
                res += "C " + (bs.x2+cx) + " " + (bs.y2+cy) + " " +
                        (bs.x3+cx) + " " + (bs.y3+cy) + " " +
                        (bs.x4+cx) + " " + (bs.y4+cy) + " ";
            }

        return res;
    }

    public ArrayList<BezSpec> createArc (float rad, float sa, float ea)  {
        ArrayList<BezSpec> curves = new ArrayList<BezSpec>();
        float a1 = sa;
        for (float ta = (ea-sa); ta > .00001f; )  {
            float a2 = a1 + Math.min(ta, HALF_PI);
            curves.add(createSmallArc(rad, a1, a2));
            ta -= Math.abs(a2-a1);
            a1 = a2;
        }

        return curves;
    }

    public BezSpec createSmallArc (float rad, float a1, float a2)  {
        float a = (a2 - a1) / 2.0f;
        float x4 = rad * cos(a);
        float y4 = rad * sin(a);
        float x1 = x4;
        float y1 = -y4;

        float q1 = x1*x1 + y1*y1;
        float q2 = q1 + x1*x4 + y1*y4;
        float k2 = 4.0f/3.0f * (sqrt(2*q1*q2)-q2) / (x1*y4 - y1*x4);

        float x2 = x1 - k2*y1;
        float y2 = y1 + k2*x1;
        float x3 = x2;
        float y3 = -y2;

        float ar = a + a1;
        float cos_ar = cos(ar);
        float sin_ar = sin(ar);

        BezSpec b = new BezSpec();
        b.x1 = rad * cos(a1);
        b.y1 = rad * sin(a1);
        b.x2 = x2*cos_ar - y2*sin_ar;
        b.y2 = x2*sin_ar + y2*cos_ar;
        b.x3 = x3*cos_ar - y3*sin_ar;
        b.y3 = x3*sin_ar + y3*cos_ar;
        b.x4 = rad * cos(a2);
        b.y4 = rad * sin(a2);

        return b;
    }




    // write out an SVG file from the current segments list
    public void buildSVG (String SVGFile)  {
        // need to handle error conditions
        if (segments.size() == 0)  {
            println("drawSegments() ERROR: No segments");
            return;
        }

        PrintWriter svg = createWriter(SVGFile);

        // from: http://www.w3.org/TR/SVG/paths.html#Introduction
        svg.println("<?xml version=\"1.0\" standalone=\"no\"?>");
        svg.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
        // meta-data
        //svg.println("<svg width=\"4cm\" height=\"4cm\" viewBox=\"0 0 400 400\" xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">");
        //svg.println("  <title>Example triangle01- simple example of a \"path\"</title>");
        //svg.println("  <desc>A path that draws a triangle</desc>");
        //svg.println("  <rect x=\"1\" y=\"1\" width=\"398\" height=\"398\" fill=\"none\" stroke=\"blue\" />");
        //svg.println("  <path d=\"M 100 100 L 300 100 L 200 300 z\" fill=\"red\" stroke=\"blue\" stroke-width=\"3\" />");
        svg.println("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">");
        svg.println("<g transform=\"scale(1,-1) translate(0,-1000)\">");    // SVG and Processing use origin in upper left, DXF is lower left

        // each segment will be a separate SVG path - made up of 1 or more Entities
        // need to reverse point order of Entities which got reversed during joining
        // how to handle gaps between Entities?
        for (int i = 0; i < segments.size(); ++i)  {
            //println("Segment #" + i);
            //printSegments();
            Segment s = segments.get(i);

            // start a path
            // assumes path is continuous, but may have small gaps due to joining tolerance
            // should either add M at start of every path or add joining paths
            svg.print("  <path d=\"M " + s.sx + " " + s.sy + " ");

            // draw all entities in this segment as a single path
            for (int j = 0; j < s.ents.size(); ++j)  {
                StringDict entData = s.ents.get(j);

                //println("  Ent #" + j + "  entData = " + entData);

                if (entData.get("type").equals("LINE"))  {
                    if (entData.get("rev").startsWith("0"))
                        svg.print("L " + entData.get("ex") + " " + entData.get("ey") + " ");
                    else
                        svg.print("L " + entData.get("sx") + " " + entData.get("sy") + " ");
                }

                // doesn't handle closing polyline yet
                if (entData.get("type").equals("POLYLINE") || entData.get("type").equals("LWPOLYLINE"))  {
                    int npts = PApplet.parseInt(entData.get("npts"));
                    if (entData.get("rev").startsWith("0"))
                        for (int k = 2; k <= npts; ++k)
                            svg.print("L " + entData.get("x" + k) + " " + entData.get("y" + k) + " ");
                    else
                        for (int k = npts-1; k >= 1; --k)
                            svg.print("L " + entData.get("x" + k) + " " + entData.get("y" + k) + " ");
                }

                // SVG arc curves (A/a) are not implemented by Geomerative - convert to bezier curves
                if (entData.get("type").equals("ARC") || entData.get("type").equals("CIRCLE"))  {
                    //println(entData);

                    float rad = PApplet.parseFloat(entData.get("rad"));
                    float cx = PApplet.parseFloat(entData.get("cx"));
                    float cy = PApplet.parseFloat(entData.get("cy"));
                    float sa = 0.0f, ea = TWO_PI-.01f;    // circle needs to be a little bit open
                    if (entData.get("type").equals("ARC"))  {
                        sa = radians(PApplet.parseFloat(entData.get("sa")));
                        ea = radians(PApplet.parseFloat(entData.get("ea")));
                    }
                    while (ea < sa) ea += TWO_PI;
                    if (entData.get("rev").startsWith("1"))  { float tmp = sa; sa = ea; ea = tmp; }    // we want ea<sa here to reverse arc dir

                    //println("ARC sa = " + sa + " ea = " + ea);

                    //svg.print("A " + rad + " " + rad + " 0 " + (ea-sa >= PI ? "1" : "0") + " 1 " + (cx+rad*cos(ea)) + " " + (cy+rad*sin(ea)) + " ");
                    svg.print(arc2bez(cx,cy,rad,sa,ea));
                }

                // don't add ELLIPSE yet
                if (entData.get("type").equals("ELLIPSE"))  {
                    println("DXF2SVG WARNING: Spline Entity types not supported yet.");
                }

                // assumes cubic Bezier - could check degree value
                // looks like SPLINE values coming out of DXF are NURBS curves
                // NURBS: numknots = numpts + degree + 1
                // can convert NURBS to Bezier with knot insertion, or sample NURBS periodically to recover point/tangent info
                if (entData.get("type").equals("SPLINE"))  {
                    //svg.print("L " + entData.get("ex") + " " + entData.get("ey") + " ");
                    //println("DXF2SVG WARNING: Spline Entity types not supported yet.");

                    int nK = PApplet.parseInt(entData.get("nknots"));
                    int npts = PApplet.parseInt(entData.get("npts"));

                    // look for special case of bezier knots = { 0,0,0,0,1,1,1,2,2,2,3,3,3,...,n,n,n,n }
                    // in that case, we can take points directly
                    // AI produces this type of DXF file
                    boolean isBez = true;
                    if (PApplet.parseFloat(entData.get("k1")) != 0.0f)
                        isBez = false;
                    else  {
                        // should probably check that knots are equally spaced as well
                        int k;
                        for (k = 2; k < nK; k += 3)
                            if ((PApplet.parseFloat(entData.get("k"+k)) != PApplet.parseFloat(entData.get("k"+(k+1)))) ||
                                    (PApplet.parseFloat(entData.get("k"+k)) != PApplet.parseFloat(entData.get("k"+(k+2)))))  {
                                isBez = false;
                                break;
                            }
                        if (isBez && (k != nK || (PApplet.parseFloat(entData.get("k"+k)) != PApplet.parseFloat(entData.get("k"+(k-1))))))
                            isBez = false;
                    }

                    if (isBez)  {
                        println("IS BEZIER");
                        if (entData.get("rev").startsWith("0"))
                            for (int k = 2; k <= npts; k += 3)
                                svg.print("C " + entData.get("cx" + (k+0)) + " " + entData.get("cy" + (k+0)) + " " +
                                        entData.get("cx" + (k+1)) + " " + entData.get("cy" + (k+1)) + " " +
                                        entData.get("cx" + (k+2)) + " " + entData.get("cy" + (k+2)) + " ");
                        else
                            for (int k = npts-1; k >= 1; k -= 3)
                                svg.print("C " + entData.get("cx" + (k-0)) + " " + entData.get("cy" + (k-0)) + " " +
                                        entData.get("cx" + (k-1)) + " " + entData.get("cy" + (k-1)) + " " +
                                        entData.get("cx" + (k-2)) + " " + entData.get("cy" + (k-2)) + " ");
                    } else  {
                        println("DXF2SVG WARNING: NURBS Spline Entity types not supported yet.");

                        // can reduce this general case to Bezier case by knot insertion, see eg:
                        //   http://scholar.lib.vt.edu/theses/available/etd-07122005-091333/unrestricted/dissertation.2.pdf
                        //   https://www.cs.drexel.edu/~david/Classes/CS430/Lectures/L-10_NURBSDrawing.pdf
                        //   http://www.cs.mtu.edu/~shene/COURSES/cs3621/NOTES/spline/NURBS-knot-insert.html
                        // still can't handle weights
                        // sometimes see NURBS described with knot vector missing 1st and last entry (3 at start/end)
                        //   not sure what difference is, but indexing becomes an issue
                        // need to check 1st 2 and last 2 knot values are equal or can't handle (what does it mean?)
                        // once multiplicities of all knots == degree then knot values don't matter any more
                        // inserting a duplicate knot in a degree 3 spline involves replacing 1 cp with 2
                        //
                        // algortithm would be:
                        //  1) Read points and knots into linked lists (for easy remove/insert)
                        //  2) check 1st 2 and last 2 knots are equal, no weights (or weights all equal), degree == 3 -> error out
                        //  3) count knot multiplicities
                        //  4) for each knot with mult < degree insert knots to bring mult to degree
                        //  5) read point values out as Beziers
                    }

                }


            }

            // close the path
            svg.println("\" fill=\"none\" stroke=\"black\" />");

        }

        svg.println("</g>");
        svg.println("</svg>");
        svg.println("");

        svg.flush();
        svg.close();
    }



    // main call point for DXF2SVG functionality
// need to specify type of filtering here
    public String DXF2SVG (String DXFFile, int _filterType)  {
        //println("DXF2SVG ft = " + _filterType);

        // get a temporary file name - give it same name as DXF
        //println("DXFFile = " + DXFFile);
        //String[] f = splitTokens(DXFFile, "\\/");
        String[] f = split(DXFFile, File.separator);
        String[] ff = split(f[f.length-1], ".");
        //String SVGFile = rootDataFolder + File.separator + "tmp" + File.separator + "tmp.svg";
        String SVGFile = rootDataFolder + File.separator + "tmp" + File.separator + ff[0] + ".svg";

        //println("init()");
        initDXF();

        filterType = _filterType;

        // parse the DXF file into the segments list
        //println("begin parse()");
        parseDXF(DXFFile);
        println("Parse: " + str(entCt) + " Entities Found.  " + str(keepCt) + " Enitites Kept.");
        //printSegments();

        // joining operation
        println("Joining.  Start with " + segments.size() + " segments");

        // first pass - low tolerance
        joinSegments(.01f);

        println("First pass done with " + segments.size() + " segments");

        // second pass - "clean" - high tolerance
        // note: doesn't fill gaps between segments
//    joinSegments(3.0);

        //printSegments();
//    println("Second pass done with " + segments.size() + " segments");

        // signal failure by returing ""
        if (segments.size() == 0)  {
            println("DXF2SVG Failed.");
            return "";
        }

        buildSVG(SVGFile);

        return SVGFile;
    }


    public void CloseGapsInDXF (float threshPct)  {
        // this only runs on previously loaded DXF file
        if (curShape == null || segments.size() == 0)
            return;

        RPoint[] r = curShape.getBoundsPoints();
        float thresh = ((r[2].x-r[0].x)*.5f + (r[2].y-r[0].y)*.5f)*.5f * (threshPct*.01f);
        joinSegments(thresh);
        buildSVG(curFile);
    }



// the reset is left over from testing
/*
void setup ()  {
    // setup Processing to draw
    int sz = 1000;
    size(sz, sz);
    background(0);
    stroke(255,255,255);
    strokeWeight(1);
    noFill();
    ellipseMode(RADIUS);

    // drawing tests
    //line(10,20, 960,980);
    //arc(500,500,450,450,radians(350),radians(370));

    // ellipse - more complicated from DXF
    //pushMatrix();
    //translate(500,500);
    //rotate(atan2(-300,-300));
    //arc(0,0,300,150,TWO_PI-5,TWO_PI);
    //popMatrix();

    // parse the DXF file into the segments list
    parseDXF(DXFFile);
    println("\n" + str(entCt) + " Entities Found.  " + str(keepCt) + " Enitites Kept.");

    // joining operation
    println("\nJoining.  Start with " + segments.size() + " segments");

    // first pass - low tolerance
    joinSegments(.01);

    println("First pass done with " + segments.size() + " segments");

    // second pass - "clean" - high tolerance
    // note: doesn't fill gaps between segments
    joinSegments(3.0);

    //printSegments();
    println("Second pass done with " + segments.size() + " segments");
}


// starting offset and scale
int offx = 10, offy = 10;
float scl = 1.0;
boolean changed = true;

void draw ()  {
    if (changed)  {
        if (offxAnimDir != 0)  {
            offxAnim += offxAnimDir;
            if (offxAnim <= 0)  { offxAnim = 0; offxAnimDir = 0; }
            if (offxAnim >= offxAnimMax)  { offxAnim = offxAnimMax; offxAnimDir = 0; }
        }

        background(0);
        drawSegments(offx, offy, scl);
        changed = false;
        if (offxAnimDir != 0) changed = true;
    }
}

void mouseDragged ()  {
    offx += mouseX - pmouseX;
    offy -= mouseY - pmouseY;
    changed = true;
}

void mouseWheel (MouseEvent ev)  {
    scl += .2 * ev.getCount();
    if (scl < .2) scl = .2;
    changed = true;
}

void keyPressed ()  {
    if (key == 'r')  {
        offx = offy = 10;
        scl = 1.0;
        changed = true;
    } else if (key == 'a')  {
        offxAnimDir = offxAnim == 0 ? +0.5 : -0.5;
        changed = true;
    } else if (key == 'p')  {
        printSegments();
    }
}
*/


/*
    GUI draw routines and event handlers

*/


    // creates all the GUI elements used in the app
    public void initializeGUI ()  {
        debugC("GUI/initializeGUI()");
        // switch to mode where we wait for a file to get dropped
        curViewMode = VIEW_LOAD;

        // set up GUI elements for all modes
        // don't set position here, only size - position is set in positionGUIElements() after first resize event

        textSize(32);

        String[] files;

        //G4P.usePre35Fonts();
        G4P.setGlobalColorScheme(9);    // use 10 for test

        // Load Mode
        files = new String[] { "images/open_static.png","images/open_hover.png","images/open_click.png" };
        btnOpenFile = new GImageButton(this, 0,0, files);
        btnOpenFile.addEventHandler(this, "openFile");
        gAll.add(btnOpenFile); gLoad.add(btnOpenFile);


        // Edit Mode

        // Material Profile list
        // can create custom color scheme by editing image default_gui_palette.png and placing in same dir as source
        String[] items;
        //items = new String[] { "Select Material" }; //should never show up
        dlMatProfEdit = new GDropList(this, 0, 0, 175, 180, 5);
        dlMatProfEdit.addEventHandler(this, "matMenuEvents");
        //dlMatProfEdit.setLocalColorScheme(9); //5);
        gAll.add(dlMatProfEdit); gEdit.add(dlMatProfEdit);gManual.add(dlMatProfEdit);

        optMM = new GOption(this, 0,0, 60, 20, "");    // no text - will add manually
        optIn = new GOption(this, 0,0, 60, 20, "");
        togUnits = new GToggleGroup();
        togUnits.addControls(optMM, optIn);
        optMM.setSelected(true);
        optMM.addEventHandler(this, "unitsClicked");
        optIn.addEventHandler(this, "unitsClicked");
        gAll.add(optMM); gEdit.add(optMM);gManual.add(optMM);
        gAll.add(optIn); gEdit.add(optIn);gManual.add(optIn);


        optNoInput = new GOption(this, 0,0, 60, 20, "");    // no text - will add manually
        optYesInput = new GOption(this, 0,0, 60, 20, "");
        togInput = new GToggleGroup();
        togInput.addControls(optNoInput, optYesInput);
        optNoInput.setSelected(true);
        optNoInput.addEventHandler(this, "optInputClicked");
        optYesInput.addEventHandler(this, "optInputClicked");
        gAll.add(optNoInput);  gManual.add(optNoInput);
        gAll.add(optYesInput);  gManual.add(optYesInput);


        txWireLength = new GTextField(this, 0,0, 60, 25);
        txWireLength.setText("");
        txWireLength.addEventHandler(this, "handleTextEvents");
        gAll.add(txWireLength); gEdit.add(txWireLength);
        gTextFields.add(txWireLength);

        txScale = new GTextField(this, 0,0, 60, 25);
        txScale.setText("" + (GUIScaleLimits[2]));
        txScale.addEventHandler(this, "handleTextEvents");
        gAll.add(txScale); gEdit.add(txScale);
        gTextFields.add(txScale);

        sldScale = new GCustomSlider(this, 0,0, settingsPanelWidth-20, 40, "grey_blue");
        sldScale.setShowLimits(true);
        sldScale.setLimits(GUIScaleLimits[2], GUIScaleLimits[0], GUIScaleLimits[1]);
        sldScale.addEventHandler(this, "sliderEvent");
        gAll.add(sldScale); gEdit.add(sldScale);

        txResolution = new GTextField(this, 0,0, 60, 25);
        txResolution.setText("" + GUIResolutionLimits[2]);
        txResolution.addEventHandler(this, "handleTextEvents");
        gAll.add(txResolution); gEdit.add(txResolution);
        gTextFields.add(txResolution);

        txRepeat = new GTextField(this, 0,0, 30, 20);
        txRepeat.setText("1");
        txRepeat.addEventHandler(this, "handleTextEvents");
        gAll.add(txRepeat); gEdit.add(txRepeat);
        gTextFields.add(txRepeat);

        txKfactor = new GTextField(this, 0,0, 60, 25);
        txKfactor.setText("" + GUIkFactorLimits[2]);
        txKfactor.addEventHandler(this, "handleTextEvents");
        gAll.add(txKfactor); gEdit.add(txKfactor);
        gTextFields.add(txKfactor);

        sldResolution = new GCustomSlider(this, 0,0, settingsPanelWidth-20, 40, "grey_blue");
        sldResolution.setShowLimits(false);
        sldResolution.setLimits(GUIResolutionLimits[2], GUIResolutionLimits[1], GUIResolutionLimits[0]);
        sldResolution.addEventHandler(this, "sliderEvent");
        gAll.add(sldResolution); gEdit.add(sldResolution);

        // updates when this changes
        items = new String[] { " Line Segments", " Whole Curve" }; // removed " Adaptive" cbarbre 10222015
        dlSampling = new GDropList(this, 0, 0, 175, 120, 3);
        dlSampling.setItems(items, 0);
        dlSampling.addEventHandler(this, "handleSamplingChange");
        //dlSampling.setLocalColorScheme(9);
        gAll.add(dlSampling); gEdit.add(dlSampling);

        cbPreview = new GCheckbox(this, 0,0, 30, 30);
        cbPreview.setIcon("images/toggle_off.png", 1, GAlign.LEFT, null);
        cbPreview.addEventHandler(this, "previewClicked");
        gAll.add(cbPreview); gEdit.add(cbPreview);

        sldGapThresh = new GCustomSlider(this, 0,0, settingsPanelWidth-20, 40, "grey_blue");
        sldGapThresh.setShowLimits(true);
        sldGapThresh.setShowValue(true);
        sldGapThresh.setLimits(GUIGapLimits[2], GUIGapLimits[0], GUIGapLimits[1]);
        gAll.add(sldGapThresh); //gEdit.add(sldGapThresh);
        sldGapThresh.setVisible(false);

        files = new String[] { "images/closegaps.png","images/closegaps_hover.png","images/closegaps_select.png" };    // 120x30
        btnCloseGaps = new GImageButton(this, 0,0, files);
        btnCloseGaps.addEventHandler(this, "closeGaps");
        gAll.add(btnCloseGaps); //gEdit.add(btnCloseGaps);
        btnCloseGaps.setVisible(false);


        files = new String[] { "images/tog_ZoomRect.png", "images/tog_ZoomRect_hover.png","images/tog_ZoomRect_hover.png" };
        itbZoomRect = new GImageButton(this, 0, 0, files);    // 30x30
        itbZoomRect.addEventHandler(this, "itbFrame");

        files = new String[] { "images/tog_ZoomOut.png", "images/tog_ZoomOut_hover.png","images/tog_ZoomOut_hover.png" };
        itbZoomOut = new GImageButton(this, 0, 0, files);    // 30x30
        itbZoomOut.addEventHandler(this, "itbZoomOut");



        itbArrow = new GImageToggleButton(this, 0, 0, "images/tog_Arrow.png", "images/tog_Arrow_hover.png", 2, 1);    // 30x30
        itbZoomIn = new GImageToggleButton(this, 0, 0, "images/tog_ZoomIn.png", "images/tog_ZoomIn_hover.png", 2, 1);    // 30x30
        //itbZoomOut = new GImageToggleButton(this, 0, 0, "images/tog_ZoomOut.png", "images/tog_ZoomOut_hover.png",2,1);    // 30x30
        itbHand = new GImageToggleButton(this, 0, 0, "images/tog_Hand.png", "images/tog_Hand_hover.png", 2, 1);    // 30x30
        gAll.add(itbArrow); gEdit.add(itbArrow);
        gAll.add(itbZoomIn); gEdit.add(itbZoomIn);
        gAll.add(itbZoomOut); gEdit.add(itbZoomOut);
        gAll.add(itbZoomRect); gEdit.add(itbZoomRect);
        gAll.add(itbHand); gEdit.add(itbHand);

        itbArrow.stateValue(1);
        gMouseSelect.add(itbArrow);
        gMouseSelect.add(itbZoomIn);
        gMouseSelect.add(itbHand);
        for (GImageToggleButton g: gMouseSelect)
            g.addEventHandler(this, "mouseSelect");



        files = new String[] { "images/bend.png","images/bend_hover.png","images/bend_select.png" };    // 200x58
        btnBeginSend = new GImageButton(this, 0,0, files);
        btnBeginSend.addEventHandler(this, "beginSend");
        gAll.add(btnBeginSend); gEdit.add(btnBeginSend);

        // BEND button changes to PAUSE/RESUME while bending is taking place
        files = new String[] { "images/pause_button.png","images/pause_hover.png","images/pause_select.png" };    // 200x58
        btnPauseSend = new GImageButton(this, 0,0, files);
        btnPauseSend.addEventHandler(this, "pauseSend");
        gAll.add(btnPauseSend); gEdit.add(btnPauseSend);

        // BEND button changes to PAUSE/RESUME while bending is taking place
        files = new String[] { "images/resume.png","images/resume_hover.png","images/resume_select.png" };    // 200x58
        btnResumeSend = new GImageButton(this, 0,0, files);
        btnResumeSend.addEventHandler(this, "resumeSend");
        gAll.add(btnResumeSend); gEdit.add(btnResumeSend);

        // Stop button appears near the pause/resume button while bending
        files = new String[] { "images/stop.png","images/stop_hover.png","images/stop_select.png" };    // 40x40
        btnStopSend = new GImageButton(this, 0,0, files);
        btnStopSend.addEventHandler(this, "stopSend");
        gAll.add(btnStopSend); gEdit.add(btnStopSend);


        // these are the buttons for the "dialogs" that appear for errors, warnings, etc.
        files = new String[] { "images/yes-button.png","images/yes-hover.png","images/yes-select.png" };
        btnIgnoreError = new GImageButton(this, 0,0, files);
        gAll.add(btnIgnoreError);

        files = new String[] { "images/no-button.png","images/no-hover.png","images/no-select.png" };
        btnCancelSendError = new GImageButton(this, 0,0, files);
        gAll.add(btnCancelSendError);

        files = new String[] { "images/ok-button.png","images/ok-hover.png","images/ok-select.png" };
        btnSerialError = new GImageButton(this, 0,0, files);
        gAll.add(btnSerialError);

        files = new String[] { "images/ok-button.png","images/ok-hover.png","images/ok-select.png" };
        btnMidBendError = new GImageButton(this, 0,0, files);
        gAll.add(btnMidBendError);

        files = new String[] { "images/ok-button.png","images/ok-hover.png","images/ok-select.png" };
        btnBackOffLimitError = new GImageButton(this, 0,0, files);
        gAll.add(btnBackOffLimitError);




        // Calibration mode

        // Material Profile list
        // can create custom color scheme by editing image default_gui_palette.png and placing in same dir as source
        //items = new String[] { "Select Material" }; //should never show up
        dlMatProfCalib = new GDropList(this, 0, 0, 175, 270, 8);
        dlMatProfCalib.addEventHandler(this, "matMenuEvents");
        //dlMatProfCalib.setLocalColorScheme(9); //5);
        gAll.add(dlMatProfCalib); gCalibrate.add(dlMatProfCalib);

        files = new String[] { "images/create.png","images/create_hover.png","images/create_click.png" };    // 120x30
        btnCalibCreate = new GImageButton(this, 0,0, files);
        btnCalibCreate.addEventHandler(this, "newEditOrDeleteCalib");
        gAll.add(btnCalibCreate); gCalibrate.add(btnCalibCreate);

        files = new String[] { "images/edit.png","images/edit_hover.png","images/edit_click.png" };    // 120x30
        btnCalibEdit = new GImageButton(this, 0,0, files);
        btnCalibEdit.addEventHandler(this, "newEditOrDeleteCalib");
        gAll.add(btnCalibEdit); gCalibrate.add(btnCalibEdit);

        files = new String[] { "images/delete.png","images/delete_hover.png","images/delete_click.png" };    // 120x30
        btnCalibDelete = new GImageButton(this, 0,0, files);
        btnCalibDelete.addEventHandler(this, "newEditOrDeleteCalib");
        gAll.add(btnCalibDelete); gCalibrate.add(btnCalibDelete);

        txCalibName = new GTextField(this, 0,0, 180, 25);
        txCalibName.setText("NewProfile");
        gAll.add(txCalibName); gCalibrate.add(txCalibName);
        gTextFields.add(txCalibName);

        txMaterialName = new GTextField(this, 0,0, 180, 25);
        txMaterialName.setText("1_16in Galvanized Steel");
        gAll.add(txMaterialName); gCalibrate.add(txMaterialName);
        gTextFields.add(txMaterialName);

        //items = new String[] { "Select Bend Head" }; //should never show up
        dlBendHead = new GDropList(this, 0, 0, 175, 270, 8);
        //dlBendHead.setItems(items,0);
        dlBendHead.addEventHandler(this, "calibEvents");
        gAll.add(dlBendHead); gCalibrate.add(dlBendHead);

        //items = new String[] { "Select Feed Wheel" }; //should never show up
        dlFeedWheel = new GDropList(this, 0, 0, 175, 270, 8);
        //dlFeedWheel.setItems(items,0);
        dlFeedWheel.addEventHandler(this, "calibEvents");
        gAll.add(dlFeedWheel); gCalibrate.add(dlFeedWheel);


        txTypeIn = new GTextField(this, 0,0, 98, 23);
        txTypeIn.setText("");
        txTypeIn.addEventHandler(this, "handleTextEvents");
        gAll.add(txTypeIn); gCalibrate.add(txTypeIn);
        gTextFields.add(txTypeIn);
        txTypeIn.setVisible(false);

        optCalibHigh = new GOption(this, 0,0, 60, 20, "");    // no text - will add manually
        optCalibStandard = new GOption(this, 0,0, 60, 20, "");
        optCalibQuick = new GOption(this, 0,0, 60, 20, "");
        togCalibAccuracy = new GToggleGroup();
        togCalibAccuracy.addControls(optCalibHigh, optCalibStandard, optCalibQuick);
        optCalibStandard.setSelected(true);
        gAll.add(optCalibHigh); gCalibrate.add(optCalibHigh);
        gAll.add(optCalibStandard); gCalibrate.add(optCalibStandard);
        gAll.add(optCalibQuick); gCalibrate.add(optCalibQuick);

        files = new String[] { "images/start_calibration.png","images/start_calibration_hover.png","images/start_calibration_click.png" };    // 120x60
        btnStartCalib = new GImageButton(this, 0,0, files);
        btnStartCalib.addEventHandler(this, "startCalib");
        gAll.add(btnStartCalib); gCalibrate.add(btnStartCalib);

        files = new String[] { "images/okCalib.png","images/okCalib_hover.png","images/okCalib_click.png" };    // 90x90
        btnOkCalib = new GImageButton(this, 0,0, files);
        btnOkCalib.addEventHandler(this, "okCalib");
        gAll.add(btnOkCalib); gCalibrate.add(btnOkCalib);
        //btnOkCalib.setVisible(false);

        files = new String[] { "images/beginCalib.png","images/beginCalib_hover.png","images/beginCalib_click.png" };    // 90x90
        btnBeginCalib = new GImageButton(this, 0,0, files);
        btnBeginCalib.addEventHandler(this, "okCalib");
        gAll.add(btnBeginCalib); gCalibrate.add(btnBeginCalib);
        //btnBeginCalib.setVisible(false);

        files = new String[] { "images/cancelCalib.png","images/cancelCalib_hover.png","images/cancelCalib_click.png" };    // 120x30
        btnCancelCalib = new GImageButton(this, 0,0, files);
        btnCancelCalib.addEventHandler(this, "cancelCalib");
        gAll.add(btnCancelCalib); gCalibrate.add(btnCancelCalib);

        files = new String[] { "images/editCalibration.png","images/editCalibration_hover.png","images/editCalibration_click.png" };    // 120x60
        btnRefineCalib = new GImageButton(this, 0,0, files);
        btnRefineCalib.addEventHandler(this, "editCalib");
        gAll.add(btnRefineCalib); gCalibrate.add(btnRefineCalib);

        files = new String[] { "images/doneCalibration.png","images/doneCalibration_hover.png","images/doneCalibration_click.png" };    // 120x60
        btnDoneCalib = new GImageButton(this, 0,0, files);
        btnDoneCalib.addEventHandler(this, "editCalib");
        gAll.add(btnDoneCalib); gCalibrate.add(btnDoneCalib);

        files = new String[] { "images/ReCalib.png","images/ReCalib_hover.png","images/ReCalib_click.png" };    // 120x60
        btnReCalib = new GImageButton(this, 0,0, files);
        btnReCalib.addEventHandler(this, "editCalib");
        gAll.add(btnReCalib); gCalibrate.add(btnReCalib);

        files = new String[] { "images/RefineCalib.png","images/RefineCalib_hover.png","images/RefineCalib_click.png" };    // 120x60
        btnRefineRangeCalib = new GImageButton(this, 0,0, files);
        btnRefineRangeCalib.addEventHandler(this, "editCalib");
        gAll.add(btnRefineRangeCalib); gCalibrate.add(btnRefineRangeCalib);

        txLow = new GTextField(this, 0,0, 40, 25);
        txLow.setText("");
        gAll.add(txLow); gCalibrate.add(txLow);
        gTextFields.add(txLow);

        txHi = new GTextField(this, 0,0, 40, 25);
        txHi.setText("");
        gAll.add(txHi); gCalibrate.add(txHi);
        gTextFields.add(txHi);

        txSteps = new GTextField(this, 0,0, 40, 25);
        txSteps.setText("");
        gAll.add(txSteps); gCalibrate.add(txSteps);
        gTextFields.add(txSteps);




        // Manual Mode
        files = new String[] { "images/arrowUpBtn.png","images/arrowUpBtn_hover.png","images/arrowUpBtn_click.png" };    // 90x90
        btnArrowUp = new GImageButton(this, 0,0, files);
        btnArrowUp.fireAllEvents(true);
        btnArrowUp.addEventHandler(this, "arrowManual");
        gAll.add(btnArrowUp); gManual.add(btnArrowUp);

        files = new String[] { "images/arrowDownBtn.png","images/arrowDownBtn_hover.png","images/arrowDownBtn_click.png" };    // 90x90
        btnArrowDown = new GImageButton(this, 0,0, files);
        btnArrowDown.fireAllEvents(true);
        btnArrowDown.addEventHandler(this, "arrowManual");
        gAll.add(btnArrowDown); gManual.add(btnArrowDown);

        files = new String[] { "images/arrowLeftBtn.png","images/arrowLeftBtn_hover.png","images/arrowLeftBtn_click.png" };    // 90x90
        btnArrowLeft = new GImageButton(this, 0,0, files);
        btnArrowLeft.fireAllEvents(true);
        btnArrowLeft.addEventHandler(this, "arrowManual");
        gAll.add(btnArrowLeft); gManual.add(btnArrowLeft);

        files = new String[] { "images/arrowRightBtn.png","images/arrowRightBtn_hover.png","images/arrowRightBtn_click.png" };    // 90x90
        btnArrowRight = new GImageButton(this, 0,0, files);
        btnArrowRight.fireAllEvents(true);
        btnArrowRight.addEventHandler(this, "arrowManual");
        gAll.add(btnArrowRight); gManual.add(btnArrowRight);

        files = new String[] { "images/homeButton.png","images/homeButton_hover.png","images/homeButton_click.png" };    // 120x30
        btnHome = new GImageButton(this, 0,0, files);
        btnHome.addEventHandler(this, "homeManual");
        gAll.add(btnHome); gManual.add(btnHome);

//    files = new String[] { "images/arrow_static.png","images/arrow_hover.png","images/arrow_click.png" };    // 120x30
//    btnZeroRight = new GImageButton(this, 0,0, files);
//    btnZeroRight.addEventHandler(this, "zeroRight");
//    gAll.add(btnZeroRight); gManual.add(btnZeroRight);

//    files = new String[] { "images/arrow_static.png","images/arrow_hover.png","images/arrow_click.png" };    // 120x30
//    btnZeroLeft = new GImageButton(this, 0,0, files);
//    btnZeroLeft.addEventHandler(this, "zeroLeft");
//    gAll.add(btnZeroLeft); gManual.add(btnZeroLeft);

        optVeryFast = new GOption(this, 0,0, 60, 20, "");    // no text - will add manually
        optFast = new GOption(this, 0,0, 60, 20, "");    // no text - will add manually
        optSlow = new GOption(this, 0,0, 60, 20, "");
        optVerySlow = new GOption(this, 0,0, 60, 20, "");
        togJogSpeed = new GToggleGroup();
        togJogSpeed.addControls(optVeryFast, optFast, optSlow, optVerySlow);
        optFast.setSelected(true);
        optVeryFast.addEventHandler(this, "jogSpeedClicked");
        optFast.addEventHandler(this, "jogSpeedClicked");
        optSlow.addEventHandler(this, "jogSpeedClicked");
        optVerySlow.addEventHandler(this, "jogSpeedClicked");
        gAll.add(optVeryFast); gManual.add(optVeryFast);
        gAll.add(optFast); gManual.add(optFast);
        gAll.add(optSlow); gManual.add(optSlow);
        gAll.add(optVerySlow); gManual.add(optVerySlow);

        btnTest = new GButton(this, 0,0, 100,30, "TEST");
        btnTest.addEventHandler(this, "btnTest");
        //gAll.add(btnTest); gManual.add(btnTest);
        btnTest.setVisible(false);

        txTest = new GTextField(this, 0,0, 180, 25);
        txTest.setText("");
        txTest.addEventHandler(this, "handleTextEvents");
        gAll.add(txTest); gManual.add(txTest);
        gTextFields.add(txTest);

        txPosFeed = new GTextField(this, 0,0, 30, 22);
        txPosFeed.setText("");
        txPosFeed.addEventHandler(this, "handleManInputs");
        gAll.add(txPosFeed); gManual.add(txPosFeed);gManualInputs.add(txPosFeed);
        gTextFields.add(txPosFeed);

        txNegFeed = new GTextField(this, 0,0, 30, 22);
        txNegFeed.setText("");
        txNegFeed.addEventHandler(this, "handleManInputs");
        gAll.add(txNegFeed); gManual.add(txNegFeed);gManualInputs.add(txNegFeed);
        gTextFields.add(txNegFeed);

        txPosBend = new GTextField(this, 0,0, 30, 22);
        txPosBend.setText("");
        txPosBend.addEventHandler(this, "handleManInputs");
        gAll.add(txPosBend); gManual.add(txPosBend);gManualInputs.add(txPosBend);
        gTextFields.add(txPosBend);

        txNegBend = new GTextField(this, 0,0, 30, 22);
        txNegBend.setText("");
        txNegBend.addEventHandler(this, "handleManInputs");
        gAll.add(txNegBend); gManual.add(txNegBend);gManualInputs.add(txNegBend);
        gTextFields.add(txNegBend);



        // initialize the material property dropdown lists for Edit and Calibrate
        //  and load the 1st item in the edit list
        createBendAndFeedDropdowns();
        createMatProfDropdowns();
        setCurrentCalibration();



        // create menu using Java awt library
        // seems to work, although most Processing docs say this is not recommended
        MenuBar mb = new MenuBar();
        Menu m;
        MenuItem mi;
        MainMenuActionListener listener = new MainMenuActionListener();

        m = new Menu("File");
        mi = new MenuItem("Open");
        mi.addActionListener(listener);
        m.add(mi);
        mi = new MenuItem("Save");
        mi.addActionListener(listener);
        m.add(mi);
        mi = new MenuItem("Save As..");
        mi.addActionListener(listener);
        m.add(mi);
        mb.add(m);
        mi = new MenuItem("Close");
        mi.addActionListener(listener);
        m.add(mi);
        m.addSeparator();
        mi = new MenuItem("Exit");
        mi.addActionListener(listener);
        m.add(mi);
        mb.add(m);

        m = new Menu("Mode");
        mi = new MenuItem("Edit");
        mi.addActionListener(listener);
        m.add(mi);
        mi = new MenuItem("Manual");
        mi.addActionListener(listener);
        m.add(mi);
        mi = new MenuItem("Material Profiles");
        mi.addActionListener(listener);
        m.add(mi);
        mb.add(m);

        mbPoints = new Menu("Points");
        mi = new MenuItem("Delete");
        mi.addActionListener(listener);
        mbPoints.add(mi);
        mi = new MenuItem("Choose Start");
        mi.addActionListener(listener);
        mbPoints.add(mi);
        mi = new MenuItem("Reverse Direction");
        mi.addActionListener(listener);
        mbPoints.add(mi);
        mi = new MenuItem("Add Pause");
        mi.addActionListener(listener);
        mbPoints.add(mi);
        mi = new MenuItem("Remove Pause");
        mi.addActionListener(listener);
        mbPoints.add(mi);
        mb.add(mbPoints);
        mbPoints.setEnabled(false);

        frame.setMenuBar(mb);
    }

    // menu callback
    class MainMenuActionListener implements ActionListener  {
        public void actionPerformed (ActionEvent event)  {
            String cmd = event.getActionCommand();
            //println("actionPerformed() cmd = " + cmd);

            // File Menu
            if (cmd.equals("Open")) openFile();
            if (cmd.equals("Save")) if (displayState == DS_LOADED) saveSVG(false);
            if (cmd.equals("Save As..")) if (displayState == DS_LOADED) saveSVG(true);
            if (cmd.equals("Close")) closeFile();
            if (cmd.equals("Exit")) exit();

            // Mode Menu
            if (cmd.equals("Edit")) curViewMode = VIEW_EDIT;
            if (cmd.equals("Material Profiles"))  { curViewMode = VIEW_CALIBRATE; curCalibMode = CALIB_START; }
            if (cmd.equals("Manual")) curViewMode = VIEW_MANUAL;


            if (cmd.equals("Delete"))  { deletePoints(); }
            if (cmd.equals("Choose Start"))  { changeStartPoint(); }
            if (cmd.equals("Reverse Direction"))  { reverseArray(); }
            if (cmd.equals("Add Pause"))  { addPause(); }
            if (cmd.equals("Remove Pause"))  { removePause(); }

        }
    }


    class JSONFilter implements FilenameFilter  {
        public boolean accept (File dir, String name) { return name.endsWith(".json"); }
    }

    public void createBendAndFeedDropdowns ()  {
        debugC("GUI/creatBendAndFeedDropdowns()");
        File calibDir = new File(rootDataFolder + File.separator + "calibration");

        String[] bendItems = new String[0];
        String[] feedItems = new String[0];

        String[] bendFeedFileNames = calibDir.list(new JSONFilter());
        for (String fileName : bendFeedFileNames)  {
            int i;

            JSONObject bfjs = loadAndCloseJSONObject(getFullPathName(fileName));
            try  {
                JSONArray bends = bfjs.getJSONArray("Bend Head List");    // only in bend-feed files
                for (i = 0; i < bends.size(); ++i)  {
                    JSONObject b = bends.getJSONObject(i);
                    String bhn = b.getString("Bend Head Name");
                    bendItems = append(bendItems, bhn);
                    mapBH2Dia.put(bhn, b.getFloat("Diameter"));
                    Float[] p2w={b.getFloat("P2W1"),b.getFloat("P2W2"),b.getFloat("P2W3"),b.getFloat("P2W4"),b.getFloat("P2W5"),b.getFloat("P2W6")};
                    Float[] w2p={b.getFloat("W2P1"),b.getFloat("W2P2"),b.getFloat("W2P3"),b.getFloat("W2P4"),b.getFloat("W2P5"),b.getFloat("W2P6")};
                    mapBH2P2W.put(bhn, p2w);
                    mapBH2W2P.put(bhn, w2p);
                    Float[] BHcornerComp = {b.getFloat("Diameter"), b.getFloat("Mandrel Radius"), b.getFloat("Mandrel Offset")};
                    println(BHcornerComp);
                    mapBHcornerComp.put(bhn, BHcornerComp);
                }
                JSONArray feeds = bfjs.getJSONArray("Feed Wheel List");    // only in bend-feed files
                for (i = 0; i < feeds.size(); ++i)  {
                    JSONObject f = feeds.getJSONObject(i);
                    feedItems = append(feedItems, f.getString("Feed Wheel Name"));
                    feedDiam.put(f.getString("Feed Wheel Name"),f.getFloat("Diameter"));
                    //println("Feed: " + f.getString("Feed Wheel Name"));
                }
            } catch (Exception e)  {
                //appendTextToFile(errorFilename, "createBendAndFeedDropdowns(): "+e.getMessage());
            }
        }

        dlBendHead.setItems(bendItems, 0);

        dlFeedWheel.setItems(feedItems, 0);
//           String bhs = cjs.getString("Bend Head");
//        while (i < 100)  {
//            dlBendHead.setSelected(i);
//            if (dlBendHead.getSelectedText().equals(bhs)) break;
//            ++i;
//        }
//        if (i >= 100)
//            println("ERROR: Could not find Bend Head " + bhs + " in drop down list");
//        i = 0;
//
//        String fws = cjs.getString("Feed Wheel");
//        while (i < 100)  {
//            dlFeedWheel.setSelected(i);
//            if (dlFeedWheel.getSelectedText().equals(fws)) break;
//            ++i;
//        }
//        if (i >= 100)
//            println("ERROR: Could not find Feed Wheel " + fws + " in drop down list");

        updateBHFormula();
        updateFWFormula();
    }

    // populate the dropdown lists for the Material Properties dropdowns for Edit and Calibrate modes
    public void createMatProfDropdowns ()  {
        String[] items = buildMatProfList();
        //println("curMatProfList = " + curMatProfList);

        dlMatProfEdit.setItems(items, 0);

        // add create new profile to top of calibration drop-down
        // insertItem() doesn't seem to work
        String[] calibList = { "Create New Profile" };
        for (int i = 0; i < items.length; ++i)
            calibList = append(calibList, items[i]);
        dlMatProfCalib.setItems(calibList, 0);
    }

    // build the Material Profile list by searching the calibration directory
    public String[] buildMatProfList ()  {
        String items[] = new String[0];
        nCalibrations = 0;

//    if (addNewOption)  {
//        items = append(items, "Create New Profile");
//    }

        File calibDir = new File(rootDataFolder + File.separator + "calibration");
        String[] calibFileNames = calibDir.list(new JSONFilter());
        for (String fileName : calibFileNames)  {
            // try to read a nice name from the file
            // if we do this, we need a way to connect the nice name back to the filename
//        JSONObject cjs = loadJSONObject(rootDataFolder + File.separator + "calibration" + File.separator + fileName);
//        try  {
//            String niceName = cjs.getString("NiceName");
//            items = append(items, niceName);
//        } catch (Exception ex) {
//            //println("ex = " + ex);
//            String fName = fileName.replace(".json", "");
//            items = append(items, fName);
//        }

            // just use filename for now
            // check that this is a calibration file, not a JSON file containing bend head and/or feed wheel parameters
            JSONObject cjs = loadAndCloseJSONObject(getFullPathName(fileName));
            try  {
                cjs.getString("Profile Name");    // only in calib files
                String fName = fileName.replace(".json", "");
                items = append(items, fName);
                ++nCalibrations;
            } catch (Exception e)  {
                //appendTextToFile(errorFilename, "buildMatProfList(): "+e.getMessage());
            }
        }

        // make sure something's there - this shouldn't ever happen
        if (nCalibrations == 0)  {
            items = append(items, "No Material Profiles");
            println("ERROR: No Materile Profile files found in " + calibDir.getAbsolutePath());
        }

        return items;
    }


    // called when material profile dropdown list changes - either in Edit or Calibrate modes
    public void matMenuEvents (GDropList list, GEvent event)  {
        println("matMenuEvents()");
        if (list == dlMatProfEdit){
            // we've changed profiles, so calculate new poly fit - should be saved in calib file
            //calculateAngle2MMPolyFit();

            setCurrentCalibration();
            saveDefaults();
            updateViewMode();

        }

        else  {
            if (dlMatProfCalib.getSelectedIndex() == 0)  {
                btnCalibCreate.setVisible(true);
                btnCalibEdit.setVisible(false);
                btnCalibDelete.setVisible(false);
            } else  {
                btnCalibCreate.setVisible(false);
                btnCalibEdit.setVisible(true);
                btnCalibDelete.setVisible(true);
            }
        }
    }
    public void calibEvents (GDropList list, GEvent event)  {
        updateBHFormula();
        updateFWFormula();
    }


    public void updateBHFormula ()  {
        debugC("updateBHFormula()");
        pinAng2wireAng=mapBH2P2W.get(dlBendHead.getSelectedText());
        wireAng2pinAng=mapBH2W2P.get(dlBendHead.getSelectedText());
        //println(pinAng2wireAng[0]);
        Float[] BHcornerComp = mapBHcornerComp.get(dlBendHead.getSelectedText());
        wireDiam = BHcornerComp[0]*25.4f; //converted to mm
        mandrelRad = BHcornerComp[1];
        feedAdjustStartEnd = BHcornerComp[2];
        calculateFeedsAndBendAngles();
    }

    public void updateFWFormula ()  {

//    JSONObject Mot2Settings = new JSONObject();
//    Mot2Settings.setFloat("tr", yrev);
//    JSONObject Mot2Json = new JSONObject();
//    Mot2Json.setJSONObject("2", Mot2Settings);
//    String m2 = Mot2Json.toString();
//    m2 = m2.replaceAll("\\s+", "");
//    TinyGSafeWrite(m2+'\n');

    }


    // is this automatically set up as the default handler for all buttons?
// now using only to handle buttons on "popup" error window
    public void handleButtonEvents (GImageButton button, GEvent event)  {
        println("*** handleButtonEvents() ***");
        if (button == btnCancelSendError)  {
            displayState = DS_LOADED;
        } else if (button == btnSerialError)  {
            displayState = DS_LOADED;
        } else if (button == btnMidBendError)  {
            sendPaused = true;
            displayState = DS_SENDING;
        } else if (button == btnStopSend)  {
            displayState = DS_LOADED;
            sendPaused = false;
        } else if (button == btnIgnoreError)  {
            startBending();
        } else if (button == btnBackOffLimitError)  {
//      tinyGFlashReady=false;
//      if (limitTripped>0 && limitTripped!=99){
//          TinyGSafeWrite("m03"+'\n'+"g0x10"+'\n');
//      }
//      else{
//          TinyGSafeWrite("m03"+'\n'+"g0x-10"+'\n');
//      }
//      limitTripped=99;
//      delay(500);
//      if (limitTripped==99 && tinyGFlashReady){
//          btnBackOffLimitError.setEnabled(false);
//          tinyGHome();
//          displayState=DS_SENDING;
//          sendPaused=true;
//      }

            displayState = DS_SENDING;
            sendPaused = true;
        }

        // hide all popup window buttons
        btnIgnoreError.setVisible(false);
        btnCancelSendError.setVisible(false);
        btnMidBendError.setVisible(false);
        btnSerialError.setVisible(false);
        btnBackOffLimitError.setVisible(false);
    }

    // start sending bend angles and feed lengths to the arduino board for printing
    public void beginSend (GImageButton button, GEvent event)  {
        //println("beginSend()");
        if (txtFileLoaded){
            startBending();
        }
        else if (curPath == -1)  {
            println("ERROR: No path selected to bend");
            return;
        }
        else{
            //printArrays();
            //button.setEnabled(false);    // BEND button gets hidden - either don't do this or re-enable when done bending

            WirePoints wps = curWirePoints.get(curPath);
            if (wps.nAngleWarnings != 0 || wps.nFeedWarnings != 0)  {
                popUpWarning = WARNING_FEED;
                displayState = DS_WARNING;
            } else {
                startBending();
            }

            //button.setEnabled(true);    // need to do this somewhere or BEND button won't work after first bend

        }

    }


    public void itbFrame (GImageButton button, GEvent event)  {
        frameModel(false);
    }

    public void itbZoomOut(GImageButton button, GEvent event)  {
        zoom(0, 0, -1);
    }


    public void pauseSend (GImageButton button, GEvent event)  {
        //println("********************* PAUSE");
        sendPaused = true;
        btnPauseSend.setVisible(false);
        btnResumeSend.setVisible(true);
        btnResumeSend.setEnabled(true);
    }
    public void pauseSendKey ()  {
        //println("********************* PAUSE");
        sendPaused = true;
        btnPauseSend.setVisible(false);
        btnResumeSend.setVisible(true);
        btnResumeSend.setEnabled(true);
    }

    public void resumeSend (GImageButton button, GEvent event)  {
        //println("********************* RESUME");
        tinyGCommandReady = true;
        //tinyGHome();
        sendPaused = false;
        btnResumeSend.setVisible(false);
        btnPauseSend.setVisible(true);
        btnPauseSend.setEnabled(true);
    }
    public void resumeSendKey ()  {
        //println("********************* RESUME");
        tinyGCommandReady = true;
        //tinyGHome();
        sendPaused = false;
        btnResumeSend.setVisible(false);
        btnPauseSend.setVisible(true);
        btnPauseSend.setEnabled(true);
    }

    public void stopSend (GImageButton button, GEvent event)  {
        //println("********************* STOP");
        //if (displayState==DS_HOMING){
        TinyGSafeWrite("!%"+'\n');     // clear commands
        //}
        displayState = DS_LOADED;
        sendPaused = false;
        doneBendingGUI();
    }

    public void doneBendingGUI ()  {
        btnBeginSend.setVisible(true);
        btnPauseSend.setVisible(false);
        btnResumeSend.setVisible(false);
        btnStopSend.setVisible(false);
    }


    // Either Create or Edit button clicked in CALIB_START or Delete
    public void newEditOrDeleteCalib (GImageButton imagebutton, GEvent event)  {
        debugC("GUI/newEditOrDeleteCalib()");
        if (imagebutton == btnCalibDelete)  {
            // delete selected profile, update drop down lists
            deleteCalibration();
            btnCalibDelete.setVisible(false);
        } else  {
            if (dlMatProfCalib.getSelectedIndex() == 0)  {  // Create New Profile is always first
                curCalibMode = CALIB_NEW;
            } else  {
                loadCalibrationData();
                curCalibMode = CALIB_EDIT;
            }
        }

        printCalibStatus();
    }

    // debugging
    public void printCalibStatus ()  {
        switch(curCalibState)  {
            case CALIB_STATE_NONE: print("CALIB_STATE_NONE"); break;
            case CALIB_STATE_STARTING: print("CALIB_STATE_STARTING"); break;
            case CALIB_STATE_FEEDING: print("CALIB_STATE_FEEDING"); break;
            case CALIB_STATE_READYTOBEND: print("CALIB_STATE_READYTOBEND"); break;
            case CALIB_STATE_BENDING: print("CALIB_STATE_BENDING"); break;
            case CALIB_STATE_WAITING: print("CALIB_STATE_WAITING"); break;
            case CALIB_STATE_SKIP_POINT: print("CALIB_STATE_SKIP_POINT"); break;
            case CALIB_STATE_FINISHED: print("CALIB_STATE_FINISHED"); break;
        }
        print(" ");
        switch (curCalibDir)  {
            case CALIB_DIR_NONE: print("CALIB_DIR_NONE"); break;
            case CALIB_DIR_CW: print("CALIB_DIR_CW"); break;
            case CALIB_DIR_CCW: print("CALIB_DIR_CCW"); break;
        }
        print(" " + curCalibIdx + " ");
        switch (curCalibMode)  {
            case CALIB_NONE: print("CALIB_NONE"); break;
            case CALIB_START: print("CALIB_START"); break;
            case CALIB_NEW: print("CALIB_NEW"); break;
            case CALIB_EDIT: print("CALIB_EDIT"); break;
            case CALIB_REFINE: print("CALIB_REFINE"); break;
            case CALIB_RUNNING: print("CALIB_RUNNING"); break;
        }
        println("");
    }

    // Start Calibration button clicked on in CALIB_NEW
// or OK button clicked in CALIB_REFINE
    public void startCalib (GImageButton imagebutton, GEvent event)  {
        startCalib();
    }

    public void startCalib ()  {
        //println("startCalib()");

        // check validity of entries in input fields

        // get filename for new calibration
        // now a JSON file
        curWorkingCalibFileName = txCalibName.getText() + ".json";
        curWorkingCalibPathName = getFullPathName(curWorkingCalibFileName);

        // erases the calibration file (if it exists) and creates new empty one
        if (curCalibMode == CALIB_NEW)
            clearCalibration();

        // check to see if there are any points to calibrate
        boolean foundCalibPt = false;
        int i;
        for (i = 0; i < posCalPts.length; ++i)
            if (posCalRes[i] == NO_CALIBRATION) foundCalibPt = true;
        for (i = 0; i < negCalPts.length; ++i)
            if (negCalRes[i] == NO_CALIBRATION) foundCalibPt = true;

        if (foundCalibPt)  {
            curCalibIdx = -1;
            curCalibDir = CALIB_DIR_NONE;
            curCalibState = CALIB_STATE_STARTING;
        } else  {
            curCalibState = CALIB_STATE_FINISHED;
            finishCalibration();
        }

        // these button states will be set automatically in updateViewMode when switching to CALIB_RUNNING
        //btnBeginCalib.setVisible(true);
        //btnOkCalib.setVisible(false);

        curCalibMode = CALIB_RUNNING;
        printCalibStatus();
    }


    // OK button in calibration - also handles Begin button
    public void okCalib (GImageButton imagebutton, GEvent event)  {
        okCalib();
    }

    public void okCalib ()  {
        //println("okCalib()");
        //println("okCalib() curCalibState = " + curCalibState + " curCalibDir = " + curCalibDir + " curCalibIdx = " + curCalibIdx);

        if (curCalibMode == CALIB_REFINE)  {
            startCalib();
        } else if (curCalibState == CALIB_STATE_STARTING)  {
            // see if we are starting calibration process
            curCalibIdx = 0;
            curCalibDir = CALIB_DIR_CW;

            // reverse button states manually here
            btnBeginCalib.setVisible(false);
            btnOkCalib.setVisible(true);

            calibFeedAmt = 40.0f;
            if (optCalibHigh.isSelected()) calibFeedAmt = 20.0f;

            tinyGHome();    // home axis before starting calibration
            sendFeed(calibFeedAmt);    // feed out some wire to bend
            curCalibState = CALIB_STATE_FEEDING;
        } else if (curCalibState == CALIB_STATE_WAITING || curCalibState == CALIB_STATE_SKIP_POINT)  {
            // record calibration and move to next point
            if (curCalibDir == CALIB_DIR_CW)  { // todo check older version method
                if (curCalibState == CALIB_STATE_WAITING)  {
                    if (posCalPts[curCalibIdx] == 0.0f)  {
                        // contact position
                        calibXContactPos = lastPositionX;
                        posCalRes[curCalibIdx] = 0.0f;
                        println("calibXContactPos = " + calibXContactPos);
                    } else
                        posCalRes[curCalibIdx] = pinMM2WireAng(lastPositionX);
                }

                ++curCalibIdx;
                if (curCalibIdx >= posCalPts.length)  {
                    // done with positive points, extend more wire to start on negative points
                    curCalibIdx = 0;
                    curCalibDir = CALIB_DIR_CCW;

                    // can't home with wire sticking out - will get caught
                    sendBend(xHomePos, true);    // move the pin out of the way before we feed
                    sendFeed(calibFeedAmt);
                    sendBend(xHomeNeg, true);
                    curCalibState = CALIB_STATE_FEEDING;
                }
            } else  {
                if (curCalibState == CALIB_STATE_WAITING)  {
                    if (negCalPts[curCalibIdx] == 0.0f)  {
                        // contact position
                        calibXContactNeg = lastPositionX;
                        negCalRes[curCalibIdx] = 0.0f;
                        println("calibXContactNeg = " + calibXContactNeg);
                    } else
                        negCalRes[curCalibIdx] = -pinMM2WireAng(lastPositionX);
                }

                ++curCalibIdx;
                if (curCalibIdx >= negCalPts.length)  {
                    sendBend(xHomeNeg, true);
                    curCalibState = CALIB_STATE_FINISHED;
                    curCalibIdx = -1;

                    // close, write and process new calibration file
                    // add new calibration file to material properties drop-downs
                    // when we go back to start, newly created calib file should be selected
                    finishCalibration();
                }
            }

            // if we didn't change state then do next bend
            if (curCalibState == CALIB_STATE_WAITING)  {
                // for High accuracy mode, do a feed between every bend
                if (optCalibHigh.isSelected())  {
                    if (curCalibDir == CALIB_DIR_CW)
                        sendBend(xHomePos, true);
                    else
                        sendBend(xHomeNeg, true);
                    sendFeed(calibFeedAmt);
                    curCalibState = CALIB_STATE_FEEDING;
                } else
                    curCalibState = CALIB_STATE_READYTOBEND;
            }

            if (curCalibState == CALIB_STATE_SKIP_POINT)
                curCalibState = CALIB_STATE_READYTOBEND;

        } else if (curCalibState == CALIB_STATE_FINISHED)  {
            // go back to start of calibration mode

            curCalibMode = CALIB_START;
        }

        printCalibStatus();
    }

    //
    public void cancelCalib (GImageButton imagebutton, GEvent event)  {
        //println("cancelCalib()");
        curCalibMode = CALIB_START;    // go back to start of calib
        printCalibStatus();
    }

    public void editCalib (GImageButton imagebutton, GEvent event)  {
        //println("editCalib()");
        if (imagebutton == btnRefineCalib)  {
            curCalibMode = CALIB_REFINE;
        } else if (imagebutton == btnDoneCalib)  {
            // save calibration file
            //curCalibMode = CALIB_START;    // go back to start of calib
            // start the calibration process - this should jump to writing out the calib file, since there are no points to calib
            startCalib();
        } else if (imagebutton == btnReCalib)  {
            // clear the calibration
            int i;
            for (i = 0; i < posCalRes.length; ++i)
                posCalRes[i] = NO_CALIBRATION;
            for (i = 0; i < negCalRes.length; ++i)
                negCalRes[i] = NO_CALIBRATION;
        } else if (imagebutton == btnRefineRangeCalib)  {
            float low = -1;
            float hi = -1;
            int steps = -1;
            try  {
                low = Float.parseFloat(txLow.getText().trim());
                hi = Float.parseFloat(txHi.getText().trim());
                steps = Integer.parseInt(txSteps.getText().trim());
            } catch (Exception e)  {
                appendTextToFile(errorFilename, "editCalib(): "+e.getMessage());
            }
            //println("low = " + low + " hi = " + hi + " steps = " + steps);
            if (steps == -1)  {
                println("ERROR: Need to specify Low, Hi and Steps values");
            } else  {
                // add range to calibration points, maintaining sorted order
                float[] newPosCalPts = new float[0];
                float[] newPosCalRes = new float[0];
                int oi = 0;
                float fstep = 10000000;
                if (steps >= 1) fstep = (hi-low)/steps;
                for (float a = low; a <= hi+.001f; a += fstep)  {
                    while (oi < posCalPts.length && posCalPts[oi] < (a-.01f))  {
                        newPosCalPts = append(newPosCalPts, posCalPts[oi]);
                        newPosCalRes = append(newPosCalRes, posCalRes[oi]);
                        ++oi;
                    }
                    if (oi >= posCalPts.length || abs(posCalPts[oi] - a) > .05f)  {
                        newPosCalPts = append(newPosCalPts, a);
                        newPosCalRes = append(newPosCalRes, NO_CALIBRATION);
                    }
                }
                while (oi < posCalPts.length)  {
                    newPosCalPts = append(newPosCalPts, posCalPts[oi]);
                    newPosCalRes = append(newPosCalRes, posCalRes[oi]);
                    ++oi;
                }
                posCalPts = newPosCalPts;
                posCalRes = newPosCalRes;

                float[] newNegCalPts = new float[0];
                float[] newNegCalRes = new float[0];
                oi = 0;
                for (float a = low; a <= hi+.001f; a += fstep)  {
                    while (oi < negCalPts.length && negCalPts[oi] < (a-.01f))  {
                        newNegCalPts = append(newNegCalPts, negCalPts[oi]);
                        newNegCalRes = append(newNegCalRes, negCalRes[oi]);
                        ++oi;
                    }
                    if (oi >= negCalPts.length || abs(negCalPts[oi] - a) > .05f)  {
                        newNegCalPts = append(newNegCalPts, a);
                        newNegCalRes = append(newNegCalRes, NO_CALIBRATION);
                    }
                }
                while (oi < negCalPts.length)  {
                    newNegCalPts = append(newNegCalPts, negCalPts[oi]);
                    newNegCalRes = append(newNegCalRes, negCalRes[oi]);
                    ++oi;
                }
                negCalPts = newNegCalPts;
                negCalRes = newNegCalRes;
            }
        }
        printCalibStatus();
    }


    // arrow buttons in Manual Mode
    public void arrowManual (GImageButton imagebutton, GEvent event)  {
        if (manInputs){
            if (event == GEvent.CLICKED || event == GEvent.RELEASED) {
                if (imagebutton == btnArrowUp){
                    float d=PApplet.parseFloat(txPosFeed.getText());
                    if (!Float.isNaN(d)) {
                        if (curUnits == UNITS_INCH){
                            d*=25.4f;
                        }
                        sendFeed(d);
                    }
                    //txPosFeed.setText("");
                }
                else if (imagebutton == btnArrowDown){
                    float d=-1.0f*(PApplet.parseFloat(txNegFeed.getText()));
                    if (!Float.isNaN(d)) {
                        if (curUnits == UNITS_INCH){
                            d*=25.4f;
                        }
                        sendFeed(d);
                    }
                    //txNegFeed.setText("");
                }
                else if (imagebutton == btnArrowLeft){
                    float d=PApplet.parseFloat(txNegBend.getText());
                    if (!Float.isNaN(d)) {
                        sendAngle(-1.0f*abs(d));
                    }
                    //txNegBend.setText("");
                }
                else if (imagebutton == btnArrowRight){
                    float d=PApplet.parseFloat(txPosBend.getText());
                    if (!Float.isNaN(d)) {
                        sendAngle(d);
                    }
                    //txPosBend.setText("");
                }
            }

        }
        else{
            // oddly when the button is first pressed you get a PRESSED event
            // when it is released on the button you get a CLICKED event and released off the button, get a RELEASED event
            if (event == GEvent.CLICKED || event == GEvent.RELEASED)
                stopJog();
            else if (event == GEvent.PRESSED)  {
                if (imagebutton == btnArrowUp)
                    startJog(JOGDIR_FORWARD);
                else if (imagebutton == btnArrowDown)
                    startJog(JOGDIR_BACKWARD);
                else if (imagebutton == btnArrowLeft)
                    startJog(JOGDIR_CCW);
                else if (imagebutton == btnArrowRight)
                    startJog(JOGDIR_CW);
            }
        }
    }

    //
    public void homeManual (GImageButton imagebutton, GEvent event)  {
        homeJog();
    }

    public void btnTest (GButton button, GEvent event)  {
        doTest();
    }

    public void jogSpeedClicked (GOption source, GEvent event)  {
        if (source == optVeryFast)
            curJogSpeed = JOGSPEED_VERY_FAST;
        else if (source == optFast)
            curJogSpeed = JOGSPEED_FAST;
        else if (source == optSlow)
            curJogSpeed = JOGSPEED_SLOW;
        else if (source == optVerySlow)
            curJogSpeed = JOGSPEED_VERY_SLOW;
    }



    // "Close Gaps" button clicked
    public void closeGaps(GImageButton imagebutton, GEvent event)  {
        if (isDXFFile)  {
            CloseGapsInDXF(sldGapThresh.getValueF());
            loadWireObjectFile(curFile);
            isDXFFile = true;    // set this back after load sets it to false
            // sldGapThresh.setVisible(true); //keeping gap threshold hidden
            // btnCloseGaps.setVisible(true); //keeping close gaps button hidden
        } else
            println("WARNING: Can't do Close Gaps on non-DXF file");
    }


    // "Preview" here means whether to draw the underlying SVG
    public void previewClicked (GCheckbox source, GEvent event)  {
        //println("previewClicked()");
        GUIDrawSVG = !GUIDrawSVG;
        displayNeedsUpdate = true;
        if (GUIDrawSVG)  {
            cbPreview.setIcon("images/toggle_off.png", 1, GAlign.LEFT, null);
        } else  {
            cbPreview.setIcon("images/toggle_on.png", 1, GAlign.LEFT, null);
        }
    }

    public void handleSamplingChange (GDropList droplist, GEvent event)  {
        //println("handleSamplingChange()");
        generateWirePoints();
        calculateFeedsAndBendAngles();
        displayNeedsUpdate = true;
    }

    float prevGUIScale = 1.0f;
    public void sliderEvent (GCustomSlider source, GEvent event)  {
        if (source == sldScale)  {
            GUIScale = sldScale.getValueF();
            txScale.setText(str((sldScale.getValueF()))); //removed round() Cbarbre 10222015

            if (curShape != null)  {
                curShape.scale(GUIScale / prevGUIScale);

                // update internal geomtery
                // maybe we shouldn't do this stuff until the user releases the mouse or something
                generateWirePoints();
                calculateFeedsAndBendAngles();

                prevGUIScale = GUIScale;
            }

            displayNeedsUpdate = true;

        } else if (source == sldResolution)  {
            GUIRes = sldResolution.getValueF();
            txResolution.setText(str(round(sldResolution.getValueF())));

            if (curShape != null)  {
                // update internal geomtery
                // maybe we shouldn't do this stuff until the user releases the mouse or something
                generateWirePoints();
                calculateFeedsAndBendAngles();
            }

            displayNeedsUpdate = true;
        }
    }

    // handle type-in events in text fields
// needs handler for Wire Length text field
    public void handleTextEvents (GTextField source, GEvent event)  {
        println("handleTextEvents()");
        println("  source = " + source);
        println("  event.toString() = " + event.toString());
        if (source == txWireLength && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            float pcl = PApplet.parseFloat(txWireLength.getText());
            if (prevPcl <= 0.0f) println("ERROR: Bad previous wire length!");
            float txs = (pcl / prevPcl) * sldScale.getValueF();
            //println("txWireLength: pcl = " + pcl + " prevPcl = " + prevPcl + " txs = " + txs);
            if (txs >= GUIScaleLimits[0] && txs <= GUIScaleLimits[1])  {
                sldScale.setValue(txs);
            } else  {
                println("Error: Scale value out of range");
                txScale.setText(str(round(sldScale.getValueF())));
            }
            prevPcl = pcl;
        } else if (source == txScale && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            float txs = PApplet.parseFloat(txScale.getText());
            if (txs >= GUIScaleLimits[0] && txs <= GUIScaleLimits[1])  {
                sldScale.setValue(txs);
            } else  {
                println("Error: Scale value out of range");
                txScale.setText(str((sldScale.getValueF()))); // removed round() CBarbre 10222015
            }
        } else if (source == txResolution && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            float txr = PApplet.parseFloat(txResolution.getText());
            if (txr >= GUIResolutionLimits[0] && txr <= GUIResolutionLimits[1])  {
                sldResolution.setValue(txr);
            } else  {
                println("Error: Resolution value out of range");
                txResolution.setText(str(sldResolution.getValueF()));
            }

        } else if (source == txKfactor && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            float txs = PApplet.parseFloat(txKfactor.getText());
            if (txs < GUIkFactorLimits[0] || txs > GUIkFactorLimits[1])  {
                println("Error: Scale value out of range");
                txKfactor.setText(str(GUIkFactorLimits[2]));
                txs = GUIkFactorLimits[2];
            }
            kFactor = txs;
            updateBHFormula();

        } else if (source == txRepeat && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            float tp = PApplet.parseFloat(txRepeat.getText());
            println(tp);

            if (Float.isNaN(tp)) {
                txRepeat.setText("1");    // test for invalid or empty value
                repeatQty=1;
            }
            else
                repeatQty=PApplet.parseInt(tp);
        }

        else if (source == txTypeIn && (event == GEvent.LOST_FOCUS || event ==  GEvent.ENTERED))  {
            if (curCalibTypeInDir != CALIB_DIR_NONE)  {    // prevent loop
                float txti = PApplet.parseFloat(txTypeIn.getText());
                if (Float.isNaN(txti))
                    txti = NO_CALIBRATION;    // test for invalid or empty value

                if (curCalibTypeInDir == CALIB_DIR_CW)
                    posCalRes[curCalibTypeInIdx] = txti;
                else
                    negCalRes[curCalibTypeInIdx] = txti;

                curCalibTypeInDir = CALIB_DIR_NONE;
                txTypeIn.setFocus(false);
                txTypeIn.setVisible(false);    // this causes event with LOST_FOCUS, creating infinite loop
            }
        } else if (source == txTest && (event ==  GEvent.ENTERED))  {
            TinyGSafeWrite(txTest.getText() + '\n');
        }
    }

    public void handleManInputs (GTextField source, GEvent event)  {

        if (source == txNegFeed && (event ==  GEvent.ENTERED))  {
            float d=PApplet.parseFloat(txNegFeed.getText());
            if (!Float.isNaN(d)) {
                if (curUnits == UNITS_INCH){
                    d*=25.4f;
                }
                sendFeed(-1*abs(d));
            }
        }
        else if (source == txPosFeed && (event ==  GEvent.ENTERED))  {
            float d=PApplet.parseFloat(txPosFeed.getText());
            if (!Float.isNaN(d)) {
                if (curUnits == UNITS_INCH){
                    d*=25.4f;
                }
                sendFeed(abs(d));
            }
        }
        else if (source == txNegBend && (event ==  GEvent.ENTERED))  {
            float d=PApplet.parseFloat(txNegBend.getText());
            if (!Float.isNaN(d)) {
                sendAngle(-1.0f*abs(d));
            }

        }
        else if (source == txPosBend && (event ==  GEvent.ENTERED))  {
            float d=PApplet.parseFloat(txPosBend.getText());
            if (!Float.isNaN(d)) {
                sendAngle(abs(d));
            }

        }




    }

    public void unitsClicked (GOption source, GEvent event)  {
        if (source == optMM)
            curUnits = UNITS_MM;
        else if (source == optIn)
            curUnits = UNITS_INCH;
    }

    public void optInputClicked (GOption source, GEvent event)  {
        if (source == optNoInput)
            manInputs = false;
        else if (source == optYesInput)
            manInputs = true;
        if (manInputs){
            for (GAbstractControl g: gManualInputs)
                g.setVisible(true);
        }
        else{
            for (GAbstractControl g: gManualInputs)
                g.setVisible(false);
        }
    }


    // reset GUI elements to defaults - used when loading a new file
    public void resetGUI ()  {
        prevGUIScale = GUIScale = 1.0f; //GUIScaleLimits[2];
        sldScale.setValue(GUIScale);
        GUIRes = GUIResolutionLimits[2];
        sldResolution.setValue(GUIRes);
        GUIDrawSVG = true;
        cbPreview.setIcon("images/toggle_off.png", 1, GAlign.LEFT, null);
        GUIDrawSegments = true;
        GUIDrawPoints = true;
        curUnits = UNITS_MM;
        optMM.setSelected(true);
        //splitMode = SPLIT_CURVE;
    }


    public void mouseSelect(GImageToggleButton button, GEvent event)  {
        for (GImageToggleButton g: gMouseSelect)
            g.stateValue(0);

        button.stateValue(1);
        if (button==itbArrow) mouseMode=MOUSE_POINTER;
        else if (button==itbZoomIn) {
            mouseMode=MOUSE_ZIN;
            cursor(zoomCursor);
        }
        //else if (button==itbZoomOut) mouseMode=MOUSE_ZOUT;
        else if (button==itbHand) mouseMode=MOUSE_HAND;

    }

    // generic GUI handlers
    public void handleToggleControlEvents (GToggleControl checkbox, GEvent event)  { /* code */ }

    public void handleSliderEvents (GValueControl slider, GEvent event)  { /* code */ }

    public void handleToggleButtonEvents(GImageToggleButton button, GEvent event)  { /* code */ }

    public void handleDropListEvents(GDropList list, GEvent event) { /* code */ }

    public void handleButtonEvents(GButton button, GEvent event) { /* code */ }
/*
    mathematical computations

 */

    // compute the feed lengths and bend angles necessary to bend the wire to the current shape,
//  with the current point set
    public void calculateFeedsAndBendAngles () {
        debugC("Math/calculateFeedsAndBendAngles()");

        int i, j;

        // compute for each path
        for (i = 0; i < curWirePoints.size (); ++i) {
            WirePoints wps = curWirePoints.get(i);

            float lastAng = 0;
            float lastlastAng = 0;

            // reset counters
            wps.nAngleWarnings = 0;
            wps.nFeedWarnings = 0;

            wps.angles.clear();
            wps.feeds.clear();

            WirePoint wp0, wp1, wp2;

            if (wps.pts.size()>2) {
                // calculate feed lengths
                wp0 = wps.pts.get(0);
                wp1 = wps.pts.get(wps.pts.size()-1);

                if (abs(dist(wp0.pt.x, wp0.pt.y, wp1.pt.x, wp1.pt.y))<5)
                    wps.curveIsClosed=true;

                for (j = 1; j < wps.pts.size (); ++j) {
                    //feed lengths are the distances between points
                    wp1 = wps.pts.get(j);
                    wps.feeds.append(dist(wp0.pt.x, wp0.pt.y, wp1.pt.x, wp1.pt.y));
                    wp0 = wp1;
                }

                // calulate bend angles - also adjust feed lengths
                wp0 = wps.pts.get(0);
                wp1 = wps.pts.get(1);
                for (j = 2; j < wps.pts.size (); ++j) {
                    float angle = 0;

                    wp2 = wps.pts.get(j);

                    float dist1A = dist(wp0.pt.x, wp0.pt.y, wp1.pt.x, wp1.pt.y);
                    float dist1B = dist(wp1.pt.x, wp1.pt.y, wp2.pt.x, wp2.pt.y);
                    float dist1C = dist(wp0.pt.x, wp0.pt.y, wp2.pt.x, wp2.pt.y);
                    float dia= circleDiameter(wp0.pt.x, wp0.pt.y, wp1.pt.x, wp1.pt.y, wp2.pt.x, wp2.pt.y);

                    if (dist1A+dist1B != dist1C) {
                        float ang1 = acos((sq(dist1B)+sq(dist1A)-sq(dist1C)) / (2*dist1B*dist1A));

                        float dx1 = wp0.pt.x - wp1.pt.x;        //change in x axis between points
                        float dy1 = wp0.pt.y - wp1.pt.y;
                        float dx2 = wp2.pt.x - wp1.pt.x;        //change in x axis between points
                        float dy2 = wp2.pt.y - wp1.pt.y;

                        int feedQuad = 0;
                        int bendQuad = 0;
                        boolean pos = true;
                        if (dx1 >= 0 && dy1 >= 0) {
                            bendQuad = 1;
                        } else if (dx1 > 0 && dy1 < 0) {
                            bendQuad = 2;
                        } else if (dx1 <= 0 && dy1 <= 0) {
                            bendQuad = 3;
                        } else if (dx1 < 0 && dy1 > 0) {
                            bendQuad = 4;
                        }

                        if (dx2 >= 0 && dy2 >= 0) {
                            feedQuad = 1;
                        } else if (dx2 > 0 && dy2 < 0) {
                            feedQuad = 2;
                        } else if (dx2 <= 0 && dy2 <= 0) {
                            feedQuad = 3;
                        } else if (dx2 < 0 && dy2 > 0) {
                            feedQuad = 4;
                        }
                        //safePrint(" dx1: "+dx1+" dy1: "+dy1+" Bend Quad : "+bendQuad+" dx2: "+dx2+" dy2: "+dy2+" Feed Quad : "+feedQuad);

                        if (feedQuad-bendQuad == -1 || feedQuad-bendQuad == 3) {
                            //safePrint("case 1");
                            pos = true;
                        } else if (feedQuad-bendQuad == 1 || feedQuad-bendQuad == -3) {
                            pos = false;
                            //safePrint("case 2");
                        } else if (feedQuad == bendQuad && (bendQuad == 1 || bendQuad == 3)) {
                            //safePrint("case 3");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = false;
                            } else {
                                pos = true;
                            }
                        } else if (feedQuad == bendQuad && (bendQuad == 4 || bendQuad == 2)) {
                            // safePrint("case 4");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = true;
                            } else {
                                pos = false;
                            }
                        } else if (feedQuad == 1 && bendQuad == 3) {
                            //safePrint("case 5");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = true;
                            } else {
                                pos = false;
                            }
                        } else if (feedQuad == 3 && bendQuad == 1) {
                            //safePrint("case 6");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = true;
                            } else {
                                pos = false;
                            }
                        } else if (feedQuad == 4 && bendQuad == 2) {
                            //safePrint("case 7");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = false;
                            } else {
                                pos = true;
                            }
                        } else if (feedQuad == 2 && bendQuad == 4) {
                            //safePrint("case 8");
                            if (abs(dy1/dx1) > abs(dy2/dx2)) {
                                pos = false;
                            } else {
                                pos = true;
                            }
                        }

                        if (pos == true) {
                            //angles = append(angles, 180-degrees(ang1));
                            angle = 180-degrees(ang1);

                        } else {
                            //angles = append(angles, -180+degrees(ang1));
                            angle = -180+degrees(ang1);
                            dia*=-1;
                        }
                    } else {
                        //angles = append(angles, 0);
                        angle = 0;
                    }
                    wps.angles.append(angle);
                    wps.diameters.append(dia);
                    // println(dia);

                    wp0 = wp1;
                    wp1 = wp2;
                }
            }

            // add extra angle at end to balance length with feeds
            wps.angles.append(0);

            // look for feed errors
            // clear all errors
            for (j = 0; j < wps.pts.size (); ++j)
                wps.pts.get(j).stat &= ~POINT_FEED_ERROR;

            for (j = 0; j < wps.feeds.size (); ++j) {
                if ((wps.feeds.get(j) < 14 || (j == wps.feeds.size()-1 && wps.feeds.get(j) < 12)) && !continuousFlag) {
                    //safePrint("WARNING - FEED TOO SHORT");
                    wps.nFeedWarnings++;
                    wps.pts.get(j).stat |= POINT_FEED_ERROR;
                    wps.pts.get(j+1).stat |= POINT_FEED_ERROR;
                }
            }

            // look for bend angle errors
            // clear all errors
            for (j = 0; j < wps.pts.size (); ++j)
                wps.pts.get(j).stat &= ~POINT_ANGLE_ERROR;

            for (j = 0; j < wps.angles.size (); ++j) {
                float a = abs(wps.angles.get(j));
                //harware restriction, will be put into JSON later, used to be byte limitation on Arduino
                if (a > 140) {
                    //safePrint("WARNING - ANGLE TOO BIG");
                    wps.nAngleWarnings++;
                    wps.pts.get(j+1).stat |= POINT_ANGLE_ERROR;
                }
            }

            //IDEAL FEED COMPENSATION (ASSUME NO ELONGATION) CBARBRE 10/14/15
            debugC("Math/calculateFeedsAndBendAngles/corner compensation");
            println("kFactor = " + str(kFactor) + ", mandrelRadius = " + str(mandrelRad));
            println(", mandrelOffset = " + str(feedAdjustStartEnd) + ", wireDiam = " + str(wireDiam));
            float[] arcLen = new float[wps.angles.size()]; // array of arc lengths of bends, each associated with an angle in wps.angles
            float[] corLen = new float[wps.angles.size()]; // array of arc tangency to sharp corner distances associated with an angle in wps.angles
            float intAng; //interior angle of the bend (wps.angles lists the exterior)

            nAxisRad=kFactor*wireDiam;
            println("nAxisRad = " + str(nAxisRad));
            for (j = 0; j < wps.feeds.size(); j++) {
                intAng = (float)((Math.PI/180)*(180 - Math.abs(wps.angles.get(j))));  // find interior angle in rads, cast to float because Math.PI returns type double
                System.out.println("Interior bend angle is" + Double.toString(intAng*180/Math.PI) + " degrees");

                if (intAng < (float)(Math.PI/4)) {          // catch interior angles < 45deg, throw a warning both to terminal and in WireWare
                    wps.nAngleWarnings++;                    // at small interior angle values this compensation breaks down, so we limit it here to the machine's capability
                    wps.pts.get(j+1).stat |= POINT_ANGLE_ERROR;
                    System.out.println("Warning, angle too sharp to compensate!");
                    continue;
                }

                arcLen[j] = (float)((mandrelRad + nAxisRad)*(Math.PI-intAng));  //find arc lenth for this bend, cast to float
                corLen[j] = (float)(2*(mandrelRad + wireDiam/2)*(Math.tan(Math.abs( (wps.angles.get(j)*Math.PI/180)/2) ))); //find corner length, cast to float due to Math.tan returning double

                // corner compensation applied to previous feed
                wps.feeds.set(j , wps.feeds.get(j) - (corLen[j] - arcLen[j]) ); //alter feed for next bend

                // corner compensation split btwn 2 corners for each feed
//          if (j>0){
//            wps.feeds.set(j , wps.feeds.get(j) - (corLen[j-1] - arcLen[j-1])/2 );
//          }
//          if (j < (wps.feeds.size()-1) ){
//           wps.feeds.set(j , wps.feeds.get(j) - (corLen[j] - arcLen[j])/2 );
//          }

            }

            // adjust start and end feeds for offset to mandrel using feedAdjustStartEnd
            wps.feeds.set(0 , wps.feeds.get(0)-feedAdjustStartEnd);
            wps.feeds.set(wps.feeds.size()-1, wps.feeds.get(wps.feeds.size()-1)+feedAdjustStartEnd);


        }
    }




    // clear all calib values, in preparation for new calibration
// set up using default calibration values
    public void clearCalibration () {
        //println("clearCalibration()");

        int i, ii, nPts;

        int step = 1;
        if (optCalibQuick.isSelected()) step = 2;

        nPts = 0; for (i = 0; i < defaultPosCalPts.length; i += step) ++nPts;
        posCalPts = new float[nPts];
        posCalRes = new float[nPts];
        for (i = ii = 0; i < defaultPosCalPts.length; i += step, ++ii)  {
            posCalPts[ii] = defaultPosCalPts[i];
            posCalRes[ii] = NO_CALIBRATION;
        }

        nPts = 0; for (i = 0; i < defaultNegCalPts.length; i += step) ++nPts;
        negCalPts = new float[nPts];
        negCalRes = new float[nPts];
        for (i = ii = 0; i < defaultNegCalPts.length; i += step, ++ii)  {
            negCalPts[ii] = defaultNegCalPts[i];
            negCalRes[ii] = NO_CALIBRATION;
        }

        calibXContactPos = xHomePos;
        calibXContactNeg = xHomeNeg;

        println("Calibration Cleared");
    }

    public String getFullPathName (String fileName)  {
        return rootDataFolder + File.separator + "calibration" + File.separator + fileName;
    }


    // loads the file, parses the JSON and closes the file
// Processing's default loadJSONObject() doesn't close the file, so it can't be delted, etc.
// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
    public JSONObject loadAndCloseJSONObject (String pathName)  {
        //println("loadAndCloseJSONObject()");

        JSONObject js = new JSONObject();
        try  {
            byte[] encoded = Files.readAllBytes(Paths.get(pathName));
            String contents = new String(encoded);
            js = parseJSONObject(contents);
        } catch (Exception e)  {
            appendTextToFile(errorFilename, "loadandclosejsonobject(): "+e.getMessage());
            println("loadAndCloseJSONObject() Failed to read: " + pathName);
        }
        return js;
    }

    // load in the calibration data (Attempted/Actual pairs) for editing
// note that this is different from setCurrentCalibration()
// this only loads the Attemped/Actual pairs, it does not change the current active calibration
    public void loadCalibrationData ()  {
        //println("loadCalibrationData()");

        int i;
        String fileName = dlMatProfCalib.getSelectedText();
        fileName += ".json";

        //JSONObject cjs = loadJSONObject(rootDataFolder + File.separator + "calibration" + File.separator + fileName);
        JSONObject cjs = loadAndCloseJSONObject(getFullPathName(fileName));
        try  {
            // read pos and neg calib pairs from calib file
            JSONArray jsa;

            jsa = cjs.getJSONArray("PosData");
            int nPosCalPts = jsa.size();
            posCalPts = new float[nPosCalPts];
            posCalRes = new float[nPosCalPts];
            for (i = 0; i < nPosCalPts; ++i)  {
                JSONObject jso = jsa.getJSONObject(i);
                posCalPts[i] = jso.getFloat("PinAngle");
                posCalRes[i] = jso.getFloat("SpringBackAngle");
            }

            jsa = cjs.getJSONArray("NegData");
            int nNegCalPts = jsa.size();
            negCalPts = new float[nNegCalPts];
            negCalRes = new float[nNegCalPts];
            for (i = 0; i < nNegCalPts; ++i)  {
                JSONObject jso = jsa.getJSONObject(i);
                negCalPts[i] = jso.getFloat("PinAngle");
                negCalRes[i] = jso.getFloat("SpringBackAngle");
            }

            calibXContactPos = cjs.getFloat("XContactPos");
            calibXContactNeg = cjs.getFloat("XContactNeg");

            // set GUI elements for editing
            //txCalibName.setText(cjs.getString("Profile Name"));
            txCalibName.setText(dlMatProfCalib.getSelectedText());
            txMaterialName.setText(cjs.getString("Material Name"));

            curWorkingCalibFileName = txCalibName.getText() + ".json";
            curWorkingCalibPathName = getFullPathName(curWorkingCalibFileName);

            // get the calibSize as well
            float bhd = cjs.getFloat("Bend Head Diameter");
            calibSize = CALIB_SIZE_1_16;
            if (abs(bhd-.125f) < .01f) calibSize = CALIB_SIZE_1_8;

            // set the dropdown menus to the correct Bend Head and Feed Wheel
            i = 0;
            String bhs = cjs.getString("Bend Head");
            while (i < 100)  {
                dlBendHead.setSelected(i);
                if (dlBendHead.getSelectedText().equals(bhs)) break;
                ++i;
            }
            if (i >= 100)
                println("ERROR: Could not find Bend Head " + bhs + " in drop down list");
            i = 0;

            String fws = cjs.getString("Feed Wheel");
            while (i < 100)  {
                dlFeedWheel.setSelected(i);
                if (dlFeedWheel.getSelectedText().equals(fws)) break;
                ++i;
            }
            if (i >= 100)
                println("ERROR: Could not find Feed Wheel " + fws + " in drop down list");

            // set the calibration accuracy
            String cas = cjs.getString("Accuracy");
            if (cas.equals("High")) optCalibHigh.setSelected(true);
            if (cas.equals("Standard")) optCalibStandard.setSelected(true);
            if (cas.equals("Quick")) optCalibQuick.setSelected(true);

            println("Loaded Material Profile data from: " + fileName);

            updateBHFormula();
            updateFWFormula();
        } catch (Exception e)  {
            appendTextToFile(errorFilename, "loadCalibrationData(): "+e.getMessage());
            println("Unable to read Material Profile file: " + fileName);
        }
    }


    // check status of calibration and advance if ready to
    public void doCalibration () {
        //println("doCalibration()");

        // see if done feeding
        if (curCalibState == CALIB_STATE_FEEDING && tinyGCommandReady) {
            curCalibState = CALIB_STATE_READYTOBEND;

            printCalibStatus();
        }

        // ready to bend when done feeding or user clicked on OK to record previous data point
        if (curCalibState == CALIB_STATE_READYTOBEND) {
            if (curCalibDir == CALIB_DIR_CW) {
                // skip previously calibrated points
                //println("posCalRes[" + curCalibIdx + "] = " + posCalRes[curCalibIdx]);
                if (posCalRes[curCalibIdx] != NO_CALIBRATION)  {
                    curCalibState = CALIB_STATE_SKIP_POINT;
                    okCalib();
                    return;
                }

                // if you tell it to move to a position it's already at, it won't report position, which
                //  causes the waiting mechanism to hang.  Also, sendBend(0.0) frequently hangs
                //if (posCalPts[curCalibIdx] != lastPositionX && posCalPts[curCalibIdx] != 0.0)
                if (posCalPts[curCalibIdx] == 0.0f)
                    sendBend(xHomePos, false);
                else
                    sendBend(wireAng2PinMM(posCalPts[curCalibIdx]), false);

            } else if (curCalibDir == CALIB_DIR_CCW) {
                // skip previously calibrated points
                if (negCalRes[curCalibIdx] != NO_CALIBRATION)  {
                    curCalibState = CALIB_STATE_SKIP_POINT;
                    okCalib();
                    return;
                }

                //if (negCalPts[curCalibIdx] != lastPositionX && negCalPts[curCalibIdx] != 0.0)
                if (negCalPts[curCalibIdx] == 0.0f)
                    sendBend(xHomeNeg, false);
                else
                    sendBend(wireAng2PinMM(-negCalPts[curCalibIdx]), false);
            }
            curCalibState = CALIB_STATE_BENDING;

            printCalibStatus();
        }

        // see if done bending
        if (curCalibState == CALIB_STATE_BENDING && tinyGCommandReady) {
            curCalibState = CALIB_STATE_WAITING;

            printCalibStatus();
        }

    }


    // select the current calibration file in the Edit menu
// seems like a dumb way of doing this, but there doesn't seem to be a way to get the item list from the GDropList
    public void selectCurCalib ()  {
        //println("selectCurCalib()");
        String fName = curInstalledCalibFileName.replace(".json", "");
        //println("fName = " + fName);
        for (int i = 0; i < nCalibrations; ++i)  {
            dlMatProfEdit.setSelected(i);
            //println("dlMatProfEdit.getSelectedText() = " + dlMatProfEdit.getSelectedText());
            if (dlMatProfEdit.getSelectedText().equals(fName)) break;
        }
    }

    public void finishCalibration () {
        debugC("finishCalibration()");

        calculateAngle2MMPolyFit();    // calculate cubic polynomial fit curve to calibration data - should save in file

        // create the calib file as a JSON object
        JSONObject jsonCalib = new JSONObject();

        // general information about calibration
        // TO DO: get "Nice Name" - human readable
        jsonCalib.setString("Profile Name", txCalibName.getText());
        jsonCalib.setString("Material Name", txMaterialName.getText());
        jsonCalib.setString("Bend Head", dlBendHead.getSelectedText());
        jsonCalib.setFloat("Bend Head Diameter", mapBH2Dia.get(dlBendHead.getSelectedText()));
        jsonCalib.setString("Feed Wheel", dlFeedWheel.getSelectedText());
        jsonCalib.setFloat("Feed Wheel Diameter", feedDiam.get(dlFeedWheel.getSelectedText()));
        jsonCalib.setString("Accuracy", (optCalibHigh.isSelected() ? "High" : (optCalibStandard.isSelected() ? "Standard" : "Quick")));
        jsonCalib.setString("Timestamp", String.format("%d_%d_%d__%d_%d_%d", year(), month(), day(), hour(), minute(), second()));
        jsonCalib.setString("Nice Name", "");
        jsonCalib.setString("Machine", curSerialNumber);
        jsonCalib.setString("User", System.getProperty("user.name"));

        // save contact positions and polynomial fit coeffs
        jsonCalib.setFloat("XContactPos", calibXContactPos);
        jsonCalib.setFloat("XContactNeg", calibXContactNeg);

        jsonCalib.setFloat("PosX0", polyFormulaPos[0]);
        jsonCalib.setFloat("PosX1", polyFormulaPos[1]);
        jsonCalib.setFloat("PosX2", polyFormulaPos[2]);
        jsonCalib.setFloat("PosX3", polyFormulaPos[3]);

        jsonCalib.setFloat("NegX0", polyFormulaNeg[0]);
        jsonCalib.setFloat("NegX1", polyFormulaNeg[1]);
        jsonCalib.setFloat("NegX2", polyFormulaNeg[2]);
        jsonCalib.setFloat("NegX3", polyFormulaNeg[3]);

        jsonCalib.setFloat("Mandrel Radius", mandrelRad);
        jsonCalib.setFloat("Mandrel Offset", feedAdjustStartEnd);

        // save calibration data itself
        int i;
        JSONArray posData = new JSONArray();
        for (i = 0; i < posCalPts.length; ++i) {
            JSONObject data = new JSONObject();
            data.setFloat("PinAngle", posCalPts[i]);
            data.setFloat("SpringBackAngle", posCalRes[i]);
            posData.setJSONObject(i, data);
        }
        jsonCalib.setJSONArray("PosData", posData);

        JSONArray negData = new JSONArray();
        for (i = 0; i < negCalPts.length; ++i) {
            JSONObject data = new JSONObject();
            data.setFloat("PinAngle", negCalPts[i]);
            data.setFloat("SpringBackAngle", negCalRes[i]);
            negData.setJSONObject(i, data);
        }
        jsonCalib.setJSONArray("NegData", negData);

        // save JSON data file
        saveJSONObject(jsonCalib, curWorkingCalibPathName);

        // save this as the currently installed calibration
        curInstalledCalibFileName = curWorkingCalibFileName;
        curInstalledCalibPathName = curWorkingCalibPathName;

        // add to drop lists
        // addItem() and insertItem() aren't working even after I upgraded G4P to 3.5
        // better this way so we get sorted order
        createMatProfDropdowns();
        selectCurCalib();

        println("curWorkingCalibPathName = " + curWorkingCalibPathName);
        println("Adding and selecting new Material Profile file: " + curInstalledCalibFileName);
    }


    public void deleteCalibration ()  {
        //println("deleteCalibration()");

        // confirm dialog
        int res = G4P.selectOption(this, "Are you sure you want to delete this Material Profile?", "Confirm Delete", G4P.QUERY, G4P.OK_CANCEL);
        if (res == G4P.OK)  {
            String fileName = dlMatProfCalib.getSelectedText() + ".json";

            // If the current calib is the one to be deleted, then we need to select another
            // what should we do if we are deleting the last calib file?
            if (curInstalledCalibFileName.equals(fileName))  {
                dlMatProfEdit.setSelected(dlMatProfEdit.getSelectedIndex() != 0 ? 0 : 1);
                setCurrentCalibration();
            }

            // delete the file
            // seems like once the file is read in it can't be deleted loadJSONObject() keeps file open
            //  switch to loadAndCloseJSONObject() - closes the file after reading
            // http://www.mkyong.com/java/how-to-delete-file-in-java/
            try  {
                File file = new File(getFullPathName(fileName));
                if (file.delete())
                    println("Deleted Material Profile " + fileName);
                else
                    println("Error deleting Material Profile file: " + fileName);
            } catch (Exception e)  {
                appendTextToFile(errorFilename, "deleteCalibration(): "+e.getMessage());
                println("Exception deleting Material Profile file: " + fileName);
            }

            // recreate the dropdown lists
            createMatProfDropdowns();
            selectCurCalib();
        }
    }

    // sets current calibration to currently selected value in dlMatProfEdit
    public void setCurrentCalibration ()  {
        //println("setCurrentCalibration()");

        String fileName = dlMatProfEdit.getSelectedText();

        if (fileName.equals("No Material Profiles"))  {
            //println("ERROR: No Material Profile files found");
            return;
        }

        fileName += ".json";
        String pathName = getFullPathName(fileName);

        if (!fileName.equals(curInstalledCalibFileName))  {
            //JSONObject cjs = loadJSONObject(rootDataFolder + File.separator + "calibration" + File.separator + fileName);
            JSONObject cjs = loadAndCloseJSONObject(pathName);
            try  {
                fwDiam = cjs.getFloat("Feed Wheel Diameter");
                // read contact positions and polynomial fit coeffs from calib file
                // turns out we don't use contact position after calibration - baked into poly fit
                calibXContactPos = cjs.getFloat("XContactPos");
                calibXContactNeg = cjs.getFloat("XContactNeg");

                // get the calibSize as well - same as for loadCalibrationData() above
                float bhs = cjs.getFloat("Bend Head Diameter");
                wireDiam = bhs;
                mandrelRad = cjs.getFloat("Mandrel Radius");
                feedAdjustStartEnd = cjs.getFloat("Mandrel Offset");
                debugC("Math/setCurrentCalibration() : wireDiam = "+str(wireDiam));
                debugC("Math/setCurrentCalibration() : mandrelRad = "+str(mandrelRad));
                debugC("Math/setCurrentCalibration() : mandrelOffset = "+str(feedAdjustStartEnd));
                calibSize = CALIB_SIZE_1_16;
                if (abs(bhs-.125f) < .01f) calibSize = CALIB_SIZE_1_8;

                polyFormulaPos = new float[4];
                polyFormulaPos[0] = cjs.getFloat("PosX0");
                polyFormulaPos[1] = cjs.getFloat("PosX1");
                polyFormulaPos[2] = cjs.getFloat("PosX2");
                polyFormulaPos[3] = cjs.getFloat("PosX3");

                polyFormulaNeg = new float[4];
                polyFormulaNeg[0] = cjs.getFloat("NegX0");
                polyFormulaNeg[1] = cjs.getFloat("NegX1");
                polyFormulaNeg[2] = cjs.getFloat("NegX2");
                polyFormulaNeg[3] = cjs.getFloat("NegX3");


                continuousFlag = cjs.hasKey("Continuous") ? cjs.getBoolean("Continuous") : false;
                //println(continuousFlag);
                polyFormulaPosArc[0] = cjs.hasKey("posArc0") ? cjs.getFloat("posArc0") : polyFormulaPosArcDefault[0];
                polyFormulaPosArc[1] = cjs.hasKey("posArc1") ? cjs.getFloat("posArc1") : polyFormulaPosArcDefault[1];
                polyFormulaPosArc[2] = cjs.hasKey("posArc2") ? cjs.getFloat("posArc2") : polyFormulaPosArcDefault[2];
                polyFormulaPosArc[3] = cjs.hasKey("posArc3") ? cjs.getFloat("posArc3") : polyFormulaPosArcDefault[3];
                polyFormulaPosArc[4] = cjs.hasKey("posArc4") ? cjs.getFloat("posArc4") : polyFormulaPosArcDefault[4];
                polyFormulaPosArc[5] = cjs.hasKey("posArc5") ? cjs.getFloat("posArc5") : polyFormulaPosArcDefault[5];

                polyFormulaNegArc[0] = cjs.hasKey("negArc0") ? cjs.getFloat("negArc0") : polyFormulaNegArcDefault[0];
                polyFormulaNegArc[1] = cjs.hasKey("negArc1") ? cjs.getFloat("negArc1") : polyFormulaNegArcDefault[1];
                polyFormulaNegArc[2] = cjs.hasKey("negArc2") ? cjs.getFloat("negArc2") : polyFormulaNegArcDefault[2];
                polyFormulaNegArc[3] = cjs.hasKey("negArc3") ? cjs.getFloat("negArc3") : polyFormulaNegArcDefault[3];
                polyFormulaNegArc[4] = cjs.hasKey("negArc4") ? cjs.getFloat("negArc4") : polyFormulaNegArcDefault[4];
                polyFormulaNegArc[5] = cjs.hasKey("negArc5") ? cjs.getFloat("negArc5") : polyFormulaNegArcDefault[5];




                curInstalledCalibFileName = fileName;
                curInstalledCalibPathName = pathName;

                println("Setting new Material Profile to: " + fileName);

                String bhn=cjs.getString("Bend Head");
                int i=0;
                while (i < 100)  {
                    dlBendHead.setSelected(i);
                    if (dlBendHead.getSelectedText().equals(bhn)) break;
                    ++i;
                }
                if (i >= 100)
                    println("ERROR: Could not find Bend Head " + bhn + " in drop down list");
                i = 0;

                String fws = cjs.getString("Feed Wheel");
                while (i < 100)  {
                    dlFeedWheel.setSelected(i);
                    if (dlFeedWheel.getSelectedText().equals(fws)) break;
                    ++i;
                }
                if (i >= 100)
                    println("ERROR: Could not find Feed Wheel " + fws + " in drop down list");

                updateBHFormula();
                updateFWFormula();
                //println("  calibSize = " + calibSize + " calibXContactPos = " + calibXContactPos + " calibXContactNeg = " + calibXContactNeg);
            } catch (Exception e)  {
                appendTextToFile(errorFilename, "setCurrentCalibration(): "+e.getMessage());
                println("Unable to read Material Profile file: " + fileName);
            }
        } else  {
            println("Material Profile already set to: " + fileName);
        }
    }

    // calculates formula to convert between bend angle and bend pin x-axis linear offset, based on calibration table data
// regression fit of cubic polynomial
// formula here from eg:
//    http://en.wikipedia.org/wiki/Polynomial_regression
//    http://mathworld.wolfram.com/LeastSquaresFittingPolynomial.html
//    http://bruce-shapiro.com/math481A/notes/least-squares.pdf
//
    public void calculateAngle2MMPolyFit () {
        println("calculateAngle2MMPolyFit()");

        int i, nGoodPts;

        nGoodPts = 0;
        for (i = 0; i < posCalPts.length; ++i)
            if (posCalRes[i] != NO_CALIBRATION) ++nGoodPts;


        if (nGoodPts >= 5) {
            float[][] xMatPos = new float[nGoodPts][4];
            float[] yMatPos = new float[nGoodPts];

            for (i = 0; i < posCalPts.length; ++i) {
                float y = posCalPts[i];
                float x = posCalRes[i];

                // skip uncalibrated points
                if (x == NO_CALIBRATION) continue;

                // add contact point to fit
                if (y == 0.0f)  {
                    y = calibXContactPos;
                    x = 0.0f;
                } else  {
                    // convert from wire angle using pre-computed formula
                    //x = pinMM2WireAng(x);
                    y = wireAng2PinMM(y);
                }

                println("Pos: angle = " + x + " pinMM = " + y);
                xMatPos[i] = new float[] {
                        1, x, x*x, x*x*x
                };
                yMatPos[i] = y;
            }

            yMatPos = Mat.multiply(Mat.transpose(xMatPos), yMatPos);
            xMatPos = Mat.inverse(Mat.multiply(Mat.transpose(xMatPos), xMatPos));
            polyFormulaPos = Mat.multiply(xMatPos, yMatPos);
            println("Calculating with " + nGoodPts + " Positive calibration Points");
        } else {
            // this sets pointer
            //polyFormulaPos = defaultFormulaPos;

            // copy values from default
            polyFormulaPos = new float[4];
            for (i = 0; i <= 3; ++i)
                polyFormulaPos[i] = defaultFormulaPos[i];
            safePrint("Only " + nGoodPts + " positive calibration points found. Using default formula");
        }

        nGoodPts = 0;
        for (i = 0; i < negCalPts.length; ++i)
            if (negCalRes[i] != NO_CALIBRATION) ++nGoodPts;

        if (nGoodPts >= 5) {
            float[][] xMatNeg = new float[nGoodPts][4];
            float[] yMatNeg = new float[nGoodPts];

            for (i = 0; i < negCalPts.length; ++i) {
                float y = negCalPts[i];
                float x = -negCalRes[i];

                if (x == NO_CALIBRATION) continue;

                if (y == 0.0f)  {
                    y = calibXContactNeg;
                    x = 0.0f;
                } else  {
                    // convert from wire angle using pre-computed formula
                    //x = pinMM2WireAng(x);
                    y = wireAng2PinMM(-y);
                }

                println("Neg: angle = " + x + " pinMM = " + y);
                xMatNeg[i] = new float[] {
                        1, x, x*x, x*x*x
                };
                yMatNeg[i] = y;
            }

            yMatNeg = Mat.multiply(Mat.transpose(xMatNeg), yMatNeg);
            xMatNeg = Mat.inverse(Mat.multiply(Mat.transpose(xMatNeg), xMatNeg));
            polyFormulaNeg = Mat.multiply(xMatNeg, yMatNeg);
            println("Calculating with " + nGoodPts + " Negative calibration Points");
        } else {
            //polyFormulaNeg = defaultFormulaNeg;
            // copy values from default
            polyFormulaNeg = new float[4];
            for (i = 0; i <= 3; ++i)
                polyFormulaNeg[i] = defaultFormulaNeg[i];
            safePrint("Only " + nGoodPts + " negative calibration points. Using default formula");
        }

//    println("Pos coeffs: " + polyFormulaPos[0] + " " + polyFormulaPos[1] + " " + polyFormulaPos[2] + " " + polyFormulaPos[3]);
//    println("Neg coeffs: " + polyFormulaNeg[0] + " " + polyFormulaNeg[1] + " " + polyFormulaNeg[2] + " " + polyFormulaNeg[3]);
//    for (float a = -120.0; a <= 121.0; a += 30.0)
//        println("wireAng2SpringBackMM(" + a + ") = " + wireAng2SpringBackMM(a));
    }
/*
    Handles drawing of wire and other graphical elements

*/

    public void initializeGeom ()  {
        RG.init(this);                       // initializes the geomerative library
    }


    // initialize drawing on whole window
    public void drawClearAll ()  {
        background(255);
        noFill();
        noStroke();
    }

    // called whenever window is resized - either by user or by adding/removing settings area
// called 3 times at startup
    public void handleResize ()  {
        //println("handleResize()  window = " + width + " " + height);

        // calculate new size of main panel
        mainPanelSize[0] = width - (showSettingsPanel ? settingsPanelWidth : 0);
        mainPanelSize[1] = height;

        // set up preview area
        previewSize[0] = mainPanelSize[0] - (previewPaddingX[0]+previewPaddingX[1]);
        previewSize[1] = mainPanelSize[1] - (headerSize+footerSize+previewPaddingY[0]+previewPaddingY[1]);
        previewCenter[0] = previewPaddingX[0] + previewSize[0]/2;
        previewCenter[1] = previewPaddingY[0] + previewSize[1]/2 + headerSize;
        //println("previewSize = " + previewSize[0] + " " + previewSize[1]);

        displayNeedsUpdate = true;
        positionGUIElements();

        dropListener.setTarget(0, 0, width, height);

        // set up preview area draw buffer
        // maybe not the best place for this - called often when user is actively resizing window
        if (previewGraphics == null || previewGraphics.width != previewSize[0] || previewGraphics.height != previewSize[1])  {
            previewGraphics = createGraphics(previewSize[0], previewSize[1]);
            //println("previewGraphics.width = " + previewGraphics.width);
        }

        windowWasResized = false;
    }


    // re-positions buttons (and other GUI elements) after window resized
// called only from handleResize()
// this routine does not concern itself with what GUI element is visible
    public void positionGUIElements ()  {
        //if (true) return;

        // Load Mode
        btnOpenFile.moveTo(mainPanelSize[0]/2-100, mainPanelSize[1]-(footerSize+80));


        // Edit Mode
        dlMatProfEdit.moveTo(mainPanelSize[0]+10, headerSize+80);
        optMM.moveTo(mainPanelSize[0]+10, headerSize+160);
        optIn.moveTo(mainPanelSize[0]+80, headerSize+160);
        txWireLength.moveTo(mainPanelSize[0]+10, headerSize+230);
        txScale.moveTo(mainPanelSize[0]+10, headerSize+295);
        sldScale.moveTo(mainPanelSize[0]+10, headerSize+315);
        txResolution.moveTo(mainPanelSize[0]+10, headerSize+410);
        sldResolution.moveTo(mainPanelSize[0]+10, headerSize+430);
        txKfactor.moveTo(mainPanelSize[0]+10, headerSize+620);
        dlSampling.moveTo(mainPanelSize[0]+10, headerSize+480);
        cbPreview.moveTo(mainPanelSize[0]+10, headerSize+515);
        txRepeat.moveTo(mainPanelSize[0]+70, headerSize+552);
        sldGapThresh.moveTo(mainPanelSize[0]+10, headerSize+595);
        btnCloseGaps.moveTo(mainPanelSize[0]+40, headerSize+645);

        itbArrow.moveTo(previewPaddingX[0]+previewSize[0]*.5f-87, headerSize+3);
        itbZoomIn.moveTo(previewPaddingX[0]+previewSize[0]*.5f-51, headerSize+3);
        itbZoomOut.moveTo(previewPaddingX[0]+previewSize[0]*.5f-15, headerSize+3);
        itbZoomRect.moveTo(previewPaddingX[0]+previewSize[0]*.5f+21, headerSize+3);
        itbHand.moveTo(previewPaddingX[0]+previewSize[0]*.5f+57, headerSize+3);

        btnBeginSend.moveTo(mainPanelSize[0]/2-100, mainPanelSize[1]-(footerSize+75));
        btnPauseSend.moveTo(mainPanelSize[0]/2-100, mainPanelSize[1]-(footerSize+75));
        btnResumeSend.moveTo(mainPanelSize[0]/2-100, mainPanelSize[1]-(footerSize+75));
        btnStopSend.moveTo(mainPanelSize[0]/2+100+30, mainPanelSize[1]-(footerSize+75)+9);


        // Calibrate Mode
        dlMatProfCalib.moveTo(mainPanelSize[0]+10, headerSize+85);
        btnCalibCreate.moveTo(mainPanelSize[0]+40, headerSize+130);
        btnCalibEdit.moveTo(mainPanelSize[0]+40, headerSize+130);
        btnCalibDelete.moveTo(mainPanelSize[0]+40, headerSize+170);

        txCalibName.moveTo(mainPanelSize[0]+10, headerSize+80);
        txMaterialName.moveTo(mainPanelSize[0]+10, headerSize+165);
        dlBendHead.moveTo(mainPanelSize[0]+10, headerSize+250);
        dlFeedWheel.moveTo(mainPanelSize[0]+10, headerSize+335);
        optCalibHigh.moveTo(mainPanelSize[0]+10, headerSize+425);
        optCalibStandard.moveTo(mainPanelSize[0]+10, headerSize+455);
        optCalibQuick.moveTo(mainPanelSize[0]+10, headerSize+485);
        btnStartCalib.moveTo(mainPanelSize[0]+40, headerSize+550);
        btnOkCalib.moveTo(mainPanelSize[0]*.5f-45, headerSize+300);
        btnBeginCalib.moveTo(mainPanelSize[0]*.5f-45, headerSize+300);
        btnCancelCalib.moveTo(mainPanelSize[0]+40, headerSize+170);
        btnRefineCalib.moveTo(mainPanelSize[0]+40, headerSize+420);
        btnDoneCalib.moveTo(mainPanelSize[0]+40, headerSize+490);
        btnReCalib.moveTo(mainPanelSize[0]+40, headerSize+270);
        btnRefineRangeCalib.moveTo(mainPanelSize[0]+40, headerSize+400);
        txLow.moveTo(mainPanelSize[0]+20, headerSize+475);
        txHi.moveTo(mainPanelSize[0]+80, headerSize+475);
        txSteps.moveTo(mainPanelSize[0]+140, headerSize+475);


        // Manual Mode
        // position is for top left, to center images, need to offset (-45, -45)
        btnArrowUp.moveTo(mainPanelSize[0]*.5f-45, headerSize+mainPanelSize[1]*.5f-165-50);
        btnArrowDown.moveTo(mainPanelSize[0]*.5f-45, headerSize+mainPanelSize[1]*.5f+75-50);
        btnArrowLeft.moveTo(mainPanelSize[0]*.5f-165, headerSize+mainPanelSize[1]*.5f-45-50);
        btnArrowRight.moveTo(mainPanelSize[0]*.5f+75, headerSize+mainPanelSize[1]*.5f-45-50);
//    btnZeroRight.moveTo(mainPanelSize[0]+100, headerSize+mainPanelSize[1]*.5+150);
//    btnZeroLeft.moveTo(mainPanelSize[0]+20, headerSize+mainPanelSize[1]*.5+150);
        int h=150;
        optVeryFast.moveTo(mainPanelSize[0]+8, headerSize+83+h);
        optFast.moveTo(mainPanelSize[0]+8, headerSize+113+h);
        optSlow.moveTo(mainPanelSize[0]+102, headerSize+83+h);
        optVerySlow.moveTo(mainPanelSize[0]+102, headerSize+113+h);
        btnHome.moveTo(mainPanelSize[0]+40, headerSize+170+h);
        optNoInput.moveTo(mainPanelSize[0]+10, headerSize+250+h);
        optYesInput.moveTo(mainPanelSize[0]+100, headerSize+250+h);

        txNegFeed.moveTo(mainPanelSize[0]*.5f-15, headerSize+mainPanelSize[1]*.5f+115);
        txPosFeed.moveTo(mainPanelSize[0]*.5f-15, headerSize+mainPanelSize[1]*.5f-240);
        txNegBend.moveTo(mainPanelSize[0]*.5f-235, headerSize+mainPanelSize[1]*.5f-60);
        txPosBend.moveTo(mainPanelSize[0]*.5f+180, headerSize+mainPanelSize[1]*.5f-60);
        // testing
        btnTest.moveTo(mainPanelSize[0]+40, headerSize+635);
        txTest.moveTo(mainPanelSize[0]+10, headerSize+670);

        btnIgnoreError.moveTo(width/2+15, height/2+70);
        btnCancelSendError.moveTo(width/2-80, height/2+70);
        btnBackOffLimitError.moveTo(width/2-32, height/2+70);
        btnSerialError.moveTo(width/2-32, height/2+70);
        btnMidBendError.moveTo(width/2-32, height/2+70);

    }


    public void toggleSettingsPanel ()  {
        int h = frame.getHeight();
        int d = frame.getWidth() - width;
        showSettingsPanel = !showSettingsPanel;
        if (showSettingsPanel)  {
            if (displayWidth < mainPanelSize[0]+settingsPanelWidth+d)  {
                windowWasResized = true;
            } else  {
                frame.setSize(mainPanelSize[0]+settingsPanelWidth+d,h);
            }
        } else  {
            if (displayWidth < mainPanelSize[0]+settingsPanelWidth+d)  {
                windowWasResized = true;
            } else  {
                frame.setSize(mainPanelSize[0]+d,h);
            }
        }
    }

    public void updateDisplayState ()  {
        txTest.setVisible(false);
        if (curViewMode != VIEW_EDIT || displayState == prevDisplayState)
            return;

        if (displayState == DS_CONNECTING){
            for (GAbstractControl g: gEdit)
                g.setEnabled(false);
            btnBeginSend.setVisible(false);
            btnStopSend.setVisible(true);
            btnStopSend.setEnabled(false);
            btnPauseSend.setVisible(true);
            btnPauseSend.setEnabled(false);
            btnResumeSend.setVisible(false);

        }
        else if (displayState == DS_HOMING){
            for (GAbstractControl g: gEdit)
                g.setEnabled(false);
            btnBeginSend.setVisible(false);
            btnStopSend.setVisible(true);
            btnStopSend.setEnabled(false);
            btnPauseSend.setVisible(true);
            btnPauseSend.setEnabled(false);
            btnResumeSend.setVisible(false);

        }
        else if (displayState == DS_SENDING){
            for (GAbstractControl g: gEdit)
                g.setEnabled(false);
            btnBeginSend.setVisible(false);
            btnStopSend.setVisible(true);
            btnStopSend.setEnabled(true);
            btnPauseSend.setVisible(true);
            btnPauseSend.setEnabled(true);
            btnResumeSend.setVisible(false);
            btnResumeSend.setEnabled(true);
        }
        else{
            for (GAbstractControl g: gEdit)
                g.setEnabled(true);
            btnBeginSend.setVisible(true);
            btnStopSend.setVisible(false);
            btnPauseSend.setVisible(false);
            btnResumeSend.setVisible(false);

        }

        prevDisplayState= displayState;


    }




    // show/hide GUI elements depending on view
// Note: since GUI elements are automatically drawn, all we have to do here is set visibility
//  other routines, such as drawSettingsPanel() are responsible for drawing the non-GUI elements
    int prevViewMode = VIEW_NONE;
    int prevCalibMode = CALIB_NONE;
    public void updateViewMode ()  {
        if (curViewMode == VIEW_EDIT)  {
            if (continuousFlag)
                cbPreview.setVisible(false);
            else
                cbPreview.setVisible(true);
        }

        if (curViewMode == prevViewMode && (curViewMode != VIEW_CALIBRATE || curCalibMode == prevCalibMode))
            return;

        //println("updateViewMode() curViewMode = " + curViewMode + (curViewMode == VIEW_CALIBRATE ? (" curCalibMode = " + curCalibMode) : ""));

        // hide everything
        for (GAbstractControl g: gAll)
            g.setVisible(false);

        if (curViewMode == VIEW_LOAD)  {
            for (GAbstractControl g: gLoad)
                g.setVisible(true);
        } else if (curViewMode == VIEW_EDIT)  {
            for (GAbstractControl g: gEdit)
                g.setVisible(true);
            // only BEND button visible
            if (displayState == DS_SENDING || displayState == DS_HOMING){
                btnStopSend.setVisible(true);
                btnPauseSend.setVisible(true);
                btnResumeSend.setVisible(false);
            }
            else{
                btnStopSend.setVisible(false);
                btnPauseSend.setVisible(false);
                btnResumeSend.setVisible(false);
            }

        } else if (curViewMode == VIEW_CALIBRATE)  {
            //       for (GAbstractControl g: gCalibrate)
            //          g.setVisible(true);

            // since there are multiple states here, we need to have finer control of what's visible
            if (curCalibMode == CALIB_START)  {
                dlMatProfCalib.setVisible(true);
                dlMatProfCalib.setSelected(0);
                btnCalibCreate.setVisible(true);
            } else if (curCalibMode == CALIB_NEW)  {
                txCalibName.setVisible(true);
                txMaterialName.setVisible(true);
                dlBendHead.setVisible(true);
                dlFeedWheel.setVisible(true);
                optCalibHigh.setVisible(true);
                optCalibStandard.setVisible(true);
                optCalibQuick.setVisible(true);
                btnStartCalib.setVisible(true);
            } else if (curCalibMode == CALIB_EDIT)  {
                txCalibName.setVisible(true);
                txMaterialName.setVisible(true);
                dlBendHead.setVisible(true);
                dlFeedWheel.setVisible(true);
                btnRefineCalib.setVisible(true);
                btnDoneCalib.setVisible(true);
            } else if (curCalibMode == CALIB_REFINE)  {
                btnReCalib.setVisible(true);
                btnRefineRangeCalib.setVisible(true);
                txLow.setVisible(true);
                txHi.setVisible(true);
                txSteps.setVisible(true);
                btnCancelCalib.moveTo(mainPanelSize[0]+40, headerSize+570);
                btnCancelCalib.setVisible(true);
                btnOkCalib.setVisible(true);
            } else if (curCalibMode == CALIB_RUNNING)  {
                //btnOkCalib.setVisible(true);
                if (curCalibState != CALIB_STATE_FINISHED)
                    btnBeginCalib.setVisible(true);    // initial state - will be switched with btnOkCalib
                else
                    btnOkCalib.setVisible(true);
                btnCancelCalib.moveTo(mainPanelSize[0]+40, headerSize+170);
                btnCancelCalib.setVisible(true);
            } else  {
                println("ERROR: Bad calibration mode: " + curCalibMode);
            }

            prevCalibMode = curCalibMode;
        } else if (curViewMode == VIEW_MANUAL)  {
            for (GAbstractControl g: gManual)
                g.setVisible(true);

            if (manInputs){
                for (GAbstractControl g: gManualInputs)
                    g.setVisible(true);
            }
            else{
                for (GAbstractControl g: gManualInputs)
                    g.setVisible(false);
            }


        } else  {
            println("ERROR: Bad view mode:" + curViewMode);
        }

        // open or close settings panel
        if ((showSettingsPanel && curViewMode == VIEW_LOAD) || (!showSettingsPanel && curViewMode != VIEW_LOAD))
            toggleSettingsPanel();

        prevViewMode = curViewMode;
    }


    // Needed to disable buttons while bending and while popup is shown - TO DO: fix these
    public void disableButtons ()  {
        txWireLength.setEnabled(false);
        txScale.setEnabled(false);
        txResolution.setEnabled(false);
        txKfactor.setEnabled(false);
        sldResolution.setEnabled(false);
        sldScale.setEnabled(false);
    }

    public void enableButtons ()  {
        txWireLength.setEnabled(true);
        txScale.setEnabled(true);
        txResolution.setEnabled(true);
        txKfactor.setEnabled(true);
        sldResolution.setEnabled(true);
        sldScale.setEnabled(true);
        btnResumeSend.setEnabled(true);
        btnPauseSend.setEnabled(true);
    }


    // draws drop area image in proper state
    public void drawLoadScreen ()  {
        if (dropListener.checkDropHover())
            image(dropHover, (mainPanelSize[0]-dropArea.width)/2, headerSize+20);
        else
            image(dropArea, (mainPanelSize[0]-dropArea.width)/2, headerSize+20);
        dropListener.setTarget(0, 0, width, height);    // need to do this every frame?
    }


    public void drawWindowFrame ()  {
        background(backgroundColor);

        // draw line separating header from menu
        stroke(headerLineColor);
        strokeWeight(headerLineWeight);
        line(0, headerSize, width, headerSize);

        // draw logo
        //image(logo, 5, 5);

        // draw version string next to logo, so current file can go into window title
        textSize(9);
        fill(160);
        textAlign(LEFT);
        text("v" + softwareVersion, 3, mainPanelSize[1]-footerSize-3);

        // draw footer
        drawStatusBar();

        // draw settings panel - seems like this draws whether visible or not
        drawSettingsPanel();

        // write window mode in upper right
        textSize(20);
        textAlign(RIGHT);
        if (curViewMode == VIEW_EDIT)
            text("EDIT", mainPanelSize[0]-30, headerSize+25);
        else if (curViewMode == VIEW_CALIBRATE){
            text("CALIBRATE", mainPanelSize[0]-30, headerSize+25);
        }
        else if (curViewMode == VIEW_MANUAL)
            text("MANUAL", mainPanelSize[0]-30, headerSize+25);

    }



    // sets up to draw the "Preview" area into the buffer - where the geometry is displayed and manipulated
    public void preparePreviewArea ()  {
        previewGraphics.beginDraw();

        previewGraphics.background(previewBackgroundColor);

        // viewing params - mouse move/zoom - scale or translate first?
        previewGraphics.pushMatrix();
        previewGraphics.translate(camX, camY);
        previewGraphics.scale(camScl);

        previewGraphics.strokeWeight(1.0f/camScl);    // lines should always have stroke of 1

        // grid - need to draw enough to always fill preview area buffer
//    previewGraphics.stroke(200);
//    for (int i = gridSize; i < previewSize[0]; i += gridSize)
//        previewGraphics.line(previewCenter[0]-previewSize[0]/2+i, previewCenter[1]+previewSize[1]/2, previewCenter[0]-previewSize[0]/2+i, previewCenter[1]-previewSize[1]/2);
//    for (int i = gridSize; i < previewSize[1]; i += gridSize)
//        previewGraphics.line(previewCenter[0]-previewSize[0]/2, previewCenter[1]+previewSize[1]/2-i, previewCenter[0]+previewSize[0]/2, previewCenter[1]+previewSize[1]/2-i);

        float[] canvasSize = {previewSize[0]/camScl, previewSize[1]/camScl};

        // draw fixed grid for now
//    previewGraphics.stroke(128);
//    previewGraphics.line(-1000,0, 1000,0);
//    previewGraphics.line(0,-1000, 0,1000);

        //previewGraphics.stroke(200);
        //for (int i = gridSize; i <= (canvasSize[1]); i += gridSize)  {
        //previewGraphics.line(-1*camX/camScl,-1*camY/camScl+i, -1*camX/camScl+canvasSize[0],-1*camY/camScl+i);
        //previewGraphics.line(-1*camX/camScl,-i, -1*camX/camScl+canvasSize[0],-i);
        //previewGraphics.line(i,-1*camY/camScl, i,-1*camY/camScl+canvasSize[1]);
        //previewGraphics.line(-i,-1*camY/camScl, -i,-1*camY/camScl+canvasSize[1]);
        //}

        if (camX>0){
            for (int i = 0; i <= camX/camScl; i += gridSize)  {
                if (i==0){
                    previewGraphics.stroke(128);
                }
                else{
                    previewGraphics.stroke(gridColor);
                }
                previewGraphics.line( -i,-1*camY/camScl,-i,-1*camY/camScl+canvasSize[1]);
            }
            for (int i = gridSize; i <= canvasSize[0]-camX/camScl; i += gridSize)  {
                previewGraphics.stroke(gridColor);
                previewGraphics.line( i,-1*camY/camScl,i,-1*camY/camScl+canvasSize[1]);
            }
        }else{
            for (int i = round(camX/camScl)/gridSize; i <= canvasSize[0]-camX/camScl; i += gridSize)  {
                previewGraphics.stroke(gridColor);
                previewGraphics.line( i,-1*camY/camScl,i,-1*camY/camScl+canvasSize[1]);
            }
        }

        if (camY>0){
            for (int i = 0; i <= camY/camScl; i += gridSize)  {
                if (i==0){
                    previewGraphics.stroke(128);
                }
                else{
                    previewGraphics.stroke(gridColor);
                }
                previewGraphics.line(-1*camX/camScl,-i,-1*camX/camScl+canvasSize[0],-i);
            }
            for (int i = gridSize; i <= canvasSize[1]-camY/camScl; i += gridSize)  {
                previewGraphics.stroke(gridColor);
                previewGraphics.line( -1*camX/camScl,i,-1*camX/camScl+canvasSize[0],i);
            }
        }else{
            for (int i = round(camY/camScl)/gridSize; i <= canvasSize[1]-camY/camScl; i += gridSize)  {
                previewGraphics.stroke(gridColor);
                previewGraphics.line( -1*camX/camScl,i,-1*camX/camScl+canvasSize[0],i);
            }
        }


        //for (int i = gridSize; i <= (canvasSize[0]/2); i += gridSize)  {
        //previewGraphics.line(-1*camY/camScl+i,-1*camY/camScl, -1*camY/camScl+i,-1*camY/camScl+canvasSize[1]);
        //previewGraphics.line(-i,-1*camY/camScl, -i,-1*camY/camScl+canvasSize[1]);
        //}
    }

    // finish drawing of the preview area buffer and draw into window
    public void drawPreviewArea ()  {
        // finish transformed view drawing
        previewGraphics.popMatrix();

        previewGraphics.strokeWeight(1.0f);    // lines should always have stroke of 1

        // can drop another file into window to load it
        if (dropListener.checkDropHover())  {
            previewGraphics.fill(254, 193, 72, 20);
            previewGraphics.noStroke();
            previewGraphics.rect(0,0, previewSize[0]-1, previewSize[1]-1);
        }

        // border rect - not transformed by camera
        previewGraphics.noFill();
        previewGraphics.stroke(160);
        //previewGraphics.rectMode(CENTER);
        //previewGraphics.rect(previewCenter[0], previewCenter[1], previewSize[0]-2, previewSize[1]-2);
        previewGraphics.rect(0,0, previewSize[0]-1, previewSize[1]-1);

        // debug - draw center of preview area screen
        //previewGraphics.line(previewSize[0]*.5-5, previewSize[1]*.5-5, previewSize[0]*.5+5, previewSize[1]*.5+5);
        //previewGraphics.line(previewSize[0]*.5+5, previewSize[1]*.5-5, previewSize[0]*.5-5, previewSize[1]*.5+5);

        // done drawing into preview buffer
        previewGraphics.endDraw();

        // draw preview buffer into main window
        image(previewGraphics, previewPaddingX[0], previewPaddingY[0]+headerSize);
    }


    // draws exact current shape from current SVG file into the preview area PGraphics buffer
    public void drawSVG ()  {
        RG.setPolygonizer(RG.ADAPTATIVE);
        RG.setPolygonizerAngle(.1f);    // keep this consistent, not affected by resolution setting
        //stroke(0, 0, 200, 150);
        //s.draw();

        // draw into preview buffer
        // draw paths, highlighting current one
        for (int i = 0; i < nPaths; ++i)  {
            previewGraphics.stroke((i == curPath) ? pathHighlight : pathNormal);
            curPaths.get(i).draw(previewGraphics);
        }
    }


    // draws the points on the current wire, as determined by scale and resolution factors
    public void drawPoints ()  {
        if (curPath == -1) return;

        WirePoints wps = curWirePoints.get(curPath);

        for (int i = 0; i < wps.pts.size(); ++i)  {
            previewGraphics.noStroke();
            previewGraphics.noFill();
            WirePoint p = wps.pts.get(i);
            int b= color(255,255,255,0);
            int f= color(255,255,255,0);
            //println("p[" + i + "] = " + p);
            previewGraphics.fill(pointPause);

            // colors here should be global consts
            if (displayState == DS_SENDING && i == lastAngleSentIdx)  {
                f=(pointSent);
                //} else if ((p.stat & POINT_ERROR) != 0)  {
            } else if ((p.stat & POINT_FEED_ERROR) != 0)  {
                f=(pointError);
                //} else if ((p.stat & POINT_SELECTED) != 0)  {
            }  else if (i == 0){
                f=pointStart;
                b=pointNormal;
            } else  {
                f=pointNormal;
            }
            if (p.pointPause)  {
                b=(pointPause);
            }

            for (int j=0;j<curPoint.length;j++){
                if (i == curPoint[j])  {
                    f=(pointSelected);
                    //previewGraphics.stroke(pointSelected);
                    //previewGraphics.strokeWeight(3);
                    //previewGraphics.stroke(0,0,255);
                    //previewGraphics.strokeWeight(2/camScl);
                    //previewGraphics.ellipse(p.pt.x, p.pt.y, 7/camScl, 7/camScl);
                    //previewGraphics.noStroke();
                    //previewGraphics.noFill();
                }
            }
            if (i==0){
                PVector v = new PVector(wps.pts.get(i+1).pt.x-p.pt.x, wps.pts.get(i+1).pt.y-p.pt.y);
                v.setMag(18/camScl);
                PVector v2=new PVector(wps.pts.get(i+1).pt.x-p.pt.x, wps.pts.get(i+1).pt.y-p.pt.y);
                v2.rotate(HALF_PI);
                v2.setMag(12/camScl);

                b=pointNormal;
                previewGraphics.fill(b);
                previewGraphics.ellipse(p.pt.x, p.pt.y, 12/camScl, 12/camScl);
                previewGraphics.triangle(p.pt.x+v2.x/2,
                        p.pt.y+v2.y/2,
                        p.pt.x-v2.x/2,
                        p.pt.y-v2.y/2,
                        p.pt.x+v.x,
                        p.pt.y+v.y);
                previewGraphics.fill(f);
                previewGraphics.ellipse(p.pt.x, p.pt.y, 8/camScl, 8/camScl);
                //previewGraphics.triangle(p.pt.x, p.pt.y+(8/camScl)/2,p.pt.x, p.pt.y-(8/camScl)/2,p.pt.x+(8/camScl), p.pt.y);
            }
            else{
                previewGraphics.fill(b);
                previewGraphics.ellipse(p.pt.x, p.pt.y, 8/camScl, 8/camScl);
                previewGraphics.fill(f);
                previewGraphics.ellipse(p.pt.x, p.pt.y, 6/camScl, 6/camScl);

            }



        }


    }

    public void drawSelectRect(){
        if (selectRect[0]>0){
            noFill();
            stroke(0);
            strokeWeight(1);
            rect(selectRect[0], selectRect[1], selectRect[2], selectRect[3]);
        }
    }

    // draws the line segments of the current wire - only current path
    public void drawSegments ()  {
        if (curPath == -1) return;

        WirePoints wps = curWirePoints.get(curPath);

        previewGraphics.noFill();
        previewGraphics.strokeWeight(5/camScl);
        //println("wps.pts.size() = " + wps.pts.size());

        for (int i = 1; i < wps.pts.size(); ++i)  {


            WirePoint p1 = wps.pts.get(i-1);
            WirePoint p2 = wps.pts.get(i);

            // color the line based on its status
            if (displayState == DS_SENDING && (i-1) < lastAngleSentIdx)  {
                previewGraphics.stroke(segmentSent);
            } else if (displayState == DS_SENDING && (i-1) == lastAngleSentIdx)  {
                previewGraphics.stroke(segmentSending);
                //} else if ((p1.stat & POINT_ERROR) != 0 || (p2.stat & POINT_ERROR) != 0)  {
            } else if ((p1.stat & POINT_ANGLE_ERROR) != 0 || (p2.stat & POINT_ANGLE_ERROR) != 0)  {
                previewGraphics.stroke(segmentError);
            } else  {
                previewGraphics.stroke(segmentNormal);
            }
            if(!continuousFlag)
                previewGraphics.line(p1.pt.x, p1.pt.y, p2.pt.x, p2.pt.y);
        }
    }


    // draw info, warnings and dimensions around preview area - use current path
    public void drawBoundingArea ()  {
        stroke(0);
        strokeWeight(3);
        textSize(12);
        fill(160);

        String unitStr = (curUnits == UNITS_MM) ? "mm" : "in";
        float grd = gridSize;
        if (curUnits == UNITS_INCH) grd = round(100.0f*gridSize*conversionFactor) / 100.0f;
        textAlign(LEFT);
        text("Grid Size: "+ grd + unitStr, previewPaddingX[0], mainPanelSize[1]-footerSize-previewPaddingY[1]+15);

        if (curPath == -1)  {
            // print out number of paths
            textAlign(RIGHT);
            text("No Path Selected of " + nPaths + " paths", mainPanelSize[0]-previewPaddingX[1], mainPanelSize[1]-footerSize-previewPaddingY[1]+15);
            txWireLength.setText("");
            return;
        }

        RPath cPath = curPaths.get(curPath);
        RPoint[] bb = cPath.getBoundsPoints();    // or curShape for whole shape
        float crvLen = cPath.getCurveLength() * GUIScale;    // this doesn't seem to scale automatically with GUI scale applied to shape

//    line(bb[0].x, mainPanelSize[1]-footerSize-previewPaddingY[1], bb[1].x, mainPanelSize[1]-footerSize-previewPaddingY[1]);
//    line(previewPaddingX[0], bb[0].y, previewPaddingX[0], bb[2].y);

        // draw bounds lines
        // dimension lines - have to convert start/end value from object space to camera space and clip
        float xs = (bb[0].x * camScl + camX);
        if (xs < 0) xs = 0;
        if (xs > previewSize[0]) xs = previewSize[0];
        float xe = (bb[1].x * camScl + camX);
        if (xe < 0) xe = 0;
        if (xe > previewSize[0]) xe = previewSize[0];
        if (xe >= 0 || xs <= previewSize[0])
            line(xs+previewPaddingX[0], mainPanelSize[1]-footerSize-previewPaddingY[1], xe+previewPaddingX[0], mainPanelSize[1]-footerSize-previewPaddingY[1]);

        float ys = (bb[0].y * camScl + camY);
        if (ys < 0) ys = 0;
        if (ys > previewSize[1]) ys = previewSize[1];
        float ye = (bb[2].y * camScl + camY);
        if (ye < 0) ye = 0;
        if (ye > previewSize[1]) ye = previewSize[1];
        if (ye >= 0 || ys <= previewSize[1])
            line(previewPaddingX[0], ys+headerSize+previewPaddingY[0], previewPaddingX[0], ye+headerSize+previewPaddingY[0]);

        // draw text: dimensions, grid size, curve length
        float w = bb[1].x - bb[0].x; //abs(dist(bb[0].x, height-20, bb[1].x, height-20));
        float h = bb[2].y - bb[0].y; //abs(dist(width-20, bb[0].y, width-20, bb[2].y));
        float pw, ph, pcl;
        if (curUnits == UNITS_MM)  {
            pw = round(10.0f*w) / 10.0f;
            ph = round(10.0f*h) / 10.0f;
            pcl = round(10.0f*crvLen) / 10.0f;
        } else  {
            pw = round(100.0f*w*conversionFactor) / 100.0f;
            ph = round(100.0f*h*conversionFactor) / 100.0f;
            pcl = round(100.0f*crvLen*conversionFactor) / 100.0f;
        }

        textAlign(CENTER, CENTER);
        text(pw + unitStr, mainPanelSize[0]/2, mainPanelSize[1]-footerSize-previewPaddingY[1]+10);
        textAlign(RIGHT);
        text("Curve Length: " + pcl + unitStr, mainPanelSize[0]-previewPaddingX[1], mainPanelSize[1]-footerSize-previewPaddingY[1]+15);

        // display sideways, since it was falling off window
        pushMatrix();
        rotate(-HALF_PI);
        text(ph + unitStr, -(headerSize+previewSize[1]*.5f), previewPaddingX[0]-8);
        popMatrix();

        // draw the curve length in the settings panel
        // by constantly updating here we can't edit the text field
        if (displayState != DS_NOFILE && prevPcl != pcl)  {
            txWireLength.setText("" + pcl);
            prevPcl = pcl;
        }

        // draw num pts text
        WirePoints wps = curWirePoints.get(curPath);
        textAlign(RIGHT);
        text(wps.pts.size() + " points", mainPanelSize[0]-previewPaddingX[1], mainPanelSize[1]-footerSize-previewPaddingY[1]+32);
        text("Path " + (curPath+1) + " of " + nPaths, mainPanelSize[0]-previewPaddingX[1], mainPanelSize[1]-footerSize-previewPaddingY[1]+50);

        // if there are warnings, draw descriptive text
        if (wps.nFeedWarnings > 0 || wps.nAngleWarnings > 0)  {
            //textAlign(CENTER);
            textAlign(LEFT);
            textSize(18);
            fill(203, 80, 80);
            text("Warnings:", 10, mainPanelSize[1]-footerSize-previewPaddingY[1]+45);
            textSize(15);
            fill(70);
            text(wps.nFeedWarnings + " Segment(s) too short", 10, mainPanelSize[1]-footerSize-previewPaddingY[1]+65);
            text(wps.nAngleWarnings + " Angle(s) too sharp", 10, mainPanelSize[1]-footerSize-previewPaddingY[1]+80);
        }
    }


    // see if any warnings are present and set status
    public void setDrawWarnings ()  {
        if (curPath == -1)  {
            //status = STATUS_OK;
            return;
        }

        WirePoints wps = curWirePoints.get(curPath);
//    if (wps.nAngleWarnings > 0 || wps.nFeedWarnings > 0)
//        status = STATUS_HAS_ERRORS;
//    else
//        status = STATUS_OK;
    }

    // draws the status bar at the footer of the main window
// status variable holds status: 1 == Not loaded, 2 = OK, 3 = warning (feed or angle), 4 = sending/printing, 5 = connecting
    public void drawStatusBar ()  {
        rectMode(CORNER);
        noStroke();

        // draw bg rect
        switch (status)  {
            case STATUS_STARTING:
                fill(243, 243, 243);
                break;
            case STATUS_OK:
                fill(224, 239, 210);
                break;
            case STATUS_HAS_ERRORS:
                fill(249, 232, 232);
                break;
            case STATUS_SENDING:
                fill(204, 227, 246);
                break;
            case STATUS_CONNECTING:
                fill(239, 222, 184);
                break;
            case STATUS_HOMING:
                fill(239, 222, 184);
                break;
            case STATUS_CONNECTED:
                fill(239, 222, 184);
                break;
            case STATUS_NOT_CONNECTED:
                fill(239, 222, 184);
                break;
        }
        rect(0, mainPanelSize[1]-footerSize, width, footerSize);

        // draw diagonals
        switch (status)  {
            case STATUS_STARTING:
                fill(232, 232, 232);
                break;
            case STATUS_OK:
                fill(209, 231, 190);
                break;
            case STATUS_HAS_ERRORS:
                fill(239, 191, 191);
                break;
            case STATUS_SENDING:
                fill(156, 206, 237);
                break;
            case STATUS_CONNECTING:
                fill(238, 198, 115);
                break;
            case STATUS_CONNECTED:
                fill(238, 198, 115);
                break;
            case STATUS_NOT_CONNECTED:
                fill(238, 198, 115);
                break;
            case STATUS_HOMING:
                fill(238, 198, 115);
                break;
        }

        int w = 40;
        for (int i = -5; i < width; i += 2*w)  {
            beginShape();
            vertex(i, mainPanelSize[1]);
            vertex(i+w/2, mainPanelSize[1]- footerSize);
            vertex(i+w/2+w, mainPanelSize[1]-footerSize);
            vertex(i+w, mainPanelSize[1]);
            endShape(CLOSE);
        }

        // draw text
        String s = "";
        switch (status)  {
            case STATUS_STARTING:
                fill(130, 130, 130);
                break;
            case STATUS_OK:
                fill(89, 191, 99);
                if (doCycleTest)
                    s = cycleCount + " Cycles";
                else
                    s = "READY";
                break;
            case STATUS_HAS_ERRORS:
                fill(203, 80, 80);
                s = "WARNING";
                break;
            case STATUS_SENDING:
                fill(49, 138, 199);
                s = "PRINTING";
                break;
            case STATUS_CONNECTING:
                fill(244, 158, 52);
                s = "CONNECTING";
                break;
            case STATUS_CONNECTED:
                fill(244, 158, 52);
                s = "CONNECTED";
                break;
            case STATUS_NOT_CONNECTED:
                fill(244, 158, 52);
                s = "NOT CONNECTED";
                break;
            case STATUS_HOMING:
                fill(244, 158, 52);
                s = "HOMING";
                break;
        }

        textSize(28);
        textAlign(CENTER, TOP);
        text(s, mainPanelSize[0]/2, mainPanelSize[1]+3-(footerSize));

        // draw sep line
        stroke(180, 180, 180);
        strokeWeight(1);
        line(0, mainPanelSize[1]-footerSize, width, mainPanelSize[1]-footerSize);
    }


    // draw warning message in an overlay window-like box
// actually just drawn right on top of main window
    public void drawWarning ()  {
        rectMode(CENTER);
        fill(255, 255, 255, 150);
        rect(width/2, height/2, width+10, height+10);

        fill(240);
        stroke(180);
        rect(width/2, height/2, 400, 250);



        if (popUpWarning == WARNING_FEED)  {
            fill(10, 10, 10);
            textSize(28);
            textAlign(CENTER, TOP);
            text("HEADS UP", width/2, height/2-80);
            if (curPath != -1)  {
                WirePoints wps = curWirePoints.get(curPath);

                textSize(16);
                textAlign(CENTER, TOP);
                text("You have: " + wps.nFeedWarnings + " short feeds and ", width/2, height/2-30);
                text("          "  + wps.nAngleWarnings + " sharp angles", width/2, height/2-10);
                text("which may cause inaccurate bending", width/2, height/2+10);
                text("Bend anyway?", width/2, height/2+40);
                btnIgnoreError.setVisible(true);
                btnCancelSendError.setVisible(true);
            }
        } else if (popUpWarning == WARNING_CONNECTION)  {
            fill(10, 10, 10);
            textSize(28);
            textAlign(CENTER, TOP);
            text("WARNING", width/2, height/2-80);
            textSize(16);
            textAlign(CENTER, TOP);
            text("There is a problem with", width/2, height/2-30 );
            text("the serial connection.", width/2, height/2-10 );
            text("Make sure the machine is", width/2, height/2+10);
            text("connected and powered on", width/2, height/2+30);
            btnSerialError.setVisible(true);
        } else if (popUpWarning == WARNING_MID_BEND)  {
            fill(10, 10, 10);
            textSize(28);
            textAlign(CENTER, TOP);
            text("WARNING", width/2, height/2-80);
            textSize(16);
            textAlign(CENTER, TOP);
            text("Lost Connection with the DIWire.", width/2, height/2-30 );
            text("Reconnect and press resume", width/2, height/2-10 );
            text("", width/2, height/2+10);
            text("", width/2, height/2+30);
            btnMidBendError.setVisible(true);
        } else if (popUpWarning == WARNING_LIMIT_SWITCH)  {
            fill(10, 10, 10);
            textSize(28);
            textAlign(CENTER, TOP);
            text("WARNING", width/2, height/2-80);
            textSize(16);
            textAlign(CENTER, TOP);
            text("Woah! That angle was too big.", width/2, height/2-30 );
            text("press the flashing blue button on the", width/2, height/2-10 );
            text("machine", width/2, height/2+10);
            text("", width/2, height/2+30);
            btnBackOffLimitError.setVisible(true);

            // looks like we wait for the button on the DIWire to be pushed here
            if (tinyGFlashReady == false)  {
                btnBackOffLimitError.setEnabled(false);
            } else {
                btnBackOffLimitError.setEnabled(true);
            }
        }
        else if (popUpWarning == WARNING_CONT_HOME)  {
            fill(10, 10, 10);
            textSize(28);
            textAlign(CENTER, TOP);
            text("WARNING", width/2, height/2-80);
            textSize(16);
            textAlign(CENTER, TOP);
            text("The DIWire is not homed", width/2, height/2-30 );
            text("Go to manual mode to set the left  ", width/2, height/2-10 );
            text("and right zero positions ", width/2, height/2+10);
            text("", width/2, height/2+30);
            btnSerialError.setVisible(true);

        }
    }


    // draw grids of calibration numbers
    public void drawCalibrationGrid ()  {
        int i;

        // if these are changed, must also change in mouseReleased() in mouse
        float hsCW = mainPanelSize[0]*.25f;
        float hsCCW = mainPanelSize[0]*.75f;
        float tw = 100, th = 25, top = 115;    // text box width, text box height, top of box
        int nRows = 21;

        // left box is Clockwise
        line(hsCW-tw,top, hsCW+tw,top);
        line(hsCW-tw,top, hsCW-tw,top+nRows*th);
        line(hsCW+tw,top, hsCW+tw,top+nRows*th);
        line(hsCW-tw,top+nRows*th, hsCW+tw,top+nRows*th);
        line(hsCW,top+th, hsCW,top+nRows*th);

        // right box is Counter-Clockwise
        line(hsCCW-tw,top, hsCCW+tw,top);
        line(hsCCW-tw,top, hsCCW-tw,top+nRows*th);
        line(hsCCW+tw,top, hsCCW+tw,top+nRows*th);
        line(hsCCW-tw,top+nRows*th, hsCCW+tw,top+nRows*th);
        line(hsCCW,top+th, hsCCW,top+nRows*th);

        // draw row lines
        for (i = 1; i < nRows; ++i)  {
            line(hsCW-tw, top+th*i, hsCW+tw, top+th*i);
            line(hsCCW-tw, top+th*i, hsCCW+tw, top+th*i);
        }

        // text at top of boxes
        textAlign(CENTER);
        textSize(15);
        text("Clockwise", hsCW, top+th-5);
        text("Attempted", hsCW-tw*.5f, top+2*th-5);
        text("Actual", hsCW+tw*.5f, top+2*th-5);
        text("Counter-Clockwise", hsCCW, top+th-5);
        text("Attempted", hsCCW-tw*.5f, top+2*th-5);
        text("Actual", hsCCW+tw*.5f, top+2*th-5);

        // text in boxes
        String degStr = "\u00b0";
        for (i = 0; i < posCalPts.length; ++i)  {
            text(nf(posCalPts[i],1,3), hsCW-tw*.5f, top+th*(i+3)-5);
            //if ((curCalibDir == CALIB_DIR_CW && i < curCalibIdx) || curCalibDir == CALIB_DIR_CCW)
            if (posCalRes[i] != NO_CALIBRATION)  {
                if (posCalPts[i] == 0.0f)
                    text(nf(0.0f,1,3), hsCW+tw*.5f, top+th*(i+3)-5);
                else
                    text(nf(posCalRes[i],1,3), hsCW+tw*.5f, top+th*(i+3)-5);
            }

            if (curCalibDir == CALIB_DIR_CW && i == curCalibIdx)  {
                fill(255,255,0,50);
                rect(hsCW, top+th*(i+2), tw, th);
                fill(130);
            }
        }

        for (i = 0; i < negCalPts.length; ++i)  {
            text(nf(negCalPts[i],1,3), hsCCW-tw*.5f, top+th*(i+3)-5);
            //if ((curCalibDir == CALIB_DIR_CCW && i < curCalibIdx) || curCalibState == CALIB_STATE_FINISHED)
            if (negCalRes[i] != NO_CALIBRATION)  {
                if (negCalPts[i] == 0.0f)
                    text(nf(0.0f,1,3), hsCCW+tw*.5f, top+th*(i+3)-5);
                else
                    text(nf(negCalRes[i],1,3), hsCCW+tw*.5f, top+th*(i+3)-5);
            }

            if (curCalibDir == CALIB_DIR_CCW && i == curCalibIdx)  {
                fill(255,255,0,50);
                rect(hsCCW, top+th*(i+2), tw, th);
                fill(130);
            }
        }

    }


    // draws the fills, lines and text for the Settings panel
//  the GUI elements (buttons, sliders, etc.) are drawn automatically
    public void drawSettingsPanel ()  {
        if (curViewMode == VIEW_LOAD)  {
            return;
        } else  {
            rectMode(CORNER);
            fill(settingsPanelColor);
            rect(mainPanelSize[0], 0, settingsPanelWidth+5, height+10);

            // draw header line - done in drawWindowFrame(), but overwritten by rect above
            stroke(headerLineColor);
            strokeWeight(headerLineWeight);
            line(mainPanelSize[0], headerSize, width, headerSize);

            // draw Control panel divider lines
            //line(mainPanelSize[0], headerSize+420, width, headerSize+420);

            // draw Control Panel Text
            fill(130);
            stroke(130);
            textSize(20);
            textAlign(LEFT);

            // title text
            text("SETTINGS", mainPanelSize[0]+10, headerSize+25);
            line(mainPanelSize[0], headerSize+35, width, headerSize+35);

            if (curViewMode == VIEW_EDIT)  {
                // section text
                // MTL PROFILE, MAT PROFILE,
                text("MATERIAL PROFILE", mainPanelSize[0]+10, headerSize+65);
                line(mainPanelSize[0], headerSize+120, width, headerSize+120);
                text("DISPLAY UNITS", mainPanelSize[0]+10, headerSize+150);
                textSize(17);
                text("mm", mainPanelSize[0]+30, headerSize+175);
                text("in", mainPanelSize[0]+100, headerSize+175);
                line(mainPanelSize[0], headerSize+190, width, headerSize+190);
                textSize(20);
                text("WIRE LENGTH", mainPanelSize[0]+10, headerSize+220);
                textSize(15);
                text(curUnits == UNITS_MM ? "mm" : "in", mainPanelSize[0]+80, headerSize+247);
                textSize(20);
                text("SCALE", mainPanelSize[0]+10, headerSize+285);
                text("x", mainPanelSize[0]+80, headerSize+313); //changed scale text box to scale factor "X", not percent
                line(mainPanelSize[0], headerSize+365, width, headerSize+365);
                text("RESOLUTION", mainPanelSize[0]+10, headerSize+400);
                textSize(13);
                text("Segment Length", mainPanelSize[0]+80, headerSize+428);
                text("Longer", mainPanelSize[0]+10, headerSize+472);
                text("Shorter", mainPanelSize[0]+145, headerSize+472);
                textSize(15);
                if (!continuousFlag)
                    text("Hide Original File", mainPanelSize[0]+40, headerSize+535);
                text("REPEAT         TIME(S)", mainPanelSize[0]+10, headerSize+570);

                line(mainPanelSize[0], headerSize+580, width, headerSize+580);
                textSize(20);
                text("K FACTOR",mainPanelSize[0]+10, headerSize+610);
                textSize(13);
                text("Default = 0.5",mainPanelSize[0]+80, headerSize+640);
                text("> 0.5 : Longer",mainPanelSize[0]+10, headerSize+660);
                text("= 0.5 : Wire Center",mainPanelSize[0]+10, headerSize+675);
                text("< 0.5 : Shorter",mainPanelSize[0]+10, headerSize+690);

                if (btnCloseGaps.isVisible())  {
                    line(mainPanelSize[0], headerSize+560, width, headerSize+560);
                    textSize(20);
                    text("GAP THRESHOLD", mainPanelSize[0]+10, headerSize+590);
                }

                // lines between tool icons
                float vc = previewPaddingX[0]+previewSize[0]*.5f;
                line(vc-54, headerSize+7, vc-54, headerSize+28);
                line(vc-18, headerSize+7, vc-18, headerSize+28);
                line(vc+18, headerSize+7, vc+18, headerSize+28);
                line(vc+54, headerSize+7, vc+54, headerSize+28);
            } else if (curViewMode == VIEW_CALIBRATE)  {
                // since there are multiple states here, we need to have finer control of what's visible
                if (curCalibMode == CALIB_START)  {
                    textAlign(CENTER);
                    textSize(25);
                    text("Choose a Material Profile\nto edit or create a new one", mainPanelSize[0]*.5f, headerSize+55);
                    textAlign(LEFT);
                    textSize(20);
                    text("MATERIAL PROFILE", mainPanelSize[0]+10, headerSize+75);
                    //line(mainPanelSize[0], headerSize+160, width, headerSize+160);
                } else if (curCalibMode == CALIB_NEW)  {
                    textAlign(CENTER);
                    textSize(25);
                    text("Choose a Profile name and settings\nand click the button to start Calibration", mainPanelSize[0]*.5f, headerSize+55);
                    textAlign(LEFT);
                    textSize(20);
                    text("PROFILE NAME", mainPanelSize[0]+10, headerSize+65);
                    line(mainPanelSize[0], headerSize+120, width, headerSize+120);
                    text("MATERIAL", mainPanelSize[0]+10, headerSize+150);
                    line(mainPanelSize[0], headerSize+205, width, headerSize+205);
                    text("BEND HEAD", mainPanelSize[0]+10, headerSize+235);
                    line(mainPanelSize[0], headerSize+290, width, headerSize+290);
                    text("FEED WHEEL", mainPanelSize[0]+10, headerSize+320);
                    line(mainPanelSize[0], headerSize+375, width, headerSize+375);
                    text("ACCURACY", mainPanelSize[0]+10, headerSize+410);
                    text("High", mainPanelSize[0]+30, headerSize+442);
                    text("Standard", mainPanelSize[0]+30, headerSize+472);
                    text("Quick", mainPanelSize[0]+30, headerSize+502);
                } else if (curCalibMode == CALIB_EDIT)  {
                    textAlign(CENTER);
                    textSize(25);
                    text("Edit Profile name and settings\nand click a button to continue", mainPanelSize[0]*.5f, headerSize+55);
                    textAlign(LEFT);
                    textSize(20);
                    text("PROFILE NAME", mainPanelSize[0]+10, headerSize+65);
                    line(mainPanelSize[0], headerSize+120, width, headerSize+120);
                    text("MATERIAL", mainPanelSize[0]+10, headerSize+150);
                    line(mainPanelSize[0], headerSize+205, width, headerSize+205);
                    text("BEND HEAD", mainPanelSize[0]+10, headerSize+235);
                    line(mainPanelSize[0], headerSize+290, width, headerSize+290);
                    text("FEED WHEEL", mainPanelSize[0]+10, headerSize+320);
                } else if (curCalibMode == CALIB_REFINE)  {
                    // text on top and Setting Panel
                    textAlign(CENTER);
                    textSize(20);
                    text("Click on an Actual entry to edit that angle\nor leave blank to re-calibrate", mainPanelSize[0]*.5f, headerSize+55);
                    line(0, 95, mainPanelSize[0], 95);
                    textAlign(LEFT);
                    textSize(20);
                    text("MATERIAL PROFILE", mainPanelSize[0]+10, headerSize+65);
                    textSize(16);
                    text(curWorkingCalibFileName, mainPanelSize[0]+10, headerSize+90);
                    //textAlign(CENTER);
                    textSize(16);
                    text("LOW", mainPanelSize[0]+22, headerSize+520);
                    text("HI", mainPanelSize[0]+91, headerSize+520);
                    text("STEPS", mainPanelSize[0]+140, headerSize+520);

                    drawCalibrationGrid();
                } else if (curCalibMode == CALIB_RUNNING)  {
                    // text on top and Setting Panel
                    textAlign(CENTER);
                    textSize(20);
                    if (curCalibState == CALIB_STATE_STARTING)
                        text("Insert wire into DIWire so it is flush with Bend Head", mainPanelSize[0]*.5f, headerSize+55);
                    else if (curCalibState == CALIB_STATE_FINISHED)
                        text("Calibration completed!  Click OK to continue.", mainPanelSize[0]*.5f, headerSize+55);
                    else
                        text("After each bend, use the keyboard arrows (\u2190 \u2192)\nto move the bend pin until it touches the wire then click OK", mainPanelSize[0]*.5f, headerSize+55);
                    line(0, 95, mainPanelSize[0], 95);
                    textAlign(LEFT);
                    textSize(20);
                    text("MATERIAL PROFILE", mainPanelSize[0]+10, headerSize+65);
                    textSize(16);
                    text(curWorkingCalibFileName, mainPanelSize[0]+10, headerSize+90);

                    drawCalibrationGrid();

                } else  {
                    println("ERROR: Bad calibration mode: " + curCalibMode);
                }
            } else if (curViewMode == VIEW_MANUAL)  {
                text("MATERIAL PROFILE", mainPanelSize[0]+10, headerSize+65);
                line(mainPanelSize[0], headerSize+120, width, headerSize+120);
                text("UNITS", mainPanelSize[0]+10, headerSize+150);
                textSize(17);
                text("mm", mainPanelSize[0]+30, headerSize+175);
                text("in", mainPanelSize[0]+100, headerSize+175);
                line(mainPanelSize[0], headerSize+190, width, headerSize+190);

                textAlign(CENTER);

                if(!manInputs){
                    textSize(18);
                    text("FEED\nFORWARD", mainPanelSize[0]*.5f, mainPanelSize[1]*.5f-260);
                    text("REVERSE\nFEED", mainPanelSize[0]*.5f, mainPanelSize[1]*.5f+140);
                    text("BEND\nCCW", mainPanelSize[0]*.5f-215, mainPanelSize[1]*.5f-60);
                    text("BEND\nCW", mainPanelSize[0]*.5f+210, mainPanelSize[1]*.5f-60);

                    text("Or use keyboard arrow keys", mainPanelSize[0]*.5f, mainPanelSize[1]*.5f+235);
                }
                else{
                    textSize(18);
                    text("FEED\nFORWARD", mainPanelSize[0]*.5f, mainPanelSize[1]*.5f-280);
                    text("REVERSE\nFEED", mainPanelSize[0]*.5f, mainPanelSize[1]*.5f+160);
                    text("BEND\nCCW", mainPanelSize[0]*.5f-215, mainPanelSize[1]*.5f-100);
                    text("BEND\nCW", mainPanelSize[0]*.5f+210, mainPanelSize[1]*.5f-100);
                    textAlign(LEFT);
                    textSize(17);
                    String un="mm";
                    if(curUnits==UNITS_INCH)
                        un="in";
                    text(un, mainPanelSize[0]*.5f+20, mainPanelSize[1]*.5f-223);
                    text(un, mainPanelSize[0]*.5f+20, mainPanelSize[1]*.5f+130);
                    text("deg", mainPanelSize[0]*.5f-200, mainPanelSize[1]*.5f-45);
                    text("deg", mainPanelSize[0]*.5f+215, mainPanelSize[1]*.5f-45);
                }

                textAlign(LEFT);
                textSize(20);
                int h=150;
                text("JOG SPEED", mainPanelSize[0]+10, headerSize+65+h);
                text("INPUTS", mainPanelSize[0]+10, headerSize+240+h);
                textSize(16);
                text("Very Fast", mainPanelSize[0]+24, headerSize+98+h);
                text("Fast", mainPanelSize[0]+24, headerSize+128+h);
                text("Slow", mainPanelSize[0]+118, headerSize+98+h);
                text("Very Slow", mainPanelSize[0]+118, headerSize+128+h);
                line(mainPanelSize[0], headerSize+150+h, width, headerSize+150+h);
                text("No", mainPanelSize[0]+30, headerSize+265+h);
                text("Yes", mainPanelSize[0]+120, headerSize+265+h);


            }
        }
    }
/*
    Handles drag-n-drop of files into app

    Can drag-n-drop into main window at any time
    BG image is drawn in Load mode, in other modes bg color changes - based on checkDropHover()
*/

    public void initializeDrop ()  {
        drop = new SDrop(this);
        dropListener = new DIWDropListener(dropArea);
        drop.addDropListener(dropListener);
        dropArea = loadImage("images/drop_static.png");
        dropHover = loadImage("images/drop_drop.png");
        dropArea.resize(0, round((mainPanelSize[0]-headerSize-footerSize)*.7f));
        dropHover.resize(0, round((mainPanelSize[0]-headerSize-footerSize)*.7f));
    }


    // a custom DropListener class.
    class DIWDropListener extends DropListener  {
        int bgColor;
        boolean hover = false;
        PImage img;

        DIWDropListener (PImage _img)  {
            bgColor = color(255);
            img = _img;
            // set a target rect for drop event.
        }

        public void draw ()  {
            fill(bgColor);
            rectMode(CENTER);
        }

        public void setTarget (int x,int y, int xx, int yy)  {
            setTargetRect(x+10,y+10,xx-20,yy-20);
        }

        // if a dragged object enters the target area.
        // dropEnter is called.
        public void dropEnter ()  {
            hover = true;
        }

        // if a dragged object leaves the target area.
        // dropLeave is called.
        public void dropLeave ()  {
            hover = false;
        }

        // used in draw routines to draw regular or highlit bg
        public boolean checkDropHover ()  {
            return hover;
        }

        public void dropEvent (DropEvent theEvent)  {
            if (theEvent.isFile())  {
                loadWireObjectFile(theEvent.file().getPath());
                //saveDefaults();
            }
        }
    }

    // loads in a wire file, either an SVG or a DXF, which gets converted to SVG
    public void loadWireObjectFile (String fileName) {
        //println("loadWireObjectFile()");
        txtFileLoaded=false;
        int l = fileName.length();
        String ext = fileName.substring(l-3);
        if (ext.toLowerCase().equals("dxf")) {
            // G4P is limited in the kinds of dialogs it can show - use java.swing here directly
            // see: http://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
            Object[] filterOptions = {
                    "No Filtering", "Blue Lines", "Dashed Lines", "Dot-Dashed Lines", "Hatched Lines"
            };
            String filterStr = (String)JOptionPane.showInputDialog(
                    frame, // already defined by Processing
                    "Choose filter option for DXF 2 SVG conversion:     \n\n", // message str
                    "Choose Filter", // title str
                    JOptionPane.PLAIN_MESSAGE, // type of message, could also be QUESTION_MESSAGE, WARNING_MESSAGE, etc.
                    null, // icon
                    filterOptions, // list of choices
                    "No Filtering");              // default choice
            if (filterStr == null || filterStr.length() == 0) {
                println("DXF2SVG Cancelled.");
                return;
            }
            int ft = FILTER_NONE;
            if (filterStr.equals("Blue Lines")) ft = FILTER_BLUE;
            if (filterStr.equals("Dashed Lines")) ft = FILTER_DASHED;
            if (filterStr.equals("Dot-Dashed Lines")) ft = FILTER_DASHDOT;
            if (filterStr.equals("Hatched Lines")) ft = FILTER_HATCH;

            fileName = DXF2SVG(fileName, ft);        // read DXF and convert to SVG, returns filename of SVG file
            if (fileName.isEmpty()) return;          // check for failure
            ext = "svg";

            isDXFFile = true;
        } else  {
            isDXFFile = false;
        }

        // sldGapThresh.setVisible(isDXFFile); // keeping gap threshold hidden
        // btnCloseGaps.setVisible(isDXFFile); // keeping close gaps button hidden

        if (ext.toLowerCase().equals("svg")) {
            //showPath = 0;

            curFile = fileName;

            curShape = RG.loadShape(fileName);    // load SVG file

            if (!isDXFFile)  {
                curShape.scale(0.35239721155f);  //account for ppi
            }

            safePrint("Loaded File: " + fileName);
            //curShape.print();

            // count and pull out paths from shape
            findPaths();

            // subdivide the paths into the wire points, based on current scale and resolution
            // wait on this until user chooses paths?
            generateWirePoints();

            // figure out viewing transforms to frame shape
            frameModel();

            // generate the feed and bend angle data and detect warning conditions
            calculateFeedsAndBendAngles();

            // put the file name into the window title - could get rid of path
            frame.setTitle("DIWire - " + curFile);

            // reset GUI params
            resetGUI();
            doneBendingGUI();

            displayState = DS_LOADED;
            curViewMode = VIEW_EDIT;
            status=STATUS_CONNECTING;
            thread("testSerial");

            if (nPaths>0) curPath=0;

            displayNeedsUpdate = true;
        }

        if (ext.toLowerCase().equals("txt")) {
            //showPath = 0;

            curFile = fileName;
            if(loadTxt(fileName)){

            }

            // put the file name into the window title - could get rid of path
            frame.setTitle("DIWire - " + curFile);

            // reset GUI params
            resetGUI();
            doneBendingGUI();
            displayState = DS_LOADED;
            curViewMode = VIEW_EDIT;
            displayNeedsUpdate = true;
        }
    }


/*
    functions used for calculations throughout the application

*/

    boolean doSafePrint = true;

    public void safePrint (String s) {
        if (doSafePrint)
            println(s);
    }



    // open a file
    public void openFile (GImageButton button, GEvent event) {
        selectInput("Select a file to read in:", "fileSelected");
    }

    public void openFile () {
        selectInput("Select a file to read in:", "fileSelected");
    }

    // close a file and return to drop screen
    public void closeFile () {
        curFile = "";
        loadWireObjectFile("empty.svg");
        frame.setTitle("DIWire - No File");
        status = STATUS_STARTING;
        displayState = DS_NOFILE;
        curViewMode = VIEW_LOAD;

    }

    public void fileSelected (File selection) {
        if (selection == null) {
            safePrint("File Selection window was closed or the user hit cancel.");
        } else {
            loadWireObjectFile(selection.getPath());
        }
    }





    // adjust camera scale and translate to fit the current shape into the preview area with a little pad around border
    public void frameModel () {
        frameModel(false);
    }

    public void frameModel (boolean allPaths) {

        if (curWirePoints.isEmpty())
            return;

        RPoint tl = new RPoint(), br = new RPoint();

        // don't use source shape, use points
//    if (allPaths || curPath == -1) {
//        tl = curShape.getTopLeft();
//        br = curShape.getBottomRight();
//    } else {
//        tl = curPaths.get(curPath).getTopLeft();
//        br = curPaths.get(curPath).getBottomRight();
//    }
        //println("tl = " + tl.x + " " + tl.y + "  br = " + br.x + " " + br.y);

        // loop over either just current path or all paths
        int start = curPath, end = curPath+1;
        if (allPaths || curPath == -1)  {
            start = 0;
            end = nPaths;
        }

        for (int p = start; p < end; ++p)  {
            WirePoints wps = curWirePoints.get(p);

            if (p == start)  {
                tl.x = br.x = wps.pts.get(0).pt.x;
                tl.y = br.y = wps.pts.get(0).pt.y;
            }

            for (int i = 0; i < wps.pts.size(); ++i)  {
                WirePoint wp = wps.pts.get(i);
                if (wp.pt.x < tl.x) tl.x = wp.pt.x;
                if (wp.pt.y < tl.y) tl.y = wp.pt.y;
                if (wp.pt.x > br.x) br.x = wp.pt.x;
                if (wp.pt.y > br.y) br.y = wp.pt.y;
            }
        }

        float oh = (br.x-tl.x) * 1.1f;
        float ov = (br.y-tl.y) * 1.1f;
        float ox = tl.x - (br.x-tl.x)*.05f;
        float oy = tl.y - (br.y-tl.y)*.05f;

        float sclh = previewSize[0] / oh;
        float sclv = previewSize[1] / ov;
        // should probably have a max scale
        if (sclh < sclv) {
            camScl = sclh;
            camX = -ox * sclh;
            camY = -oy * sclh + (previewSize[1]-(ov*sclh))*.5f;    // center
        } else {
            camScl = sclv;
            camX = -ox * sclv + (previewSize[0]-(oh*sclv))*.5f;    // center
            camY = -oy * sclv;
        }

        //println("cam params: " + camX + " " + camY + " " + camScl);
    }


    // find all the paths in the current shape
    public void findPaths () {
        nPaths = 0;
        curPaths.clear();
        findPaths(curShape);
        println("Found " + nPaths + " paths in shape.");
        curPath = -1; //(nPaths > 0) ? 0 : -1;
    }

    // recurse on shape, pulling out path info
    public void findPaths (RShape shp) {
        int i, nc = shp.countChildren(), np = shp.countPaths();

        for (i = 0; i < np; ++i) {
            if (shp.paths[i].countCommands()>0){
                ++nPaths;
                curPaths.add(shp.paths[i]);
            }

            // try to adjust the path style - these don't do anything
            // seems like somewhere RG.ignoreStyles is set to true
            //        shp.paths[i].setFill(false);
            //        shp.paths[i].setStroke(true);
            //        //shp.paths[i].setStroke(0);
            //        shp.paths[i].setStroke(0xFF0000);
            //        //shp.paths[i].setStrokeWeight(1);
            //        shp.paths[i].setStrokeWeight(5);
        }

        for (i = 0; i < nc; ++i)
            findPaths(shp.children[i]);
    }


    // sample the current paths to generate wire points
    public void generateWirePoints () {
        curWirePoints.clear();
        for (int i = 0; i < nPaths; ++i) {
            WirePoints wps = new WirePoints();

            // the points we will sample go in here
            RPoint[] pts = {};

            if (dlSampling.getSelectedIndex() == 2)  {
                // adaptive method (new)

                // turn on special tolerances
                // shouldn't be any distance aspect to an angle-based sampler
                RG.setPolygonizerDistTolSqr(1000000);        // default value is 0.25
                RG.setPolygonizerDistTolMnhttn(1000000);     // default value is 4.0

                RG.setPolygonizer(RG.ADAPTATIVE);
                //RG.setPolygonizerAngle(.25);    // sets ADAPTATIVE - radians - seems to max out around .25 rad = ~14 deg

                // compute the adaptive angle from the resolution value in the GUI slider
                float t = (GUIRes - GUIResolutionLimits[0]) / (GUIResolutionLimits[1] - GUIResolutionLimits[0]);
                float ang = .15f + 1.2f*t;
                RG.setPolygonizerAngle(ang);

                pts = curPaths.get(i).getPoints();

            } else if (dlSampling.getSelectedIndex() == 1 )  {
                // old method - curve (Line Segments OFF)

                // set these to defaults
                RG.setPolygonizerDistTolSqr(.25f);
                RG.setPolygonizerDistTolMnhttn(4.0f);

                double m = curPaths.get(i).getCurveLength();
                double quant = floor((float)m/GUIRes);
                //continuous Bend feed dist
                // println("curve length "+m*GUIScale);
                //println("quant "+quant);

                wps.continuousBendDist=(float)m*GUIScale/(float)quant;

                RG.setPolygonizer(RG.UNIFORMLENGTH);
                RG.setPolygonizerLength((float)m/(float)quant);

                pts = curPaths.get(i).getPoints();
                //pts.remove(pts.length-1);

            } else if (dlSampling.getSelectedIndex() == 0 )  {
                // old method - paths (Line Segments ON)

                // set these to defaults
                RG.setPolygonizerDistTolSqr(.25f);
                RG.setPolygonizerDistTolMnhttn(4.0f);

                //println("curPaths.get(i).countCommands() = " + curPaths.get(i).countCommands());
                RPoint lastPt = new RPoint();
                for (int k = 0; k < curPaths.get(i).countCommands(); ++k) {
                    RCommand cmd = curPaths.get(i).commands[k];
                    cmd.setSegmentator(RG.UNIFORMSTEP);
                    // if command is a straight line, just do one step
                    if (cmd.getCommandType() == 0) {
                        //println("command " + k + " - line seg");
                        cmd.setSegmentStep(1);
                    } else {
                        float m = cmd.getCurveLength();
                        //println("command " + k + " - seg step = " + floor(m/GUIRes) + " m = " + m + " GUIRes = " + GUIRes);
                        float step = floor(m/GUIRes);
                        if (step < 1.0f) step = 1.0f;    // value < 1.0 means step length not num steps
                        cmd.setSegmentStep(step);
                    }

                    // difference with other methods - points are accumulated per-command
                    // otherwise, seems to use last setSegmentStep setting throughout
                    RPoint[] cp = cmd.getPoints();
                    int start = 0;
                    if (k > 0 && cp[0].x == lastPt.x && cp[0].y == lastPt.y) start = 1;
                    // http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
                    RPoint[] tmp = Arrays.copyOf(pts, pts.length + cp.length - start);
                    System.arraycopy(cp, start, tmp, pts.length, cp.length-start);
                    pts = tmp;
                    lastPt = cp[cp.length-1];
                }

            }

            int npts = pts.length;
            //println("npts = " + npts);

            // seem to always get a duplicated point at the end
            if (npts > 1 && abs(pts[npts-2].x - pts[npts-1].x)<4 && abs(pts[npts-2].y - pts[npts-1].y)<4) {
                //println("Remove duplicate end point");
                --npts;
            }
            //println("Path " + i + " npts = " + npts);
            for (int j = 0; j < npts; ++j) {
                //print(pts[j].x + "," + pts[j].y + " ");
                WirePoint wp = new WirePoint(new RPoint(pts[j]), POINT_NONE);
                wps.pts.add(wp);
            }
            //println("");

            curWirePoints.add(wps);

        }

    }

    // save file
    public void saveSVG (boolean saveAs) {
        // TO DO: make this work, with embedded meta-data

        saveShp = new RShape();

        for (int i = 0; i < curWirePoints.size (); ++i) {
            WirePoints wps = curWirePoints.get(i);
            RPath pt = new RPath();
            for (int j = 0; j < wps.pts.size ()-1; ++j) {
                WirePoint wp0, wp1;
                wp0 = wps.pts.get(j);
                wp1 = wps.pts.get(j+1);
                RPoint s = new RPoint(wp0.pt.x, wp0.pt.y);
                RPoint v = new RPoint(wp1.pt.x, wp1.pt.y);
                RCommand c = new RCommand(s, v);
                pt.addCommand(c);
            }
            saveShp.addPath(pt);

        }


        saveShp.setFill("none");
        saveShp.setStroke("#000000");
        saveShp.scale(1/0.35239721155f);

        int l = curFile.length();
        String ext = curFile.substring(l-4);

        if (!ext.toLowerCase().equals(".svg")) {
            saveAs=true;
        }

        if (saveAs){
            File tmp = new File("Bend_this.svg");
            selectOutput("Select a file to write to:", "fileSelectOut", tmp);
        }
        else {
            File tmp = new File(curFile);
            fileSelectOut (tmp);
        }



    }

    public void fileSelectOut (File selection) {
        String n = selection.getAbsolutePath();
        int l = n.length();
        String ext = n.substring(l-4);

        if (!ext.toLowerCase().equals(".svg")) {
            n += ".svg";
        }

        RG.saveShape(n, saveShp.toShape());
        String lines[] = loadStrings(n);

        for (int i = 0; i < lines.length; i++) {
            int ck = lines[i].indexOf("<path");
            if (lines[i].indexOf("<g")>-1 || lines[i].indexOf("</g>")>-1) {
                lines[i] = "";
            } else if (ck > -1) {
                lines[i] = "<path fill=\"none\" stroke=\"#000000\" "+lines[i].substring(ck+5, lines[i].length());
            }
        }
        saveStrings(n, lines);
        loadWireObjectFile(n);
    }



/*
void setSerialNumber (String st) {
    for (TableRow row : serialNumbersTable.rows ()) {
        int serial = row.getInt("serial_number");
        String id = row.getString("tinyg_id");
        if (st.equals(id)) {
            curSerialNumber = serial;
            break;
        }
    }

    if (curSerialNumber == 0) {
        TableRow newRow2 = serialNumbersTable.addRow();
        newRow2.setInt("serial_number", serialNumbersTable.getRowCount());
        newRow2.setString("tinyg_id", st);
        saveTable(serialNumbersTable, serialNumFileName);
        curSerialNumber = newRow2.getInt("serial_number");
    }
    safePrint("curSerialNumber: " + curSerialNumber);
    //calculateAngle2MMPolyFit();    // this was called here because serial number used to be part of calib file name
}
*/


    // not sure what this is doing, cycling the hardware probably
// note that currently this will never end (cycleCount never incremented)
    public void cycleTest () {
        if (cycleCount < 1500) {
            if(!cycleDir){
                cycleAng=cycleAng+5;
                sendAngle(cycleAng);
                if (cycleAng>=125){
                    cycleAng=0;
                    cycleDir=true;
                    sendFeed(25);
                }
            }
            else{
                cycleAng=cycleAng-5;
                sendAngle(cycleAng);
                if (cycleAng<=-125){
                    cycleAng=0;
                    cycleDir=false;
                    sendFeed(25);
                    cycleCount++;
                    println("completed "+cycleCount+ " Loops");
                }
            }

//            if (!cycleDir) {
//                sendAngle(72);
//                cycleDir = true;
//            } else {
//                cycleDir = false;
//                sendFeed(35);
//            }
        } else {
            doCycleTest = false;
            println("ended!!!");
        }
    }


    public void zoom(float x, float y, float delta) {
        if (x==0&&y==0) {
            x=previewSize[0]*.5f;
            y=previewSize[1]*.5f;
        } else {
            x=(x-previewPaddingX[0]);
            y=(y-(headerSize+previewPaddingY[0]));
        }
        // should zoom around either mouse or center of preview area
        // currently scales around (0,0) in object space
        //camScl += delta*.1;
        //if (camScl < .1) camScl = .1;

        // scale around preview area center
        float oldScl = camScl;

        if (abs(delta)==1) {
            camScl *= pow(1.1f, delta);    // uniform speed
            //camScl += delta*.1;            // this slows down as scale factor gets large
        } else {
            camScl *= (previewSize[0])/delta;
        }
        if (camScl < .1f) camScl = .1f;
        camX = previewSize[0]*.5f - (camScl/oldScl)*(x - camX);
        camY = previewSize[1]*.5f - (camScl/oldScl)*(y - camY);
    }


    public boolean valueIsInArray(int v, int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (v == arr[i]) {
                return true;
            }
        }
        return false;
    }


    public void arrow(int x1, int y1, int x2, int y2) {
        previewGraphics.line(x1, y1, x2, y2);
        previewGraphics.pushMatrix();
        previewGraphics.translate(x2, y2);
        float a = atan2(x1-x2, y2-y1);
        previewGraphics.rotate(a);
        previewGraphics.line(0, 0, -10, -10);
        previewGraphics.line(0, 0, 10, -10);
        previewGraphics.popMatrix();
    }


    public void reverseArray() {
        WirePoints tmp= curWirePoints.get(curPath);
        Collections.reverse(tmp.pts);
        calculateFeedsAndBendAngles();
    }


    public void changeStartPoint() {
        if (curPoint.length > 0) {
            WirePoints tmp = curWirePoints.get(curPath);
            if (curPoint[0]==tmp.pts.size()-1 ){
                reverseArray();
            }
            else if ( curPoint[0]!=0){
                tmp.startPoint = curPoint[0];
                if (tmp.pts.get(0)==tmp.pts.get(tmp.pts.size()-1)){
                    tmp.pts.remove(tmp.pts.size()-1);
                }
                Collections.rotate(tmp.pts, -tmp.startPoint);
                tmp.pts.add(new WirePoint(new RPoint(tmp.pts.get(0).pt.x, tmp.pts.get(0).pt.y), POINT_NONE) );
                tmp.startPoint=0;
                calculateFeedsAndBendAngles();
            }


        }
    }


    public void appendTextToFile(String filename, String text){
        File f = new File(dataPath(filename));
        if(!f.exists()){
            createFile(f);
        }
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
            out.println(text);
            out.close();
        }catch (IOException e){
            //appendTextToFile(errorFilename, "appendTextToFile(): "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a new file including all subfolders
     */
    public void createFile(File f){
        File parentDir = f.getParentFile();
        try{
            parentDir.mkdirs();
            f.createNewFile();
        }catch(Exception e){
            //appendTextToFile(errorFilename, "createFile(): "+e.getMessage());
            e.printStackTrace();
        }
    }

    public void deletePoints(){
        if (curPath != -1 && curPoint.length > 0) {
            curPoint= sort(curPoint);
            int[]  filtered=new int[0];
            for (int i = 0; i < curPoint.length; i++) {
                if (!valueIsInArray(curPoint[i], filtered)) {
                    filtered = append(filtered, curPoint[i]);
                }
            }

            WirePoints wps = curWirePoints.get(curPath);

            for (int i=0; i<filtered.length; i++) {
                wps.pts.remove(filtered[i]-i);
            }
            curPoint = new int[0];
            //generateWirePoints();    // should do only for curPath - NO! don't generate points - that will just put back deleted point
            // do want to generate feed/bend data again for this path
            calculateFeedsAndBendAngles();    // should do only for curPath, maybe only do on demand?
        }
    }

    public void addPause(){
        if (curPath != -1 && curPoint.length > 0) {
            curPoint= sort(curPoint);

            WirePoints wps = curWirePoints.get(curPath);

            for (int i=0; i<curPoint.length; i++) {
                wps.pts.get(curPoint[i]).pointPause=true;
            }
            calculateFeedsAndBendAngles();    // should do only for curPath, maybe only do on demand?
        }
    }

    public void removePause(){
        if (curPath != -1 && curPoint.length > 0) {
            curPoint= sort(curPoint);

            WirePoints wps = curWirePoints.get(curPath);
            for (int i=0; i<curPoint.length; i++) {
                wps.pts.get(curPoint[i]).pointPause=false;
            }
            calculateFeedsAndBendAngles();    // should do only for curPath, maybe only do on demand?
        }
    }

    public void printBendData ()  {
        int i;

        println("Current calibration: " + curInstalledCalibFileName);

        println("Pos coeffs: " + polyFormulaPos[0] + " " + polyFormulaPos[1] + " " + polyFormulaPos[2] + " " + polyFormulaPos[3]);
        println("Neg coeffs: " + polyFormulaNeg[0] + " " + polyFormulaNeg[1] + " " + polyFormulaNeg[2] + " " + polyFormulaNeg[3]);

//    for (i = 1; i <= 21; ++i)
//        println("pinMM2WireAng(" + i + ") = " + pinMM2WireAng(i));
//        println("wireAng2PinMM(pinMM2WireAng(" + i + ")) = " + wireAng2PinMM(pinMM2WireAng(i)));

        for (i = -135; i <= 135; i += 15)  {
            println("wireAng2PinMM(" + i + ") = " + wireAng2PinMM(i));
            println("wireAng2SpringBackMM(" + i + ") = " + wireAng2SpringBackMM(i));
        }
    }

    public void saveDefaults(){
        JSONObject json;
        PrintWriter output;
        json = new JSONObject();
        json.setInt("bh", dlBendHead.getSelectedIndex());
        json.setInt("fw", dlFeedWheel.getSelectedIndex());
        json.setInt("units", curUnits);
        json.setInt("material", dlMatProfEdit.getSelectedIndex());
        json.setString("curFile", curFile);

        saveJSONObject(json, defaultsFilename);
    }
    public void loadDefaults(){
        JSONObject json;

        json = loadJSONObject(defaultsFilename);
        //loadWireObjectFile(json.getString("curFile"));
        //dlFeedWheel.setSelected(json.getInt("fw"));
        //dlBendHead.setSelected(json.getInt("bh"));
        dlMatProfEdit.setSelected(json.getInt("material"));
        setCurrentCalibration ();
    }
    public boolean loadTxt(String txtFile){

        try{
            gcodeStrings = loadStrings(txtFile);
            txtFileLoaded=true;
            for(int i=0;i<gcodeStrings.length;i++){
                println(gcodeStrings[i]);
            }

            return true;
        }
        catch(Exception e){
            return false;
        }

    }

    public float circleDiameter(float Ax,float Ay,float Bx,float By,float Cx,float Cy) {

        float yDelta_a = By - Ay;
        float xDelta_a = Bx - Ax;
        float yDelta_b = Cy - By;
        float xDelta_b = Cx - Bx;
        float centerx = 0;
        float centery = 0;

        float aSlope = yDelta_a/xDelta_a;
        float bSlope = yDelta_b/xDelta_b;
        centerx = (aSlope*bSlope*(Ay - Cy) + bSlope*(Ax + Bx) - aSlope*(Bx+Cx) )/(2* (bSlope-aSlope) );
        centery = -1*(centerx - (Ax+Bx)/2)/aSlope +  (Ay+By)/2;
        float diameter=round(2.0f*dist(centerx,centery,Bx,By));
        if (Float.isNaN(diameter)||diameter>1000 || diameter==0){
            diameter=1000.0f;
        }
        //println("diameter = " + diameter);

        return diameter;
    }

    public void zeroLeft(GImageButton imagebutton, GEvent event){
        if (!serialConnected)
            testSerial();
        if (serialConnected)
            tinyGCode("g28.3x0");

    }
    public void zeroRight(GImageButton imagebutton, GEvent event){
        if (!serialConnected)
            testSerial();
        if (serialConnected) {
            zeroRequest=true;
            tinyGRequest("posx");
        }
    }

    public void debugC(String debugStr){
        println("debugC: "+debugStr);
        delay(10); //<>//
    }
    public void keyPressed () {
        // see if we are in a text edit field and don't respond to key presses if so
        for (GAbstractControl tf : gTextFields)
            if (tf.hasFocus()) return;

        if (key == 27)
            exit();

        // switch modes
        if (key == 'e') {
            curViewMode = VIEW_EDIT;
        }
        if (key == 'c') {
            curViewMode = VIEW_CALIBRATE;
            curCalibMode = CALIB_START;
        }
        if (key == 'm') {
            curViewMode = VIEW_MANUAL;
        }

        // framing
        if (key == 'f') {
            if (curViewMode == VIEW_EDIT && curShape != null)
                frameModel();
        }
        if (key == 'F') {
            //curPath = -1;
            if (curViewMode == VIEW_EDIT && curShape != null)
                frameModel(true);
        }

        // debug camera
        if (key == 'z') println("cam params: " + camX + " " + camY + " " + camScl);


        if (key == 'r') {
            //camX = camY = 0.0; camScl = 1.0;
            reverseArray();
        }
        if (key == 's') {
            changeStartPoint();
        }

        if ((key == DELETE || key==BACKSPACE) && !continuousFlag) {
            deletePoints();
        }

        // connect to DIWire
        if (key == 't') {
            if (!serialConnected) {
                testSerial();
            }
        }
        // print position of DIWire pin/feed axes
        if (key == 'p') {
            if (!serialConnected) testSerial();
            tinyGRequest("posx");
            tinyGRequest("posy");
        }
        if (key == 'd') {
            printBendData();
        }


        // load a default model
        if (key == 'l') {
            if (displayState == DS_NOFILE) {
                loadWireObjectFile("bendMe.svg");
            }
        }

        // H = home, T = cycle test
        if (key == 'H') {
            testSerial();
            if (serialConnected) {
                tinyGHome();
            }
        }

        // cycle test - feed,bend,feed,bend ...
        if (key == 'T') {
            doCycleTest = !doCycleTest;
            println("Cycle Test: " + doCycleTest);
            if (doCycleTest) {
                sendFeed(25);
                cycleDir = false;
                cycleAng = 0;
                cycleCount = 0;
            }
        }
        if (key == ' ') {
            if (displayState == DS_SENDING){
                if (sendPaused){
                    resumeSendKey();
                }
                else{
                    pauseSendKey();
                }

            }
        }
        if (key == 'B') {
            if (displayState == DS_LOADED){
                beginSend (btnBeginSend, GEvent.CLICKED);
            }
        }


        if (key == CODED) {
            if (keyCode == CONTROL) {
                ctrlFlag=true;
            }
            if (keyCode == SHIFT) {
                shiftFlag=true;
            }


            if (keyCode == UP) {
                if (curViewMode == VIEW_EDIT) {
                    if (nPaths > 0) {
                        // put curPath == -1 - no path selected - into selection loop
                        if (++curPath >= nPaths) curPath = -1;
                        displayNeedsUpdate = true;
                    } else
                        curPath = -1;
                    curPoint = new int [0];    // clear point selection
                } else if (curViewMode == VIEW_MANUAL && !manInputs) {
                    startJog(JOGDIR_FORWARD);
                }
            }

            if (keyCode == DOWN) {
                if (curViewMode == VIEW_EDIT) {
                    if (nPaths > 0) {
                        if (--curPath < -1) curPath = nPaths - 1;
                        displayNeedsUpdate = true;
                    } else
                        curPath = -1;
                    curPoint =new int [0];
                } else if (curViewMode == VIEW_MANUAL && !manInputs) {
                    startJog(JOGDIR_BACKWARD);
                }
            }

            if (keyCode == LEFT) {
                if (curViewMode == VIEW_MANUAL && !manInputs)
                    startJog(JOGDIR_CCW);

                if (curCalibMode == CALIB_RUNNING && curCalibState == CALIB_STATE_WAITING)
                    startJog(JOGDIR_CCW, true);
            }

            if (keyCode == RIGHT) {
                if (curViewMode == VIEW_MANUAL && !manInputs)
                    startJog(JOGDIR_CW);

                if (curCalibMode == CALIB_RUNNING && curCalibState == CALIB_STATE_WAITING)
                    startJog(JOGDIR_CW, true);
            }
        }
    }


    public void keyReleased () {
        if (key == CODED) {
            if (keyCode == CONTROL) {
                ctrlFlag=false;
            }
            if (keyCode == SHIFT) {
                shiftFlag=false;
            }
        }

        if (isJogging && key == CODED && (keyCode == LEFT || keyCode == RIGHT || keyCode == UP || keyCode == DOWN))
            stopJog();
    }

/*
    handles mouse and keyboard input

 */

    boolean mouseClickInPreviewArea = false;

    // find closest point to click
// need to see what we've clicked on and what's currently selected
// click on path, click on point, click on BG to pan/zoom
    public void mousePressed () {
        if (curViewMode == VIEW_EDIT &&
                mouseX >= previewPaddingX[0] && mouseX <= previewPaddingX[0]+previewSize[0] &&
                mouseY >= headerSize+previewPaddingY[0] && mouseY <= headerSize+previewPaddingY[0]+previewSize[1])
            mouseClickInPreviewArea = true;
        else
            mouseClickInPreviewArea = false;

        if (!mouseClickInPreviewArea && curViewMode == VIEW_EDIT &&  mouseX >=0 && mouseX <= mainPanelSize[0] && mouseY >= height-footerSize && status==STATUS_NOT_CONNECTED){
            status=STATUS_CONNECTING;
            thread("testSerial");
        }

        if (mouseClickInPreviewArea && curPath >= 0) {
            // mouse pos from screen space to object space
            float mx = (mouseX-previewPaddingX[0] - camX) / camScl;
            float my = (mouseY-(headerSize+previewPaddingY[0]) - camY) / camScl;

            if (!ctrlFlag) curPoint = new int[0];

            if (mouseButton == LEFT && mouseMode != MOUSE_ZIN) {
                // find closest point and select it
                float cdist = 0;
                int closest = -1;
                WirePoints wps = curWirePoints.get(curPath);
                for (int i = 0; i < wps.pts.size (); ++i) {
                    WirePoint wp = wps.pts.get(i);
                    float d = dist(mx, my, wp.pt.x, wp.pt.y);
                    if (closest == -1 || d < cdist) {
                        closest = i;
                        cdist = d;
                    }
                }

                if (cdist < 5) {
                    pointClicked = closest;
                    curPoint = append(curPoint, closest);
                    WirePoint wpo = wps.pts.get(closest);
                    dragOffsets = new float[curPoint.length][2];

                    // if multiple points are selected get offsets for dragging
                    for (int j = 0; j < curPoint.length; j++) {
                        WirePoint wp2 = wps.pts.get(curPoint[j]);
                        dragOffsets[j][0]=wpo.pt.x-wp2.pt.x;
                        dragOffsets[j][1]=wpo.pt.y-wp2.pt.y;
                    }
                } else {
                    curPoint = new int[0];
                }
            } else if (mouseButton == RIGHT) {
                // add point on segment
                WirePoints wps = curWirePoints.get(curPath);
                WirePoint wp0 = wps.pts.get(0);
                float d0 = dist(mx, my, wp0.pt.x, wp0.pt.y);
                for (int i = 1; i < wps.pts.size (); ++i) {
                    WirePoint wp1 = wps.pts.get(i);
                    float d1 = dist(mx, my, wp1.pt.x, wp1.pt.y);
                    float dp = dist(wp0.pt.x, wp0.pt.y, wp1.pt.x, wp1.pt.y);

                    // add point if click was on segment
                    if (d0+d1-dp <= .1f) {
                        wps.pts.add(i, new WirePoint(new RPoint(mx, my)));
                        curPoint = new int[0];
                        calculateFeedsAndBendAngles();
                        break;
                    }

                    wp0 = wp1;
                    d0 = d1;
                }
            }
        }


        mouseClickStart[0] = mouseX;
        mouseClickStart[1] = mouseY;
    }

    public void mouseDragged () {
        if (mouseClickInPreviewArea && (mouseButton == LEFT || mouseButton == CENTER)) {
            if (curPath == -1 || curPoint.length == 0 && (mouseMode==MOUSE_HAND || mouseButton == CENTER)) {
                // pan camera around
                camX += mouseX-mouseClickStart[0];
                camY += mouseY-mouseClickStart[1];

                mouseClickStart[0] = mouseX;
                mouseClickStart[1] = mouseY;
            } else if (curPoint.length > 0 && !continuousFlag) {
                // move closest point around with mouse
                float mx = (mouseX-previewPaddingX[0] - camX) / camScl;
                float my = (mouseY-(headerSize+previewPaddingY[0]) - camY) / camScl;

                WirePoints wps = curWirePoints.get(curPath);
                wps.modified = true;
                WirePoint wp = wps.pts.get(pointClicked);

                //lock horizontal/vertical while dragging
                if (shiftFlag) {
                    if (abs(mouseClickStart[1]-mouseY)<abs(mouseClickStart[0]-mouseX)) {
                        my=(mouseClickStart[1]-(headerSize+previewPaddingY[0]) - camY) / camScl;
                    } else {
                        mx=(mouseClickStart[0]-previewPaddingX[0] - camX) / camScl;
                    }
                }

                //move all selected points
                for (int i=0; i<curPoint.length; i++) {
                    WirePoint wpo = wps.pts.get(curPoint[i]);
                    wpo.pt.y = my-dragOffsets[i][1];
                    wpo.pt.x = mx-dragOffsets[i][0];
                }

                calculateFeedsAndBendAngles();
            } else if (mouseMode==MOUSE_POINTER || mouseMode==MOUSE_ZIN) {
                selectRect[0]=mouseClickStart[0];
                selectRect[1]=mouseClickStart[1];
                selectRect[2]=mouseX-mouseClickStart[0];
                selectRect[3]=mouseY-mouseClickStart[1];

                if (selectRect[0]+selectRect[2]<previewPaddingX[0]) {
                    selectRect[2]=-1*(selectRect[0]-previewPaddingX[0]-1);
                } else if (selectRect[0]+selectRect[2]>previewPaddingX[0]+previewSize[0]) {
                    selectRect[2]=previewPaddingX[0]+previewSize[0]- selectRect[0]-1;
                }
                if (selectRect[1]+selectRect[3]<headerSize+previewPaddingY[0]) {
                    selectRect[3]=-1*(selectRect[1]-(headerSize+previewPaddingY[0])-1);
                } else if (selectRect[1]+selectRect[3]>headerSize+previewPaddingY[0]+previewSize[1]) {
                    selectRect[3]=headerSize+previewPaddingY[0]+previewSize[1]- selectRect[1]-1;
                }
            }
        }
    }

    public void mouseWheel (int delta) {
        // zooms around center of preview area
        if (curViewMode == VIEW_EDIT &&
                mouseX >= previewPaddingX[0] && mouseX <= previewPaddingX[0]+previewSize[0] &&
                mouseY >= headerSize+previewPaddingY[0] && mouseY <= headerSize+previewPaddingY[0]+previewSize[1])
            zoom(0, 0, delta);
//            zoom(mouseX,mouseY,delta);
    }

    // on release, add or delete a point based on which button
    public void mouseReleased () {
        if (mouseClickInPreviewArea) {
            if (mouseMode == MOUSE_ZIN && selectRect[0] < 0 && mouseButton == LEFT) {
                zoom(mouseX, mouseY, 1);
            } else if (mouseMode == MOUSE_ZIN && selectRect[0] > 0 && abs(selectRect[2]) > 10 && curPoint.length == 0 && mouseButton == LEFT) {
                zoom(selectRect[0]+selectRect[2]/2, selectRect[1]+selectRect[3]/2, abs(selectRect[2]));
            } else if (mouseMode == MOUSE_ZOUT && selectRect[0] < 0 && mouseButton == LEFT) {
                zoom(mouseX, mouseY, -1);
            } else if (mouseMode == MOUSE_POINTER && selectRect[0]  >0) {
                float mx = (mouseX-previewPaddingX[0] - camX) / camScl;
                float my = (mouseY-(headerSize+previewPaddingY[0]) - camY) / camScl;

                WirePoints wps = curWirePoints.get(curPath);
                for (int i = 0; i < wps.pts.size (); ++i) {
                    WirePoint wp = wps.pts.get(i);
                    float [] tmp= {
                            (min(selectRect[0], selectRect[0]+selectRect[2])-previewPaddingX[0] - camX) / camScl,
                            (min(selectRect[1], selectRect[1]+selectRect[3])-(headerSize+previewPaddingY[0]) - camY) / camScl,
                            abs(selectRect[2])/ camScl,
                            abs(selectRect[3])/ camScl
                    };

                    if (wp.pt.x>=tmp[0] && dist(wp.pt.x, 0, tmp[0], 0)<=tmp[2]  && wp.pt.y>=tmp[1] && dist(wp.pt.y, 0, tmp[1], 0)<=tmp[3]) {
                        curPoint=append(curPoint, i);
                    }
                }
            }

            // clears the selecting rectangle
            selectRect[0] = -1;
            selectRect[1] = -1;
            selectRect[2] = -1;
            selectRect[3] = -1;

            if (curPoint.length>0){
                mbPoints.setEnabled(true);
            }
            else{
                mbPoints.setEnabled(false);
            }
        }

        // if we're in calibration mode, see if we clicked on a type-in area
        if (curViewMode == VIEW_CALIBRATE)  {
            if (curCalibMode == CALIB_REFINE && curCalibTypeInDir == CALIB_DIR_NONE)  {
                // same as in drawCalibrationGrid()
                float hsCW = mainPanelSize[0]*.25f;
                float hsCCW = mainPanelSize[0]*.75f;
                float tw = 100, th = 25, top = 115;    // text box width, text box height, top of box

                if (mouseX >= hsCW && mouseX <= hsCW+tw)  {
                    int idx = PApplet.parseInt((mouseY-top) / th) - 2;
                    //if (idx >= 0 && idx < posCalRes.length && posCalRes[idx] != NO_CALIBRATION)  {
                    if (idx >= 0 && idx < posCalRes.length)  {
                        //println("Click in CW idx = " + idx);
                        curCalibTypeInDir = CALIB_DIR_CW;
                        curCalibTypeInIdx = idx;

                        txTypeIn.setVisible(true);
                        txTypeIn.moveTo(hsCW+1, top+th*(idx+2)+1);

                        if (posCalRes[idx] != NO_CALIBRATION)
                            txTypeIn.setText(nf(posCalRes[idx],1,3));
                        else
                            txTypeIn.setText("");
                        txTypeIn.setFocus(true);
                    }
                } else if (mouseX >= hsCCW && mouseX <= hsCCW+tw) {
                    int idx = PApplet.parseInt((mouseY-top) / th) - 2;
                    if (idx >= 0 && idx < negCalRes.length)  {
                        //println("Click in CCW idx = " + idx);
                        curCalibTypeInDir = CALIB_DIR_CCW;
                        curCalibTypeInIdx = idx;

                        txTypeIn.setVisible(true);
                        txTypeIn.moveTo(hsCCW+1, top+th*(idx+2)+1);

                        if (negCalRes[idx] != NO_CALIBRATION)
                            txTypeIn.setText(nf(negCalRes[idx],1,3));
                        else
                            txTypeIn.setText("");
                        txTypeIn.setFocus(true);
                    }
                }

            }
        }
    }


/*
    Handles serial communication with DIWire device

 */

    boolean verbose = true;
    boolean debugSerial = true;    // print all stuff sent to and received from tinyg over serial

    // stores the target values for bend and feed commands so we know when we get there
    float feedSent = NO_POSITION;
    float bendSent = NO_POSITION;

    final float POSITION_TOLERANCE = .001f;

    // mm to retract after bend for calibration (not returning to home position)
    final float bendRetract = 2;




    // Called automatically when an incoming serial event happens
    public void serialEvent (Serial p) throws java.io.IOException, java.lang.NullPointerException {
        String inString = null;
        inString = p.readString();

        if (inString != null) {
            if (debugSerial) print("serialEvent(): inString = " + inString);

            try {
                JSONObject jSerial = JSONObject.parse(inString);

                // not sure what r, sr and er are - I think er is error
                JSONObject r = !jSerial.isNull("r") ? jSerial.getJSONObject("r") : new JSONObject();
                JSONObject sr = !jSerial.isNull("sr") ? jSerial.getJSONObject("sr") : new JSONObject();
                JSONObject er = !jSerial.isNull("er") ? jSerial.getJSONObject("er") : new JSONObject();

                // https://github.com/synthetos/TinyG/wiki/TinyG-Status-Reports
                // https://github.com/synthetos/TinyG/wiki/TinyG-Status-Codes
                float    check_posy = NO_POSITION;
                float    check_posx = NO_POSITION;
                int      check_f = -1;        // footer array - status is second element
                String   check_id = null;
                int      check_err = -1;      // error? - looks like limit switch error
                int      check_homx = 0;

                if (sr.size() > 0) {
                    check_posy = !sr.isNull("posy") ? sr.getFloat("posy") : NO_POSITION;
                    check_posx = !sr.isNull("posx") ? sr.getFloat("posx") : NO_POSITION;
                } else if (r.size() > 0) {
                    check_posy = !r.isNull("posy") ? r.getFloat("posy") : NO_POSITION;
                    check_posx = !r.isNull("posx") ? r.getFloat("posx") : NO_POSITION;
                    check_f = !r.isNull("f") ? r.getJSONArray("f").size() : 0;    // above set to -1
                    check_id = !r.isNull("id") ? r.getString("id") : null;
                    check_homx = !r.isNull("homx") ? r.getInt("homx") : 0;
                } else if (er.size() > 0) {
                    check_err = !er.isNull("val") ? er.getInt("val") : -1;
                }

                if (check_f <= 0) {
                    check_f = !jSerial.isNull("f") ? jSerial.getJSONArray("f").size() : -1;
                }

                //if (check_posy > NO_POSITION && !jogFeedFwd)  {
                if (check_posy != NO_POSITION) {
                    println("serialEvent(): posy = " + check_posy);

                    lastPositionY = check_posy;

                    // check if we are feeding and reached the target
                    // we get reports on posy before it reaches feedSent
                    if (feedSent != NO_POSITION && abs(check_posy - feedSent) <= POSITION_TOLERANCE) {
                        println("At feedSent");
                        feedSent = NO_POSITION;
                        TinyGSafeWrite("g28.3 y0" +'\n');     // set y position to 0
                        tinyGCommandReady = true;
                    }
                }

                if (check_posx != NO_POSITION) {
                    println("serialEvent(): posx = " + check_posx);

                    lastPositionX = check_posx;

                    if (zeroRequest){
                        zeroRight=lastPositionX;
                        zeroRequest=false;

                    }

                    // check if we are bending and reached the target
                    // we get reports on posx before it reaches bendSent
                    if (bendSent != NO_POSITION && abs(check_posx - bendSent) <= POSITION_TOLERANCE) {
                        println("At bendSent");
                        bendSent = NO_POSITION;
                        tinyGCommandReady = true;
                    }
                }

                if (check_f > 0) {
                    //println("serialEvent(): f = " + check_f);    // get a lot of these all f = 4
                    // this is "footer record" - 4 elem array, 2nd elem is status
                    tinyGFlashReady = true;
                }

                if (check_id != null) {
                    println("serialEvent(): id = " + check_id);
                    tinyGFlashReady = true;
                    //setSerialNumber(check_id);
                    curSerialNumber = check_id;
                }

                if (check_err >= 0) {
                    println("serialEvent(): val = " + check_err);
                    tinyGFlashReady = false;
                    sendPaused = true;
                    if (check_err == 0)
                        limitTripped = 1;
                    else
                        limitTripped = -1;
                    popUpWarning = WARNING_LIMIT_SWITCH;
                    displayState = DS_WARNING;
                }

                if (check_homx == 1) {
                    println("serialEvent(): homx = " + check_homx);
                    homeResponse = true;
                    tinyGHomed = true;
                    tinyGFlashReady = true;
                    lastPositionX = lastPositionY = 0;
                    if (verbose) println("Homed Sweet Homed");
                }
            }
            catch (Exception e) {
                //TinyG.stop();
                appendTextToFile(errorFilename, "serialEvent(): "+e.getMessage());
                println("serialEvent(): Exception while parsing serial message: " + inString);
            }
        }
    }

    // tests the connection using a simple command
    public boolean testConnection () {
        serialConnected = false;
        tinyGFlashReady = false;
        TinyGSafeWrite("{\"id\":\"\"}"+'\n');
        delay(500);
        println("testConnection() = " + tinyGFlashReady);
        return tinyGFlashReady;
    }

    // initializes the serial connection to the tinyG
    public boolean testSerial () {
        println("testSerial()");
        if (!testConnection()) {
            println("Serial.list().length = " + Serial.list().length);
            appendTextToFile(errorFilename, "Serial.list().length = " + Serial.list().length);

            for (int i = 0; i < Serial.list().length; i++) {
                println("Try serial #" + i + ": " + Serial.list()[i]);
                try {
                    TinyG.stop();
                    serialConnected = false;
                } catch (Exception e) {
                    appendTextToFile(errorFilename, "testSerial() Stop "+i+" : "+e);
                    println("Exception: 1st TinyG.stop()");
                }
                try {
                    TinyG = new Serial(this, Serial.list()[i], 115200); //sets TinyG usb port
                    TinyG.bufferUntil('\n');
                    TinyG.clear();
                } catch (Exception e) {
                    appendTextToFile(errorFilename, "testSerial() Start: "+e);
                    println("Exception: new Serial(); TinyG.bufferUntil(); TinyG.clear()");
                }
                tinyGFlashReady = false;
                tinyGSetting("ej", 1);
                TinyGSafeWrite("{\"id\":\"\"}"+'\n');
                appendTextToFile(errorFilename, "Ping "+i);
                delay(1000);
                if (tinyGFlashReady) {
                    println("Connected!");
                    serialConnected = true;
                    //status=STATUS_CONNECTED;
                    break;
                } else {
                    println("Connection Failed");
                    try {
                        TinyG.stop();
                    } catch (Exception e) {
                        appendTextToFile(errorFilename, "testSerial(): "+e.getMessage());
                        println("Exception: 2nd TinyG.stop()");
                    }
                    serialConnected = false;
                    //status=STATUS_NOT_CONNECTED;
                }
            }

            // do this here?
            if (serialConnected && !serialFlashed)  {
                flashTinyG();
            }
        } else {

            serialConnected = true;
        }
        if(serialConnected)
            status=STATUS_CONNECTED;
        else
            status=STATUS_NOT_CONNECTED;

        return serialConnected;
    }



    public void printArrays () {
        if (!verbose)
            return;

        if (curPath == -1) {
            println("printArrays(): no selected path");
            return;    // shouldn't get here
        }

        WirePoints wps = curWirePoints.get(curPath);

        for (int i = 0; i < wps.feeds.size(); ++i)
            safePrint("F: " + wps.feeds.get(i) + "\tB: " + wps.angles.get(i));
    }

    public boolean flashTinyG () {
        println("Flashing TinyG");
        if (serialFlashed) {
            return true;
        }

        int flashCommand = 0;
        JSONObject XSettings = new JSONObject();
        XSettings.setInt("am", 1);                       //Axis Mode
        XSettings.setFloat("vm", XG0Rate);               //Velocity Maximun
        XSettings.setFloat("fr", XMaxRate);              //Feed Rate Maximum
        XSettings.setFloat("tm", XTravelMax);            //Travel Maximum
        XSettings.setFloat("jm", XAcceleration);         //Jerk Maximum
        XSettings.setFloat("jh", XHomingJerk);           //Jerk Homing
        //XSettings.setFloat("jd", 0.05000);             //Junction Deviation
        //XSettings.setInt("sn", 3);                     //Minimum Switch Mode
        //XSettings.setInt("sx", 2);                     //Maximum Switch Mode
        XSettings.setInt("sn", 3);                       //Minimum Switch Mode
        XSettings.setInt("sx", 2);                       //Maximum Switch Mode
        XSettings.setFloat("sv", XSearchVelocity);       //Search Velocity
        XSettings.setFloat("lv", XLatchOffVelocity);     //Latch Velocity
        XSettings.setFloat("lb", XLatchBackoff);         //Homing Latch Backoff
        XSettings.setFloat("zb", homeOffsetPos);         //zero Backoff
        JSONObject XJson = new JSONObject();
        XJson.setJSONObject("x", XSettings);

        JSONObject YSettings = new JSONObject();
        YSettings.setFloat("vm", (yrev*YG0Rate));
        YSettings.setFloat("fr", (5000));                  //Velocity Maximun
        YSettings.setFloat("jm", (yrev*YAcceleration));    //Jerk Maximum
        JSONObject YJson = new JSONObject();
        YJson.setJSONObject("y", YSettings);

        JSONObject Mot1Settings = new JSONObject();
        Mot1Settings.setInt("ma", 0);
        //Mot1Settings.setFloat("sa", (yrev*YAcceleration));

        Mot1Settings.setFloat("tr", xrev);
        Mot1Settings.setFloat("mi", 1);


        Mot1Settings.setFloat("po", 0); //1);
        Mot1Settings.setFloat("pm", 0);
        JSONObject Mot1Json = new JSONObject();
        Mot1Json.setJSONObject("1", Mot1Settings);

        JSONObject Mot2Settings = new JSONObject();
        Mot2Settings.setInt("ma", 1);
        Mot2Settings.setFloat("tr", yrev);
        Mot2Settings.setFloat("po", 0); //1);
        Mot2Settings.setFloat("pm", 0);
        JSONObject Mot2Json = new JSONObject();
        Mot2Json.setJSONObject("2", Mot2Settings);




        JSONObject SysSettings = new JSONObject();
        SysSettings.setInt("sv", 1);
        SysSettings.setInt("st", 1);
        SysSettings.setInt("gun", 1);
        SysSettings.setInt("si", 20000);
        //SysSettings.setInt("mt", 1);     {"yfr":
        JSONObject SysJson = new JSONObject();
        SysJson.setJSONObject("sys", SysSettings);

        long flashStartMillis = System.currentTimeMillis();
        while (flashCommand <= 12 && (System.currentTimeMillis() - flashStartMillis) < 3000) {
            if (tinyGFlashReady) {
                flashStartMillis = System.currentTimeMillis();

                tinyGFlashReady = false;
                switch (flashCommand) {
                    case 0:
                        TinyGSafeWrite("$defa=1"+'\n'); // factory default
                        break;
                    case 1:
                        String yy = YJson.toString();
                        yy = yy.replaceAll("\\s+", "");
                        TinyGSafeWrite(yy+'\n');
                        break;
                    case 2:
                        String xx = XJson.toString();
                        xx = xx.replaceAll("\\s+", "");
                        TinyGSafeWrite(xx+'\n');
                        break;
                    case 3:
                        String m1 = Mot1Json.toString();
                        m1 = m1.replaceAll("\\s+", "");
                        TinyGSafeWrite(m1+'\n');
                        break;
                    case 4:
                        String m2 = Mot2Json.toString();
                        m2 = m2.replaceAll("\\s+", "");
                        TinyGSafeWrite(m2+'\n');
                        break;
                    case 5:
                        tinyGSetting("sv", 1);
                        break;
                    case 6:
                        tinyGSetting("st", 1);
                        break;
                    case 7:
                        tinyGSetting("gun", 1);
                        break;
                    case 8:
                        tinyGSetting("si", 20000);
                        break;
                    case 9:
                        tinyGSetting("mt", 1);
                        break;
                    case 10:
                        tinyGRequest("md");
                        break;
                    case 11:
                        tinyGRequest("id");
                        break;
                    case 12:
                        println("Board initialized");
                        serialFlashed = true;
                        tinyGRequest("sr");
                        tinyGCommandReady = true;
                        break;
                }
                flashCommand++;
                delay(1000);
            }
        }

        if (!serialFlashed) {
            popUpWarning = WARNING_CONNECTION;
            displayState = DS_WARNING;
            println("Failed to flash TinyG");
            return false;
        } else {
            println("Flash Successful");
            return true;
        }
    }


    // looks like this homes the axes if not already (????)
    public boolean checkHomed() {
        tinyGHomed = false;
        homeResponse = false;
        TinyGSafeWrite("{\"homx\":\"\"}"+'\n');
        long flashStartMillis = System.currentTimeMillis();
        while (!homeResponse && (System.currentTimeMillis() - flashStartMillis) < 1000) {
            delay(10);
        }

        return tinyGHomed;
    }


    public boolean tinyGHome () {
        println("Homing...");

        if (!serialConnected) {
            testSerial();
        }
        if (serialConnected && !continuousFlag) {
            tinyGCommandReady = false;
            tinyGCode("g28.3 x0"); // mark current x as 0
            tinyGCode("g28.2 x0"); // seems to home x - sends to some absolute 0 position, despite location of 0-setting in x
            // all the way to the left (CCW)
            tinyGCode("g28.3 y0"); // mark current y as 0

            tinyGFlashReady = true;

            tinyGRequest("homx");
            homeResponse = false;
            tinyGHomed = false;
            long homeStartMillis = System.currentTimeMillis();
            while (!homeResponse && (System.currentTimeMillis() - homeStartMillis) < 15000) {
                delay(10);
            }
            tinyGHomed = homeResponse; //true;

            tinyGRequest("posy");
            tinyGRequest("posx");
            tinyGCommandReady = true;

            println("Done.");
        }
        return tinyGHomed;
    }


    // sends a string to the tinyG inside a try/catch
    public void TinyGSafeWrite (String st) {
        try {
            //safePrint("write: " + st);
            if (debugSerial) print("Sending to tinyG: " + st);
            TinyG.write(st);
            delay(70);
        }
        catch (Exception nullPointer) {
            //TinyG.stop();
            //appendTextToFile(errorFilename, "TintGSafeWrite() exception: "+e);
            appendTextToFile(errorFilename, "TintGSafeWrite() : "+nullPointer);
            //safePrint("Exception: " + e);
            safePrint("write fail");
            //popUpWarning = WARNING_CONNECTION;
            //displayState = DS_WARNING;
        }
    }

    public void tinyGCode (String IN) {
        TinyGSafeWrite("{\"gc\":\""+IN+"\"}"+'\n');
    }

    public void tinyGSetting (String set, int va) {
        TinyGSafeWrite("{\""+set+"\":"+va+"}"+'\n');
    }

    public void tinyGRequest (String bla) {
        TinyGSafeWrite("{\""+bla+"\":\"\"}"+'\n');
    }



    public void startBending () {
        println("startBending()");

        displayState = DS_CONNECTING;
        btnBeginSend.setVisible(false);
        btnPauseSend.setVisible(true);
        btnStopSend.setVisible(true);

//    status = STATUS_CONNECTING;
//    testSerial();
//    if (!serialConnected) {
//        popUpWarning = WARNING_CONNECTION;
//        displayState = DS_WARNING;
//    }
//
//    if (serialConnected && !serialFlashed) {
//        flashTinyG();
//    }
//
//    if (serialFlashed && serialConnected) {
//        tinyGHome();
//        if (tinyGHomed) {
//            status = STATUS_SENDING;
//            lastFeedSentIdx = 0;
//            lastAngleSentIdx = 0;
//            displayState = DS_SENDING;
//
//            // change buttons
//            btnBeginSend.setVisible(false);
//            btnPauseSend.setVisible(true);
//            btnStopSend.setVisible(true);
//        } else
//            println("startBending(): Failed to home DIWire");
//    } else
//        println("startBending(): Unable to establish serial connection to DIWire");
    }


    public void sendBendCommands () {
        //for continuous Bend
        if (continuousFlag){
            tinyGCode("g0x0");
            tinyGCode("g28.3y0");
            tinyGCode("g0y10");
            tinyGCode("g28.3y0");
            WirePoints wpd = curWirePoints.get(curPath);
            for (int i=0;i<wpd.diameters.size();i++){
                sendArc(wpd.diameters.get(i),wpd.continuousBendDist);
            }
            tinyGCode("g0x-3");
            displayState=DS_LOADED;
            status=STATUS_CONNECTED;

        }
        else{


            debugC("serial/sendBendCommands()");
            if (txtFileLoaded && tinyGCommandReady){
                for (int m=0;m<gcodeStrings.length;m++){
                    tinyGCode(gcodeStrings[m]);
                }

                displayState = DS_LOADED;
            }
            else{

                if (curPath == -1) {
                    println("sendBendCommands(): no current path");
                    return;    // shouldn't get here with this case
                }

                // wait for last command to complete
                if (!tinyGCommandReady)
                    return;

                WirePoints wps = curWirePoints.get(curPath);
                // println("Homed "+tinyGHomed);

                long commandStartMillis = System.currentTimeMillis();

                if (tinyGHomed == true && !sendPaused) {
                    println("into the loop");

                    // alternate sending feed and bend commands
                    if (lastFeedSentIdx == lastAngleSentIdx) {
                        println("feed");
                        float feedAmt = wps.feeds.get(lastFeedSentIdx);

                        // adjust feed at start and end
//            if (lastFeedSentIdx == 0) {
//                feedAmt -= feedAdjustStartEnd;
//                if (feedAmt < 0) feedAmt = 0;
//            } else if (lastFeedSentIdx == wps.feeds.size()-1) {
//                feedAmt += feedAdjustStartEnd;
//            }
// start and end offset to mandrel is now done in calculateFeedsAndBendAngles()

                        // send a feed command

                        if (continuousFlag){
                            //sendFeed(wps.continuousBendDist);
                        }
                        else{
                            sendFeed(feedAmt);
                        }


                        lastFeedSentIdx++;

                        // see if we are done bending
                        if (lastFeedSentIdx == wps.feeds.size()) {

                            if (repeatCurrent>=repeatQty){
                                //tinyGCode ("g0 y5");    // feed a little (?)
                                lastAngleSentIdx = 0;
                                lastFeedSentIdx = 0;
                                repeatCurrent=1;
                                status=STATUS_CONNECTED;
                                displayState = DS_LOADED;    // return to loaded state (from DS_SENDING)
                                doneBendingGUI();
                            }
                            else{
                                repeatCurrent++;
                                lastAngleSentIdx = 0;
                                lastFeedSentIdx = 0;
                            }

                        }
                    } else {

                        if (lastAngleSentIdx>0 && wps.pts.get(lastFeedSentIdx).pointPause){
                            wps.pts.get(lastFeedSentIdx).pointPause=false;
                            sendPaused = true;
                            btnPauseSend.setVisible(false);
                            btnResumeSend.setVisible(true);
                            btnResumeSend.setEnabled(true);
                        }
                        else{
                            println("bend");
                            // send an angle command

                            if (Float.isNaN(wps.angles.get(lastAngleSentIdx)) || (abs(wps.angles.get(lastAngleSentIdx)) < 0.10f)) {
                                //tinyGCommandReady = false;
                                //println("zero Bend");
                                //tinyGCode("g0 x0.01");
                                // tinyGCode("g0 x0");
                            } else if (wps.angles.get(lastAngleSentIdx) != 0.0f) {
                                if (continuousFlag){
                                    sendArc(wps.diameters.get(lastAngleSentIdx),wps.continuousBendDist);
                                }
                                else{
                                    sendAngle(wps.angles.get(lastAngleSentIdx));
                                }
                            }

                            lastAngleSentIdx++;
                        }
                    }
                } else if ((System.currentTimeMillis()-commandStartMillis) > 10000 && !sendPaused) {
                    try {
                        TinyG.stop();
                        serialConnected = false;
                    }
                    catch (Exception e) {
                        appendTextToFile(errorFilename, "append(): "+e.getMessage());
                    }
                    //tinyGHome();
                    sendPaused = true;
                    popUpWarning = WARNING_MID_BEND;
                    displayState = DS_WARNING;
                }

            }
        }

    }


    public void sendArc(float diam,float len){
        if (!serialConnected)
            testSerial();
        // convert the angle to a mm offset
        len=round(len*(360.00f/(3.14159f*fwDiam))*1000.0f)/1000.0f;
        float mm = round(wireArc2MM(diam)*100.0f)/100.0f;
        println("sendArc(" + diam + "mm) -> " + mm + "mm");
        String tmp="g1x"+mm+"y"+len+"f20000";
        feedSent=len;
        tinyGCommandReady=false;
        tinyGCode("g28.3y0");
        tinyGCode(tmp);

    }

    // send bend angle (in degrees?)
// what if ang == 0?
    public void sendAngle (float ang) {
        if (!serialConnected)
            testSerial();

        // are we really homing for every angle sent?
        // tinyGHomed doesn't store whether we are currently homed, but whether we have been recently homed (?)
        if (!tinyGHomed) {
            tinyGHome();
            delay(100);
        }

        // convert the angle to a mm offset
        float mm = round(wireAng2SpringBackMM(ang)*1000.0f)/1000.0f;
        println("sendAngle(" + ang + "deg) -> " + mm + "mm");

        sendBend(mm, true);
    }

    // send bend command (in mm?)
// bend is x-axis, absolute movement
// sendBend(0) often hangs - I think because of the way we send 2 bends - the main and the retract
//  and the fact that xhome is 0.0 and if you send a bend command for an angle it's already at, there
//  is no report back
    public void sendBend (float mm, boolean backToHome) {

        if (!serialConnected)
            testSerial();

        if (serialConnected) {
            println("sendBend(" + mm + ")");

            mm = round(mm*1000.0f)*.001f;

            //soft max and min limits
            mm = (mm > (37*xrev-(homeOffsetPos))) ? (37*xrev-(homeOffsetPos)) : mm;
            mm = (mm < -1*(homeOffsetPos-.2f)) ? -1*(homeOffsetPos-.2f) : mm;



            bendSent = mm;        // allows for completion to be detected via serialEvent()
            if (abs(lastPositionX - bendSent) > POSITION_TOLERANCE)  {
                tinyGCommandReady = false;
                //tinyGCode("g0 x" + mm);
                TinyGSafeWrite("!%"+'\n');     // clear previous cmd
                TinyGSafeWrite("g90"+'\n');    // put in absolute move mode
                tinyGCode("g1 f" + XFeedRate + " x" + bendSent);    // do the bend at slow speed
                delay(25);
            } else  {
                tinyGCommandReady = true;
            }

            // don't wait here for bend completion - instead continue on sending bend pin back to 0
            // but what if we pass through 0 on the way to the first bend?  tinyGCommandReady will be set prematurely
            // TO DO: fix limit switch hang problem
            while (!tinyGCommandReady) delay(10);    // probably bad to hold up whole app waiting, also hangs if we hit limit

            // send back to either home position or a few mm back
            // commands will queue
            if (backToHome) {
                bendSent = (mm >= 0.0f ? xHomePos : xHomeNeg);
            } else {
                if (mm >= 0.0f) {
                    bendSent = (mm - bendRetract);
                    if (bendSent < xHomePos) bendSent = xHomePos;
                } else {
                    bendSent = (mm + bendRetract);
                    if (bendSent > xHomeNeg) bendSent = xHomeNeg;
                }
            }

            if (abs(lastPositionX - bendSent) > POSITION_TOLERANCE)  {
                tinyGCommandReady = false;
                tinyGCode("g0 x" + bendSent);    // do the bend and then back off as quickly as possible
                delay(25);
            } else  {
                tinyGCommandReady = true;
            }

            while (!tinyGCommandReady) delay(10);    // probably bad to hold up whole app waiting, also hangs if we hit limit
        } else
            println("sendBend(): Unable to establish serial connection to DIWire");
    }


    // send feed command (in mm?)
// feed is y-axis, relative movement
    public void sendFeed (float mm) {
        if (!serialConnected)
            testSerial();
        if (!tinyGHomed) {
            tinyGHome();
            delay(100);
        }

        if (serialConnected) {
            println("sendFeed(" + mm + ")");

            mm = round(mm*(360.00f/(3.14159f*fwDiam))*1000.0f)/1000.0f;
            feedSent = mm;
            if (abs(lastPositionY - feedSent) > POSITION_TOLERANCE)  {
                tinyGCommandReady = false;
                TinyGSafeWrite("!%"+'\n');     // clear previous cmd
                TinyGSafeWrite("g91"+'\n');    // put in relative move mode
                tinyGCode("g0 y" + mm);        // move as fast as possible
            } else  {
                tinyGCommandReady = true;
            }

            while (!tinyGCommandReady) delay(10);    // probably bad to hold up whole app waiting, also hangs if we hit limit
        } else
            println("sendFeed():  Unable to establish serial connection to DIWire");
    }




    // manual-controlled jogging
    public void startJog (int jogDir) {
        startJog(jogDir, false);
    }

    public void startJog (int jogDir, boolean calib) {
        // key repeat means that we will get this call alot when holding down key
        if (isJogging && jogDir == curJogDir) return;    // doesn't account for change of speed

        if (!serialConnected)
            testSerial();

        if (serialConnected) {
            //println("startJog(" + jogDir + ", " + curJogSpeed + ")");

            // 5000 seems to be max speed for g1 (?)
            int js = 0;
            //if (curJogSpeed == JOGSPEED_VERY_FAST) js = 5000;
            if (curJogSpeed == JOGSPEED_FAST) js = 5000;
            if (curJogSpeed == JOGSPEED_SLOW) js = 250;
            if (curJogSpeed == JOGSPEED_VERY_SLOW) js = 50;
            if (calib) js = 50;    // extra slow for calibration, could go faster with SHIFT or something

            String jd = ((jogDir == JOGDIR_FORWARD || jogDir == JOGDIR_BACKWARD) ? "y" : "x");
            int jl = ((jogDir == JOGDIR_FORWARD || jogDir == JOGDIR_CW) ? 10000 : -10000);
            TinyGSafeWrite("!%"+'\n');     // clear previous cmd
            TinyGSafeWrite("g91"+'\n');    // put in relative move mode
            if (curJogSpeed == JOGSPEED_VERY_FAST)
                TinyGSafeWrite("g0 " + jd + jl + '\n');
            else
                TinyGSafeWrite("g1 f" + js + " " + jd + jl + '\n');    // send cmd to move relative a long distance

            isJogging = true;
            curJogDir = jogDir;
        } else
            println("startJog(): Unable to establish serial connection to DIWire");
    }

    public void stopJog () {
        if (!isJogging) return;

        if (!serialConnected)
            testSerial();

        if (serialConnected) {
            //println("stopJog()");
            TinyGSafeWrite("g90"+'\n');    // absolute mode
            TinyGSafeWrite("!%"+'\n');     // clear commands
            isJogging = false;
        } else
            println("stopJog(): Unable to establish serial connection to DIWire");
    }

    public void homeJog () {
        if (!serialConnected)
            testSerial();

        if (serialConnected) {
            //println("homeJog()");
            stopJog();
            tinyGHome();
        } else
            println("homeJog(): Unable to establish serial connection to DIWire");
    }


    public void doTest () {
        if (!serialConnected)
            testSerial();

        if (serialConnected) {
            TinyGSafeWrite("!%"+'\n');    // clear previous command(s) - no reply
        } else
            println("doTest(): Unable to establish serial connection to DIWire");
    }
/*
    setup and initialization functions
*/


    // configures that main window
    public void setupMainWindow ()  {
        size(mainPanelSize[0], mainPanelSize[1], JAVA2D);   //creates window

        // frame is a built-in object which controls the main window
        frame.setResizable(true);
        frame.setMinimumSize(new Dimension(minPanelSize[0], minPanelSize[1]));
        icon = loadImage("data/images/icon.png");
        frame.setIconImage(icon.getImage());
        frame.setTitle("DIWire - No File");
        frame.addComponentListener(new ComponentAdapter()  {
            public void componentResized (ComponentEvent e)  {
                // gets called while dragging window size
                // maybe check mouse buttons are up?
                if (e.getSource() == frame)  {
                    delay(10);    // ? - all calls still made, just stack up
                    windowWasResized = true;
                }
            }
        });

        smooth();
        frameRate(30);

        addMouseWheelListener(new MouseWheelListener()  {
            public void mouseWheelMoved(MouseWheelEvent mwe)  {
                mouseWheel(mwe.getWheelRotation());
            }});
    }


    // determines location of data folder where calibration etc files are located
// on Windows seems to default to: C:\Users\<user>\Documents\DIWire
// should be a way for the user to change this
    public void setupDataFolder ()  {
        // rootDataFolder = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory() + File.separator;
        debugC("setup/setupDataFolder()");

        if (System.getProperty("os.name").toLowerCase().indexOf("win") < 0)  {
            // rootDataFolder += "Documents" + File.separator;
        }
        //rootDataFolder += "DIWire";
        rootDataFolder = dataPath("userData");
        println("Data Folder = " + rootDataFolder);
        String fileName = dataPath(errorFilename);
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }

    }


// not using these any more
/*
// not sure what is happening here - shouldn't materials come from calibration (material profile) files?
void setupMaterials ()  {
    materialFileName = rootDataFolder + File.separator + "materials" + File.separator + "Materials.csv";
    if (new File(materialFileName).isFile())  {
        curMaterialTable = loadTable(materialFileName, "header");
    } else  {
        curMaterialTable = new Table();
        curMaterialTable.addColumn("material_id");
        curMaterialTable.addColumn("name");
        TableRow newRow = curMaterialTable.addRow();
        newRow.setInt("material_id", 0);
        newRow.setString("name", "1/8 Galv. Steel");
        TableRow newRow2 = curMaterialTable.addRow();
        newRow2.setInt("material_id", 1);
        newRow2.setString("name", "1/16 Galv. Steel");
        saveTable(curMaterialTable, materialFileName);
    }
    println("Material File = " + materialFileName);
}

void setupSerialNumbers ()  {
    serialNumFileName = rootDataFolder + File.separator + "machines" + File.separator + "SerialNumbers.csv";
    if (new File(serialNumFileName).isFile())  {
        serialNumbersTable = loadTable(serialNumFileName, "header");
    } else  {
        serialNumbersTable = new Table();
        serialNumbersTable.addColumn("serial_number");
        serialNumbersTable.addColumn("tinyg_id");
        saveTable(serialNumbersTable, serialNumFileName);
    }
    println("Serial Number File = " + serialNumFileName);
}
*/

    public void setupLogo ()  {
        logo = loadImage("images/logo.png");
        //logo.resize(0, headerSize-7);
    }
    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "edu.mah.www.DIWire" };
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }
}
