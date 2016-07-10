# RoundedCorners
RoundedCorners for view, now ImageView only.

Features
========
* draw custom or draw by RoundedCornersHelper
* if draw custom, achieved by Canvas.drawRoundRect(), set cornerShaderMode="true" in layout
* By default use RoundedCornersHelper, draw corner with background color

How to use
========
<com.ttwishing.roundedcorners.library.RoundedCornersImageView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_centerInParent="true"
    android:src="@drawable/test_photo"
    app:cornerRadius="3dip" />


