package siebertbrandon.edu.orchestraandroid

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView



/**
 * Created by Brandon on 2/13/2018.
 */

class VideoWindow(context: Context, attrSet: AttributeSet) : SurfaceView(context, attrSet), SurfaceHolder.Callback
{

    init
    {

        super(ctx, attrSet);

        context = ctx;

//the bitmap we wish to draw

        mbitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.logintab_off);

        SurfaceHolder holder = getHolder();

        holder.addCallback(this);

    }



    @Override

    public void surfaceDestroyed(SurfaceHolder holder)

    {

    }



    @Override

    public void surfaceChanged(SurfaceHolder holder, int format, int width,

            int height)

    {

    }



    @Override

    public void surfaceCreated(SurfaceHolder holder)

    {

    }