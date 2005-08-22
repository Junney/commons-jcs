package org.apache.jcs;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.jcs.engine.control.CompositeCacheManager;

import junit.extensions.ActiveTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Test which exercises the indexed disk cache. Runs three threads against the
 * same region.
 *
 * @version $Id$
 */
public class TestBDBJEDiskCacheConcurrent
    extends TestCase {
  /**
   * Constructor for the TestDiskCache object.
   */
  public TestBDBJEDiskCacheConcurrent(String testName) {
    super(testName);
  }

  /**
   * Main method passes this test to the text test runner.
   */
  public static void main(String args[]) {
    String[] testCaseName = {
        TestBDBJEDiskCacheConcurrent.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  /**
   * A unit test suite for JUnit
   *
   * @return    The test suite
   */
  public static Test suite() {
    ActiveTestSuite suite = new ActiveTestSuite();

    suite.addTest(new TestBDBJE("testBDBJECache1") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion4", 0, 200);
      }
    });

    suite.addTest(new TestBDBJE("testBDBJECache2") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion4", 1000, 1200);
      }
    });

    suite.addTest(new TestBDBJE("testBDBJECache3") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion4", 2000, 3200);
      }
    });

    suite.addTest(new TestBDBJE("testBDBJECache4") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion4", 2200, 5200);
      }
    });

    /*
    suite.addTest(new TestBDBJE("testBDBJECache5") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion4", 0, 10000);
      }
    });

    suite.addTest(new TestBDBJE("testBDBJECache6") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion3", 0, 5000);
      }
    });

    suite.addTest(new TestBDBJE("testIndexedDiskCache5") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion2", 0, 5000);
      }
    });

    suite.addTest(new TestBDBJE("testIndexedDiskCache5") {
      public void runTest() throws Exception {
        runTestForRegion("indexedRegion1", 0, 5000);
      }
    });
*/
    return suite;
  }

  /**
   * Test setup
   */
  public void setUp() {
    JCS.setConfigFilename("/TestBDBJEDiskCacheCon.ccf");
  }

  /**
   * Test tearDown.  Dispose of the cache.
   */
  public void tearDown() {
    try {
      CompositeCacheManager cacheMgr = CompositeCacheManager.getInstance();
      //cacheMgr.shutDown();
      /*
      String[] names = cacheMgr.getCacheNames();
      StringBuffer buf = new StringBuffer();
      int len = names.length;
      for (int i = 0; i < len; i++) {
        cacheMgr.freeCache(names[i]);
        buf.append("\n Freed cache region '" + names[i] + "'");
      }
      System.out.println( buf.toString() );
      */
    }
    catch (Exception e) {
      //log.error(e);
    }
  }

}
