package com.periut.retroapi.voxelshapes;

import com.periut.retroapi.voxelshapes.impl.BoxToLinesConverter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class VoxelData {
    private final VoxelVec3d center;
    private final VoxelBox[] boxes;
    private Line[] lines = null;

    public VoxelData(Box... boxes) {
        this(VoxelBox.voxelify(boxes));
    }

    public VoxelData(VoxelBox... boxes) {
        this(new VoxelVec3d(0.5, 0.5, 0.5), boxes);
    }

    public VoxelData(Vec3d center, Box[] boxes) {
        this(VoxelVec3d.voxelify(center), VoxelBox.voxelify(boxes));
    }

    public VoxelData(VoxelVec3d center, VoxelBox[] boxes) {
        this.center = center;
        this.boxes = boxes;
    }

    public VoxelData withBox(VoxelBox voxelBox) {
        VoxelBox[] newBoxes = new VoxelBox[boxes.length + 1];
        System.arraycopy(boxes, 0, newBoxes, 0, boxes.length);
        newBoxes[boxes.length] = voxelBox;
        return new VoxelData(center, newBoxes);
    }

    public VoxelData withBox(Box box) {
        return withBox(VoxelBox.voxelify(box));
    }

    public VoxelData preCache() {
        computeLines();
        return this;
    }

    public VoxelShape withOffset(VoxelVec3d offset) {
        return new VoxelShape(this, offset);
    }

    public VoxelShape withOffset(Vec3d offset) {
        return withOffset(VoxelVec3d.voxelify(offset));
    }

    public VoxelShape withOffset(int x, int y, int z) {
        return withOffset(new VoxelVec3d(x, y, z));
    }

    public List<VoxelBox> getVoxelBoxes() {
        return List.of(boxes);
    }

    public List<Box> getBoxes() {
        return List.of(VoxelBox.devoxelify(boxes));
    }

    protected void computeLines() {
        lines = BoxToLinesConverter.convertBoxesToLines(boxes, center).toArray(Line[]::new);
    }

    public List<Line> getLines() {
        if (lines == null) {
            computeLines();
        }
        return List.of(lines);
    }
}
