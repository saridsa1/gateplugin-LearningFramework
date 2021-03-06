/*
 * Copyright (c) 2015-2016 The University Of Sheffield.
 *
 * This file is part of gateplugin-LearningFramework 
 * (see https://github.com/GateNLP/gateplugin-LearningFramework).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package gate.plugin.learningframework.tests;

import gate.plugin.learningframework.engines.Utils4Engines;
import java.net.MalformedURLException;
import gate.test.GATEPluginTests;

/**
 *
 * @author Johann Petrak
 */
public class TestUtils4Engines extends GATEPluginTests {
  // Cannot use this test any more: with the new Maven-based approach for running the tests,
  // we do not have the JAR/ZIP yet, so we cannot find and copy anything out of it
  // @Test
  public void test1() throws MalformedURLException {
    Utils4Engines.copyWrapper("FileJsonPyTorch", Utils.TESTS_DIR);
  }
}
