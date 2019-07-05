package com.dmp.project.kamatis.version2;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by cedri on 27.07.2017.
 */

public class CameraSetting {
    List<Camera.Size> frontCameraSizeList;
    List<Camera.Size> backCameraSizeList;

    public List<Camera.Size> getAllCommonSizesList() {
        backCameraSizeList.retainAll(frontCameraSizeList);
        return backCameraSizeList;
    }

}
