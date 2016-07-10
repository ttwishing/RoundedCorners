package com.ttwishing.roundedcorners.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.lang.ref.SoftReference;

/**
 * Created by kurt on 10/26/15.
 * <p/>
 * 1.自己绘制: onDrawd中实现rawRoundRect
 * 适用于背景是非单纯色
 * 2.RoundCornersHelper根据所处背影的色值来绘制与背景相同的边角
 * 适用于背影是单纯色
 */
public class RoundedCornersImageView extends ImageView implements ImageViewWithPhotoPadding, RoundCornersHelper.RoundCornersProvider {

    private static Paint sPhotoPaint;

    private RoundCornersHelper roundCornersHelper;

    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();
    private final Rect tempRect = new Rect();
    private final RectF bitmapRectF = new RectF();

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    private SoftReference<DrawingCanvas> drawingCanvasRef;//aRS
    protected boolean shouldReDraw;//是否需要重新绘制

    // true: 当前view负责绘制， false: 父类，即ImageView来实现绘制
    protected boolean isDrawCustom = true;

    // true:当前view负责绘制corner, false:由helper类来实现绘制角标
    private boolean shaderCornersMode = true;

    private final Paint roundRectPaint = new Paint();

    private BitmapShader bitmapShader;
    private final Matrix matrix = new Matrix();
    private final Paint paint = new Paint();

    public RoundedCornersImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoundedCornersImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoundedCornersImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        this.roundCornersHelper = RoundCornersHelper.newInstance(this, attrs, defStyleAttr);
        this.roundRectPaint.setAntiAlias(true);

        if (attrs != null) {
            TypedArray a = null;
            try {
                a = context.obtainStyledAttributes(attrs, R.styleable.RoundCornersImageView, defStyleAttr, 0);
                int defaultPadding = a.getDimensionPixelSize(R.styleable.RoundCornersImageView_photo_padding, 0);
                this.paddingLeft = a.getDimensionPixelSize(R.styleable.RoundCornersImageView_photo_paddingLeft, defaultPadding);
                this.paddingTop = a.getDimensionPixelSize(R.styleable.RoundCornersImageView_photo_paddingTop, defaultPadding);
                this.paddingRight = a.getDimensionPixelSize(R.styleable.RoundCornersImageView_photo_paddingLeft, defaultPadding);
                this.paddingBottom = a.getDimensionPixelSize(R.styleable.RoundCornersImageView_photo_paddingBottom, defaultPadding);
                setShaderCornersMode(a.getBoolean(R.styleable.RoundCornersImageView_cornerShaderMode, false));
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isDrawCustom) {//自定制的绘制
            int saveCount = canvas.getSaveCount();
            try {
                DrawingCanvas drawingCanvas;
                if (drawingCanvasRef != null) {
                    drawingCanvas = drawingCanvasRef.get();
                    if (drawingCanvas == null) {//cond_3
                        //需新建画板
                        drawingCanvas = DrawingCanvas.newInstance(getWidth(), getHeight());
                        this.drawingCanvasRef = new SoftReference(drawingCanvas);
                        this.shouldReDraw = true;
                    } else if (drawingCanvas.setSize(getWidth(), getHeight())) {
                        //画板新旧尺寸不同
                        this.shouldReDraw = true;
                    } else {
                        //新旧画板尺寸相同,不需要重新
                    }
                } else {
                    drawingCanvas = DrawingCanvas.newInstance(getWidth(), getHeight());
                    this.drawingCanvasRef = new SoftReference(drawingCanvas);
                    this.shouldReDraw = true;
                }

                if (shouldReDraw) {
                    //重新绘制
                    drawOnDrawingCanvas(drawingCanvas);
                }

                //在当前canvas上绘制drawingCanvas的绘制结果
                canvas.drawBitmap(drawingCanvas.getBitmap(), 0.0F, 0.0F, this.paint);

            } finally {
                canvas.restoreToCount(saveCount);
            }
        } else {
            //ImageView.class 来实现绘制
            super.onDraw(canvas);
            //因为是ImageView.class来实现绘制,此处必须由RoundCornersHelper来实现绘制corner
            roundCornersHelper.dispatchDraw(canvas);
        }
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        Drawable drawable = getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable && equal(((BitmapDrawable) drawable).getBitmap(), bm)) {
            return;
        }
        this.shouldReDraw = true;
        super.setImageBitmap(bm);
        initBitmapShader();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (getDrawable() != drawable) {
            this.shouldReDraw = true;
        }
        super.setImageDrawable(drawable);
        initBitmapShader();
    }

    @Override
    public void setImageResource(int resId) {
        Drawable drawable = null;
        if (resId > 0) {
            drawable = getDrawableViaId(resId);
        }
        setImageDrawable(drawable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.shouldReDraw = true;
        super.onSizeChanged(w, h, oldw, oldh);
        this.roundCornersHelper.onSizeChanged(w, h, oldw, oldh);
        scaleBitmapShader();
    }

    @Override
    public void refreshDrawableState() {
        this.shouldReDraw = true;
        super.refreshDrawableState();
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.paint.getAlpha() != alpha) {
            this.paint.setAlpha(alpha);
            invalidate();
        }
    }

    /**
     * {@link ImageView.configureBounds()}
     *
     * @param drawingCanvas
     */
    protected void drawOnDrawingCanvas(DrawingCanvas drawingCanvas) {
        drawingCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

        int left = paddingLeft;
        int top = paddingTop;
        int right = getWidth() - paddingRight;
        int bottom = getHeight() - paddingBottom;

        Drawable drawable = getDrawable();
        if (drawable != null) {
            Matrix matrix = getImageMatrix();
            if (getScaleType() == ScaleType.CENTER_CROP && drawable instanceof BitmapDrawable) {
                //bitmap 同时是ScaleType.CENTER_CROP

                this.srcRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                this.dstRect.set(left, top, right, bottom);

                float scale;
                if ((this.srcRect.width() - this.dstRect.width()) / 2 < (this.srcRect.height() - this.dstRect.height()) / 2) {
                    //高缩放或扩大的比例大
                    scale = (float) this.srcRect.width() / this.dstRect.width();
                } else {
                    scale = (float) this.srcRect.height() / this.dstRect.height();
                }
                this.tempRect.set((int) (scale * this.dstRect.left), (int) (scale * this.dstRect.top), (int) (scale * this.dstRect.right), (int) (scale * this.dstRect.bottom));
                int dx = (this.srcRect.width() - this.tempRect.width()) / 2;
                int dy = (this.srcRect.height() - this.tempRect.height()) / 2;
                this.srcRect.inset(Math.abs(dx), Math.abs(dy));
                if (getShaderCornersMode() && this.roundCornersHelper.getRadius() > 0) {
                    drawingCanvas.drawRoundRect(roundCornersHelper.getRectF(), roundCornersHelper.getRadius(), roundCornersHelper.getRadius(), roundRectPaint);
                } else {
                    drawingCanvas.drawBitmap(((BitmapDrawable) drawable).getBitmap(), this.srcRect, this.dstRect, getPhotoPaint());
                }

            } else if (getScaleType() == ScaleType.MATRIX && matrix != null && drawable instanceof BitmapDrawable) {
                //bitmap 同时是ScaleType.MATRIX

                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                this.bitmapRectF.set(0.0F, 0.0F, bitmap.getWidth(), bitmap.getHeight());

                matrix.mapRect(this.bitmapRectF);
                this.bitmapRectF.offset(left, top);
                if (getShaderCornersMode() && roundCornersHelper.getRadius() > 0) {
                    drawingCanvas.drawRoundRect(roundCornersHelper.getRectF(), roundCornersHelper.getRadius(), roundCornersHelper.getRadius(), roundRectPaint);
                } else {
                    drawingCanvas.drawBitmap(bitmap, null, this.bitmapRectF, getPhotoPaint());
                }
            } else if (drawable instanceof BitmapDrawable && getShaderCornersMode() && roundCornersHelper.getRadius() > 0) {
                //bitmap, 有corner,同时自绘制
                drawingCanvas.drawRoundRect(roundCornersHelper.getRectF(), roundCornersHelper.getRadius(), roundCornersHelper.getRadius(), roundRectPaint);
            } else {
                drawable.setBounds(left, top, right, bottom);
                drawable.draw(drawingCanvas);
            }
        }

        if (!getShaderCornersMode()) {
            //RoundCornersHelper来绘制corner
            roundCornersHelper.dispatchDraw(drawingCanvas);
        }
        this.shouldReDraw = false;
    }


    public static Paint getPhotoPaint() {
        if (sPhotoPaint == null) {
            sPhotoPaint = new Paint();
            sPhotoPaint.setFilterBitmap(true);
        }
        return sPhotoPaint;
    }

    protected Drawable getDrawableViaId(int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        return drawable;
    }

    public void setDrawCustom(boolean isDrawCustom) {
        if (isDrawCustom != this.isDrawCustom) {
            this.isDrawCustom = isDrawCustom;
            reDraw();
        }
    }

    public void setShaderCornersMode(boolean mode) {
        this.shaderCornersMode = mode;
        initBitmapShader();
    }

    public void reDraw() {
        this.shouldReDraw = true;
        invalidate();
    }

    public boolean getShaderCornersMode() {
        return this.shaderCornersMode;
    }

    private void initBitmapShader() {
        if (getShaderCornersMode()) {
            Drawable drawable = getDrawable();
            if (drawable != null && drawable instanceof BitmapDrawable) {
                bitmapShader = new BitmapShader(((BitmapDrawable) drawable).getBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                roundRectPaint.setShader(bitmapShader);
                scaleBitmapShader();
            }
        } else {
            this.bitmapShader = null;
        }

    }

    private void scaleBitmapShader() {
        Drawable drawable = getDrawable();
        if (this.bitmapShader != null && drawable != null) {
            RectF rectF = roundCornersHelper.getRectF();
            float width = rectF.width();
            float height = rectF.height();
            if (width > 0.0F && height > 0.0F) {
                float xScale = width / drawable.getIntrinsicWidth();
                float yScale = height / drawable.getIntrinsicHeight();
                this.matrix.reset();
                this.matrix.setScale(xScale, yScale);
                this.bitmapShader.setLocalMatrix(this.matrix);
            }
        }
    }

    public void setCornerRadius(int cornerRadius) {
        this.roundCornersHelper.setCornerRadius(cornerRadius);
    }

    @Override
    public RoundCornersHelper getRoundCornersHelper() {
        return roundCornersHelper;
    }

    public void setPhotoPadding(int padding) {
        setPhotoPaddingLeft(padding);
        setPhotoPaddingTop(padding);
        setPhotoPaddingRight(padding);
        setPhotoPaddingBottom(padding);
    }

    public void setPhotoPaddingBottom(int paddingBottom) {
        if (this.paddingBottom != paddingBottom) {
            this.shouldReDraw = true;
            this.paddingBottom = paddingBottom;
        }
    }

    public void setPhotoPaddingLeft(int paddingLeft) {
        if (this.paddingLeft != paddingLeft) {
            this.shouldReDraw = true;
            this.paddingLeft = paddingLeft;
        }
    }

    public void setPhotoPaddingRight(int paddingRight) {
        if (this.paddingRight != paddingRight) {
            this.shouldReDraw = true;
            this.paddingRight = paddingRight;
        }
    }

    public void setPhotoPaddingTop(int paddingTop) {
        if (this.paddingTop != paddingTop) {
            this.shouldReDraw = true;
            this.paddingTop = paddingTop;
        }
    }

    @Override
    public int getPhotoPaddingBottom() {
        return paddingBottom;
    }

    @Override
    public int getPhotoPaddingLeft() {
        return paddingLeft;
    }

    @Override
    public int getPhotoPaddingRight() {
        return paddingRight;
    }

    @Override
    public int getPhotoPaddingTop() {
        return paddingTop;
    }
}
