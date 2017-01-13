/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.test;

import com.yahoo.squidb.processor.plugins.PluginEnvironment;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SquidbProcessorTestCase {

    @Mock protected PluginEnvironment pluginEnv;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

}
