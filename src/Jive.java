import java.util.Scanner;
import java.util.ArrayList;
import java.io.*;

public class Jive {

  private static Scanner keys = new Scanner( System.in );
  private static Scanner input;
  private static PrintWriter output;
  private static PrintWriter output2;

  // "global" info --- applies across entire program
  // ------------------------------------------------

  // holds the vpl code as it is produced
  private static ArrayList<Integer> vpl = new ArrayList<Integer>();

  // holds the name and starting index of all functions
  private static ArrayList<StringIntPair> funcStart = new ArrayList<StringIntPair>();

  // string at given index is name of function called at hole -index
  private static ArrayList<String> callHoles = new ArrayList<String>();

  // stores for entire program the start label assigned to each
  // given function name
  private static ArrayList<StringIntPair> callInfo = new ArrayList<StringIntPair>();


  // little info for actual globals in VPL:
  // ----------------------------------------
  // holds all the global variables for entire program
  private static ArrayList<String> globsList;


  // "local" info --- used separately for each function
  // --------------------------------------------------

  // holds the local variables and literals temporarily for each function
  private static ArrayList<String> locsList = new ArrayList<String>();

  // once have a locsList for a function, store it in allLocs for output2
  private static ArrayList<String> allLocs = new ArrayList<String>();

  // holds the locations of the holes and the corresponding labels in current function
  private static ArrayList<StringIntPair> labelHoles = new ArrayList<StringIntPair>();

  // holds the label and corresponding index for all labels in current function
  private static ArrayList<StringIntPair> labelInfo = new ArrayList<StringIntPair>();



  public static void main(String[] args) throws Exception {
//     if (args.length != 1) {
//        System.out.println("Usage:  java Jive <fileName>");
//        System.exit(1);
//     }

     String fileName = "sept17";

     input = new Scanner( new File( fileName ) );
     output = new PrintWriter( new File( fileName + ".vpl" ) );
     output2 = new PrintWriter( new File( fileName + ".txt" ) );

     // scan Jive file one word at a time 
     // (line breaks mean nothing more than a space)

     // start anonymous "main" function
     //  since human readability is not a big issue, go ahead
     //  and treat the "main" function like all others, even
     //  though the label at the top is meaningless, since it
     //  can't be called
     // (all other functions do these things when "Def" is encountered)

        vpl.add( 1 );
        vpl.add( nextLabel(true) );  // all functions start with a  1 command

        // note index where command 4 will be inserted
        // (can change if there's a Globs command)
        funcStart.add( new StringIntPair( "?", 2 ) );

        // note label for start of the function
        callInfo.add( new StringIntPair( "?", currentLabel ) );

        // make scratch cell (local cell 0) for scratch space for main
        locsList.add( "-" );
        int scratch = 0;

     String rememberWord = "huh?";  // used a little to pass word to next state

     callHoles.add("---");  // waste spot 0 so call holes -1, -2, -3, ..
                            // match nicely to positions in list

     int state = 1;

     while ( input.hasNext() ) {

        String word = input.next();

        System.out.println("--------------------->>>" + 
            "\nIn state    " + state + "    processing  [" + word + "]" ); 
 
        if ( state == 1 ) {
           if ( word.equals("/*") ) {
              state = 2;
           }
           else if ( word.equals("Halt") ) {
              vpl.add( 26 );
              state = 1;
           }
           else if ( word.equals("NL") ) {
              vpl.add( 29 );
              state = 1;
           }
           else if ( word.equals("Def") ) {// starting a function definition
 
              // stuff to do when see Def, also when see end of input file
              finishFunctionDef();
 
              // initialize things for the upcoming function definition
              locsList = new ArrayList<String>();
              labelHoles = new ArrayList<StringIntPair>();
              labelInfo = new ArrayList<StringIntPair>();

              // move on to see the function name
              state = 3;
           }
           else if ( isLabel( word ) ) {// process a label

              int label = nextLabel(false);

              labelInfo.add( new StringIntPair( word, label ) );

              vpl.add( 1 );
              vpl.add( label );

              state = 1;

           }
           else if ( word.equals("Globs") ) {// global declarations section
              // global declarations must be first command in program, if there at all
              if ( vpl.size() == 2 ) {// have only put in the command 1 for "main"
                                      // so still at start
                 vpl.add( 0, 32 );

                 globsList = new ArrayList<String>();

                 state = 6;
              }
              else {
                 error( "Globals declaration must occur at very beginning of a Jive program");
              }
           }
           else if ( word.equals("Jmp") ) {
              vpl.add( 7 );
              state = 7;
           }

           else if ( findBIF2( word ) >= 0 ) {// is built-in function with 2 args
              // cheaply get opcode corresponding to word

              int op;
              if ( word.equals("Get") ) {
                 op = 24;
              }
              else {
                 op = findBIF2(word) + 9;
              }
                
              vpl.add( op );  // the binary op
              vpl.add( scratch );   // where to put the result
              state = 8;
           }

           else if ( findBIF1( word ) >= 0 ) {// is built-in function with 1 arg
              if ( word.equals( "Not" ) )
                 vpl.add( 20 );
              else if ( word.equals( "Opp" ) )
                 vpl.add( 21 );
              else if ( word.equals( "New" ) )
                 vpl.add( 31 );
              else 
                 error("[" + word + "] is not a valid one argument built-in function");
 
              vpl.add( scratch );

              state = 11;
           }

           else if ( word.equals("Keys") ) {// is built-in function with 0 args
              vpl.add( 27 );
              state = 13;
           }
 
           else if ( word.equals("Fet") ) {// have to look for built-in Fet before user-def
              state = 16;
           }

           else if ( isFuncName( word ) ) {
              rememberWord = word;
              state = 14;
           }

           else if ( isVar( word ) ) {
              // can put in entire part 1 code here
              vpl.add( 23 );
              vpl.add( scratch );
              vpl.add( processVar( word, locsList ) );

              state = 15;
           }

        }// state 1

        else if ( state == 2 ) {
           if ( word.equals("*/") ) {// reached end of comment
              state = 1;  // start a new command
           }
           else {// part of comment
              // consume the word and stay in state 2
           }
        }

        else if ( state == 3 ) {
           if ( ! isFuncName( word ) ) {
              error( "[" + word + "] is not a valid function name");
           }

           vpl.add( 1 );
           vpl.add( nextLabel(true) );  // all functions start with a  1 command

           // note index where command 4 will be inserted
           funcStart.add( new StringIntPair( word, vpl.size() ) );

           // note label for start of the function
           callInfo.add( new StringIntPair( word, currentLabel ) );

           state = 5;
        }

        // state 4 lost in change from f ( a b c )  to  f a b c .

        else if ( state == 5 ) {
           if ( word.equals( "." ) ) {
  
              // make scratch cell for this function after params cells
              locsList.add( "-" ); 
              scratch = locsList.size()-1;

              state = 1;  // go on to body of function

           }
           else if ( isParam( word ) ) {
              // note another parameter
              locsList.add( word );
              // loop back to stay in state 5
           }
        }

        else if ( state == 6 ) {
           if ( word.equals( "." ) ) {
              // done creating list of globals, generate VPL code for command 32 
              vpl.add( 1, globsList.size() );

              // adjust start index of "main" to be 2 farther
              funcStart.get( 0 ).x += 2;

              state = 1;
           }
           else if ( isParam( word ) ) {
              // add global
              globsList.add( word );
              // stay in state 6
           }
           else {
              error("[" + word + "] is not a valid global variable name");
           }
        }

        else if ( state == 7 ) {
           if ( isLabel( word ) ) {
              // add the hole
              vpl.add( -1 );
              labelHoles.add( new StringIntPair( word, vpl.size()-1 ) );
      
              state = 1;
           }
           else {
              error( "[" + word + "] is not a valid label");
           }
        }

        else if ( state == 8 ) {
           if ( isVar( word ) ) {
              vpl.add( processVar( word, locsList ) );  // first arg
              state = 9;
           }
           else {
              error( "[" + word + "] is not a valid variable or literal");
           }
        }

        else if ( state == 9 ) {
           if ( isVar( word ) ) {
              vpl.add( processVar( word, locsList ) );  // second arg
              state = 10;
           }
           else {
              error( "[" + word + "] is not a valid variable or literal");
           }
        }

        else if ( state == 10 ) {
           if ( word.equals("->") ) {
              state = 100;
           }
           else {
              error("a part 1 must be followed by ->");
           }
        }

        else if ( state == 11 ) {
           if ( isVar( word ) ) {
              vpl.add( processVar( word, locsList ) );
              state = 12;
           }
           else {
              error( "[" + word + "] is not a valid variable or literal");
           }
        }

        else if ( state == 12 ) {
           if ( word.equals("->") ) {
              state = 100;
           }
           else {
              error("a part 1 must be followed by ->");
           }
        }

        else if ( state == 13 ) {
           if ( word.equals("->") ) {
              vpl.add( scratch );

              state = 100;
           }
           else {
              error("a part 1 must be followed by ->");
           }
        }

        else if ( state == 14 ) {
           if ( isVar( word ) ) {
              vpl.add( 3 );
              vpl.add( processVar( word, locsList ) );
              // state loops to 14
           }
           else if ( word.equals("->") ) {
              // done with function call, send out the vpl code

              vpl.add( 2 );
              vpl.add( - nextCallHole() );  // leave a hole
              callHoles.add( rememberWord );

              // followed by command 6 for when return from doing called function
              vpl.add( 6 );
              vpl.add( scratch );

              state = 100;
           }
           else {
              error("a part 1 must be followed by ->");
           }

        }

        else if ( state == 15 ) {
           if ( word.equals("->") ) {
              state = 100;
           }
           else {
             error("a part 1 must be followed by ->");
           }
        }

        else if ( state == 16 ) {
           if ( isParam( word ) ) {
              vpl.add( 34 );
              vpl.add( scratch );
              vpl.add( processVar( word, globsList ) );
      
              state = 17;
           }
           else {
              error("[" + word + "] is not a valid argument for Fet");
           }
        }

        else if ( state == 17 ) {
           if ( word.equals("->") ) {
              state = 100;
           }
           else {
             error("a part 1 must be followed by ->");
           }
 
        }

        // end of part 1 states

        // begin part 2 states

        else if ( state == 100 ) {// start state for part 2

           if ( word.equals(".") ) {
              // do nothing with the value in scratch cell from part 1
              state = 1;
           }

           else if ( word.equals("Prt") ) {
              // generate code to print the value in the scratch cell
              vpl.add( 28 );
              vpl.add( scratch );
              state = 1;
           }

           else if ( word.equals("Sym") ) {
              // generate code to print the value in the scratch cell
              vpl.add( 30 );
              vpl.add( scratch );
              state = 1;
           }

           else if ( word.equals("Ret") ) {
              // generate code to return the value in scratch cell
              vpl.add( 5 );
              vpl.add( scratch );
              state = 1;
           }

           else if ( isParam( word ) ) {
              vpl.add( 23 );
              vpl.add( processVar( word, locsList ) );
              vpl.add( scratch );
              state = 1;
           }

           else if ( word.equals("Jmp") ) {
              state = 101;
           }
 
           else if ( word.equals("Put") ) {
              state = 102;
           }

           else if ( word.equals("Sto") ) {
              state = 104;
           }

           else {// unknown first word in a part 2
              error("[" + word + "] is invalid as start of a part 2");
           }

        }// start state for part 2 (100)

        else if ( state == 101 ) {
           if ( isLabel( word ) ) {
              vpl.add( 8 );
              vpl.add( -1 );  // hole for target of jump
              labelHoles.add( new StringIntPair( word, vpl.size()-1 ) );
              vpl.add( scratch );
 
              state = 1;
           }
           else {
              error("[" + word + "] is not a valid label to use after conditional Jmp");
           }
        }

        else if ( state == 102 ) {// processing first <var> for Put
           if ( isVar( word ) ) {// have valid first argument
              vpl.add( 25 );
              vpl.add( processVar( word, locsList ) );

              state = 103;
           }
           else {
              error("[" + word + "] is not a valid first argument for Put");
           }
        }
  
        else if ( state == 103 ) {// processing second <var> for Put
           if ( isVar( word ) ) {// have valid second argument
              vpl.add( processVar( word, locsList ) );

              vpl.add( scratch );

              state = 1;
           }
           else {
              error("[" + word + "] is not a valid first argument for Put");
           }
        }
  
        else if ( state == 104 ) {
           if ( isParam( word ) ) {// valid argument for Sto
              // generate VPL code 33 n a 
              vpl.add( 33 );
              vpl.add( processVar( word, globsList ) );
              vpl.add( scratch );

              state = 1;
           }
           else {
              error("[" + word + "] is not a valid argument for Sto");
           }
        }

     }// loop to scan all words in Jive source file

     // finish off last function definition

     finishFunctionDef();

     System.out.println("vpl before filling call holes:");
     for ( int k=0; k<vpl.size(); k++) {
        System.out.printf("%4d %6d\n", k, vpl.get(k) );
     }
     System.out.println();

     // display the holes and info:
     System.out.println("Call holes:");
     for (int k=0; k<callHoles.size(); k++) {
        System.out.println( callHoles.get(k) );
     }

     // scan vpl to line-oriented output file
     //   and to doc file
     // and fill call holes
     
     int funcNumber = 0;

     int ip = 0;
     while ( ip < vpl.size() ) {

        int op = vpl.get( ip );

        System.out.println("at ip: " + ip + ", sending op: " + op + " to vpl file");

        if ( op==26 || op==29 ) {// operations with 0 arguments
           output2.printf("[%5d]    ", ip );
           out( op + "\n" ); ip++;
        }
        else if ( op==4 ) {
System.out.println("function number is " + funcNumber);
           output2.println("\n------------ " + funcStart.get(funcNumber).s + " -------------");
           output2.print( allLocs.get( funcNumber ) );   funcNumber++;
           output2.println();
           output2.printf("[%5d]    ", ip );
           out( op + " " );  ip++;
           out( vpl.get(ip) + "\n" );  ip++;
        }
        else if ( op== 2 ) {
           // before send to files, replace call hole with actual label
           String fName = callHoles.get( - vpl.get( ip+1 ) );
           // now search callInfo for the label
           int label = -1;
           for (int k=0; k<callInfo.size() && label < 0; k++) {
              if ( callInfo.get(k).s.equals( fName ) ) {
                 label = callInfo.get(k).x;
              }
           }
           if ( label == -1 ) 
              error("Could not find function [" + fName + "] in callInfo");

           output2.printf("[%5d]    ", ip );
           out( op + " " );  ip++;
           out( label + "\n" );  ip++;
        }
        else if ( op==1 || op==3 || op==5 || op==6 || op==7 ||
                  op==27 || op==28 || op==30 || op==32 ) {// ops with 1 arg
           output2.printf("[%5d]    ", ip );
           out( op + " " );  ip++;
           out( vpl.get(ip) + "\n" );  ip++;
        }
        else if ( op==8 || op==20 || op==21 || op==22 || op==23 || 
                  op==31 || op==33 || op==34 ) {// ops with 2 args
           output2.printf("[%5d]    ", ip );
           out( op + " " );  ip++;
           out( vpl.get(ip) + " " );  ip++;
           out( vpl.get(ip) + "\n" );  ip++;
        }
        else if ( (9<=op && op<=19) || op==24 || op==25 ) {
           output2.printf("[%5d]    ", ip );
           out( op + " " );  ip++;
           out( vpl.get(ip) + " " );  ip++;
           out( vpl.get(ip) + " " );  ip++;
           out( vpl.get(ip) + "\n" );  ip++;
        }
        else {
           output2.printf("[%5d]    ", ip );
           System.out.println( op + " is invalid op code, stop translation");
           ip = vpl.size()+1;  // just to stop it
           //error("[" + op + "] is an invalid operation code");
        }

     }

     output.close();
     output2.close();
     input.close();

   }// main

   // return whether w starts with lowercase,
   // followed by 0 or more letters or digits
   private static boolean isParam( String w ) {

      if ( w.length() == 0 ) return false;

      if ( ! ( 'a' <= w.charAt(0) && w.charAt(0) <= 'z' ) ) return false;

      if ( w.length() ==1 ) return true;

      for (int k=1; k<w.length(); k++) {
         char x = w.charAt(k);
         if ( !letter(x) && !digit(x) )  return false;
      }

      return true;

   }

   // return whether w starts with uppercase,
   // followed by 0 or more letters or digits
   private static boolean isFuncName( String w ) {
 
      if ( w.length() == 0 ) return false;

      if ( ! ( 'A' <= w.charAt(0) && w.charAt(0) <= 'Z' ) ) return false;

      if ( w.length() == 1 ) return true;

      for (int k=1; k<w.length(); k++) {
         char x = w.charAt(k);
         if ( !letter(x) && !digit(x) ) return false;
      }

      return true;

   }

   private static boolean isLabel( String w ) {
      if ( ! w.endsWith( ":" ) ) {
         return false;
      }
      else if ( w.length() < 2 || ! lowercase( w.charAt(0) ) ) {
         return false;
      }
      else {
         for (int k=1; k<w.length()-1; k++) {
            char x = w.charAt(k);
            if ( !letter(x) && !digit(x) ) {
               return false;
            }
         }
      }
      return true;
   }

   private static boolean lowercase( char x ) {
      return 'a'<=x && x<='z';
   }

   private static boolean uppercase( char x ) {
      return 'A'<=x && x<='Z';
   }

   private static boolean letter( char x ) {
      return lowercase(x) || uppercase(x);
   }

   private static boolean digit( char x ) {
      return '0'<=x && x<='9';
   }

   private static String[] bifs2 = { "Add", "Sub", "Mult", "Quot", "Rem", 
                                     "Eq", "NotEq", "Less", "LessEq", 
                                     "And", "Or",
                                     "Get" };

   private static String[] bifs1 = { "Not", "Opp", "New" };

   private static int findBIF2( String word ) {
      int loc = -1;
      for (int k=0; k<bifs2.length && loc < 0; k++) {
         if ( word.equals( bifs2[k] ) ) {
            loc = k;
         }
      }
      return loc;
   }

   private static int findBIF1( String word ) {
      int loc = -1;
      for (int k=0; k<bifs1.length && loc < 0; k++) {
         if ( word.equals( bifs1[k] ) ) {
            loc = k;
         }
      }
      return loc;
   }

   // return whether word is an int literal or
   //  a parameter
   private static boolean isVar( String word ) {
      return isParam(word) || isInt(word);
   }

   // given word which is a variable name or an int
   // literal, search for it in list (which will be
   // either locsList or globsList) and if found, just
   // return its location, otherwise append to end,
   // and if is an int literal, add to litsList the 22 command to
   // create the literal, and return its location
   private static int processVar( String word, ArrayList<String> list ) {

      for (int k=0; k<list.size(); k++) {
         if ( word.equals( list.get(k) ) ) {
            // found word in the list
            return k;
         }
      }

      // if still here, word was not found, process it further
      if ( isInt(word) ) {// is an int literal, not in list
         list.add( word );
         return list.size()-1;
      }
      else {// is a newly discovered variable
         list.add( word );
         return list.size()-1;
      }

   }// processVar

   private static boolean isInt( String s ) {
      boolean result;
      try {
         int x = Integer.parseInt( s );
         result = true;
      }
      catch( Exception e ) {
         result = false;
      }
      return result;
   }

   private static void showLocals() {
      for (int k=0; k<locsList.size(); k++) {
         System.out.printf("%4d %s\n", k, locsList.get(k) );
      }
   }

   // find item in list with string matching w, and return
   // its matching integer,
   // or report error
   private static int findString( String w, ArrayList<StringIntPair> list ) {
      for (int k=0; k<list.size(); k++) {
         if (list.get(k).s.equals(w) ) {
            return list.get(k).x;  
         }
      }
      // if still here, didn't find
      error("could not find info with string [" + w + "]");
      return -1;
   }

   private static void out( String s ) {
       output.print( s );
       output2.print( s );
   }

   private static void error( String message ) {
      System.out.println( message );
      System.exit(1);
   }

   public static void main2(String[] args) {
      String w = "A37";
      System.out.println("func?" + isFuncName( w ) );
      System.out.println("var?" + isVar( w ) );
      System.out.println("param?" + isParam( w ) );
      System.out.println("label?" + isLabel( w ) );
   }

   // do everything that needs to be done when realize
   // we've reached the end of a function definition
   private static void finishFunctionDef() {
   
System.out.println("Start finishFunctionDef:");

System.out.println("    vpl at start: " );
showVPL();

showTables();

       // get name of function being finished, just for convenience
       String funcName = funcStart.get( funcStart.size()-1).s;

       // debug output
       System.out.println("Local cells for function just finished, " + funcName + ":" );
       showLocals();

       // for output2, store locsList for this function
       String s = "";
       for (int k=0; k<locsList.size(); k++) {
          s += k + ": " + locsList.get(k) + "\n";
       }
       allLocs.add( s );
              
       // insert command 4 with correct count

            // first find number of params by searching for scratch cell named "-"
            int numParams = -1;
            for (int k=0; k<locsList.size() && numParams < 0; k++) {
               if ( locsList.get(k).equals("-") ) {
                  numParams = k;
               }
            }

            if (numParams == -1) {
               error("Something wrong in finishFunctionDef, no scratch cell found");
            }

            // get index in vpl for insertion of command 4
            // (just 2 more than where the label for the function starts)
            int start = funcStart.get( funcStart.size()-1 ).x;

            // insert the command 4
            vpl.add( start, 4 );
            vpl.add( start+1, locsList.size()-numParams );

       // insert the command 22's for each literal in locsList
            int count = 2;  // have to count the two cells used by command 4
            int index = start + 2;

            for (int k=0; k<locsList.size(); k++) {
               if ( isInt( locsList.get(k) ) ) {// is a literal
                  vpl.add( index, 22 ); index++;
                  vpl.add( index, k ); index++;
                  vpl.add( index, Integer.parseInt( locsList.get(k) ) ); index++;
    
                  count += 3;  // inserting 3 additional values for each int literal
               }
            }

//System.out.println("finishFunctionDef, vpl after insertions: " );
//showVPL();

            // shift hole locations due to insertions of command 4 and command 22's
            for (int k=0; k<labelHoles.size(); k++) {
               StringIntPair pair = labelHoles.get(k);
               pair.x += count;
            }

System.out.println("after shifting locations for insertions, tables are: ");
showTables();

       // fill in all label holes
            for (int k=0; k<labelHoles.size(); k++) {
               StringIntPair pair = labelHoles.get(k);
               // find location of this label from info:
               int loc = -1;
               for (int j=0; j<labelInfo.size() && loc < 0; j++) {
                  if ( labelInfo.get(j).s.equals( pair.s ) ) {// found it
                     loc = labelInfo.get(j).x;
                  }
               }

               // important error check---Jive program might be missing the label
               if ( loc == -1 ) 
                  error("couldn't find label [" + pair.s + "]");

System.out.println("filling label hole [" + pair.s + "," + pair.x + "] with " + loc );
               vpl.set( pair.x, loc );
            }

   }// finishFunctionDef

   private static void showVPL() {
      for (int k=0; k<vpl.size(); k++) {
         System.out.println(k + ": " + vpl.get(k) );
      }
   }

   private static void showTables() {
      System.out.println("Label holes: ");
      for (int k=0; k<labelHoles.size(); k++) {
         System.out.println(labelHoles.get(k));
      }

      System.out.println("Label info: ");
      for (int k=0; k<labelInfo.size(); k++) {
         System.out.println(labelInfo.get(k));
      }

      System.out.println("Call holes: ");
      for (int k=0; k<callHoles.size(); k++) {
         System.out.println(callHoles.get(k));
      }
      System.out.println("Function starts: ");
      for (int k=0; k<funcStart.size(); k++) {
         System.out.println(funcStart.get(k));
      }
 
   }

   // label generating scheme:
   
   // functions are numbered while being scanned
   private static int functionNumber = 0;
   private static int spread = 1000;
   private static int currentLabel = 0;

   // generate next label, incrementing by 1
   // if not starting a function, otherwise
   // starting with multiple of spread
   private static int nextLabel( boolean startingFunction ) {
       if ( startingFunction ) {
          functionNumber++;
          currentLabel = spread*functionNumber;
          return currentLabel;
       }
       else {
          currentLabel++;
          return currentLabel;
       }
   }

   // call hole stuff:
   private static int currentCallHole = 0;
   private static int nextCallHole() {
      currentCallHole++;
      return currentCallHole;
   }

}// Jive
