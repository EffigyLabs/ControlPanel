/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EffigyControlPanel;

/**
 *
 * @author Code Strategies
 * 
 * 
 */
/*****************************************************************/
/* Copyright 2013 Code Strategies                                */
/* This code may be freely used and distributed in any project.  */
/* However, please do not remove this credit if you publish this */
/* code in paper or electronic form, such as on a web site.      */
/*****************************************************************/

//package test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class DirectoryContents {

	public static void main(String[] args) throws IOException {
                System.out.println("starting...");
		File f = new File("."); // current directory

		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".java")) {
                                        System.out.println("found "+name);
					return true;
				} else {
					return false;
				}
			}
		};

		File[] files = f.listFiles(textFilter);
                if(files.length == 0) {
                    System.out.println("no files found.");
                    System.out.println(f.getAbsolutePath());
                }
		for (File file : files) {
			if (file.isDirectory()) {
				System.out.print("directory:");
			} else {
				System.out.print("     file:");
			}
			System.out.println(file.getCanonicalPath());
		}

	}

}