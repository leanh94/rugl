
package com.ryanm.droid.rugl.input;

import android.util.FloatMath;

import com.ryanm.droid.config.annote.Summary;
import com.ryanm.droid.config.annote.Variable;
import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.ShapeUtil;
import com.ryanm.droid.rugl.gl.StackedRenderer;
import com.ryanm.droid.rugl.input.Touch.Pointer;
import com.ryanm.droid.rugl.util.Colour;
import com.ryanm.droid.rugl.util.Trig;
import com.ryanm.droid.rugl.util.math.Range;

/**
 * Simulates a thumbstick
 * 
 * @author ryanm
 */
@Variable( "TouchStick" )
@Summary( "Control sensitivity and ramp" )
public class TouchStick extends AbstractTouchStick
{
	private ColouredShape limit;

	private ColouredShape stick;

	/***/
	@Variable( "Radius" )
	@Summary( "Limit of touchstick range, smaller values = more sensitive" )
	public float radius;

	/***/
	@Variable( "Ramp" )
	@Summary( "Input exponent. 1 = linear, >1 = deadzone, <1 = antideadzone, <0 = stupid" )
	public float ramp = 1;

	private float xPos;

	private float yPos;

	/**
	 * Indicates that the touch has been lifted
	 */
	boolean touchLeft = false;

	long touchTime = -1;

	/**
	 * @param x
	 *           position, in screen coordinates
	 * @param y
	 *           position, in screen coordinates
	 * @param limitRadius
	 *           radius, in screen coordinates
	 */
	public TouchStick( float x, float y, float limitRadius )
	{
		setPosition( x, y );
		radius = limitRadius;
	}

	private void buildShape()
	{
		limit =
				new ColouredShape( ShapeUtil.innerCircle( 0, 0, radius, 10, 30, 0 ),
						Colour.white, null );

		stick = new ColouredShape( limit.clone(), Colour.white, null );
		stick.scale( 0.5f, 0.5f, 1 );
		Colour.withAlphai( stick.colours, 128 );
		for( int i = 0; i < limit.colours.length; i += 2 )
		{
			limit.colours[ i ] = Colour.withAlphai( limit.colours[ i ], 0 );
		}

		limit.translate( xPos, yPos, 0 );
		stick.translate( xPos, yPos, 0 );
	}

	/**
	 * @param x
	 * @param y
	 */
	public void setPosition( float x, float y )
	{
		if( limit != null )
		{
			limit.translate( x - xPos, y - yPos, 0 );
			stick.translate( x - xPos, y - yPos, 0 );
		}

		xPos = x;
		yPos = y;
	}

	@Override
	public void advance()
	{
		if( touchLeft )
		{
			touch = null;
			touchLeft = false;

			long tapDuration = System.currentTimeMillis() - touchTime;

			if( tapDuration < tapTime && listener != null )
			{
				listener.onClick();
			}
		}

		if( touch != null )
		{
			float dx = touch.x - xPos;
			float dy = touch.y - yPos;

			float a = Trig.atan2( dy, dx );

			float r = FloatMath.sqrt( dx * dx + dy * dy ) / radius;
			r = Range.limit( r, 0, 1 );

			r = ( float ) Math.pow( r, ramp );

			x = r * Trig.cos( a );
			y = r * Trig.sin( a );
		}
		else
		{
			x = 0;
			y = 0;
		}
	}

	@Override
	public void pointerRemoved( Pointer p )
	{
		if( p == touch )
		{
			touchLeft = true;
		}
	}

	@Override
	public void pointerAdded( Pointer p )
	{
		if( touch == null && Math.hypot( p.x - xPos, p.y - yPos ) < radius )
		{
			touch = p;
			touchTime = System.currentTimeMillis();
		}
	}

	/**
	 * @param r
	 */
	@Override
	public void draw( StackedRenderer r )
	{
		if( limit == null )
		{
			buildShape();
		}

		limit.render( r );

		r.pushMatrix();
		{
			r.translate( x * radius, y * radius, 0 );
			stick.render( r );
		}
		r.popMatrix();
	}
}
