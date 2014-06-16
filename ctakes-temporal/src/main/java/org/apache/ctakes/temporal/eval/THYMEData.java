/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * A class for splitting the THYME data into appropriate sets for evaluation.
 */
public class THYMEData {
  public static final Set<String> SEGMENTS_TO_SKIP = Sets.newHashSet("20104", "20105", "20116", "20138");

  public static List<Integer> getTrainPatientSets(List<Integer> patientSets) {
    List<Integer> items = new ArrayList<Integer>();
    for (Integer i : patientSets) {
      int remainder = i % 8;
      if (remainder < 4) {
        items.add(i);
      }
    }
    return items;
  }

  public static List<Integer> getDevPatientSets(List<Integer> patientSets) {
    List<Integer> items = new ArrayList<Integer>();
    for (Integer i : patientSets) {
      int remainder = i % 8;
      if (4 <= remainder && remainder < 6) {
        items.add(i);
      }
    }
    return items;
  }

  public static List<Integer> getTestPatientSets(List<Integer> patientSets) {
    List<Integer> items = new ArrayList<Integer>();
    for (Integer i : patientSets) {
      int remainder = i % 8;
      if (6 <= remainder) {
        items.add(i);
      }
    }
    return items;
  }
  
  public static List<File> getFilesFor(List<Integer> patientSets, File rawTextDirectory) {
	  if ( !rawTextDirectory.exists() ) {
		  return Collections.emptyList();
	  }
	  List<File> files = new ArrayList<File>();
	  for (Integer set : patientSets) {
		  final int setNum = set;
		  for (File file : rawTextDirectory.listFiles(new FilenameFilter(){
			  @Override
			  public boolean accept(File dir, String name) {
				  return name.contains(String.format("ID%03d", setNum));
			  }})) {
			  // skip hidden files like .svn
			  if (!file.isHidden()) {
				  files.add(file);
			  } 
		  }
	  }
	  return files;
  }
}
