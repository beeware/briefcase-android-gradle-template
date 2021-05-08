package org.beeware.android;

public class CustomView extends android.view.View {
    private IView view = null;

    public CustomView(android.content.Context context) {
        super(context);
    }

    public void setView(IView view) {
        this.view = view;
    }

    public void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        view.onDraw(canvas);
    }
}
