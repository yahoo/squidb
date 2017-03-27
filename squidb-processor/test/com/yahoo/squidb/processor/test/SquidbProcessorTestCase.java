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

import javax.annotation.processing.Messager;

import static org.mockito.Mockito.when;

public class SquidbProcessorTestCase {

    @Mock protected PluginEnvironment pluginEnv;
    @Mock private Messager mockMessager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(pluginEnv.getMessager()).thenReturn(mockMessager);
    }

}
