package com.dmp.project.kamatis.version1.utils;

import android.util.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoUtils {
    private static final int MAX_WIDTH = 1280;

    /**
     * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size 1080p,720px
     */
    public static Size chooseVideoSize(Size[] choices) {
//        //use 1080p
//        for (Size size : choices) {
//            if (1920 == size.getWidth() && 1080 == size.getHeight()) {
//                return size;
//            }
//        }

//        //use 720p
//        for (Size size : choices) {
//            if (1280== size.getWidth() && 720== size.getHeight()) {
//                return size;
//            }
//        }
//        for (Size size : choices) {
//            Timber.d("sample size: %s", size);
//        }
//
//        return chooseVideoSizeResolution(1280,720,choices);
        return chooseVideoSizeAspectRatio(16,9,choices,MAX_WIDTH);
    }

    public static Size chooseVideoSize(Size[] choices,int maxWidth) {
//        //use 1080p
//        for (Size size : choices) {
//            if (1920 == size.getWidth() && 1080 == size.getHeight()) {
//                return size;
//            }
//        }

//        //use 720p
//        for (Size size : choices) {
//            if (1280== size.getWidth() && 720== size.getHeight()) {
//                return size;
//            }
//        }
//        for (Size size : choices) {
//            Timber.d("sample size: %s", size);
//        }
//
//        return chooseVideoSizeResolution(1280,720,choices);
        return chooseVideoSizeAspectRatio(16, 9, choices, MAX_WIDTH);

    }

    private static Size chooseVideoSizeAspectRatio(int numerator,int denaminator, Size[] choices, int maxWidth) {
//        for (Size size : choices) {
//                        Timber.d("sample size: %s", size);
//            if (size.getWidth() == size.getHeight() * numerator / denaminator && size.getWidth() <= 1080) {
//                Timber.d("RETURNING THIS: %s",size);
//                return size;
//            }
//        }

//        for(int i =choices.length-1 ; i>=0;i--){
//            Size size = choices[i];
//            Timber.d("sample size: %s", size);
//            if (size.getWidth() == size.getHeight() * numerator / denaminator && size.getWidth() <= 1080) {
//                Timber.d("RETURNING THIS: %s",size);
//                return size;
//            }
//        }

        for(int i =0; i<choices.length-1;i++){
            Size size = choices[i];
//            Timber.d("sample size: %s", size);
            if (size.getWidth() == size.getHeight() * numerator / denaminator && size.getWidth() <= maxWidth) {
//                Timber.d("RETURNING THIS: %s",size);
                return size;
            }
        }
//        Timber.e( "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
//            Timber.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {

            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}
