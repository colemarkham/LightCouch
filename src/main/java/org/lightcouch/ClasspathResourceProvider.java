/**
 * Copyright 2014 Cole Markham, all rights reserved.
 */
package org.lightcouch;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Cole Markham
 *
 */
public class ClasspathResourceProvider implements ResourceProvider{

   static final String LINE_SEP = System.getProperty("line.separator");
   
   private Class<?> resourceClass;
   
   public ClasspathResourceProvider(){
      this(ClasspathResourceProvider.class);
   }

   /**
    *    
    * @param resourceClass Any java class that lives in the same place as the resources you want.
    */
   public ClasspathResourceProvider(Class<?> resourceClass){
      this.resourceClass = resourceClass;
   }
   
   /**
    * List directory contents for a resource folder. Not recursive.
    * This is basically a brute-force implementation.
    * Works for regular files and also JARs.
    * 
    * @author Greg Briggs
    * @param path Should end with "/", but not start with one.
    * @return Just the name of each member item, not the full paths.
    */
   public List<String> listResources(String path){
      try {
         //       Class<CouchDbUtil> clazz = CouchDbUtil.class;
         URL dirURL = resourceClass.getClassLoader().getResource(path);
         if (dirURL != null && dirURL.getProtocol().equals("file")) {
            return Arrays.asList(new File(dirURL.toURI()).list());
         }
         if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); 
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); 
            Set<String> result = new HashSet<String>(); 
            while(entries.hasMoreElements()) {
               String name = entries.nextElement().getName();
               if (name.startsWith(path)) { 
                  String entry = name.substring(path.length());
                  int checkSubdir = entry.indexOf("/");
                  if (checkSubdir >= 0) {
                     entry = entry.substring(0, checkSubdir);
                  }
                  if(entry.length() > 0) {
                     result.add(entry);
                  }
               }
            }
            CouchDbUtil.close(jar);
            return new ArrayList<String>(result);
         } 
         return null;
      } catch (Exception e) {
         throw new CouchDbException(e);
      }
   }

   public String readFile(String path){
   	InputStream instream = resourceClass.getResourceAsStream(path);
   	StringBuilder content = new StringBuilder();
   	Scanner scanner = null;
   	try {
   		scanner = new Scanner(instream);
   		while(scanner.hasNextLine()) {        
   			content.append(scanner.nextLine() + ClasspathResourceProvider.LINE_SEP);
   		}
   	} finally {
   		scanner.close();
   	}
   	return content.toString();
   }

}
