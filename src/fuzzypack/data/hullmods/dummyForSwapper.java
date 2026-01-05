package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;

public class dummyForSwapper extends BaseHullMod {


    // so Hmod always on top
    @Override
    public int getDisplaySortOrder() {
        return 2000;
    }

    @Override
    public int getDisplayCategoryIndex() {
        return 3;
    }

}
