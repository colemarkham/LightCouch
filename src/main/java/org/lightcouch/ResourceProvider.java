/**
 * Copyright 2014 Cole Markham, all rights reserved.
 */
package org.lightcouch;

import java.util.List;

/**
 * @author Cole Markham
 *
 */
public interface ResourceProvider{
   public List<String> listResources(String path);
   public String readFile(String path);
}
