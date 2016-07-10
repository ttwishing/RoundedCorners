package com.ttwishing.roundedcorners.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kurt on 10/26/15.
 */
public class RoundCornersHelper implements View.OnAttachStateChangeListener {

    private int radius;
    private int diameter;//diameter = 2*radius

    private int color = 0;
    private boolean newColor = false;

    //绘制边色
    private Path path;
    private final Paint paint = new Paint();
    private final RectF rectF = new RectF();

    private View view;

    private RoundCornersHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        this.paint.setAntiAlias(true);
        if (attrs != null) {//cond_0
            TypedArray a = null;
            try {
                a = context.obtainStyledAttributes(attrs, R.styleable.RoundCornersImageView, defStyleAttr, 0);
                setCornerRadius(a.getDimensionPixelSize(R.styleable.RoundCornersImageView_cornerRadius, 0));
                setCornerColor(a.getColor(R.styleable.RoundCornersImageView_cornerColor, 0));
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }
    }

    private RoundCornersHelper(View view, AttributeSet attrs, int defStyleAttr) {
        this(view.getContext(), attrs, defStyleAttr);
        this.view = view;
        view.addOnAttachStateChangeListener(this);
    }

    public static RoundCornersHelper newInstance(View view, AttributeSet attrs, int defStyleAttr) {
        return new RoundCornersHelper(view, attrs, defStyleAttr);
    }

    public void dispatchDraw(Canvas canvas) {
        if (radius > 0 && rectF.width() > 0.0F && rectF.height() > 0.0F) {
            canvas.drawPath(path, paint);
        }
    }

    public void setRectF(RectF rectF) {
        this.rectF.set(rectF);
        if (this.radius > 0 && rectF.width() > 0.0F && rectF.height() > 0.0F) {
            configPath();
        }
    }

    private void initCornerPath() {
        if (this.radius > 0 && this.path == null) {
            this.path = new Path();
            if (rectF.width() > 0.0F && rectF.height() > 0.0F) {
                configPath();
            }
        }
    }

    private void configPath() {
        path.rewind();
        //左上
        path.arcTo(new RectF(rectF.left, rectF.top, rectF.left + diameter, rectF.top + diameter), 180.0F, 90.0F, true);
        path.rLineTo(-radius, 0.0F);
        path.rLineTo(0.0F, radius);

        //右上
        path.arcTo(new RectF(rectF.right - diameter, rectF.top, rectF.right, rectF.top + diameter), 270.0F, 90.0F, true);
        path.rLineTo(0.0F, -radius);
        path.rLineTo(-radius, 0.0F);

        //左下
        path.arcTo(new RectF(rectF.left, rectF.bottom - diameter, rectF.left + diameter, rectF.bottom), 90.0F, 90.0F, true);
        path.rLineTo(0.0F, radius);
        path.rLineTo(radius, 0.0F);

        //右下
        path.arcTo(new RectF(rectF.right - diameter, rectF.bottom - diameter, rectF.right, rectF.bottom), 0.0F, 90.0F, true);
        path.rLineTo(radius, 0.0F);
        path.rLineTo(0.0F, -radius);
    }


    /**
     * 使view的coner的颜色和view所处的父view颜色一致
     *
     * @param view
     */
    private void setCornerColor(View view) {//nutmeg
        Drawable drawable = view.getBackground();
        while (drawable == null || (!(drawable instanceof ColorDrawable) && view.getParent() != null && view.getParent() instanceof View)) {
            View parent = (View) view.getParent();
            drawable = parent.getBackground();
            view = parent;
        }

        if (drawable != null && drawable instanceof ColorDrawable) {
            int color = ((ColorDrawable) drawable).getColor();
            setCornerColor(color);
            this.newColor = true;
        }
    }

    public void setCornerRadius(int radius) {
        this.radius = radius;
        this.diameter = radius * 2;
        initCornerPath();
    }

    public void setCornerColor(int color) {
        if (color != 0 && color != this.color) {
            this.color = color;
            this.paint.setColor(color);
            this.newColor = false;
        }
    }

    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (this.view == null) {
            return;
        }
        if (this.radius > 0 && w > 0 && h > 0 && (w != oldw || h != oldh)) {
            rectF.set(view.getPaddingLeft(), view.getPaddingTop(), w - view.getPaddingRight(), h - view.getPaddingBottom());
            if (view instanceof ImageViewWithPhotoPadding) {
                ImageViewWithPhotoPadding imageViewWithPhotoPadding = (ImageViewWithPhotoPadding) this.view;

                rectF.left += imageViewWithPhotoPadding.getPhotoPaddingLeft();
                rectF.right -= imageViewWithPhotoPadding.getPhotoPaddingRight();
                rectF.top += imageViewWithPhotoPadding.getPhotoPaddingTop();
                rectF.bottom -= imageViewWithPhotoPadding.getPhotoPaddingBottom();
            }
            configPath();
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        if (this.color == 0) {
            setCornerColor(v);
        }
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        if (this.newColor) {
            this.color = 0;
            this.newColor = false;
        }
    }

    public RectF getRectF() {
        return this.rectF;
    }

    public int getRadius() {
        return this.radius;
    }


    public interface RoundCornersProvider {
        RoundCornersHelper getRoundCornersHelper();
    }
}
