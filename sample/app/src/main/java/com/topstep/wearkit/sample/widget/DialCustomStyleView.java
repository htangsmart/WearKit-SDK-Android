package com.topstep.wearkit.sample.widget;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.topstep.wearkit.apis.model.config.WKShape;

//At present, only background images can be displayed
public class DialCustomStyleView extends FrameLayout {

    private final RectF destination = new RectF();

    @Nullable
    private MaterialShapeDrawable shadowDrawable;

    @Nullable
    private ShapeAppearanceModel shapeAppearanceModel;

    private WKShape shape = WKShape.Companion.createCircle(466);

    public DialCustomStyleView(Context context) {
        this(context, null, 0);
    }

    public DialCustomStyleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialCustomStyleView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new OutlineProvider());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        float scale = shape.getWidth() / (float) shape.getHeight();
        int height = (int) (width / scale);
        int limitHeight = getDefaultSize(Integer.MAX_VALUE, heightMeasureSpec);
        if (height > limitHeight) {
            height = limitHeight;
            width = (int) (height * scale);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateShapeAppearanceModel();
    }

    private void updateShapeAppearanceModel() {
        final int width = getWidth();
        final int height = getHeight();
        if (width == 0 || height == 0) return;
        if (shape.isShapeRectangle()) {
            float cornersScale = shape.getWidth() / (float) width;
            shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                    .setAllCorners(CornerFamily.ROUNDED, shape.getCorners() / cornersScale)
                    .build();
        } else {
            shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                    .setAllCorners(CornerFamily.ROUNDED, width / 2.0f)
                    .build();
        }
        if (shadowDrawable != null) {
            shadowDrawable.setShapeAppearanceModel(shapeAppearanceModel);
        }
        destination.set(0, 0, width, height);
        invalidate();
        invalidateOutline();
    }

    //public method
    public void setShape(@NonNull WKShape shape) {
        if (this.shape == shape) return;
        this.shape = shape;
        updateShapeAppearanceModel();
    }

    public WKShape getShape() {
        return shape;
    }

    class OutlineProvider extends ViewOutlineProvider {

        private final Rect rect = new Rect();

        @Override
        public void getOutline(View view, Outline outline) {
            if (shapeAppearanceModel == null) {
                return;
            }

            if (shadowDrawable == null) {
                shadowDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
            }

            destination.round(rect);
            shadowDrawable.setBounds(rect);
            shadowDrawable.getOutline(outline);
        }
    }
}
