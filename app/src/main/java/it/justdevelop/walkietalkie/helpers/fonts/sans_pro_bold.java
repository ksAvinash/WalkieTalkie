package it.justdevelop.walkietalkie.helpers.fonts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class sans_pro_bold extends TextView {

    public sans_pro_bold(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public sans_pro_bold(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public sans_pro_bold(Context context) {
        super(context);
        init();
    }

    private void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/sans_pro_bold.ttf" );
        setTypeface(tf);
    }

}