/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.monitoring.reporting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.monitoring.Repository;
import org.apache.commons.monitoring.repositories.ObserverRepository;

/**
 * Base class to periodically log a fixed set of monitored data.
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public abstract class AbstractPeriodicLogger
{
    /** The timer that handles the period */
    private ScheduledExecutorService scheduler;

    /** The observed repository */
    private Repository.Observable repository;

    /** The observed repository */
    private ObserverRepository secondary;

    /** The interval for periodic logging of monitored state */
    private int period;

    /**
     * @param period the period (in ms) to log the monitoring state
     * @param repository the observed repository
     */
    public AbstractPeriodicLogger( int period, Repository.Observable repository )
    {
        this();
        this.repository = repository;
        this.period = period;
    }

    public AbstractPeriodicLogger()
    {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void init()
    {
        if ( period <= 0 )
        {
            throw new IllegalStateException( "A positive period must be set" );
        }
        if ( repository == null )
        {
            throw new IllegalStateException( "A Repository must be set" );
        }
        observeRepositoryForPeriod();
        scheduler.scheduleAtFixedRate( new Runnable()
        {
            public void run()
            {
                Repository observed = observeRepositoryForPeriod();
                log( observed );
            }
        }, period, period, TimeUnit.MILLISECONDS );
    }

    private Repository observeRepositoryForPeriod()
    {
        ObserverRepository previous = this.secondary;
        this.secondary = new ObserverRepository( repository );
        if ( previous != null )
        {
            previous.detach();
        }
        return previous;
    }

    public void stop()
    {
        scheduler.shutdown();
        try
        {
            scheduler.awaitTermination( 100, TimeUnit.MILLISECONDS );
        }
        catch ( InterruptedException e )
        {
            // Can be ignored, we are stopping anyway;
        }
    }

    /**
     * Use the data from the (observer) repository generated during the last period
     *
     * @param observeRepositoryForPeriod
     */
    protected abstract void log( Repository repositoryForPeriod );

    public void setRepository( Repository.Observable repository )
    {
        this.repository = repository;
    }

    public void setPeriod( int period )
    {
        this.period = period;
    }
}
