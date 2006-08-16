package org.apache.jcs.auxiliary.disk.jdbc.mysql.util;

import java.util.Date;

import junit.framework.TestCase;

/**
 * Unit tests for the schedule parser.
 * <p>
 * @author Aaron Smuts
 */
public class ScheduleParserUtilUnitTest
    extends TestCase
{

    /**
     * Verify that we get an exception and not a null pointer for null input.
     */
    public void testGetDatesWithNullInput()
    {
        try
        {
            ScheduleParser.createDatesForSchedule( null );

            fail( "Should have thrown an exception" );
        }
        catch ( ScheduleFormatException e )
        {
            // expected
        }
    }

    /**
     * Verify that we get an exception and not a null pointer for null input.
     */
    public void testGetDateWithNullInput()
    {
        try
        {
            ScheduleParser.getDateForSchedule( null );

            fail( "Should have thrown an exception" );
        }
        catch ( ScheduleFormatException e )
        {
            // expected
        }
    }

    /**
     * Verify that we get one date for one date.
     * @throws ScheduleFormatException
     */
    public void testGetsDatesSingle()
        throws ScheduleFormatException
    {
        String schedule = "12:34:56";
        Date[] dates = ScheduleParser.createDatesForSchedule( schedule );

        assertEquals( "Wrong number of dates returned.", 1, dates.length );
    }
    /**
     * Verify that we get one date for one date.
     * @throws ScheduleFormatException
     */
    public void testGetsDatesMultiple()
        throws ScheduleFormatException
    {
        String schedule = "12:34:56,03:51:00,12:34:12";
        Date[] dates = ScheduleParser.createDatesForSchedule( schedule );
        //System.out.println( dates );
        assertEquals( "Wrong number of dates returned.", 3, dates.length );
    }
    
    /**
     * Verify that we get an exception for a single bad date in a list.
     */
    public void testGetDatesMalformedNoColon()
    {
        try
        {
            String schedule = "12:34:56,03:51:00,123234";
            ScheduleParser.createDatesForSchedule( schedule );

            fail( "Should have thrown an exception for a malformed date" );
        }
        catch ( ScheduleFormatException e )
        {
            // expected
        }
    }
    /**
     * Verify that we get an exception for a schedule that has a non numeric item.
     */
    public void testGetDatesMalformedNan()
    {
        try
        {
            String schedule = "12:34:56,03:51:00,aa:12:12";
            ScheduleParser.createDatesForSchedule( schedule );

            fail( "Should have thrown an exception for a malformed date" );
        }
        catch ( ScheduleFormatException e )
        {
            // expected
        }
    }
}