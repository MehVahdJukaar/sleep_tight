package net.mehvahdjukaar.sleep_tight.common.tiles;

import net.mehvahdjukaar.sleep_tight.core.BedData;
import org.jetbrains.annotations.Nullable;

public interface IExtraBedDataProvider {
    @Nullable
    BedData st_getBedData();

    void st_setBedData(BedData data);
}
