/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.exec.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Supplement of commons-lang, the stringSubstitution() was in a simpler
 * implementation available in an older commons-lang implementation.
 * This class is not part of the public API and could change without
 * warning.
 *
 * @author <a href="mailto:siegfried.goeschl@it20one.at">Siegfried Goeschl</a>
 */
public class StringUtils
{
  /**
   * Perform a series of substitutions. The substitions
   * are performed by replacing ${variable} in the target
   * string with the value of provided by the key "variable"
   * in the provided hashtable.
   *
   * @param argStr the argument string to be processed
   * @param vars name/value pairs used for substitution
   * @param isLenient ignore a key not found in vars?
   * @return String target string with replacements.
   */
  public static StringBuffer stringSubstitution(String argStr, Map vars, boolean isLenient)
  {
      StringBuffer argBuf = new StringBuffer();

      if(argStr == null || argStr.length() == 0) {
          return argBuf;
      }

      if(vars == null || vars.size() == 0) {
          return argBuf.append(argStr);
      }

      int argStrLength = argStr.length();

      for (int cIdx = 0 ; cIdx < argStrLength;)
      {
          char ch = argStr.charAt(cIdx);
          char del = ' ';

          switch (ch)
          {
              case '$':
                  StringBuffer nameBuf = new StringBuffer();
                  del = argStr.charAt(cIdx+1);
                  if( del == '{')
                  {
                      cIdx++;

                      for (++cIdx ; cIdx < argStr.length(); ++cIdx)
                      {
                          ch = argStr.charAt(cIdx);
                          if (ch == '_' || ch == '.' || ch == '-' || ch == '+' || Character.isLetterOrDigit(ch))
                              nameBuf.append(ch);
                          else
                              break;
                      }

                      if (nameBuf.length() > 0)
                      {
                          Object temp = vars.get(nameBuf.toString());                          
                          String value = ( temp != null ? temp.toString() : null);

                          if (value != null)
                          {
                              argBuf.append(value);
                          }
                          else
                          {
                              if (isLenient)
                              {
                                  // just append the unresolved variable declaration
                                  argBuf.append("${" + nameBuf.toString() + "}");
                              }
                              else
                              {
                                  // complain that no variable was found
                                  throw new RuntimeException("No value found for : " + nameBuf );
                              }
                          }

                          del = argStr.charAt(cIdx);

                          if( del != '}')
                          {
                              throw new RuntimeException("Delimiter not found for : " + nameBuf );
                          }
                      }

                      cIdx++;
                  }
                  else
                  {
                      argBuf.append(ch);
                      ++cIdx;
                  }

                  break;

              default:
                  argBuf.append(ch);
                  ++cIdx;
                  break;
          }
      }

      return argBuf;
  }
  
  /**
   * Split a string into an array of strings
   * @param input what to split
   * @param splitChar what to split on
   * @return the array of strings
   */
  public static String[] split(String input, String splitChar){
      StringTokenizer tokens = new StringTokenizer(input, splitChar);
      List strList=new ArrayList();
      while (tokens.hasMoreTokens()) {
          strList.add(tokens.nextToken());
      }
     return (String[])strList.toArray(new String[0]);
  }
}